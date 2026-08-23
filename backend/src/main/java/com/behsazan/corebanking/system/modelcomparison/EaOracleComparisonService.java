package com.behsazan.corebanking.system.modelcomparison;

import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ColumnComparison;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ColumnStatus;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonConfiguration;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonReport;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonSummary;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.DatabaseOnlyTable;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.PrimaryKeyStatus;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.TableComparison;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.TableStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
class EaOracleComparisonService {
    private final EaXmiModelParser parser;
    private final OracleSchemaInspector inspector;
    private final ConfiguredDatabaseSchemas schemas;

    EaOracleComparisonService(EaXmiModelParser parser, OracleSchemaInspector inspector, ConfiguredDatabaseSchemas schemas) {
        this.parser = parser;
        this.inspector = inspector;
        this.schemas = schemas;
    }

    ComparisonConfiguration configuration() {
        return inspector.configuration();
    }

    ComparisonReport compare(MultipartFile file, String requestedSchema, boolean includeRowCounts) {
        validateFile(file);
        long started = System.nanoTime();
        String schema = schemas.require(requestedSchema);
        EaXmiModel model;
        try (InputStream input = file.getInputStream()) {
            model = parser.parse(input);
        } catch (IOException exception) {
            throw new ModelComparisonValidationException("فایل XML/XMI قابل خواندن نیست.", exception);
        }

        Set<String> eaTableNames = new HashSet<>();
        model.tables().forEach(table -> eaTableNames.add(table.tableName()));
        OracleSchemaSnapshot database = inspector.inspect(schema, eaTableNames, includeRowCounts);

        List<TableComparison> tables = new ArrayList<>();
        List<String> warnings = new ArrayList<>(model.warnings());
        for (EaTableDefinition eaTable : model.tables()) {
            OracleTableDefinition dbTable = database.tables().get(eaTable.tableName());
            TableComparison comparison = compareTable(eaTable, dbTable);
            tables.add(comparison);
            if (comparison.rowCountNote() != null) {
                warnings.add(eaTable.tableName() + ": " + comparison.rowCountNote());
            }
        }
        tables.sort(Comparator.comparing(TableComparison::tableName));

        List<DatabaseOnlyTable> databaseOnly = database.tables().entrySet().stream()
                .filter(entry -> !eaTableNames.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DatabaseOnlyTable(entry.getKey(), entry.getValue().comment()))
                .toList();

        int eaColumnCount = model.tables().stream().mapToInt(table -> table.columns().size()).sum();
        int matchingTables = (int) tables.stream().filter(table -> table.status() == TableStatus.MATCH).count();
        int differentTables = (int) tables.stream().filter(table -> table.status() == TableStatus.DIFFERENT).count();
        int missingTables = (int) tables.stream().filter(table -> table.status() == TableStatus.MISSING_IN_DATABASE).count();
        int matchingColumns = tables.stream().mapToInt(TableComparison::matchingColumnCount).sum();
        int differentColumns = tables.stream().mapToInt(TableComparison::differentColumnCount).sum();
        int missingColumns = tables.stream().mapToInt(TableComparison::missingColumnCount).sum();
        int extraColumns = tables.stream().mapToInt(TableComparison::extraColumnCount).sum();
        Long totalRows = includeRowCounts
                ? tables.stream().map(TableComparison::rowCount).filter(value -> value != null).reduce(0L, Long::sum)
                : null;
        long durationMillis = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);

        ComparisonSummary summary = new ComparisonSummary(
                model.rawTableDefinitionCount(),
                model.tables().size(),
                eaColumnCount,
                database.tables().size(),
                matchingTables,
                differentTables,
                missingTables,
                databaseOnly.size(),
                matchingColumns,
                differentColumns,
                missingColumns,
                extraColumns,
                totalRows,
                durationMillis
        );

