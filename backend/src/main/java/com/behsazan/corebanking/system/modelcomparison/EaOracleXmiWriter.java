package com.behsazan.corebanking.system.modelcomparison;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
class EaOracleXmiWriter {
    private static final String UML_NS = "omg.org/UML1.3";
    private static final DateTimeFormatter EA_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    byte[] write(OracleEaPhysicalModel model, String databaseVersion) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().newDocument();

            Element xmi = document.createElement("XMI");
            xmi.setAttribute("xmi.version", "1.1");
            xmi.setAttribute("xmlns:UML", UML_NS);
            xmi.setAttribute("timestamp", EA_TIMESTAMP.format(LocalDateTime.now()));
            document.appendChild(xmi);

            Element header = append(document, xmi, "XMI.header");
            Element documentation = append(document, header, "XMI.documentation");
            textElement(document, documentation, "XMI.exporter", "Enterprise Architect");
            textElement(document, documentation, "XMI.exporterVersion", "2.5");
            textElement(document, documentation, "XMI.exporterID", "1554");

            Element content = append(document, xmi, "XMI.content");
            Element umlModel = uml(document, content, "Model");
            umlModel.setAttribute("name", model.schemaName() + " Oracle Physical Model");
            umlModel.setAttribute("xmi.id", modelId(model.schemaName()));
            Element modelOwned = uml(document, umlModel, "Namespace.ownedElement");

            Element rootClass = uml(document, modelOwned, "Class");
            rootClass.setAttribute("name", "EARootClass");
            rootClass.setAttribute("xmi.id", "EAID_11111111_5487_4080_A7F4_41526CB0AA00");
            rootClass.setAttribute("isRoot", "true");
            rootClass.setAttribute("isLeaf", "false");
            rootClass.setAttribute("isAbstract", "false");

            String rootPackageId = packageId("ROOT|" + model.schemaName());
            Element rootPackage = uml(document, modelOwned, "Package");
            rootPackage.setAttribute("name", model.schemaName() + " - Oracle Physical Model");
            rootPackage.setAttribute("xmi.id", rootPackageId);
            rootPackage.setAttribute("isRoot", "false");
            rootPackage.setAttribute("isLeaf", "false");
            rootPackage.setAttribute("isAbstract", "false");
            rootPackage.setAttribute("visibility", "public");
            addPackageTags(document, rootPackage, rootPackageId, model.schemaName(), databaseVersion, false);
            Element rootOwned = uml(document, rootPackage, "Namespace.ownedElement");

            Map<String, Element> ownerOwned = new LinkedHashMap<>();
            ownerOwned.put(model.schemaName(), rootOwned);
            Set<String> externalOwners = new LinkedHashSet<>();
            model.tables().stream().filter(OracleEaTable::externalReference).map(OracleEaTable::owner).forEach(externalOwners::add);
            for (String owner : externalOwners) {
                if (owner.equals(model.schemaName())) continue;
                String packageKey = "EXT|" + model.schemaName() + "|" + owner;
                String packageId = packageId(packageKey);
                Element externalPackage = uml(document, rootOwned, "Package");
                externalPackage.setAttribute("name", "External References - " + owner);
                externalPackage.setAttribute("xmi.id", packageId);
                externalPackage.setAttribute("isRoot", "false");
                externalPackage.setAttribute("isLeaf", "false");
                externalPackage.setAttribute("isAbstract", "false");
                externalPackage.setAttribute("visibility", "public");
                addPackageTags(document, externalPackage, packageId, owner, databaseVersion, true);
                ownerOwned.put(owner, uml(document, externalPackage, "Namespace.ownedElement"));
            }

            Map<String, OracleEaTable> tableMap = new LinkedHashMap<>();
            Map<String, String> classIds = new LinkedHashMap<>();
            Map<String, Map<String, OracleEaColumn>> columnsByQualifiedTable = new LinkedHashMap<>();
            for (OracleEaTable table : model.tables()) {
                String qualified = qualified(table.owner(), table.tableName());
                tableMap.put(qualified, table);
                classIds.put(qualified, classId(qualified));
                Map<String, OracleEaColumn> columns = new LinkedHashMap<>();
                table.columns().forEach(column -> columns.put(column.columnName(), column));
                columnsByQualifiedTable.put(qualified, columns);
            }

