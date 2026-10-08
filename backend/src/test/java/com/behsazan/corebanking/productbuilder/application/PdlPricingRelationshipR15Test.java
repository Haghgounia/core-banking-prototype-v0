package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PB-R15: executable regression for the governed business forms. No Oracle required. */
class PdlPricingRelationshipR15Test {
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();
    private static SelectOption option(String code) {return new SelectOption(code, code, code);}

    @Test
    void reviewedPricingSelectorsMatchBankingContract() {
        assertThat(PdlReferenceOptionService.businessFormOptions("PRODUCT_PRICING_RULE", "PRICING_PURPOSE_CODE", List.of()))
                .extracting(SelectOption::code).containsExactly("DEPOSIT_PROFIT", "LOAN_INTEREST", "COMMISSION", "PENALTY");
        assertThat(PdlReferenceOptionService.businessFormOptions("PRODUCT_PRICING_RULE", "PRICING_METHOD_CODE", List.of()))
                .extracting(SelectOption::code).containsExactly("FIXED", "FLOATING", "TIERED", "FORMULA");
        assertThat(PdlReferenceOptionService.businessFormOptions("PRODUCT_PRICING_RULE", "SETTLEMENT_FREQUENCY_CODE", List.of()))
                .extracting(SelectOption::code).containsExactly("MONTHLY", "QUARTERLY", "ANNUAL", "MATURITY");
        assertThat(PdlReferenceOptionService.businessFormOptions("PRODUCT_PRICING_RULE", "DESTINATION_RULE_CODE", List.of()))
                .extracting(SelectOption::code).containsExactly("SINGLE_ACCOUNT", "MULTIPLE_ACCOUNTS", "CUSTOMER_SELECTED", "SYSTEM_DEFINED");
    }

    @Test
    void databaseCheckConstraintAlwaysNarrowsAvailableOptions() {
        assertThat(PdlReferenceOptionService.businessFormOptions("PRODUCT_PRICING_RULE", "PRICING_METHOD_CODE",
                List.of(option("FIXED"), option("UNSUPPORTED")))).extracting(SelectOption::code).containsExactly("FIXED");
        assertThat(PdlReferenceOptionService.businessFormOptions("PRODUCT_PRICING_RULE", "PRICING_METHOD_CODE",
                List.of(option("UNSUPPORTED")))).isEmpty();
    }

    @Test
    void relationshipUsesActualProductBuilderCodesNotGenericDpsFallback() {
        assertThat(PdlReferenceOptionService.businessFormOptions("PRODUCT_RELATIONSHIP", "RELATIONSHIP_TYPE_CODE", List.of()))
                .extracting(SelectOption::code).containsExactly("REQUIRES", "BUNDLE", "RATE_DEPENDENCY", "COLLATERAL_ACCOUNT");
    }

    @Test
    void earlyTerminationRateIsConditionalAndBounded() {
        Map<String, Object> noEarly = Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "EARLY_TERMINATION_ALLOWED", 0, "EARLY_TERMINATION_RATE", 0.12);
        assertThatThrownBy(() -> validator.validate("PRODUCT_PRICING_RULE", noEarly))
                .isInstanceOf(ProductBuilderValidationException.class);
        Map<String, Object> earlyAllowed = Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "EARLY_TERMINATION_ALLOWED", 1, "EARLY_TERMINATION_RATE", 0.12);
        assertThatCode(() -> validator.validate("PRODUCT_PRICING_RULE", earlyAllowed)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate("PRODUCT_PRICING_RULE", Map.of(
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "PRICING_METHOD_CODE", "FIXED",
                "EARLY_TERMINATION_ALLOWED", 1, "EARLY_TERMINATION_RATE", 1.2)))
                .isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test
    void relationshipPriorityAndAmountBoundsAreValidated() {
        Map<String, Object> normal = Map.of("TARGET_PRODUCT_ID", 42, "PRIORITY_NO", 1,
                "MIN_RELATION_AMOUNT", 1000, "MAX_RELATION_AMOUNT", 2000);
        assertThatCode(() -> validator.validate("PRODUCT_RELATIONSHIP", normal)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate("PRODUCT_RELATIONSHIP", Map.of("TARGET_PRODUCT_ID", 42, "PRIORITY_NO", 0)))
                .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("PRODUCT_RELATIONSHIP", Map.of("TARGET_PRODUCT_ID", 42,
                "PRIORITY_NO", 1, "MIN_RELATION_AMOUNT", 2000, "MAX_RELATION_AMOUNT", 1000)))
                .isInstanceOf(ProductBuilderValidationException.class);
    }
}
