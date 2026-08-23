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
            tables.add(new EaTableDefinition(
                    selected.tableName(),
                    definitions.size(),
                    enrichedColumns,
                    selected.primaryKeyColumns()
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
        Element feature = directChild(tableElement, UML_NAMESPACE, "Classifier.feature");
        List<EaColumnDefinition> columns = new ArrayList<>();
        List<String> primaryKey = new ArrayList<>();
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
                }
            }
        }

        if (primaryKey.isEmpty()) {
            primaryKey = primaryKeyFromConstraint(tableElement);
        }

        return new EaTableDefinition(tableName, 1, List.copyOf(columns), List.copyOf(primaryKey));
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
        String defaultValue = null;
        Element initialValue = directChild(attribute, UML_NAMESPACE, "Attribute.initialValue");
        if (initialValue != null) {
            Element expression = directChild(initialValue, UML_NAMESPACE, "Expression");
            if (expression != null) {
                defaultValue = trimToNull(expression.getAttribute("body"));
            }
        }
        return new EaColumnDefinition(columnName, dataType, length, precision, scale, nullable, lengthSemantics, defaultValue);
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
        return new EaColumnDefinition(
                primary.columnName(), dataType, length, precision, scale, nullable, lengthSemantics, defaultValue
        );
    }

    private static boolean usefulNumber(Integer value) {
        return value != null && value > 0;
    }

    private static boolean sameStructure(EaTableDefinition left, EaTableDefinition right) {
        return left.columns().equals(right.columns()) && left.primaryKeyColumns().equals(right.primaryKeyColumns());
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
