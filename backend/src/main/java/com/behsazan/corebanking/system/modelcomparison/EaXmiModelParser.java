package com.behsazan.corebanking.system.modelcomparison;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
class EaXmiModelParser {
    private static final String UML_NAMESPACE = "omg.org/UML1.3";

    EaXmiModel parse(InputStream input) {
        if (input == null) {
            throw new ModelComparisonValidationException("فایل XML/XMI ارسال نشده است.");
        }

        Document document;
        try {
            DocumentBuilderFactory factory = secureFactory();
            document = factory.newDocumentBuilder().parse(input);
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new ModelComparisonValidationException("فایل XML/XMI معتبر نیست یا قابل خواندن نیست.", exception);
        }

        Element root = document.getDocumentElement();
        String modelName = firstAttribute(document, UML_NAMESPACE, "Model", "name");
        String exporter = firstText(document, "XMI.exporter");
        String exporterVersion = firstText(document, "XMI.exporterVersion");
        String exportedAt = trimToNull(root.getAttribute("timestamp"));

        List<EaTableDefinition> rawTables = new ArrayList<>();
        NodeList classes = document.getElementsByTagNameNS(UML_NAMESPACE, "Class");
        for (int index = 0; index < classes.getLength(); index++) {
            Element tableElement = (Element) classes.item(index);
            if (!hasDirectStereotype(tableElement, "table")) {
                continue;
            }
            String tableName = normalizeIdentifier(tableElement.getAttribute("name"));
            if (tableName == null) {
                continue;
            }
            rawTables.add(parseTable(tableElement, tableName));
        }

        if (rawTables.isEmpty()) {
            throw new ModelComparisonValidationException("در فایل انتخاب‌شده هیچ UML Class با stereotype=table پیدا نشد.");
        }

        Map<String, List<EaForeignKeyDefinition>> associationForeignKeys = parseForeignKeyAssociations(document);

        Map<String, List<EaTableDefinition>> grouped = new LinkedHashMap<>();
        for (EaTableDefinition table : rawTables) {
            grouped.computeIfAbsent(table.tableName(), ignored -> new ArrayList<>()).add(table);
        }

        List<String> warnings = new ArrayList<>();
        List<EaTableDefinition> tables = new ArrayList<>();
        grouped.forEach((tableName, definitions) -> {
            EaTableDefinition selected = definitions.stream()
                    .max(Comparator.comparingInt((EaTableDefinition table) -> table.columns().size())
                            .thenComparingInt(table -> table.primaryKeyColumns().size()))
                    .orElseThrow();
            if (definitions.size() > 1) {
                boolean sameStructure = definitions.stream().allMatch(candidate -> sameStructure(selected, candidate));
                warnings.add(sameStructure
                        ? "جدول " + tableName + " در مدل EA " + definitions.size() + " بار تکرار شده بود؛ تعریف‌های یکسان ادغام شدند."
                        : "جدول " + tableName + " در مدل EA چند تعریف متفاوت دارد؛ کامل‌ترین تعریف برای مقایسه انتخاب شد.");
            }
            List<EaColumnDefinition> enrichedColumns = enrichSelectedColumns(selected.columns(), definitions);
            List<EaForeignKeyDefinition> foreignKeys = mergeForeignKeys(
                    definitions.stream().flatMap(definition -> definition.foreignKeys().stream()).toList(),
                    associationForeignKeys.getOrDefault(tableName, List.of())
            );
            List<EaCheckConstraintDefinition> checks = mergeChecks(
                    definitions.stream().flatMap(definition -> definition.checkConstraints().stream()).toList()
            );
            tables.add(new EaTableDefinition(
                    selected.tableName(),
                    definitions.size(),
                    firstPresent(definitions, EaTableDefinition::persianTitle, selected.persianTitle()),
                    firstPresent(definitions, EaTableDefinition::documentation, selected.documentation()),
                    enrichedColumns,
                    selected.primaryKeyColumns(),
                    foreignKeys,
                    checks
            ));
        });
        tables.sort(Comparator.comparing(EaTableDefinition::tableName));

        return new EaXmiModel(
                modelName,
                exporter,
                exporterVersion,
                exportedAt,
                rawTables.size(),
                List.copyOf(tables),
                List.copyOf(warnings)
        );
    }

