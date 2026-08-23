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
        List<EaColumnDefinition> columns,
        List<String> primaryKeyColumns
) {
}

record EaColumnDefinition(
        String columnName,
        String dataType,
        Integer length,
        Integer precision,
        Integer scale,
        Boolean nullable,
        String lengthSemantics,
        String defaultValue
) {
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
