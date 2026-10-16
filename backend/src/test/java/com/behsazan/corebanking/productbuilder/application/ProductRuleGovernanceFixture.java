package com.behsazan.corebanking.productbuilder.application;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal valid persisted PDL example rows for governance mock tests. */
final class ProductRuleGovernanceFixture {
    private ProductRuleGovernanceFixture() {}
    static Map<String, Object> row(String table) {
        Map<String, Object> v = new LinkedHashMap<>(Map.of(
                "RULE_STATUS_CODE", "ACTIVE", "CHANNEL_CODE", "BRANCH",
                "PRICING_PURPOSE_CODE", "SAVINGS", "CURRENCY_CODE", "IRR",
                "TRANSACTION_TYPE_CODE", "WITHDRAWAL", "CLOSURE_TYPE_CODE", "NORMAL"));
        switch (table) {
            case "DEPOSIT_PRODUCT_PROFILE" -> {
                v.put("PRODUCT_VERSION_ID", 55L);
                v.put("DEPOSIT_GROUP_CODE", "CURRENT");
                v.put("DEPOSIT_TYPE_CODE", "CURRENT_NO_INTEREST");
            }
            case "DEPOSIT_PROFIT_PAYMENT_RULE" -> {
                v.put("PRODUCT_VERSION_ID", 55L);
                v.put("PAYMENT_FREQUENCY_CODE", "MONTHLY");
                v.put("PAYMENT_DAY_RULE_CODE", "LAST_DAY_OF_MONTH");
                v.put("FIRST_PAYMENT_RULE_CODE", "PRORATE_FROM_OPENING");
                v.put("HOLIDAY_ADJUSTMENT_CODE", "NEXT_BUSINESS_DAY");
                v.put("PAYMENT_DESTINATION_CODE", "SAME_DEPOSIT");
                v.put("CAPITALIZATION_ALLOWED", 0);
                v.put("CAPITALIZATION_DEFAULT", 0);
                v.put("IS_ACTIVE", 1);
            }
            case "LOAN_PRODUCT_PROFILE" -> {
                v.put("PRODUCT_VERSION_ID", 55L);
                v.put("LOAN_TYPE_ID", 101L);
                v.put("PLAN_TYPE_ID", 201L);
                v.put("CURRENCY_TYPE_CODE", "IRR");
                v.put("PARTY_NATURE_CODE", "ALL");
            }
            case "PRODUCT_RELATIONSHIP" -> {
                v.put("SOURCE_PRODUCT_VERSION_ID", 55L);
                v.put("TARGET_PRODUCT_ID", 99L);
                v.put("RELATIONSHIP_TYPE_CODE", "PACKAGE");
                v.put("IS_MANDATORY", 0);
                v.put("PRIORITY_NO", 1);
            }
            case "PRODUCT_CHANNEL_RULE" -> {
                v.put("CHANNEL_RULE_ID", 811L); v.put("IS_ALLOWED", 0);
            }
            case "PRODUCT_ELIGIBILITY_RULE" -> v.put("ELIGIBILITY_RULE_ID", 812L);
            case "PRODUCT_PRICING_RULE" -> {
                v.put("PRICING_RULE_ID", 813L);
                v.put("PRICING_METHOD_CODE", "FIXED");
                v.put("IS_TIERED_RATE", 0);
                v.put("EARLY_TERMINATION_ALLOWED", 0);
            }
            case "DEPOSIT_PRODUCT_TERM_RULE" -> {
                v.put("TERM_RULE_ID", 712L);
                v.put("IS_ACTIVE", 1);
                v.put("IS_RENEWABLE", 0);
            }
            case "DEPOSIT_PRODUCT_ALLOWED_TERM" -> {
                v.put("TERM_RULE_ID", 712L);
                v.put("TERM_UNIT_CODE", "MONTH");
                v.put("TERM_VALUE", 6);
                v.put("IS_ACTIVE", 1);
                v.put("IS_DEFAULT", 1);
            }
            case "PRODUCT_PRICING_COMPONENT" -> {
                v.put("PRICING_COMPONENT_ID", 814L);
                v.put("PRICING_RULE_ID", 813L);
            }
            case "PRODUCT_RATE_TIER" -> {
                v.put("PRICING_COMPONENT_ID", 814L);
                v.put("TIER_NO", 1);
                v.put("RATE_VALUE", "0.12");
            }
            case "PRODUCT_REQUIRED_DOCUMENT" -> {
                v.put("DOCUMENT_TYPE_ID", 21);v.put("REQUIREMENT_STAGE_CODE", "OPENING");
                v.put("IS_MANDATORY", 1);
            }
            case "PRODUCT_REQUIRED_INQUIRY" -> {
                v.put("INQUIRY_TYPE_CODE", "KYC");v.put("REQUIREMENT_STAGE_CODE", "OPENING");
                v.put("IS_MANDATORY", 1);
            }
            case "PRODUCT_ORG_SCOPE" -> {
                v.put("ORG_UNIT_ID", 61);v.put("IS_ALLOWED", 1);
            }
            case "DEPOSIT_PRODUCT_OPENING_RULE" -> {
                v.put("IS_INTRODUCER_REQUIRED", 0);
                v.put("IS_OVERDRAFT_ALLOWED", 0);
                v.put("IS_STAMP_DUTY_APPLICABLE", 0);
            }
            case "DEPOSIT_PRODUCT_TRANSACTION_RULE" -> v.put("IS_ALLOWED", 0);
            case "DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA" -> {
                v.put("WITHDRAWAL_MEDIA_CODE", "CARD"); v.put("IS_ALLOWED", 0);
            }
            case "DEPOSIT_PRODUCT_JOINT_RULE" -> {
                v.put("OWNERSHIP_TYPE_CODE", "JOINT"); v.put("SIGNING_RULE_CODE", "ANY_TO_SIGN");
                v.put("MIN_JOINT_OWNER_COUNT", 2); v.put("MAX_JOINT_OWNER_COUNT", 3);
                v.put("MIN_REQUIRED_SIGNER_COUNT", 1);
            }
            case "DEPOSIT_PRODUCT_DORMANCY_RULE" -> {
                v.put("INACTIVITY_PERIOD_VALUE", 180); v.put("INACTIVITY_PERIOD_UNIT_CODE", "DAY");
                v.put("REACTIVATION_METHOD_CODE", "BRANCH"); v.put("AUTO_REACTIVATION_ALLOWED", 0);
            }
            case "DEPOSIT_PRODUCT_HOLD_RULE" -> {
                v.put("HOLD_TYPE_CODE", "LEGAL"); v.put("IS_SUPPORTED", 0);
            }
            case "DEPOSIT_PRODUCT_CLOSURE_RULE" -> {
                v.put("CLOSURE_RULE_ID", 818L);
                v.put("IS_CLOSURE_ALLOWED", 0); v.put("IS_PARTIAL_CLOSURE_ALLOWED", 0);
                v.put("STATUS_CODE", "ACTIVE"); v.put("EFFECTIVE_FROM_DATE", "2026-10-10");
            }
            case "LOAN_FINANCIAL_EXTENSION" -> {
                v.put("INTEREST_DISCOUNT_ALLOWED", 0); v.put("PENALTY_HOLIDAY_CALCULATION", 0);
            }
            case "LOAN_PRODUCT_REPAYMENT_RULE" -> {
                v.put("REPAYMENT_RULE_CODE", "EQUAL_INSTALLMENT");
                v.put("REPAYMENT_RULE_NAME", "اقساط مساوی"); v.put("REPAYMENT_METHOD_CODE", "EQUAL_INSTALLMENT");
                v.put("GRACE_ALLOWED", 0); v.put("PREPAYMENT_ALLOWED", 0);
                v.put("PARTIAL_PREPAYMENT_ALLOWED", 0); v.put("EARLY_SETTLEMENT_ALLOWED", 0);
            }
            case "LOAN_PRODUCT_PROCESS_RULE" -> {
                for (String k : new String[]{"MANUAL_APPROVAL_REQUIRED", "COMMITTEE_APPROVAL_REQUIRED",
                        "MULTI_LEVEL_APPROVAL_REQUIRED", "CBI_TRACKING_REQUIRED",
                        "EXTERNAL_INQUIRY_REQUIRED", "ALLOW_OVERRIDE"}) v.put(k, 0);
            }
            case "LOAN_PRODUCT_COLLATERAL_RULE" -> {
                v.put("LOAN_PRODUCT_COLLATERAL_ID", 701);
                v.put("PRIORITY_NO", 1);
                for (String k : new String[]{"IS_REQUIRED", "SUBSTITUTION_ALLOWED",
                        "THIRD_PARTY_ALLOWED", "CURRENCY_MATCH_REQUIRED"}) v.put(k, 0);
            }
            default -> { }
        }
        return v;
    }
}
