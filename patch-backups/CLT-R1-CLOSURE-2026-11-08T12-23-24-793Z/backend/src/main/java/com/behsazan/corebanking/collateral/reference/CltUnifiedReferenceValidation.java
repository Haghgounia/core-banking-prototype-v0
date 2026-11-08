package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Column;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Table;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Typed, catalog-allowlisted validation for local UAT reference CRUD on all 17 physical tables. */
final class CltUnifiedReferenceValidation {
    private static final Set<String> MANAGED = Set.of("CREATED_AT", "CREATED_BY", "UPDATED_AT",
            "UPDATED_BY", "RECORD_VERSION");
    private static final Pattern NUMBER = Pattern.compile("NUMBER\\((\\d+),(\\d+)\\)");
    private static final Pattern TEXT = Pattern.compile("(?:N?VARCHAR2|CHAR)\\((\\d+)(?:\\s+(?:CHAR|BYTE))?\\)");
    private CltUnifiedReferenceValidation() { }

    static Map<String, Object> normalized(Table table, Map<String, Object> values, boolean create) {
        if (values == null || values.isEmpty() || values.size() > table.columns().size())
            throw new IllegalArgumentException("CLT_UNIFIED_VALUES_REQUIRED");
        Map<String, Column> names = new LinkedHashMap<>();
        for (Column c : table.columns()) names.put(c.name(), c);
        Map<String, Object> safe = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Column column = names.get(entry.getKey());
            if (column == null || column.primaryKey() || MANAGED.contains(column.name()))
                throw new IllegalArgumentException("CLT_UNIFIED_COLUMN_DENIED");
            Object raw = entry.getValue();
            if (raw == null) {
                if (column.required()) throw new IllegalArgumentException("CLT_UNIFIED_REQUIRED_NULL");
                safe.put(column.name(), null);
                continue;
            }
            if (!(raw instanceof String || raw instanceof Number || raw instanceof Boolean))
                throw new IllegalArgumentException("CLT_UNIFIED_SCALAR_ONLY");
            String value = raw.toString();
            if (value.isBlank() || value.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("CLT_UNIFIED_EMPTY_OR_CONTROL");
            String type = column.dataType();
            Matcher numeric = NUMBER.matcher(type);
            Matcher text = TEXT.matcher(type);
            if (numeric.matches()) {
                final BigDecimal decimal;
                try { decimal = new BigDecimal(value); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("CLT_UNIFIED_NUMBER_INVALID", ex); }
                int p = Integer.parseInt(numeric.group(1));
                int s = Integer.parseInt(numeric.group(2));
                if (decimal.scale() > s || decimal.precision() - decimal.scale() > p - s)
                    throw new IllegalArgumentException("CLT_UNIFIED_NUMBER_OVERFLOW");
                safe.put(column.name(), decimal);
            } else if (text.matches()) {
                if (value.codePointCount(0, value.length()) > Integer.parseInt(text.group(1)))
                    throw new IllegalArgumentException("CLT_UNIFIED_TEXT_OVERFLOW");
                safe.put(column.name(), value);
            } else {
                throw new IllegalArgumentException("CLT_UNIFIED_UNSUPPORTED_TYPE");
            }
        }
        if (create) {
            for (Column column : table.columns()) {
                if (column.required() && !column.primaryKey() && !MANAGED.contains(column.name())
                        && !safe.containsKey(column.name()))
                    throw new IllegalArgumentException("CLT_UNIFIED_MISSING_REQUIRED_FIELD");
            }
        }
        return safe;
    }

    static BigDecimal id(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_UNIFIED_INVALID_ID");
        return new BigDecimal(raw);
    }

    static long version(long raw) {
        if (raw < 1 || raw > 9_999_999_999L)
            throw new IllegalArgumentException("CLT_UNIFIED_INVALID_VERSION");
        return raw;
    }
}
