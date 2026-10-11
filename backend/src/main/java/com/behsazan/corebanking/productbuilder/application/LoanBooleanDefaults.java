package com.behsazan.corebanking.productbuilder.application;

import java.util.Map;

/** New policy records: untouched loan switches must persist as explicit NO (0), not NULL. */
public final class LoanBooleanDefaults {
    private LoanBooleanDefaults() { }
    public static void apply(String table, Map<String, Object> values) {
        String[] fields = switch (table == null ? "" : table.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "LOAN_ELIGIBILITY_EXTENSION" -> new String[]{"CBI_TRACKING_REQUIRED", "CREDIT_INQUIRY_REQUIRED", "AML_CHECK_REQUIRED"};
            case "LOAN_FINANCIAL_EXTENSION" -> new String[]{"INTEREST_DISCOUNT_ALLOWED", "PENALTY_HOLIDAY_CALCULATION"};
            case "LOAN_PRODUCT_PROCESS_RULE" -> new String[]{"MANUAL_APPROVAL_REQUIRED", "COMMITTEE_APPROVAL_REQUIRED",
                    "MULTI_LEVEL_APPROVAL_REQUIRED", "CBI_TRACKING_REQUIRED", "EXTERNAL_INQUIRY_REQUIRED", "ALLOW_OVERRIDE"};
            case "LOAN_PRODUCT_COLLATERAL_RULE" -> new String[]{"IS_REQUIRED", "SUBSTITUTION_ALLOWED", "THIRD_PARTY_ALLOWED", "CURRENCY_MATCH_REQUIRED"};
            case "LOAN_PRODUCT_REPAYMENT_RULE" -> new String[]{"GRACE_ALLOWED", "PREPAYMENT_ALLOWED", "PARTIAL_PREPAYMENT_ALLOWED", "EARLY_SETTLEMENT_ALLOWED"};
            default -> new String[0];
        };
        for (String field : fields) {
            Object previous = values.get(field);
            if (previous == null || String.valueOf(previous).isBlank()) values.put(field, 0);
        }
    }
}
