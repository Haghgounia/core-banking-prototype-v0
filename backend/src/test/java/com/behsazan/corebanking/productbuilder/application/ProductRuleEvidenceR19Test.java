package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRuleEvidenceR19Test {
    private static final LocalDate START = LocalDate.of(2026, 10, 10);
    private static final LocalDate END = LocalDate.of(2027, 10, 10);

    @Test void inactiveRecordDoesNotSatisfyMandatoryRulePresence() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("PRODUCT_CHANNEL_RULE", List.of(
                Map.of("CHANNEL_CODE", "BRANCH", "RULE_STATUS_CODE", "INACTIVE")), START, END);
        assertThat(evidence.effectiveCount()).isZero();
        assertThat(evidence.historicalCount()).isEqualTo(1);
    }

    @Test void isActiveZeroDoesNotSatisfyDepositTermRequirement() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_TERM_RULE", List.of(
                Map.of("TERM_RULE_ID", 1, "IS_ACTIVE", 0)), START, END);
        assertThat(evidence.effectiveCount()).isZero();
    }

    @Test void forbiddenTransactionIsStillAConfiguredRuleAndNotInactiveRecord() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_TRANSACTION_RULE", List.of(
                Map.of("TRANSACTION_TYPE_CODE", "CASH_WITHDRAWAL", "IS_ALLOWED", 0)), START, END);
        assertThat(evidence.effectiveCount()).isEqualTo(1);
    }

    @Test void duplicateTransactionTypeIsAConflictEvenIfBothAllow() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_TRANSACTION_RULE", List.of(
                Map.of("TRANSACTION_TYPE_CODE", "CASH_WITHDRAWAL", "IS_ALLOWED", 1),
                Map.of("TRANSACTION_TYPE_CODE", "CASH_WITHDRAWAL", "IS_ALLOWED", 1)), START, END);
        assertThat(evidence.conflict()).isTrue();
    }

    @Test void overlappingPricingIntervalsForSamePurposeAndCurrencyConflict() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("PRODUCT_PRICING_RULE", List.of(
                Map.of("PRICING_PURPOSE_CODE", "PROFIT", "CURRENCY_CODE", "IRR", "VALID_FROM", "2026-10-01", "VALID_TO", "2027-01-01"),
                Map.of("PRICING_PURPOSE_CODE", "PROFIT", "CURRENCY_CODE", "IRR", "VALID_FROM", "2026-12-01", "VALID_TO", "2027-04-01")), START, END);
        assertThat(evidence.conflict()).isTrue();
    }

    @Test void nonOverlappingPricingIntervalsAreNotMisclassified() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("PRODUCT_PRICING_RULE", List.of(
                Map.of("PRICING_PURPOSE_CODE", "PROFIT", "CURRENCY_CODE", "IRR", "VALID_FROM", "2026-10-01", "VALID_TO", "2026-12-01"),
                Map.of("PRICING_PURPOSE_CODE", "PROFIT", "CURRENCY_CODE", "IRR", "VALID_FROM", "2027-01-01", "VALID_TO", "2027-04-01")), START, END);
        assertThat(evidence.conflict()).isFalse();
    }

    @Test void futureOnlyRuleDoesNotCoverVersionEffectiveStart() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("PRODUCT_CHANNEL_RULE", List.of(
                Map.of("CHANNEL_CODE", "MOBILE", "VALID_FROM", "2027-01-01")), START, END);
        assertThat(evidence.effectiveCount()).isZero();
    }

    @Test void unknownRuleStatusFailsClosed() {
        var evidence = ProductRuleEvidenceEvaluator.evaluate("PRODUCT_CHANNEL_RULE", List.of(
                Map.of("CHANNEL_CODE", "MOBILE", "RULE_STATUS_CODE", "UNMAPPED_STATUS")), START, END);
        assertThat(evidence.conflict()).isTrue();
        assertThat(evidence.unrecognizedStatuses()).isEqualTo(1);
    }
}
