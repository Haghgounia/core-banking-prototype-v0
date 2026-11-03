package com.behsazan.corebanking.collateral.reference;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure-Java whitelist / datatype / maker-checker rules, without a database dependency. */
public final class CollateralReferenceChangePolicy {
    public static final Set<String> BUSINESS_TABLES = Set.of(
            "COLLATERAL_GROUP", "COLLATERAL_TYPE", "COLLATERAL_ATTRIBUTE_DEFINITION",
            "COLLATERAL_TYPE_ATTRIBUTE", "COLLATERAL_INSURANCE_TYPE", "INSURANCE_COMPANY");
    private static final Set<String> MANAGED = Set.of("CREATED_AT", "CREATED_BY", "UPDATED_AT",
            "UPDATED_BY", "RECORD_VERSION");
    private static final Pattern NUMBER = Pattern.compile("NUMBER\\((\\d+),(\\d+)\\)");
    private static final Pattern TEXT = Pattern.compile("(?:N?VARCHAR2|CHAR)\\((\\d+)(?:\\s+(?:CHAR|BYTE))?\\)");
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{12,100}");
    private CollateralReferenceChangePolicy() { }

    public static String safeActor(String actor) {
        if (actor == null || actor.isBlank() || actor.length() > 100 || hasControl(actor))
            throw new IllegalArgumentException("CLT_INVALID_AUTHENTICATED_SUBJECT");
        return actor;
    }
    public static String reason(String value) {
        if (value == null || value.isBlank() || value.length() > 500 || hasControl(value))
            throw new IllegalArgumentException("CLT_REFERENCE_REASON_REQUIRED");
        return value.strip();
    }
    public static String idempotencyKey(String value) {
        if (value == null || !KEY.matcher(value).matches())
            throw new IllegalArgumentException("CLT_REFERENCE_IDEMPOTENCY_KEY_INVALID");
        return value;
    }
    public static String correlationId(String value) {
        if (value == null || value.isBlank()) return java.util.UUID.randomUUID().toString();
        if (value.length() > 100 || !value.matches("[A-Za-z0-9_-]{8,100}"))
            throw new IllegalArgumentException("CLT_REFERENCE_CORRELATION_ID_INVALID");
        return value;
    }
    public static void requireDistinctActors(String maker, String checker) {
        if (safeActor(maker).equals(safeActor(checker)))
            throw new IllegalArgumentException("CLT_REFERENCE_MAKER_CHECKER_SAME_ACTOR");
    }
    public static void requireTarget(String operation, String targetId, Long version) {
        if (!"CREATE".equals(operation) && !"UPDATE".equals(operation))
            throw new IllegalArgumentException("CLT_REFERENCE_ACTION_INVALID");
        if ("CREATE".equals(operation)) {
            if (targetId != null || version != null)
                throw new IllegalArgumentException("CLT_REFERENCE_CREATE_MUST_NOT_SET_ID_OR_VERSION");
        } else if (targetId == null || !targetId.matches("[1-9][0-9]{0,18}")
                || version == null || version < 1 || version > 9999999999L) {
            throw new IllegalArgumentException("CLT_REFERENCE_UPDATE_REQUIRES_TARGET_AND_VERSION");
        }
    }
    public static Map<String, Object> normalize(String table, String operation,
             String primaryKey, Map<String, String> definitions, Set<String> required,
             Map<String, Object> values) {
        if (!BUSINESS_TABLES.contains(table)) throw new IllegalArgumentException("CLT_REFERENCE_TABLE_READ_ONLY");
        if (!"CREATE".equals(operation) && !"UPDATE".equals(operation))
            throw new IllegalArgumentException("CLT_REFERENCE_ACTION_INVALID");
        if (values == null || values.isEmpty() || values.size() > definitions.size())
            throw new IllegalArgumentException("CLT_REFERENCE_VALUES_REQUIRED");
        Map<String, Object> validated = new LinkedHashMap<>();
        for (var entry : values.entrySet()) {
            String column = entry.getKey();
            String type = definitions.get(column);
            if (type == null || column.equals(primaryKey) || MANAGED.contains(column))
                throw new IllegalArgumentException("CLT_REFERENCE_COLUMN_DENIED");
            Object raw = entry.getValue();
            if (raw == null) {
                if (required.contains(column)) throw new IllegalArgumentException("CLT_REFERENCE_REQUIRED_COLUMN_NULL");
                validated.put(column, null);
                continue;
            }
            if (!(raw instanceof String || raw instanceof Number || raw instanceof Boolean))
                throw new IllegalArgumentException("CLT_REFERENCE_SCALAR_ONLY");
            String value = String.valueOf(raw);
            if (hasControl(value) || value.isBlank())
                throw new IllegalArgumentException("CLT_REFERENCE_EMPTY_OR_CONTROL_VALUE");
            Matcher number = NUMBER.matcher(type.toUpperCase(Locale.ROOT));
            Matcher text = TEXT.matcher(type.toUpperCase(Locale.ROOT));
            if (number.matches()) {
                try {
                    BigDecimal decimal = new BigDecimal(value);
                    int precision = Integer.parseInt(number.group(1));
                    int scale = Integer.parseInt(number.group(2));
                    if (decimal.scale() > scale || decimal.precision() - decimal.scale() > precision - scale)
                        throw new IllegalArgumentException("CLT_REFERENCE_NUMBER_OVERFLOW");
                    validated.put(column, decimal.toPlainString());
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("CLT_REFERENCE_NUMBER_INVALID", exception);
                }
            } else if (text.matches()) {
                int max = Integer.parseInt(text.group(1));
                if (value.codePointCount(0, value.length()) > max)
                    throw new IllegalArgumentException("CLT_REFERENCE_TEXT_TOO_LONG");
                validated.put(column, value);
            } else if (type.startsWith("DATE")) {
                try { validated.put(column, LocalDate.parse(value).toString()); }
                catch (DateTimeParseException e) { throw new IllegalArgumentException("CLT_REFERENCE_DATE_INVALID", e); }
            } else if (type.startsWith("TIMESTAMP")) {
                try { validated.put(column, LocalDateTime.parse(value).toString()); }
                catch (DateTimeParseException e) { throw new IllegalArgumentException("CLT_REFERENCE_TIMESTAMP_INVALID", e); }
            } else {
                throw new IllegalArgumentException("CLT_REFERENCE_UNSUPPORTED_TYPE");
            }
        }
        if ("CREATE".equals(operation)) {
            for (String key : required) {
                if (!key.equals(primaryKey) && !MANAGED.contains(key) && !validated.containsKey(key))
                    throw new IllegalArgumentException("CLT_REFERENCE_MISSING_REQUIRED_COLUMN: " + key);
            }
        }
        return validated;
    }
    public static Object jdbcValue(String type, Object value) {
        if (value == null) return null;
        if (type.startsWith("NUMBER")) return new BigDecimal(value.toString());
        if (type.startsWith("DATE")) return java.sql.Date.valueOf(value.toString());
        if (type.startsWith("TIMESTAMP")) return java.sql.Timestamp.valueOf(LocalDateTime.parse(value.toString()));
        return value.toString();
    }
    private static boolean hasControl(String value) {
        return value.chars().anyMatch(c -> Character.isISOControl(c) || c == '\u202e');
    }
}
