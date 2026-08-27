package com.behsazan.corebanking.system.modelcomparison;

import java.util.List;

record EaXmiModel(
        String modelName,
        String exporter,
        String exporterVersion,
        String exportedAt,
        int rawTableDefinitionCount,
        List<EaTableDefinition> tables,
        List<String> warnings
) {
}

record EaTableDefinition(
        String tableName,
        int sourceDefinitionCount,
        String persianTitle,
        String documentation,
        List<EaColumnDefinition> columns,
        List<String> primaryKeyColumns,
        List<EaForeignKeyDefinition> foreignKeys,
        List<EaCheckConstraintDefinition> checkConstraints
) {
    EaTableDefinition(
            String tableName,
            int sourceDefinitionCount,
            String persianTitle,
            String documentation,
            List<EaColumnDefinition> columns,
            List<String> primaryKeyColumns
    ) {
        this(tableName, sourceDefinitionCount, persianTitle, documentation, columns, primaryKeyColumns, List.of(), List.of());
    }

    EaTableDefinition(String tableName, int sourceDefinitionCount, List<EaColumnDefinition> columns, List<String> primaryKeyColumns) {
        this(tableName, sourceDefinitionCount, null, null, columns, primaryKeyColumns, List.of(), List.of());
    }
}

record EaForeignKeyDefinition(
        String constraintName,
        List<String> childColumns,
        String parentTable,
        List<String> parentColumns
) {
    String displayDefinition() {
        String child = childColumns == null || childColumns.isEmpty() ? "?" : String.join(", ", childColumns);
        String parent = parentTable == null || parentTable.isBlank() ? "?" : parentTable;
        String parentCols = parentColumns == null || parentColumns.isEmpty() ? "?" : String.join(", ", parentColumns);
        return "FOREIGN KEY (" + child + ") REFERENCES " + parent + " (" + parentCols + ")";
    }
}

record EaCheckConstraintDefinition(
        String constraintName,
        String condition
) {
    String displayDefinition() {
        return condition == null || condition.isBlank() ? "CHECK (?)" : "CHECK (" + condition + ")";
    }
}

record EaColumnDefinition(
        String columnName,
        String dataType,
        Integer length,
        Integer precision,
        Integer scale,
        Boolean nullable,
        String lengthSemantics,
        String defaultValue,
        String comment
) {
    EaColumnDefinition(
            String columnName, String dataType, Integer length, Integer precision, Integer scale,
            Boolean nullable, String lengthSemantics, String defaultValue
    ) {
        this(columnName, dataType, length, precision, scale, nullable, lengthSemantics, defaultValue, null);
    }

    String displayType() {
        String normalized = dataType == null ? "?" : dataType;
        if ("NUMBER".equals(normalized) && precision != null && precision > 0) {
            return normalized + "(" + precision + "," + (scale == null ? 0 : scale) + ")";
        }
        if (("VARCHAR2".equals(normalized) || "NVARCHAR2".equals(normalized) || "CHAR".equals(normalized) || "RAW".equals(normalized))
                && length != null && length > 0) {
            String semantics = lengthSemantics == null || lengthSemantics.isBlank() ? "" : " " + lengthSemantics;
            return normalized + "(" + length + semantics + ")";
        }
        if (("TIMESTAMP".equals(normalized) || "TIMESTAMP WITH TIME ZONE".equals(normalized))
                && length != null && length >= 0) {
            return normalized.replace("TIMESTAMP", "TIMESTAMP(" + length + ")");
        }
        return normalized;
    }
}
