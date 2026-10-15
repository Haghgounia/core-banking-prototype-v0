package com.behsazan.corebanking.productbuilder.application;

import java.util.Locale;
import java.util.Map;

/**
 * Explicit server-side create defaults matching the reviewed PDL deposit form.
 * A new rule must persist an OFF switch without requiring a client toggle event.
 * Never changes an explicit zero (false), or existing records on update.
 */
final class DepositRuleDefaults {
    private DepositRuleDefaults() {}

    static void apply(String table, Map<String, Object> values) {
        if (table == null || values == null) return;
        switch (table.trim().toUpperCase(Locale.ROOT)) {
            case "DEPOSIT_PRODUCT_DORMANCY_RULE" -> fill(values, "AUTO_REACTIVATION_ALLOWED", 0);
            case "DEPOSIT_PRODUCT_CLOSURE_RULE" -> {
                fill(values, "IS_CLOSURE_ALLOWED", 1);
                fill(values, "IS_PARTIAL_CLOSURE_ALLOWED", 0);
            }
            case "DEPOSIT_PRODUCT_CLOSURE_PRECHECK", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE" -> {
                fill(values, "IS_MANDATORY", 1);
                fill(values, "SEQUENCE_NO", 1);
            }
            case "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE" -> {
                fill(values, "IS_MANDATORY", 1);
                fill(values, "APPROVAL_SEQUENCE_NO", 1);
            }
            default -> { }
        }
    }

    private static void fill(Map<String, Object> values, String key, Object fallback) {
        Object supplied = values.get(key);
        if (supplied == null || supplied instanceof String text && text.isBlank())
            values.put(key, fallback);
    }
}
