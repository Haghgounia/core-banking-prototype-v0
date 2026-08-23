package com.behsazan.corebanking.system.modelcomparison;

import java.time.Instant;
import java.util.List;

public final class EaOracleComparisonModels {
    private EaOracleComparisonModels() {
    }

    public enum TableStatus {
        MATCH,
        DIFFERENT,
        MISSING_IN_DATABASE
    }

    public enum ColumnStatus {
        MATCH,
        DIFFERENT,
        MISSING_IN_DATABASE,
        EXTRA_IN_DATABASE
    }

    public enum PrimaryKeyStatus {
        MATCH,
        DIFFERENT,
        NOT_DEFINED_IN_EA
    }

    public record SchemaOption(String code, String label) {
    }

    public record ComparisonConfiguration(
            List<SchemaOption> schemas,
            String defaultSchema,
            String databaseProduct,
            String databaseVersion,
            String jdbcUrl,
            String connectionUser
    ) {
    }

    public record ComparisonSummary(
            int rawEaTableDefinitionCount,
            int eaTableCount,
            int eaColumnCount,
            int databaseTableCount,
            int matchingTableCount,
            int differentTableCount,
            int missingTableCount,
            int databaseOnlyTableCount,
            int matchingColumnCount,
            int differentColumnCount,
            int missingColumnCount,
            int extraColumnCount,
            Long totalRowsInComparedTables,
            long durationMillis
    ) {
    }

    public record ComparisonReport(
            String sourceFileName,
            String modelName,
            String exporter,
            String exporterVersion,
            String exportedAt,
            String targetSchema,
            boolean rowCountsIncluded,
            Instant comparedAt,
            ComparisonSummary summary,
            List<TableComparison> tables,
            List<DatabaseOnlyTable> databaseOnlyTables,
            List<String> warnings
    ) {
    }

    public record TableComparison(
            String tableName,
            TableStatus status,
            int sourceDefinitionCount,
            int eaColumnCount,
            int databaseColumnCount,
            Long rowCount,
            String rowCountNote,
            PrimaryKeyStatus primaryKeyStatus,
            List<String> eaPrimaryKey,
            List<String> databasePrimaryKey,
            int matchingColumnCount,
            int differentColumnCount,
            int missingColumnCount,
            int extraColumnCount,
            List<ColumnComparison> columns
    ) {
    }

    public record ColumnComparison(
            String columnName,
            ColumnStatus status,
            String eaDefinition,
            String databaseDefinition,
            Boolean eaNullable,
            Boolean databaseNullable,
            List<String> differences
    ) {
    }

    public record DatabaseOnlyTable(String tableName) {
    }
}
