package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regression for the reviewed Persian loan forms; no database connection or seed data. */
class LoanProductFinalUxR25Test {
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();

    @Test void civilPartnershipIsNotEveryParticipatoryContract() {
        assertThat(LoanCivilPartnershipPolicy.matches(type("MOSHAREKAT_MADANI", "مشارکت مدنی", 3))).isTrue();
        assertThat(LoanCivilPartnershipPolicy.matches(type("123", "مشاركت مدني", 3))).isTrue();
        assertThat(LoanCivilPartnershipPolicy.matches(type("123", "مشارکت‌مدنی", 3))).isTrue();
        assertThat(LoanCivilPartnershipPolicy.matches(type("MODARABA", "مضاربه", 3))).isFalse();
        assertThat(LoanCivilPartnershipPolicy.matches(type("MOSHAREKAT_MADANI", "مشارکت مدنی", 2))).isFalse();
        assertThat(LoanCivilPartnershipPolicy.matches(type("OTHER", "مشارکت حقوقی", 3))).isFalse();
        assertThat(LoanCivilPartnershipPolicy.matches(Map.of())).isFalse();
    }

    /** Valid persisted rule data must include both explicit loan switches (HF7-A1).
     * Missing values are defaulted on CREATE by LoanBooleanDefaults, not by the
     * standalone semantic validator used for approval and this regression test. */
    private static Map<String, Object> financial() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("INTEREST_DISCOUNT_ALLOWED", 0);
        row.put("PENALTY_HOLIDAY_CALCULATION", 0);
        return row;
    }

    private static Map<String, Object> collateral(int priority) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("LOAN_PRODUCT_COLLATERAL_ID", 701); // Governed collateral type reference; not the rule PK.
        row.put("PRIORITY_NO", priority);
        row.put("IS_REQUIRED", 0);
        row.put("SUBSTITUTION_ALLOWED", 0);
        row.put("THIRD_PARTY_ALLOWED", 0);
        row.put("CURRENCY_MATCH_REQUIRED", 0);
        return row;
    }

    @Test void contributionStoredAsFractionAndAcceptsNoneOrBoundaries() {
        validator.validate("LOAN_FINANCIAL_EXTENSION", financial());
        var row = financial();
        row.put("MIN_CUSTOMER_CONTRIBUTION", BigDecimal.ZERO);
        row.put("MAX_CUSTOMER_CONTRIBUTION", BigDecimal.ONE);
        validator.validate("LOAN_FINANCIAL_EXTENSION", row);
        row.put("MIN_CUSTOMER_CONTRIBUTION", new BigDecimal("0.25"));
        row.put("MAX_CUSTOMER_CONTRIBUTION", new BigDecimal("0.75"));
        validator.validate("LOAN_FINANCIAL_EXTENSION", row);
    }

    @Test void invalidContributionFractionsAndOrderAreRejected() {
        var negative = financial();
        negative.put("MIN_CUSTOMER_CONTRIBUTION", new BigDecimal("-0.001"));
        negative.put("MAX_CUSTOMER_CONTRIBUTION", new BigDecimal("0.5"));
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", negative))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("منفی");
        var excessive = financial();
        excessive.put("MIN_CUSTOMER_CONTRIBUTION", BigDecimal.ZERO);
        excessive.put("MAX_CUSTOMER_CONTRIBUTION", new BigDecimal("1.001"));
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", excessive))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("صفر تا یک");
        var reversed = financial();
        reversed.put("MIN_CUSTOMER_CONTRIBUTION", new BigDecimal("0.7"));
        reversed.put("MAX_CUSTOMER_CONTRIBUTION", new BigDecimal("0.4"));
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", reversed))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("حداکثر");
        var unpaired = financial();
        unpaired.put("MIN_CUSTOMER_CONTRIBUTION", new BigDecimal("0.2"));
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", unpaired))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("حداقل و حداکثر");
    }

    @Test void interestDiscountDaysFollowTheExplicitPermissionSwitch() {
        var allowed = financial();
        allowed.put("INTEREST_DISCOUNT_ALLOWED", 1);
        allowed.put("INTEREST_DISCOUNT_DAY", 30);
        validator.validate("LOAN_FINANCIAL_EXTENSION", allowed);
        validator.validate("LOAN_FINANCIAL_EXTENSION", financial());
        var disallowed = financial();
        disallowed.put("INTEREST_DISCOUNT_DAY", 30);
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", disallowed))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("غیرفعال");
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", Map.of()))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("NULL مجاز نیست");
    }

    private static Map<String, Object> repayment() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("REPAYMENT_RULE_CODE", "EQUAL_INSTALLMENT");
        row.put("REPAYMENT_RULE_NAME", "اقساط مساوی");
        row.put("REPAYMENT_METHOD_CODE", "INSTALLMENT");
        row.put("GRACE_ALLOWED", 0);
        row.put("PREPAYMENT_ALLOWED", 0);
        row.put("PARTIAL_PREPAYMENT_ALLOWED", 0);
        row.put("EARLY_SETTLEMENT_ALLOWED", 0);
        return row;
    }

    @Test void allOptionalRepaymentPermissionsCanRemainOff() {
        validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", repayment());
        Map<String, Object> withDuration = repayment();
        withDuration.put("TERM_UNIT_CODE", "MONTH");
        withDuration.put("MIN_TERM_DURATION", 12);
        withDuration.put("MAX_TERM_DURATION", 36);
        validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", withDuration);
    }

    @Test void durationsNeedUnitAndPartialNeedsPrepayment() {
        final Map<String, Object> missingUnit = repayment();
        missingUnit.put("MIN_TERM_DURATION", 2);
        assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", missingUnit))
                .isInstanceOf(ProductBuilderValidationException.class);
        final Map<String, Object> partial = repayment();
        partial.put("PARTIAL_PREPAYMENT_ALLOWED", 1);
        assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", partial))
                .isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test void collateralRulePriorityZeroMeansUnspecifiedAndOneMeansFirst() {
        validator.validate("LOAN_PRODUCT_COLLATERAL_RULE", collateral(0));
        validator.validate("LOAN_PRODUCT_COLLATERAL_RULE", collateral(1));
        assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_COLLATERAL_RULE", collateral(-1)))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("غیرمنفی");
        var missingReference = collateral(0);
        missingReference.remove("LOAN_PRODUCT_COLLATERAL_ID");
        assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_COLLATERAL_RULE", missingReference))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining("شناسه صحیح مثبت");
    }

    private static Map<String, Object> type(String code, String name, int group) {
        return Map.of("LOAN_TYPE_CODE", code, "LOAN_TYPE_NAME", name, "LOAN_GROUP", group);
    }
}
