package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regression for R30-HF7-A3/HF3 child-rule numeric and dependent field semantics. */
class ProductRuleChildValidationHF7A3HF3Test {
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();

    private static Map<String,Object> term() {
        var row = new LinkedHashMap<String,Object>();
        row.put("TERM_UNIT_CODE", "MONTH");
        row.put("TERM_VALUE", 6);
        row.put("IS_ACTIVE", 1);
        row.put("IS_DEFAULT", 0);
        row.put("DISPLAY_ORDER", 1);
        return row;
    }

    private static Map<String,Object> tier() {
        var row = new LinkedHashMap<String,Object>();
        row.put("TIER_NO", 1);
        row.put("MIN_AMOUNT", 0);
        row.put("MAX_AMOUNT", 1000000);
        row.put("MIN_TERM_VALUE", 3);
        row.put("MAX_TERM_VALUE", 12);
        row.put("TERM_UNIT_CODE", "MONTH");
        row.put("RATE_VALUE", "0.18");
        return row;
    }

    private void bad(String table, Map<String,Object> row, String reason) {
        assertThatThrownBy(() -> validator.validate(table, row))
                .isInstanceOf(ProductBuilderValidationException.class)
                .hasMessageContaining(reason);
    }

    @Test void allowedTermRequiresPositiveIntegralDurationAndExplicitUnit() {
        var row = term();
        validator.validate("DEPOSIT_PRODUCT_ALLOWED_TERM", row);
        row.remove("TERM_UNIT_CODE");
        bad("DEPOSIT_PRODUCT_ALLOWED_TERM", row, "واحد مدت");
        row.put("TERM_UNIT_CODE", "HOUR");
        bad("DEPOSIT_PRODUCT_ALLOWED_TERM", row, "روز، ماه یا سال");
        row.put("TERM_UNIT_CODE", "MONTH");
        row.put("TERM_VALUE", 0);
        bad("DEPOSIT_PRODUCT_ALLOWED_TERM", row, "عدد صحیح مثبت");
        row.put("TERM_VALUE", "1.5");
        bad("DEPOSIT_PRODUCT_ALLOWED_TERM", row, "عدد صحیح");
        row.put("TERM_VALUE", 7);
        row.put("IS_DEFAULT", 2);
        bad("DEPOSIT_PRODUCT_ALLOWED_TERM", row, "بله");
        row.put("IS_DEFAULT", 0);
        row.put("DISPLAY_ORDER", "3.5");
        bad("DEPOSIT_PRODUCT_ALLOWED_TERM", row, "عدد صحیح");
    }

    @Test void allowedTermDatePeriodMustBeChronological() {
        var row = term();
        row.put("EFFECTIVE_FROM_DATE", "2026-10-19");
        row.put("EFFECTIVE_TO_DATE", "2026-10-18");
        bad("DEPOSIT_PRODUCT_ALLOWED_TERM", row, "تاریخ پایان");
    }

    @Test void rateTierRequiresRateAndValidFractionAndTermUnit() {
        var row = tier();
        validator.validate("PRODUCT_RATE_TIER", row);
        row.remove("RATE_VALUE");
        bad("PRODUCT_RATE_TIER", row, "نرخ پله");
        row.put("RATE_VALUE", "1.2");
        bad("PRODUCT_RATE_TIER", row, "صفر و یک");
        row.put("RATE_VALUE", "0.18");
        row.remove("TERM_UNIT_CODE");
        bad("PRODUCT_RATE_TIER", row, "واحد مدت");
        row.put("TERM_UNIT_CODE", "HOUR");
        bad("PRODUCT_RATE_TIER", row, "روز، ماه یا سال");
        row.put("TERM_UNIT_CODE", "MONTH");
        row.put("MIN_TERM_VALUE", "2.5");
        bad("PRODUCT_RATE_TIER", row, "عدد صحیح");
    }

    @Test void rateTierRejectsContradictionsWithoutInventingBounds() {
        var row = tier();
        row.put("MIN_TERM_VALUE", 18);
        bad("PRODUCT_RATE_TIER", row, "حداکثر مدت");
        row.put("MIN_TERM_VALUE", 3);
        row.put("MIN_AMOUNT", 1200000);
        bad("PRODUCT_RATE_TIER", row, "حداکثر");
        row.put("MIN_AMOUNT", 0);
        row.put("TIER_NO", "1.2");
        bad("PRODUCT_RATE_TIER", row, "عدد صحیح");
        row.put("TIER_NO", 1);
        row.remove("MIN_TERM_VALUE");
        row.remove("MAX_TERM_VALUE");
        row.remove("TERM_UNIT_CODE");
        validator.validate("PRODUCT_RATE_TIER", row); // amount-only tier is meaningful
        row.put("TERM_UNIT_CODE", "MONTH");
        bad("PRODUCT_RATE_TIER", row, "بدون تعیین");
    }
}
