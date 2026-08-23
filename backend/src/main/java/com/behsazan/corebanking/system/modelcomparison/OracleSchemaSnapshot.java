package com.behsazan.corebanking.system.modelcomparison;

import java.util.List;
import java.util.Locale;
import java.util.Map;

record OracleSchemaSnapshot(
        String schemaName,
        Map<String, OracleTableDefinition> tables
) {
}

record OracleTableDefinition(
        String tableName,
        List<OracleColumnDefinition> columns,
        List<String> primaryKeyColumns,
        Long rowCount,
        String rowCountNote
) {
}

record OracleColumnDefinition(
        String columnName,
        String dataType,
        Integer dataLength,
        Integer charLength,
        String charUsed,
        Integer precision,
        Integer scale,
        boolean nullable,
        int position
) {
    String normalizedDataType() {
        if (dataType == null) return null;
        return dataType.toUpperCase(Locale.ROOT)
                .replaceAll("TIMESTAMP\\(\\d+\\) WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE")
                .replaceAll("TIMESTAMP\\(\\d+\\) WITH LOCAL TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE")
                .replaceAll("TIMESTAMP\\(\\d+\\)", "TIMESTAMP");
    }

    String displayType() {
        return displayType(true);
    }

    String displayType(boolean includeLengthSemantics) {
        String type = normalizedDataType();
        if (type == null) return "?";
        if ("NUMBER".equals(type) && precision != null) {
            return type + "(" + precision + "," + (scale == null ? 0 : scale) + ")";
        }
        if ("VARCHAR2".equals(type) || "NVARCHAR2".equals(type) || "CHAR".equals(type)) {
            Integer len = charLength != null && charLength > 0 ? charLength : dataLength;
            if (len != null && len > 0) {
                String semantics = includeLengthSemantics
                        ? ("C".equalsIgnoreCase(charUsed) ? " CHAR" : "B".equalsIgnoreCase(charUsed) ? " BYTE" : "")
                        : "";
                return type + "(" + len + semantics + ")";
            }
        }
        if ("RAW".equals(type) && dataLength != null) {
            return type + "(" + dataLength + ")";
        }
        if (("TIMESTAMP".equals(type) || "TIMESTAMP WITH TIME ZONE".equals(type) || "TIMESTAMP WITH LOCAL TIME ZONE".equals(type))
                && scale != null) {
            return type.replace("TIMESTAMP", "TIMESTAMP(" + scale + ")");
        }
        return type;
    }
}
