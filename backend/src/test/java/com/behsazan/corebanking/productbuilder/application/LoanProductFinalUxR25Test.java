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

    @Test void contributionStoredAsFractionAndAcceptsNoneOrBoundaries() {
        validator.validate("LOAN_FINANCIAL_EXTENSION", Map.of());
        validator.validate("LOAN_FINANCIAL_EXTENSION", Map.of(
                "MIN_CUSTOMER_CONTRIBUTION", BigDecimal.ZERO,
                "MAX_CUSTOMER_CONTRIBUTION", BigDecimal.ONE));
        validator.validate("LOAN_FINANCIAL_EXTENSION", Map.of(
                "MIN_CUSTOMER_CONTRIBUTION", new BigDecimal("0.25"),
                "MAX_CUSTOMER_CONTRIBUTION", new BigDecimal("0.75")));
    }

    @Test void invalidContributionFractionsAndOrderAreRejected() {
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", Map.of(
                "MIN_CUSTOMER_CONTRIBUTION", new BigDecimal("-0.001"))))
                .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", Map.of(
                "MAX_CUSTOMER_CONTRIBUTION", new BigDecimal("1.001"))))
                .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("LOAN_FINANCIAL_EXTENSION", Map.of(
                "MIN_CUSTOMER_CONTRIBUTION", new BigDecimal("0.7"),
                "MAX_CUSTOMER_CONTRIBUTION", new BigDecimal("0.4"))))
                .isInstanceOf(ProductBuilderValidationException.class);
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
        validator.validate("LOAN_PRODUCT_COLLATERAL_RULE", Map.of("PRIORITY_NO", 0));
        validator.validate("LOAN_PRODUCT_COLLATERAL_RULE", Map.of("PRIORITY_NO", 1));
        assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_COLLATERAL_RULE", Map.of("PRIORITY_NO", -1)))
                .isInstanceOf(ProductBuilderValidationException.class);
    }

    private static Map<String, Object> type(String code, String name, int group) {
        return Map.of("LOAN_TYPE_CODE", code, "LOAN_TYPE_NAME", name, "LOAN_GROUP", group);
    }
}
