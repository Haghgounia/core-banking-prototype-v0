package com.behsazan.corebanking.collateral.monitoring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BUC-14: conservative, exact recorded-amount comparison of existing allocations.
 * This is a risk-monitoring signal, not a bank-approved collateral coverage ratio.
 * Cross-currency values and unapproved factors are NEVER silently normalized.
 */
public final class CollateralCoverageMonitoringPolicy {
    private CollateralCoverageMonitoringPolicy() {}

    public record Snapshot(String collateralId, String collateralNumber, String collateralStatus,
            String currencyCode, String valuationId, String valuationStatus,
            String valuationCurrency, String valuationFrom, String valuationTo,
            String asOfDate, BigDecimal eligibleValue, BigDecimal sameCurrencyExposure,
            long foreignCurrencyCount, long activeAllocationCount) {}

    public record Finding(String collateralId, String collateralNumber, String collateralStatus,
            String currencyCode, String valuationStatus, String valuationEffectiveTo,
            String asOfDate, String eligibleValue, String sameCurrencyAllocated,
            String recordedShortfall, long foreignCurrencyAllocationCount,
            long activeAllocationCount, String riskStatus, List<String> reasons,
            boolean newAllocationAuthorized) {}

    public static int size(int size) {
        if (size < 1 || size > 100) throw new IllegalArgumentException("CLT_MONITOR_SIZE_1_TO_100");
        return size;
    }
    public static int page(int page) {
        if (page < 0 || page > 100_000) throw new IllegalArgumentException("CLT_MONITOR_PAGE_OUT_OF_RANGE");
        return page;
    }
    static LocalDate day(String text) {
        if (text == null) return null;
        return LocalDate.parse(text);
    }
    static String money(BigDecimal value) {return value == null ? null : value.toPlainString();}

    public static Finding evaluate(Snapshot s) {
        if (s == null || s.activeAllocationCount() < 1 || s.foreignCurrencyCount() < 0
                || s.foreignCurrencyCount() > s.activeAllocationCount()
                || s.sameCurrencyExposure() == null || s.sameCurrencyExposure().signum() < 0
                || s.eligibleValue() != null && s.eligibleValue().signum() < 0)
            throw new IllegalArgumentException("CLT_MONITOR_INVALID_ORACLE_SNAPSHOT");
        var reasons = new ArrayList<String>();
        var currentDay = day(s.asOfDate());
        if (currentDay == null) throw new IllegalArgumentException("CLT_MONITOR_AS_OF_DATE_MISSING");
        LocalDate from = day(s.valuationFrom()), to = day(s.valuationTo());
        boolean currentValuation = s.valuationId() != null && "FINAL".equals(s.valuationStatus())
                && from != null && to != null && !currentDay.isBefore(from) && !currentDay.isAfter(to);
        if (!currentValuation) reasons.add("VALUATION_NOT_CURRENT_FINAL_OR_VALIDITY_UNPROVEN");
        if (s.eligibleValue() == null) reasons.add("RECORDED_ELIGIBLE_VALUE_MISSING");
        boolean currencyComparable = s.currencyCode() != null
                && s.currencyCode().matches("[A-Z]{3}")
                && s.currencyCode().equals(s.valuationCurrency());
        if (!currencyComparable) reasons.add("VALUATION_CURRENCY_DIFFERS_OR_UNKNOWN");
        if (s.foreignCurrencyCount() > 0) reasons.add("ACTIVE_ALLOCATIONS_IN_OTHER_CURRENCIES_FX_NOT_VERIFIED");
        BigDecimal rawDelta = currentValuation && currencyComparable && s.eligibleValue() != null
                ? s.sameCurrencyExposure().subtract(s.eligibleValue()) : null;
        String risk;
        String shortage = null;
        if (rawDelta != null && rawDelta.signum() > 0) {
            risk = "RECORDED_SHORTFALL";
            shortage = money(rawDelta);
            reasons.add("ACTIVE_SAME_CURRENCY_AMOUNT_EXCEEDS_RECORDED_ELIGIBLE_VALUE");
        } else if (!currentValuation || !currencyComparable || s.eligibleValue() == null) {
            risk = "VALUATION_REVIEW_REQUIRED";
        } else if (s.foreignCurrencyCount() > 0) {
            risk = "FX_REVIEW_REQUIRED";
        } else {
            risk = "NO_RECORDED_SHORTFALL_DETECTED";
        }
        // Even the favorable category is NOT proof of approved factor, case/debtor, insurance or coverage.
        reasons.add("BANK_APPROVED_FACTORS_AND_OBLIGATION_COVERAGE_NOT_ATTESTED");
        return new Finding(s.collateralId(), s.collateralNumber(), s.collateralStatus(),
                s.currencyCode(), s.valuationStatus(), s.valuationTo(), s.asOfDate(),
                money(s.eligibleValue()), money(s.sameCurrencyExposure()), shortage,
                s.foreignCurrencyCount(), s.activeAllocationCount(), risk, List.copyOf(reasons), false);
    }
}