    private static EaTableDefinition parseTable(Element tableElement, String tableName) {
        Map<String, String> tableTags = directTaggedValues(tableElement);
        String persianTitle = cleanEaText(tableTags.get("alias"));
        String documentation = cleanEaText(tableTags.get("documentation"));
        Element feature = directChild(tableElement, UML_NAMESPACE, "Classifier.feature");
        List<EaColumnDefinition> columns = new ArrayList<>();
        List<String> primaryKey = new ArrayList<>();
        List<EaForeignKeyDefinition> foreignKeys = new ArrayList<>();
        List<EaCheckConstraintDefinition> checks = new ArrayList<>();
        if (feature != null) {
            for (Element child : directElementChildren(feature)) {
                if (matches(child, UML_NAMESPACE, "Attribute")) {
                    String columnName = normalizeIdentifier(child.getAttribute("name"));
                    if (columnName != null) {
                        columns.add(parseColumn(child, columnName));
                    }
                } else if (matches(child, UML_NAMESPACE, "Operation") && hasDirectStereotype(child, "PK")) {
                    List<String> operationColumns = operationParameterNames(child);
                    if (operationColumns.size() > primaryKey.size()) {
                        primaryKey = operationColumns;
                    }
                } else if (matches(child, UML_NAMESPACE, "Operation") && hasDirectStereotype(child, "FK")) {
                    String name = normalizeIdentifier(child.getAttribute("name"));
                    if (name != null) {
                        foreignKeys.add(new EaForeignKeyDefinition(name, operationParameterNames(child), null, List.of()));
                    }
                } else if (matches(child, UML_NAMESPACE, "Operation") && hasDirectStereotype(child, "check")) {
                    String name = normalizeIdentifier(child.getAttribute("name"));
                    Map<String, String> tags = directTaggedValues(child);
                    String condition = checkCondition(tags.get("code"), tags.get("documentation"));
                    if (name != null) {
                        checks.add(new EaCheckConstraintDefinition(name, condition));
                    }
                }
            }
        }

        if (primaryKey.isEmpty()) {
            primaryKey = primaryKeyFromConstraint(tableElement);
        }
        mergeConstraintFallbacks(tableElement, foreignKeys, checks);

        return new EaTableDefinition(
                tableName, 1, persianTitle, documentation, List.copyOf(columns), List.copyOf(primaryKey),
                mergeForeignKeys(foreignKeys, List.of()), mergeChecks(checks)
        );
    }

    private static EaColumnDefinition parseColumn(Element attribute, String columnName) {
        Map<String, String> tags = directTaggedValues(attribute);
        String dataType = normalizeType(tags.get("type"));
        Integer length = positiveOrZero(tags.get("length"));
        Integer precision = positiveOrZero(tags.get("precision"));
        Integer scale = positiveOrZero(tags.get("scale"));
        String lowerBound = trimToNull(tags.get("lowerBound"));
        Boolean nullable = lowerBound == null ? null : "0".equals(lowerBound);
        String lengthSemantics = normalizeLengthSemantics(tags.get("LengthType"));
        String comment = cleanEaText(tags.get("description"));
        String defaultValue = null;
        Element initialValue = directChild(attribute, UML_NAMESPACE, "Attribute.initialValue");
        if (initialValue != null) {
            Element expression = directChild(initialValue, UML_NAMESPACE, "Expression");
            if (expression != null) {
                defaultValue = trimToNull(expression.getAttribute("body"));
            }
        }
        return new EaColumnDefinition(columnName, dataType, length, precision, scale, nullable, lengthSemantics, defaultValue, comment);
    }

    private static List<String> operationParameterNames(Element operation) {
        Element parameters = directChild(operation, UML_NAMESPACE, "BehavioralFeature.parameter");
        if (parameters == null) return List.of();
        List<String> result = new ArrayList<>();
        for (Element child : directElementChildren(parameters)) {
            if (!matches(child, UML_NAMESPACE, "Parameter")) continue;
            if ("return".equalsIgnoreCase(child.getAttribute("kind"))) continue;
            String name = normalizeIdentifier(child.getAttribute("name"));
            if (name != null) result.add(name);
        }
        return result;
    }

