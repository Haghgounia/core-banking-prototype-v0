package com.behsazan.corebanking.system.modelcomparison;

import java.util.List;

record OracleEaPhysicalModel(
        String schemaName,
        List<OracleEaTable> tables,
        List<OracleEaForeignKey> foreignKeys,
        List<String> warnings
) {
}

record OracleEaTable(
        String owner,
        String tableName,
        String comment,
        String tablespace,
        boolean externalReference,
        List<OracleEaColumn> columns,
        List<OracleEaKeyConstraint> keyConstraints,
        List<OracleEaIndex> indexes,
        List<OracleEaCheckConstraint> checks
) {
}

record OracleEaColumn(
        String columnName,
        String dataType,
        Integer dataLength,
        Integer charLength,
        String charUsed,
        Integer precision,
        Integer scale,
        boolean nullable,
        int position,
        String defaultValue,
        String comment,
        boolean identityColumn,
        boolean virtualColumn
) {
    String normalizedDataType() {
        if (dataType == null || dataType.isBlank()) return "VARCHAR2";
        return dataType.trim().toUpperCase()
                .replaceAll("TIMESTAMP\\(\\d+\\) WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE")
                .replaceAll("TIMESTAMP\\(\\d+\\) WITH LOCAL TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE")
                .replaceAll("TIMESTAMP\\(\\d+\\)", "TIMESTAMP");
    }

    int eaLength() {
        String type = normalizedDataType();
        if ("VARCHAR2".equals(type) || "NVARCHAR2".equals(type) || "CHAR".equals(type) || "NCHAR".equals(type)) {
            if (charLength != null && charLength > 0) return charLength;
        }
        if (dataLength != null && dataLength > 0) return dataLength;
        if (type.startsWith("TIMESTAMP") && scale != null) return scale;
        return 0;
    }
}

record OracleEaKeyConstraint(
        String constraintName,
        String constraintType,
        List<String> columns,
        String status,
        String indexName
) {
    boolean primaryKey() {
        return "P".equalsIgnoreCase(constraintType);
    }

    boolean uniqueKey() {
        return "U".equalsIgnoreCase(constraintType);
    }
}

record OracleEaIndex(
        String indexName,
        boolean unique,
        String indexType,
        String status,
        List<OracleEaIndexColumn> columns
) {
}

record OracleEaIndexColumn(
        String columnName,
        int position,
        String descend
) {
}

record OracleEaCheckConstraint(
        String constraintName,
        String condition,
        String status
) {
}

record OracleEaForeignKey(
        String constraintName,
        String childOwner,
        String childTable,
        String parentOwner,
        String parentTable,
        String parentConstraintName,
        String parentConstraintType,
        String deleteRule,
        String status,
        String deferrable,
        String deferred,
        List<OracleEaForeignKeyColumn> columns
) {
}

record OracleEaForeignKeyColumn(
        String childColumn,
        String parentColumn,
        int position
) {
}

record OracleEaExportOptions(
        String schema,
        String tablePattern,
        boolean includeForeignKeys,
        boolean includeIndexes,
        boolean includeChecks,
        boolean includeComments,
        boolean includeExternalReferences
) {
}