        return new ComparisonReport(
                safeFileName(file.getOriginalFilename()),
                model.modelName(),
                model.exporter(),
                model.exporterVersion(),
                model.exportedAt(),
                schema,
                includeRowCounts,
                Instant.now(),
                summary,
                List.copyOf(tables),
                databaseOnly,
                List.copyOf(warnings)
        );
    }

    static TableComparison compareTable(EaTableDefinition ea, OracleTableDefinition db) {
        if (db == null) {
            List<ColumnComparison> missingColumns = ea.columns().stream()
                    .map(column -> new ColumnComparison(
                            column.columnName(),
                            ColumnStatus.MISSING_IN_DATABASE,
                            column.displayType(),
                            null,
                            column.nullable(),
                            null,
                            column.comment(),
                            null,
                            List.of("ستون در Oracle وجود ندارد.")
                    ))
                    .toList();
            return new TableComparison(
                    ea.tableName(),
                    TableStatus.MISSING_IN_DATABASE,
                    ea.sourceDefinitionCount(),
                    ea.persianTitle(),
                    ea.documentation(),
                    null,
                    false,
                    List.of("جدول در Oracle وجود ندارد؛ عنوان و Comment فارسی قابل مقایسه نیست."),
                    ea.columns().size(),
                    0,
                    null,
                    null,
                    ea.primaryKeyColumns().isEmpty() ? PrimaryKeyStatus.NOT_DEFINED_IN_EA : PrimaryKeyStatus.DIFFERENT,
                    ea.primaryKeyColumns(),
                    List.of(),
                    0,
                    0,
                    missingColumns.size(),
                    0,
                    missingColumns
            );
        }

        Map<String, OracleColumnDefinition> dbColumns = new LinkedHashMap<>();
        db.columns().forEach(column -> dbColumns.put(column.columnName().toUpperCase(Locale.ROOT), column));
        List<ColumnComparison> comparisons = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (EaColumnDefinition eaColumn : ea.columns()) {
            String key = eaColumn.columnName().toUpperCase(Locale.ROOT);
            OracleColumnDefinition dbColumn = dbColumns.get(key);
            visited.add(key);
            comparisons.add(compareColumn(eaColumn, dbColumn));
        }
        for (OracleColumnDefinition dbColumn : db.columns()) {
            String key = dbColumn.columnName().toUpperCase(Locale.ROOT);
            if (visited.contains(key)) continue;
            comparisons.add(new ColumnComparison(
                    dbColumn.columnName(),
                    ColumnStatus.EXTRA_IN_DATABASE,
                    null,
                    dbColumn.displayType(),
                    null,
                    dbColumn.nullable(),
                    null,
                    dbColumn.comment(),
                    List.of("ستون در Oracle وجود دارد اما در مدل EA تعریف نشده است.")
            ));
        }

        List<String> tableMetadataDifferences = compareTablePersianMetadata(ea, db);
        boolean persianMetadataMatch = tableMetadataDifferences.isEmpty();

        int matches = (int) comparisons.stream().filter(column -> column.status() == ColumnStatus.MATCH).count();
        int differences = (int) comparisons.stream().filter(column -> column.status() == ColumnStatus.DIFFERENT).count();
        int missing = (int) comparisons.stream().filter(column -> column.status() == ColumnStatus.MISSING_IN_DATABASE).count();
        int extra = (int) comparisons.stream().filter(column -> column.status() == ColumnStatus.EXTRA_IN_DATABASE).count();
        PrimaryKeyStatus pkStatus = primaryKeyStatus(ea.primaryKeyColumns(), db.primaryKeyColumns());
        TableStatus status = differences == 0 && missing == 0 && extra == 0
                && pkStatus != PrimaryKeyStatus.DIFFERENT && persianMetadataMatch
                ? TableStatus.MATCH
                : TableStatus.DIFFERENT;

        return new TableComparison(
                ea.tableName(),
                status,
                ea.sourceDefinitionCount(),
                ea.persianTitle(),
                ea.documentation(),
                db.comment(),
                persianMetadataMatch,
                List.copyOf(tableMetadataDifferences),
                ea.columns().size(),
                db.columns().size(),
                db.rowCount(),
                db.rowCountNote(),
                pkStatus,
                ea.primaryKeyColumns(),
                db.primaryKeyColumns(),
                matches,
                differences,
                missing,
                extra,
                List.copyOf(comparisons)
        );
    }

    private static ColumnComparison compareColumn(EaColumnDefinition ea, OracleColumnDefinition db) {
        if (db == null) {
            return new ColumnComparison(
                    ea.columnName(),
                    ColumnStatus.MISSING_IN_DATABASE,
                    ea.displayType(),
                    null,
                    ea.nullable(),
                    null,
                    ea.comment(),
                    null,
                    List.of("ستون در Oracle وجود ندارد.")
            );
        }

        List<String> differences = new ArrayList<>();
        String eaType = normalizeType(ea.dataType());
        String dbType = normalizeType(db.normalizedDataType());
        if (!same(eaType, dbType)) {
            differences.add("نوع داده: EA=" + value(eaType) + "، Oracle=" + value(dbType));
        } else {
            compareShape(ea, db, differences);
        }
        if (ea.nullable() != null && ea.nullable() != db.nullable()) {
            differences.add("Nullable: EA=" + nullableLabel(ea.nullable()) + "، Oracle=" + nullableLabel(db.nullable()));
        }
        compareTextMetadata("COMMENT فارسی ستون", ea.comment(), db.comment(), differences);

        return new ColumnComparison(
                ea.columnName(),
                differences.isEmpty() ? ColumnStatus.MATCH : ColumnStatus.DIFFERENT,
                ea.displayType(),
                db.displayType(ea.lengthSemantics() != null),
                ea.nullable(),
                db.nullable(),
                ea.comment(),
                db.comment(),
                List.copyOf(differences)
        );
    }


    private static List<String> compareTablePersianMetadata(EaTableDefinition ea, OracleTableDefinition db) {
        List<String> differences = new ArrayList<>();
        String eaTitle = ea.persianTitle();
        String eaDocumentation = ea.documentation();
        String dbComment = db.comment();

        if (eaTitle != null) {
            if (dbComment == null) {
                differences.add("عنوان فارسی جدول در EA='" + eaTitle + "' است ولی COMMENT جدول در Oracle ثبت نشده است.");
            } else if (!normalizedContains(dbComment, eaTitle)) {
                differences.add("عنوان فارسی جدول: EA='" + eaTitle + "'، Oracle COMMENT='" + dbComment + "'.");
            }
        } else if (dbComment != null) {
            differences.add("عنوان فارسی جدول (alias) در EA تعریف نشده است؛ Oracle COMMENT='" + dbComment + "'.");
        }

        compareTextMetadata("COMMENT/Documentation جدول", eaDocumentation, dbComment, differences);
        return differences;
    }

    private static void compareTextMetadata(String label, String eaText, String dbText, List<String> differences) {
        if (eaText == null && dbText == null) return;
        if (eaText == null) {
            differences.add(label + ": در EA تعریف نشده است؛ Oracle='" + dbText + "'.");
            return;
        }
        if (dbText == null) {
            differences.add(label + ": EA='" + eaText + "'؛ در Oracle ثبت نشده است.");
            return;
        }
        if (!normalizePersianMetadata(eaText).equals(normalizePersianMetadata(dbText))) {
            differences.add(label + ": EA='" + eaText + "'، Oracle='" + dbText + "'.");
        }
    }

    private static boolean normalizedContains(String container, String value) {
        String normalizedContainer = normalizePersianMetadata(container);
        String normalizedValue = normalizePersianMetadata(value);
        return !normalizedValue.isEmpty() && normalizedContainer.contains(normalizedValue);
    }

    private static String normalizePersianMetadata(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('ي', 'ی')
                .replace('ى', 'ی')
                .replace('ك', 'ک')
                .replace('ة', 'ه')
                .replace('ۀ', 'ه')
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[\\s\\p{P}\\p{Cf}]+", "");
    }

    private static void compareShape(EaColumnDefinition ea, OracleColumnDefinition db, List<String> differences) {
        String type = normalizeType(ea.dataType());
        if ("NUMBER".equals(type)) {
            if (ea.precision() != null && ea.precision() > 0 && !sameNumber(ea.precision(), db.precision())) {
                differences.add("Precision: EA=" + ea.precision() + "، Oracle=" + value(db.precision()));
            }
            if (ea.scale() != null && !sameNumber(ea.scale(), db.scale() == null ? 0 : db.scale())) {
                differences.add("Scale: EA=" + ea.scale() + "، Oracle=" + value(db.scale()));
            }
            return;
        }
        if ("VARCHAR2".equals(type) || "NVARCHAR2".equals(type) || "CHAR".equals(type)) {
            Integer dbLength = db.charLength() != null && db.charLength() > 0 ? db.charLength() : db.dataLength();
            if (ea.length() != null && ea.length() > 0 && !sameNumber(ea.length(), dbLength)) {
                differences.add("طول: EA=" + ea.length() + "، Oracle=" + value(dbLength));
            }
            if (ea.lengthSemantics() != null && db.charUsed() != null) {
                String dbSemantics = "C".equalsIgnoreCase(db.charUsed()) ? "CHAR" : "B".equalsIgnoreCase(db.charUsed()) ? "BYTE" : db.charUsed();
                if (!ea.lengthSemantics().equalsIgnoreCase(dbSemantics)) {
                    differences.add("Length semantics: EA=" + ea.lengthSemantics() + "، Oracle=" + dbSemantics);
                }
            }
            return;
        }
        if ("RAW".equals(type)) {
            if (ea.length() != null && ea.length() > 0 && !sameNumber(ea.length(), db.dataLength())) {
                differences.add("طول: EA=" + ea.length() + "، Oracle=" + value(db.dataLength()));
            }
            return;
        }
        if ("TIMESTAMP".equals(type) || "TIMESTAMP WITH TIME ZONE".equals(type) || "TIMESTAMP WITH LOCAL TIME ZONE".equals(type)) {
            if (ea.length() != null && db.scale() != null && !sameNumber(ea.length(), db.scale())) {
                differences.add("Fractional seconds: EA=" + ea.length() + "، Oracle=" + db.scale());
            }
        }
    }

    private static PrimaryKeyStatus primaryKeyStatus(List<String> ea, List<String> db) {
        if (ea == null || ea.isEmpty()) return PrimaryKeyStatus.NOT_DEFINED_IN_EA;
        List<String> eaNormalized = ea.stream().map(value -> value.toUpperCase(Locale.ROOT)).toList();
        List<String> dbNormalized = db.stream().map(value -> value.toUpperCase(Locale.ROOT)).toList();
        return eaNormalized.equals(dbNormalized) ? PrimaryKeyStatus.MATCH : PrimaryKeyStatus.DIFFERENT;
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ModelComparisonValidationException("یک فایل XML یا XMI خروجی Enterprise Architect انتخاب کنید.");
        }
        String name = safeFileName(file.getOriginalFilename());
        String lower = name.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".xml") || lower.endsWith(".xmi"))) {
            throw new ModelComparisonValidationException("پسوند فایل باید .xml یا .xmi باشد.");
        }
    }

    private static String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "ea-model.xml";
        String normalized = fileName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private static String normalizeType(String value) {
        if (value == null) return null;
        return value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("TIMESTAMP\\(\\d+\\) WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE")
                .replaceAll("TIMESTAMP\\(\\d+\\) WITH LOCAL TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE")
                .replaceAll("TIMESTAMP\\(\\d+\\)", "TIMESTAMP");
    }

    private static boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static boolean sameNumber(Integer left, Integer right) {
        return left == null ? right == null : left.equals(right);
    }

    private static String nullableLabel(boolean nullable) {
        return nullable ? "NULL" : "NOT NULL";
    }

    private static String value(Object value) {
        return value == null ? "نامشخص" : String.valueOf(value);
    }
}