    private static List<String> primaryKeyFromConstraint(Element tableElement) {
        Element constraints = directChild(tableElement, UML_NAMESPACE, "ModelElement.constraint");
        if (constraints == null) return List.of();
        for (Element constraint : directElementChildren(constraints)) {
            if (!matches(constraint, UML_NAMESPACE, "Constraint")) continue;
            String name = constraint.getAttribute("name");
            if (name == null) continue;
            String upper = name.toUpperCase(Locale.ROOT);
            int marker = upper.indexOf(" PRIMARY KEY (");
            if (marker < 0) continue;
            int open = name.indexOf('(', marker);
            int close = name.indexOf(')', open + 1);
            if (open < 0 || close < 0) continue;
            List<String> columns = new ArrayList<>();
            for (String value : name.substring(open + 1, close).split(",")) {
                String normalized = normalizeIdentifier(value);
                if (normalized != null) columns.add(normalized);
            }
            return columns;
        }
        return List.of();
    }



    private static Map<String, List<EaForeignKeyDefinition>> parseForeignKeyAssociations(Document document) {
        Map<String, List<EaForeignKeyDefinition>> byChildTable = new LinkedHashMap<>();
        NodeList associations = document.getElementsByTagNameNS(UML_NAMESPACE, "Association");
        for (int index = 0; index < associations.getLength(); index++) {
            Element association = (Element) associations.item(index);
            if (!hasDirectStereotype(association, "FK")) continue;
            Map<String, String> tags = directTaggedValues(association);
            String childTable = normalizeIdentifier(tags.get("ea_sourceName"));
            String parentTable = normalizeIdentifier(tags.get("ea_targetName"));
            String constraintName = stripConstraintPrefix(tags.get("lt"));
            if (constraintName == null) {
                constraintName = associationEndName(association, "source");
            }
            if (childTable == null || constraintName == null) continue;

            String mapping = firstNonBlank(tags.get("mt"), association.getAttribute("name"));
            List<String> childColumns = new ArrayList<>();
            List<String> parentColumns = new ArrayList<>();
            parseForeignKeyMapping(mapping, childColumns, parentColumns);
            EaForeignKeyDefinition definition = new EaForeignKeyDefinition(
                    constraintName,
                    List.copyOf(childColumns),
                    parentTable,
                    List.copyOf(parentColumns)
            );
            byChildTable.computeIfAbsent(childTable, ignored -> new ArrayList<>()).add(definition);
        }
        Map<String, List<EaForeignKeyDefinition>> result = new LinkedHashMap<>();
        byChildTable.forEach((table, values) -> result.put(table, mergeForeignKeys(values, List.of())));
        return result;
    }

    private static String associationEndName(Element association, String expectedEnd) {
        Element connection = directChild(association, UML_NAMESPACE, "Association.connection");
        if (connection == null) return null;
        for (Element end : directElementChildren(connection)) {
            if (!matches(end, UML_NAMESPACE, "AssociationEnd")) continue;
            Map<String, String> tags = directTaggedValues(end);
            if (expectedEnd.equalsIgnoreCase(tags.get("ea_end"))) {
                return normalizeIdentifier(end.getAttribute("name"));
            }
        }
        return null;
    }

    private static void parseForeignKeyMapping(String value, List<String> childColumns, List<String> parentColumns) {
        String text = trimToNull(value);
        if (text == null) return;
        text = stripOuterParentheses(text);
        for (String pair : splitTopLevel(text, ',')) {
            int equals = pair.indexOf('=');
            if (equals < 0) continue;
            String child = normalizeIdentifier(pair.substring(0, equals));
            String parent = normalizeIdentifier(pair.substring(equals + 1));
            if (child != null && parent != null) {
                childColumns.add(child);
                parentColumns.add(parent);
            }
        }
    }

    private static void mergeConstraintFallbacks(
            Element tableElement,
            List<EaForeignKeyDefinition> foreignKeys,
            List<EaCheckConstraintDefinition> checks
    ) {
        Element constraints = directChild(tableElement, UML_NAMESPACE, "ModelElement.constraint");
        if (constraints == null) return;
        for (Element constraint : directElementChildren(constraints)) {
            if (!matches(constraint, UML_NAMESPACE, "Constraint")) continue;
            String ddl = trimToNull(constraint.getAttribute("name"));
            if (ddl == null) continue;
            String upper = ddl.toUpperCase(Locale.ROOT);
            if (upper.contains(" FOREIGN KEY (")) {
                EaForeignKeyDefinition parsed = parseForeignKeyConstraint(ddl);
                if (parsed != null) foreignKeys.add(parsed);
            } else if (upper.contains(" CHECK (")) {
                EaCheckConstraintDefinition parsed = parseCheckConstraint(ddl);
                if (parsed != null) checks.add(parsed);
            }
        }
    }

