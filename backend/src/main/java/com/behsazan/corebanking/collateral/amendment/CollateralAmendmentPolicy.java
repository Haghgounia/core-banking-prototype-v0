package com.behsazan.corebanking.collateral.amendment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Fixed allowlisted mutable fields; owner, type, number and start date are immutable. */
public final class CollateralAmendmentPolicy {
    private CollateralAmendmentPolicy() {}
    public static final Set<String> ALLOWED = Set.of("ESTIMATE_AMOUNT", "VALID_TO", "DEPOSIT_NUMBER", "REMARKS");

    public static String id(String text) {
        if (text == null || !text.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("CLT_G01_ID_INVALID");
        return text;
    }
    public static String version(String text) {
        if (text == null || !text.matches("[1-9][0-9]{0,9}")) throw new IllegalArgumentException("CLT_G01_VERSION_INVALID");
        return text;
    }
    public static String reason(String text) {
        if (text == null || text.strip().length() < 10 || text.strip().length() > 500)
            throw new IllegalArgumentException("CLT_G01_REASON_LENGTH");
        return text.strip();
    }
    public static Map<String, String> normalize(Map<String, String> before, Map<String, String> changes) {
        if (changes == null || changes.isEmpty() || changes.size() > ALLOWED.size())
            throw new IllegalArgumentException("CLT_G01_NO_CHANGES");
        Map<String, String> after = new LinkedHashMap<>(before);
        for (var entry : changes.entrySet()) {
            String field = entry.getKey();
            if (!ALLOWED.contains(field)) throw new IllegalArgumentException("CLT_G01_FIELD_IMMUTABLE:" + field);
            String raw = entry.getValue();
            String value = raw == null ? null : raw.strip();
            switch (field) {
                case "ESTIMATE_AMOUNT" -> {
                    if (value == null || !value.matches("[0-9]{1,16}(\\.[0-9]{1,4})?"))
                        throw new IllegalArgumentException("CLT_G01_AMOUNT_INVALID");
                    BigDecimal amt = new BigDecimal(value);
                    if (amt.signum() <= 0 || amt.precision() > 20 || amt.scale() > 4)
                        throw new IllegalArgumentException("CLT_G01_AMOUNT_INVALID");
                    value = amt.stripTrailingZeros().toPlainString();
                }
                case "VALID_TO" -> {
                    if (value != null && !value.isBlank()) {
                        try {
                            LocalDate until = LocalDate.parse(value);
                            if (until.isBefore(LocalDate.parse(before.get("VALID_FROM"))))
                                throw new IllegalArgumentException("CLT_G01_VALID_TO_BEFORE_START");
                        } catch (java.time.format.DateTimeParseException ex) {
                            throw new IllegalArgumentException("CLT_G01_DATE_INVALID");
                        }
                    } else value = null;
                }
                case "DEPOSIT_NUMBER" -> {
                    if (value != null && !value.isBlank() && !value.matches("[0-9]{1,15}"))
                        throw new IllegalArgumentException("CLT_G01_DEPOSIT_NUMBER_INVALID");
                    if (value != null && value.isBlank()) value = null;
                }
                case "REMARKS" -> {
                    if (value != null && value.length() > 150)
                        throw new IllegalArgumentException("CLT_G01_REMARKS_TOO_LONG");
                    if (value != null && value.isBlank()) value = null;
                }
                default -> throw new IllegalArgumentException("CLT_G01_FIELD_IMMUTABLE");
            }
            after.put(field, value);
        }
        // Changes must not be cosmetic or no-ops.
        boolean changed = false;
        for (String key : ALLOWED) if (!java.util.Objects.equals(before.get(key), after.get(key))) changed = true;
        if (!changed) throw new IllegalArgumentException("CLT_G01_NO_EFFECTIVE_CHANGE");
        return java.util.Collections.unmodifiableMap(after);
    }
    public static boolean amendableStatus(String status) { return "ACTIVE".equals(status); }
}
