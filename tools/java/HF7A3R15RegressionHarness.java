package com.behsazan.corebanking.productbuilder.application;

import java.math.BigDecimal;
import java.util.Map;

/** Real production validation, no Spring container, SQL, or mocking. */
public final class HF7A3R15RegressionHarness {
    private static final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();
    private static int pass;

    private static void good(String name, Map<String,Object> row) {
        validator.validate("PRODUCT_PRICING_RULE", row);
        pass(name);
    }
    private static void bad(String name, String expectedReason, Map<String,Object> row) {
        try {
            validator.validate("PRODUCT_PRICING_RULE", row);
        } catch (ProductBuilderValidationException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains(expectedReason))
                throw new AssertionError(name + ": incorrect reason: " + ex.getMessage());
            pass(name);
            return;
        }
        throw new AssertionError(name + ": expected ProductBuilderValidationException");
    }
    private static void pass(String name) { pass++; System.out.println("PASS | " + name); }

    public static void main(String[] args) {
        good("non-tiered fixed rate, no early rate, flags zero", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", 0, "EARLY_TERMINATION_ALLOWED", 0));
        good("early termination enabled with fractional rate", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", 0, "EARLY_TERMINATION_ALLOWED", 1, "EARLY_TERMINATION_RATE", 0.12));
        bad("early termination disallowed but rate supplied", "هنگامی که خاتمه پیش از موعد مجاز نیست", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", 0, "EARLY_TERMINATION_ALLOWED", 0, "EARLY_TERMINATION_RATE", 0.12));
        bad("early termination rate greater than 1", "نرخ خاتمه زودهنگام باید بین صفر و یک باشد", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", 0, "EARLY_TERMINATION_ALLOWED", 1, "EARLY_TERMINATION_RATE", 1.2));
        bad("missing tier flag is rejected", "پلکانی بودن نرخ", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "EARLY_TERMINATION_ALLOWED", 0));
        bad("invalid tier flag 9 is rejected", "پلکانی بودن نرخ", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", 9, "EARLY_TERMINATION_ALLOWED", 0));
        good("Oracle-style decimal zero for tier flag", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", new BigDecimal("0.0"), "EARLY_TERMINATION_ALLOWED", new BigDecimal("0.0")));
        good("Oracle-style decimal one for early flag", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", new BigDecimal("0.0"), "EARLY_TERMINATION_ALLOWED", new BigDecimal("1.0"),
                "EARLY_TERMINATION_RATE", new BigDecimal("0.12")));
        good("numeric-string decimal one for early flag", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", "0.0", "EARLY_TERMINATION_ALLOWED", "1.0",
                "EARLY_TERMINATION_RATE", new BigDecimal("0.12")));
        bad("missing early-termination flag is rejected", "مجاز بودن خاتمه زودهنگام", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "IS_TIERED_RATE", 0));
        if (pass != 10) throw new AssertionError("Unexpected number of assertions: " + pass);
        System.out.println("PDL_R30_HF7A3_HF5_HF1_R15_JAVA_PASS=" + pass + " FAIL=0");
    }
}