    private static EaForeignKeyDefinition parseForeignKeyConstraint(String ddl) {
        String upper = ddl.toUpperCase(Locale.ROOT);
        int constraintMarker = upper.indexOf("CONSTRAINT ");
        int fkMarker = upper.indexOf(" FOREIGN KEY (");
        int referencesMarker = upper.indexOf(" REFERENCES ", fkMarker);
        if (fkMarker < 0 || referencesMarker < 0) return null;
        String name = constraintMarker >= 0
                ? normalizeIdentifier(ddl.substring(constraintMarker + "CONSTRAINT ".length(), fkMarker))
                : null;
        if (name == null) return null;
        int childOpen = ddl.indexOf('(', fkMarker);
        int childClose = findClosingParenthesis(ddl, childOpen);
        if (childOpen < 0 || childClose < 0) return null;
        List<String> childColumns = parseIdentifierList(ddl.substring(childOpen + 1, childClose));

        String afterReferences = ddl.substring(referencesMarker + " REFERENCES ".length()).trim();
        int parentOpen = afterReferences.indexOf('(');
        int parentClose = findClosingParenthesis(afterReferences, parentOpen);
        if (parentOpen < 0 || parentClose < 0) return null;
        String qualifiedParent = afterReferences.substring(0, parentOpen).trim();
        String parentTable = normalizeIdentifier(qualifiedParent.contains(".")
                ? qualifiedParent.substring(qualifiedParent.lastIndexOf('.') + 1)
                : qualifiedParent);
        List<String> parentColumns = parseIdentifierList(afterReferences.substring(parentOpen + 1, parentClose));
        return new EaForeignKeyDefinition(name, childColumns, parentTable, parentColumns);
    }

    private static EaCheckConstraintDefinition parseCheckConstraint(String ddl) {
        String upper = ddl.toUpperCase(Locale.ROOT);
        int constraintMarker = upper.indexOf("CONSTRAINT ");
        int checkMarker = upper.indexOf(" CHECK (");
        if (checkMarker < 0) return null;
        String name = constraintMarker >= 0
                ? normalizeIdentifier(ddl.substring(constraintMarker + "CONSTRAINT ".length(), checkMarker))
                : null;
        int open = ddl.indexOf('(', checkMarker);
        int close = findClosingParenthesis(ddl, open);
        String condition = open >= 0 && close > open ? trimToNull(ddl.substring(open + 1, close)) : null;
        return name == null ? null : new EaCheckConstraintDefinition(name, condition);
    }

    private static String checkCondition(String code, String documentation) {
        String condition = trimToNull(code);
        if (condition != null) return stripOuterParentheses(condition);
        String text = trimToNull(documentation);
        if (text == null) return null;
        String upper = text.toUpperCase(Locale.ROOT);
        int marker = upper.indexOf("CHECK (");
        if (marker < 0) return text;
        int open = text.indexOf('(', marker);
        int close = findClosingParenthesis(text, open);
        return open >= 0 && close > open ? trimToNull(text.substring(open + 1, close)) : text;
    }

    private static List<EaForeignKeyDefinition> mergeForeignKeys(
            List<EaForeignKeyDefinition> operationOrConstraintDefinitions,
            List<EaForeignKeyDefinition> associationDefinitions
    ) {
        Map<String, EaForeignKeyDefinition> merged = new LinkedHashMap<>();
        for (EaForeignKeyDefinition definition : operationOrConstraintDefinitions) {
            if (definition == null || definition.constraintName() == null) continue;
            merged.merge(definition.constraintName(), definition, EaXmiModelParser::mergeForeignKey);
        }
        for (EaForeignKeyDefinition definition : associationDefinitions) {
            if (definition == null || definition.constraintName() == null) continue;
            merged.merge(definition.constraintName(), definition, EaXmiModelParser::mergeForeignKey);
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(EaForeignKeyDefinition::constraintName))
                .toList();
    }

    private static EaForeignKeyDefinition mergeForeignKey(EaForeignKeyDefinition left, EaForeignKeyDefinition right) {
        List<String> childColumns = richerList(left.childColumns(), right.childColumns());
        List<String> parentColumns = richerList(left.parentColumns(), right.parentColumns());
        String parentTable = firstNonBlank(right.parentTable(), left.parentTable());
        return new EaForeignKeyDefinition(left.constraintName(), childColumns, parentTable, parentColumns);
    }