            List<OracleEaTable> sortedTables = model.tables().stream()
                    .sorted(Comparator.comparing(OracleEaTable::externalReference)
                            .thenComparing(OracleEaTable::owner)
                            .thenComparing(OracleEaTable::tableName))
                    .toList();
            for (OracleEaTable table : sortedTables) {
                Element container = ownerOwned.getOrDefault(table.owner(), rootOwned);
                writeTable(document, container, table, classIds.get(qualified(table.owner(), table.tableName())), rootPackageId, model.schemaName(),
                        model.foreignKeys(), columnsByQualifiedTable, databaseVersion);
            }

            for (OracleEaForeignKey fk : model.foreignKeys()) {
                String childKey = qualified(fk.childOwner(), fk.childTable());
                String parentKey = qualified(fk.parentOwner(), fk.parentTable());
                if (!classIds.containsKey(childKey) || !classIds.containsKey(parentKey)) continue;
                writeAssociation(document, rootOwned, fk, classIds.get(childKey), classIds.get(parentKey),
                        tableMap.get(childKey), databaseVersion);
            }

            Map<String, String> typeIds = collectDataTypes(sortedTables);
            for (Map.Entry<String, String> type : typeIds.entrySet()) {
                Element dataType = uml(document, modelOwned, "DataType");
                dataType.setAttribute("xmi.id", type.getValue());
                if (!"<void>".equals(type.getKey())) dataType.setAttribute("name", type.getKey());
                dataType.setAttribute("visibility", "private");
                dataType.setAttribute("isRoot", "false");
                dataType.setAttribute("isLeaf", "false");
                dataType.setAttribute("isAbstract", "false");
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("EA XMI generation failed", exception);
        }
    }

    private void writeTable(
            Document document,
            Element container,
            OracleEaTable table,
            String classId,
            String rootPackageId,
            String sourceSchema,
            List<OracleEaForeignKey> foreignKeys,
            Map<String, Map<String, OracleEaColumn>> allColumns,
            String databaseVersion
    ) {
        Element tableElement = uml(document, container, "Class");
        tableElement.setAttribute("name", table.tableName());
        tableElement.setAttribute("xmi.id", classId);
        tableElement.setAttribute("visibility", "public");
        tableElement.setAttribute("namespace", table.externalReference() && !table.owner().equals(sourceSchema)
                ? packageId("EXT|" + sourceSchema + "|" + table.owner())
                : rootPackageId);
        tableElement.setAttribute("isRoot", "false");
        tableElement.setAttribute("isLeaf", "false");
        tableElement.setAttribute("isAbstract", "false");
        tableElement.setAttribute("isActive", "false");
        stereotype(document, tableElement, "table");

        Element tags = taggedValues(document, tableElement);
        tagged(document, tags, "documentation", table.comment());
        tagged(document, tags, "isSpecification", "false");
        tagged(document, tags, "ea_stype", "Class");
        tagged(document, tags, "ea_ntype", "0");
        tagged(document, tags, "version", "1.0");
        tagged(document, tags, "gentype", "Oracle");
        tagged(document, tags, "package_name", table.externalReference() ? "Referenced Oracle Table" : "Oracle Physical Table");
        tagged(document, tags, "alias", shortAlias(table.comment()));
        tagged(document, tags, "author", "Core Banking Prototype Oracle EA Exporter");
        tagged(document, tags, "product_name", "Oracle");
        tagged(document, tags, "status", table.externalReference() ? "Referenced" : "Approved");
        tagged(document, tags, "stereotype", "table");
        tagged(document, tags, "ea_guid", eaGuid("TABLE|" + qualified(table.owner(), table.tableName())));
        tagged(document, tags, "Owner", table.owner());
        tagged(document, tags, "Tablespace", table.tablespace());
        tagged(document, tags, "DBVersion", databaseVersion);
        tagged(document, tags, "external_reference", table.externalReference() ? "true" : "false");

        Element feature = uml(document, tableElement, "Classifier.feature");
        Map<String, OracleEaColumn> tableColumns = allColumns.getOrDefault(qualified(table.owner(), table.tableName()), Map.of());
        Map<String, String> typeIds = collectDataTypesFromColumns(table.columns());
        for (OracleEaColumn column : table.columns()) {
            writeColumn(document, feature, table, column, typeIds.getOrDefault(column.normalizedDataType(), dataTypeId(column.normalizedDataType())));
        }
        for (OracleEaKeyConstraint key : table.keyConstraints()) {
            writeOperation(document, feature, table, key.constraintName(), key.primaryKey() ? "PK" : "unique", key.columns(), tableColumns,
                    key.status(), null, databaseVersion);
        }
        for (OracleEaIndex index : table.indexes()) {
            String indexMetadata = "IndexType=" + (index.indexType() == null ? "" : index.indexType())
                    + ";Columns=" + index.columns().stream()
                    .sorted(Comparator.comparingInt(OracleEaIndexColumn::position))
                    .map(column -> column.columnName() + " " + (column.descend() == null ? "ASC" : column.descend()))
                    .reduce((left, right) -> left + "," + right).orElse("") + ";";
            writeOperation(document, feature, table, index.indexName(), index.unique() ? "unique" : "index",
                    index.columns().stream().map(OracleEaIndexColumn::columnName).toList(), tableColumns,
                    index.status(), indexMetadata, databaseVersion);
        }
        foreignKeys.stream()
                .filter(fk -> fk.childOwner().equals(table.owner()) && fk.childTable().equals(table.tableName()))
                .forEach(fk -> writeOperation(document, feature, table, fk.constraintName(), "FK",
                        fk.columns().stream().map(OracleEaForeignKeyColumn::childColumn).toList(), tableColumns,
                        fk.status(), "Delete " + normalizeDeleteRule(fk.deleteRule()), databaseVersion));

        Element constraints = uml(document, tableElement, "ModelElement.constraint");
        for (OracleEaKeyConstraint key : table.keyConstraints()) {
            String ddl = key.primaryKey()
                    ? "CONSTRAINT " + key.constraintName() + " PRIMARY KEY (" + String.join(", ", key.columns()) + ")"
                    : "CONSTRAINT " + key.constraintName() + " UNIQUE (" + String.join(", ", key.columns()) + ")";
            writeConstraint(document, constraints, ddl, key.status());
        }
        foreignKeys.stream()
                .filter(fk -> fk.childOwner().equals(table.owner()) && fk.childTable().equals(table.tableName()))
                .forEach(fk -> {
                    String parent = fk.parentOwner().equals(table.owner()) ? fk.parentTable() : fk.parentOwner() + "." + fk.parentTable();
                    String ddl = "CONSTRAINT " + fk.constraintName() + " FOREIGN KEY ("
                            + String.join(", ", fk.columns().stream().map(OracleEaForeignKeyColumn::childColumn).toList())
                            + ") REFERENCES " + parent + "("
                            + String.join(", ", fk.columns().stream().map(OracleEaForeignKeyColumn::parentColumn).toList()) + ")";
                    writeConstraint(document, constraints, ddl, fk.status());
                });
        for (OracleEaCheckConstraint check : table.checks()) {
            writeConstraint(document, constraints, "CONSTRAINT " + check.constraintName() + " CHECK (" + check.condition() + ")", check.status());
        }
    }

    private void writeColumn(Document document, Element feature, OracleEaTable table, OracleEaColumn column, String typeId) {
        Element attribute = uml(document, feature, "Attribute");
        attribute.setAttribute("name", column.columnName());
        attribute.setAttribute("changeable", "none");
        attribute.setAttribute("visibility", "public");
        attribute.setAttribute("ownerScope", "instance");
        attribute.setAttribute("targetScope", "instance");

        Element initial = uml(document, attribute, "Attribute.initialValue");
        Element expression = uml(document, initial, "Expression");
        if (column.defaultValue() != null) expression.setAttribute("body", column.defaultValue());

        Element structuralType = uml(document, attribute, "StructuralFeature.type");
        Element classifier = uml(document, structuralType, "Classifier");
        classifier.setAttribute("xmi.idref", typeId);
        stereotype(document, attribute, "column");

        Element tags = taggedValues(document, attribute);
        tagged(document, tags, "description", column.comment());
        tagged(document, tags, "type", column.normalizedDataType());
        tagged(document, tags, "derived", column.virtualColumn() ? "1" : "0");
        tagged(document, tags, "containment", "Not Specified");
        tagged(document, tags, "length", Integer.toString(column.eaLength()));
        tagged(document, tags, "ordered", "0");
        tagged(document, tags, "precision", Integer.toString(column.precision() == null ? 0 : column.precision()));
        tagged(document, tags, "scale", Integer.toString(column.scale() == null ? 0 : column.scale()));
        tagged(document, tags, "static", "0");
        tagged(document, tags, "stereotype", "column");
        tagged(document, tags, "collection", "false");
        tagged(document, tags, "position", Integer.toString(Math.max(0, column.position() - 1)));
        tagged(document, tags, "lowerBound", column.nullable() ? "0" : "1");
        tagged(document, tags, "upperBound", "1");
        tagged(document, tags, "duplicates", column.nullable() ? "0" : "1");
        tagged(document, tags, "ea_guid", eaGuid("COLUMN|" + qualified(table.owner(), table.tableName()) + "|" + column.columnName()));
        if ("C".equalsIgnoreCase(column.charUsed())) tagged(document, tags, "LengthType", "CHAR");
        if ("B".equalsIgnoreCase(column.charUsed())) tagged(document, tags, "LengthType", "BYTE");
        boolean autoNumber = column.identityColumn() || (column.defaultValue() != null && column.defaultValue().toUpperCase(Locale.ROOT).contains(".NEXTVAL"));
        if (autoNumber) {
            tagged(document, tags, "AutoNum", "True");
            tagged(document, tags, "property", "AutoNum=True;");
        }
        if (column.virtualColumn()) tagged(document, tags, "IsVirtual", "True");
    }

    private void writeOperation(
            Document document,
            Element feature,
            OracleEaTable table,
            String name,
            String stereotypeName,
            List<String> columns,
            Map<String, OracleEaColumn> columnMap,
            String status,
            String extra,
            String databaseVersion
    ) {
        if (name == null || name.isBlank()) return;
        Element operation = uml(document, feature, "Operation");
        operation.setAttribute("name", name);
        operation.setAttribute("visibility", "public");
        operation.setAttribute("ownerScope", "instance");
        operation.setAttribute("isQuery", "false");
        operation.setAttribute("concurrency", "sequential");
        stereotype(document, operation, stereotypeName);
        Element tags = taggedValues(document, operation);
        tagged(document, tags, "const", "false");
        tagged(document, tags, "stereotype", stereotypeName);
        tagged(document, tags, "synchronised", "0");
        tagged(document, tags, "concurrency", "Sequential");
        tagged(document, tags, "position", "0");
        tagged(document, tags, "returnarray", "0");
        tagged(document, tags, "pure", "0");
        tagged(document, tags, "ea_guid", eaGuid("OP|" + qualified(table.owner(), table.tableName()) + "|" + name));
        tagged(document, tags, "status", status);
        tagged(document, tags, "DBVersion", databaseVersion);
        if (extra != null && !extra.isBlank()) tagged(document, tags, "property", extra);

        Element parameters = uml(document, operation, "BehavioralFeature.parameter");
        Element returnParameter = uml(document, parameters, "Parameter");
        returnParameter.setAttribute("kind", "return");
        returnParameter.setAttribute("visibility", "public");
        Element returnType = uml(document, returnParameter, "Parameter.type");
        Element returnClassifier = uml(document, returnType, "Classifier");
        returnClassifier.setAttribute("xmi.idref", dataTypeId("<void>"));
        Element returnTags = taggedValues(document, returnParameter);
        tagged(document, returnTags, "pos", "0");
        tagged(document, returnTags, "const", "0");
        tagged(document, returnTags, "ea_guid", eaGuid("PARAM|" + qualified(table.owner(), table.tableName()) + "|" + name + "|RETURN"));
        Element returnDefault = uml(document, returnParameter, "Parameter.defaultValue");
        uml(document, returnDefault, "Expression");

        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);
            OracleEaColumn column = columnMap.get(columnName);
            Element parameter = uml(document, parameters, "Parameter");
            parameter.setAttribute("name", columnName);
            parameter.setAttribute("kind", "in");
            parameter.setAttribute("visibility", "public");
            Element parameterType = uml(document, parameter, "Parameter.type");
            Element classifier = uml(document, parameterType, "Classifier");
            String type = column == null ? "VARCHAR2" : column.normalizedDataType();
            classifier.setAttribute("xmi.idref", dataTypeId(type));
            Element parameterTags = taggedValues(document, parameter);
            tagged(document, parameterTags, "pos", Integer.toString(i));
            tagged(document, parameterTags, "type", type);
            tagged(document, parameterTags, "const", "0");
            tagged(document, parameterTags, "ea_guid", eaGuid("PARAM|" + qualified(table.owner(), table.tableName()) + "|" + name + "|" + columnName));
            Element defaultValue = uml(document, parameter, "Parameter.defaultValue");
            uml(document, defaultValue, "Expression");
        }
    }

    private void writeConstraint(Document document, Element constraints, String ddl, String status) {
        Element constraint = uml(document, constraints, "Constraint");
        constraint.setAttribute("name", ddl);
        Element tags = taggedValues(document, constraint);
        tagged(document, tags, "type", "Invariant");
        tagged(document, tags, "weight", "0.00");
        tagged(document, tags, "status", status == null ? "Approved" : status);
    }

    private void writeAssociation(
            Document document,
            Element container,
            OracleEaForeignKey fk,
            String childClassId,
            String parentClassId,
            OracleEaTable childTable,
            String databaseVersion
    ) {
        String expression = fk.columns().stream()
                .sorted(Comparator.comparingInt(OracleEaForeignKeyColumn::position))
                .map(pair -> pair.childColumn() + " = " + pair.parentColumn())
                .reduce((left, right) -> left + ", " + right)
                .orElse(fk.constraintName());
        Element association = uml(document, container, "Association");
        association.setAttribute("name", "(" + expression + ")");
        association.setAttribute("xmi.id", associationId(fk));
        association.setAttribute("visibility", "public");
        association.setAttribute("isRoot", "false");
        association.setAttribute("isLeaf", "false");
        association.setAttribute("isAbstract", "false");
        stereotype(document, association, "FK");
        Element tags = taggedValues(document, association);
        tagged(document, tags, "style", "3");
        tagged(document, tags, "ea_type", "Association");
        tagged(document, tags, "direction", "Source -> Destination");
        tagged(document, tags, "linemode", "3");
        tagged(document, tags, "linecolor", "-1");
        tagged(document, tags, "linewidth", "0");
        tagged(document, tags, "seqno", "0");
        tagged(document, tags, "stereotype", "FK");
        tagged(document, tags, "styleex", "FKINFO=SRC=" + fk.constraintName() + ":DST=" + fk.parentConstraintName() + ":;");
        tagged(document, tags, "ea_sourceName", fk.childTable());
        tagged(document, tags, "ea_targetName", fk.parentTable());
        tagged(document, tags, "ea_sourceType", "Class");
        tagged(document, tags, "ea_targetType", "Class");
        tagged(document, tags, "lt", "+" + fk.constraintName());
        tagged(document, tags, "mt", "(" + expression + ")");
        tagged(document, tags, "rt", "+" + fk.parentConstraintName());
        tagged(document, tags, "DBVersion", databaseVersion);
        tagged(document, tags, "Delete", normalizeDeleteRule(fk.deleteRule()));

        boolean optional = false;
        if (childTable != null) {
            Map<String, OracleEaColumn> childColumns = new LinkedHashMap<>();
            childTable.columns().forEach(column -> childColumns.put(column.columnName(), column));
            optional = fk.columns().stream().anyMatch(pair -> {
                OracleEaColumn column = childColumns.get(pair.childColumn());
                return column == null || column.nullable();
            });
        }

        Element connection = uml(document, association, "Association.connection");
        Element source = uml(document, connection, "AssociationEnd");
        source.setAttribute("visibility", "public");
        source.setAttribute("multiplicity", "0..*");
        source.setAttribute("name", fk.constraintName());
        source.setAttribute("aggregation", "none");
        source.setAttribute("isOrdered", "false");
        source.setAttribute("targetScope", "instance");
        source.setAttribute("changeable", "none");
        source.setAttribute("isNavigable", "false");
        source.setAttribute("type", childClassId);
        Element sourceTags = taggedValues(document, source);
        tagged(document, sourceTags, "containment", "Unspecified");
        tagged(document, sourceTags, "sourcestyle", "Union=0;Derived=0;AllowDuplicates=0;Owned=0;Navigable=Unspecified;");
        tagged(document, sourceTags, "ea_end", "source");

        Element target = uml(document, connection, "AssociationEnd");
        target.setAttribute("visibility", "public");
        target.setAttribute("multiplicity", optional ? "0..1" : "1");
        target.setAttribute("name", fk.parentConstraintName());
        target.setAttribute("aggregation", "none");
        target.setAttribute("isOrdered", "false");
        target.setAttribute("targetScope", "instance");
        target.setAttribute("changeable", "none");
        target.setAttribute("isNavigable", "true");
        target.setAttribute("type", parentClassId);
        Element targetTags = taggedValues(document, target);
        tagged(document, targetTags, "containment", "Unspecified");
        tagged(document, targetTags, "deststyle", "Union=0;Derived=0;AllowDuplicates=0;Owned=0;Navigable=Navigable;");
        tagged(document, targetTags, "ea_end", "target");
    }

    private static Map<String, String> collectDataTypes(List<OracleEaTable> tables) {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        types.add("<void>");
        tables.forEach(table -> table.columns().forEach(column -> types.add(column.normalizedDataType())));
        Map<String, String> result = new LinkedHashMap<>();
        types.forEach(type -> result.put(type, dataTypeId(type)));
        return result;
    }

    private static Map<String, String> collectDataTypesFromColumns(List<OracleEaColumn> columns) {
        Map<String, String> result = new LinkedHashMap<>();
        columns.forEach(column -> result.put(column.normalizedDataType(), dataTypeId(column.normalizedDataType())));
        return result;
    }

    private static void addPackageTags(Document document, Element packageElement, String packageId, String owner, String databaseVersion, boolean external) {
        Element tags = taggedValues(document, packageElement);
        tagged(document, tags, "iscontrolled", "FALSE");
        tagged(document, tags, "version", "1.0");
        tagged(document, tags, "isprotected", "FALSE");
        tagged(document, tags, "status", external ? "Referenced" : "Approved");
        tagged(document, tags, "author", "Core Banking Prototype Oracle EA Exporter");
        tagged(document, tags, "ea_stype", "Public");
        tagged(document, tags, "gentype", "Oracle");
        tagged(document, tags, "Owner", owner);
        tagged(document, tags, "DBVersion", databaseVersion);
        tagged(document, tags, "ea_guid", eaGuid("PACKAGE|" + packageId));
    }

    private static void stereotype(Document document, Element parent, String name) {
        Element stereotypes = uml(document, parent, "ModelElement.stereotype");
        Element stereotype = uml(document, stereotypes, "Stereotype");
        stereotype.setAttribute("name", name);
    }

    private static Element taggedValues(Document document, Element parent) {
        return uml(document, parent, "ModelElement.taggedValue");
    }

    private static void tagged(Document document, Element parent, String tag, String value) {
        if (value == null || value.isBlank()) return;
        Element element = uml(document, parent, "TaggedValue");
        element.setAttribute("tag", tag);
        element.setAttribute("value", value);
    }

    private static Element uml(Document document, Element parent, String localName) {
        Element element = document.createElementNS(UML_NS, "UML:" + localName);
        parent.appendChild(element);
        return element;
    }


    private static Element append(Document document, Element parent, String name) {
        Element element = document.createElement(name);
        parent.appendChild(element);
        return element;
    }

    private static void textElement(Document document, Element parent, String name, String text) {
        Element element = append(document, parent, name);
        element.setTextContent(text);
    }

    private static String shortAlias(String comment) {
        if (comment == null || comment.isBlank()) return null;
        String normalized = comment.replaceAll("\\s+", " ").trim();
        int sentence = normalized.indexOf('.');
        if (sentence > 0 && sentence < 160) normalized = normalized.substring(0, sentence);
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 197) + "...";
    }

    private static String normalizeDeleteRule(String rule) {
        if (rule == null || rule.isBlank() || "NO ACTION".equalsIgnoreCase(rule)) return "No Action";
        return rule.replace('_', ' ');
    }

    private static String qualified(String owner, String table) {
        return owner.toUpperCase(Locale.ROOT) + "." + table.toUpperCase(Locale.ROOT);
    }

    private static String modelId(String schema) {
        return "MX_" + eaId("MODEL|" + schema);
    }

    private static String packageId(String key) {
        return "EAPK_" + uuidToken("PACKAGE|" + key);
    }

    private static String classId(String qualifiedTable) {
        return eaId("TABLE|" + qualifiedTable);
    }

    private static String associationId(OracleEaForeignKey fk) {
        return eaId("FK|" + fk.childOwner() + "." + fk.childTable() + "|" + fk.constraintName());
    }

    private static String dataTypeId(String type) {
        return "EADT_" + uuidToken("TYPE|" + type);
    }

    private static String eaId(String key) {
        return "EAID_" + uuidToken(key);
    }

    private static String eaGuid(String key) {
        return "{" + UUID.nameUUIDFromBytes(("CBP-ORACLE-EA|" + key).getBytes(StandardCharsets.UTF_8)).toString().toUpperCase(Locale.ROOT) + "}";
    }

    private static String uuidToken(String key) {
        return UUID.nameUUIDFromBytes(("CBP-ORACLE-EA|" + key).getBytes(StandardCharsets.UTF_8))
                .toString().toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
