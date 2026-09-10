package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductBuilderBusinessValidatorTest {
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();

    @Test
    void rejectsProfitPaymentDayOutsideFixedDayRule() {
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PROFIT_PAYMENT_RULE", Map.of(
                "PAYMENT_FREQUENCY_CODE", "MONTHLY",
                "PAYMENT_DAY_RULE_CODE", "OPENING_ANNIVERSARY",
                "PAYMENT_DAY_NO", 25,
                "CAPITALIZATION_ALLOWED", 0,
                "CAPITALIZATION_DEFAULT", 0
        ))).isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test
    void rejectsDefaultCapitalizationWhenCapitalizationIsNotAllowed() {
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PROFIT_PAYMENT_RULE", Map.of(
                "PAYMENT_FREQUENCY_CODE", "MONTHLY",
                "PAYMENT_DAY_RULE_CODE", "OPENING_ANNIVERSARY",
                "CAPITALIZATION_ALLOWED", 0,
                "CAPITALIZATION_DEFAULT", 1
        ))).isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test
    void rejectsInvalidCorrespondentCutoffTime() {
        assertThatThrownBy(() -> validator.validate("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE", Map.of(
                "CUTOFF_TIME", "25:00",
                "VALID_FROM", "2026-09-10"
        ))).isInstanceOf(ProductBuilderValidationException.class);
    }
}