    private static List<EaCheckConstraintDefinition> mergeChecks(List<EaCheckConstraintDefinition> checks) {
        Map<String, EaCheckConstraintDefinition> merged = new LinkedHashMap<>();
        for (EaCheckConstraintDefinition check : checks) {
            if (check == null || check.constraintName() == null) continue;
            merged.merge(check.constraintName(), check, (left, right) ->
                    new EaCheckConstraintDefinition(
                            left.constraintName(),
                            firstNonBlank(right.condition(), left.condition())
                    ));
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(EaCheckConstraintDefinition::constraintName))
                .toList();
    }

    private static List<String> richerList(List<String> left, List<String> right) {
        List<String> a = left == null ? List.of() : left;
        List<String> b = right == null ? List.of() : right;
        return b.size() > a.size() ? List.copyOf(b) : List.copyOf(a);
    }

    private static List<String> parseIdentifierList(String value) {
        List<String> result = new ArrayList<>();
        for (String item : splitTopLevel(value, ',')) {
            String normalized = normalizeIdentifier(item);
            if (normalized != null) result.add(normalized);
        }
        return List.copyOf(result);
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> result = new ArrayList<>();
        if (value == null) return result;
        int depth = 0;
        boolean inString = false;
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\'') {
                if (inString && index + 1 < value.length() && value.charAt(index + 1) == '\'') {
                    index++;
                    continue;
                }
                inString = !inString;
            } else if (!inString) {
                if (ch == '(') depth++;
                else if (ch == ')') depth = Math.max(0, depth - 1);
                else if (ch == delimiter && depth == 0) {
                    result.add(value.substring(start, index).trim());
                    start = index + 1;
                }
            }
        }
        result.add(value.substring(start).trim());
        return result;
    }

    private static int findClosingParenthesis(String value, int open) {
        if (value == null || open < 0 || open >= value.length() || value.charAt(open) != '(') return -1;
        int depth = 0;
        boolean inString = false;
        for (int index = open; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\'') {
                if (inString && index + 1 < value.length() && value.charAt(index + 1) == '\'') {
                    index++;
                    continue;
                }
                inString = !inString;
                continue;
            }
            if (inString) continue;
            if (ch == '(') depth++;
            else if (ch == ')' && --depth == 0) return index;
        }
        return -1;
    }

    private static String stripOuterParentheses(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        while (text.startsWith("(") && findClosingParenthesis(text, 0) == text.length() - 1) {
            text = trimToNull(text.substring(1, text.length() - 1));
            if (text == null) return null;
        }
        return text;
    }

    private static String stripConstraintPrefix(String value) {
        String normalized = normalizeIdentifier(value);
        if (normalized == null) return null;
        return normalized.startsWith("+") ? normalizeIdentifier(normalized.substring(1)) : normalized;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String value = trimToNull(preferred);
        return value != null ? value : trimToNull(fallback);
    }

    private static List<EaColumnDefinition> enrichSelectedColumns(
            List<EaColumnDefinition> selectedColumns,
            List<EaTableDefinition> definitions
    ) {
        List<EaColumnDefinition> result = new ArrayList<>();
        for (EaColumnDefinition selected : selectedColumns) {
            EaColumnDefinition enriched = selected;
            for (EaTableDefinition definition : definitions) {
                EaColumnDefinition candidate = definition.columns().stream()
                        .filter(column -> column.columnName().equals(selected.columnName()))
                        .findFirst()
                        .orElse(null);
                if (candidate == null) continue;
                enriched = mergeMissingMetadata(enriched, candidate);
            }
            result.add(enriched);
        }
        return List.copyOf(result);
    }

    private static EaColumnDefinition mergeMissingMetadata(EaColumnDefinition primary, EaColumnDefinition fallback) {
        String dataType = primary.dataType() != null ? primary.dataType() : fallback.dataType();
        Integer length = usefulNumber(primary.length()) ? primary.length() : fallback.length();
        Integer precision = usefulNumber(primary.precision()) ? primary.precision() : fallback.precision();
        Integer scale = primary.scale() != null ? primary.scale() : fallback.scale();
        Boolean nullable = primary.nullable() != null ? primary.nullable() : fallback.nullable();
        String lengthSemantics = primary.lengthSemantics() != null ? primary.lengthSemantics() : fallback.lengthSemantics();
        String defaultValue = primary.defaultValue() != null ? primary.defaultValue() : fallback.defaultValue();
        String comment = primary.comment() != null ? primary.comment() : fallback.comment();
        return new EaColumnDefinition(
                primary.columnName(), dataType, length, precision, scale, nullable, lengthSemantics, defaultValue, comment
        );
    }


    private static String firstPresent(
            List<EaTableDefinition> definitions,
            java.util.function.Function<EaTableDefinition, String> getter,
            String preferred
    ) {
        if (preferred != null && !preferred.isBlank()) return preferred;
        for (EaTableDefinition definition : definitions) {
            String value = getter.apply(definition);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String cleanEaText(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        text = text.replaceAll("(?i)<br\s*/?>", " ")
                .replaceAll("<[^>]+>", " ");
        text = text.replace('\u00A0', ' ')
                .replace('\u200E', ' ')
                .replace('\u200F', ' ')
                .replace('\u202A', ' ')
                .replace('\u202B', ' ')
                .replace('\u202C', ' ');
        text = text.replaceAll("\s+", " ").trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean usefulNumber(Integer value) {
        return value != null && value > 0;
    }

    private static boolean sameStructure(EaTableDefinition left, EaTableDefinition right) {
        if (!left.primaryKeyColumns().equals(right.primaryKeyColumns())
                || !left.foreignKeys().equals(right.foreignKeys())
                || !left.checkConstraints().equals(right.checkConstraints())
                || left.columns().size() != right.columns().size()) {
            return false;
        }
        for (int index = 0; index < left.columns().size(); index++) {
            EaColumnDefinition a = left.columns().get(index);
            EaColumnDefinition b = right.columns().get(index);
            if (!java.util.Objects.equals(a.columnName(), b.columnName())
                    || !java.util.Objects.equals(a.dataType(), b.dataType())
                    || !java.util.Objects.equals(a.length(), b.length())
                    || !java.util.Objects.equals(a.precision(), b.precision())
                    || !java.util.Objects.equals(a.scale(), b.scale())
                    || !java.util.Objects.equals(a.nullable(), b.nullable())
                    || !java.util.Objects.equals(a.lengthSemantics(), b.lengthSemantics())
                    || !java.util.Objects.equals(a.defaultValue(), b.defaultValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasDirectStereotype(Element element, String expected) {
        Element stereotypes = directChild(element, UML_NAMESPACE, "ModelElement.stereotype");
        if (stereotypes == null) return false;
        for (Element child : directElementChildren(stereotypes)) {
            if (matches(child, UML_NAMESPACE, "Stereotype") && expected.equalsIgnoreCase(child.getAttribute("name"))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> directTaggedValues(Element element) {
        Map<String, String> values = new LinkedHashMap<>();
        Element tagged = directChild(element, UML_NAMESPACE, "ModelElement.taggedValue");
        if (tagged == null) return values;
        for (Element child : directElementChildren(tagged)) {
            if (!matches(child, UML_NAMESPACE, "TaggedValue")) continue;
            String tag = trimToNull(child.getAttribute("tag"));
            if (tag != null) values.put(tag, trimToNull(child.getAttribute("value")));
        }
        return values;
    }

    private static DocumentBuilderFactory secureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // JDK parser supports these attributes; keep feature-based hardening as the fallback.
        }
        return factory;
    }

    private static String firstAttribute(Document document, String namespace, String localName, String attribute) {
        NodeList nodes = document.getElementsByTagNameNS(namespace, localName);
        if (nodes.getLength() == 0) return null;
        return trimToNull(((Element) nodes.item(0)).getAttribute(attribute));
    }

    private static String firstText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        return trimToNull(nodes.item(0).getTextContent());
    }

    private static Element directChild(Element parent, String namespace, String localName) {
        for (Element child : directElementChildren(parent)) {
            if (matches(child, namespace, localName)) return child;
        }
        return null;
    }

    private static List<Element> directElementChildren(Element parent) {
        List<Element> children = new ArrayList<>();
        Node node = parent.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) children.add((Element) node);
            node = node.getNextSibling();
        }
        return children;
    }

    private static boolean matches(Element element, String namespace, String localName) {
        return localName.equals(element.getLocalName()) && namespace.equals(element.getNamespaceURI());
    }

    private static String normalizeIdentifier(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeType(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String normalizeLengthSemantics(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return "CHAR".equals(upper) || "BYTE".equals(upper) ? upper : null;
    }

    private static Integer positiveOrZero(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;
        try {
            int parsed = Integer.parseInt(trimmed);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
