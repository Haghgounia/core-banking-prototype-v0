package com.behsazan.corebanking.collateral.capacity;

import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.AllocationBucket;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure, conservative SVC-09 arithmetic. NEVER grants operational capacity.
 * `ALLOCATE_AMOUNT` is a stored amount, not necessarily a base-value equivalent;
 * official factor application and FX belong to the approved consuming-domain contract.
 */
public final class CollateralCapacityPolicy {
    private CollateralCapacityPolicy() {}
    public static final Set<String> DOMAINS = Set.of("LOAN", "GUARANTEE", "LETTER_OF_CREDIT", "GREEN_LICENSE");
    public static final List<String> POLICY_GAPS = List.of(
            "G-02_ALLOCATION_REQUEST_AND_IDEMPOTENCY_NOT_PERSISTED",
            "G-04_INSURANCE_REQUIREMENTS_BY_TYPE_NOT_GOVERNED",
            "G-05_G-11_VERIFIED_SPONSOR_LIFECYCLE_AND_MINIMUM_NOT_GOVERNED",
            "G-07_G-18_VALUATION_FINAL_STATUS_AND_REVALUATION_UNIT_UNRESOLVED",
            "G-09_CONSUMER_OBLIGATION_DEBTOR_AND_FX_NOT_VERIFIED",
            "G-10_PERCENT_USAGE_AND_RESERVE_FORMULA_NOT_BANK_APPROVED",
            "G-13_INTRODUCTION_LEVEL_CROSS_BRANCH_PERMISSION_NOT_PERSISTED",
            "R4A_NO_ATOMIC_ALLOCATION_LOCK_OR_WRITE_CONTRACT");

    public record Input(String collateralStatus, boolean activeReferenceStatus,
            boolean typeActive, boolean sponsorRequired, boolean insurable,
            boolean exclusiveUsage, String collateralCurrency, String valuationId,
            String valuationStatus, String valuationCurrency, String effectiveFrom,
            String effectiveTo, String asOfDate, String collateralValidFrom,
            String collateralValidTo, BigDecimal eligibleValue, List<AllocationBucket> buckets) {}

    public record Arithmetic(String sameCurrencyAllocated, String residual,
            String shortfall, boolean periodProven, long activeCount,
            List<String> blockers) {}

    public static BigDecimal storedMoney(String text) {
        if (text == null || !text.matches("(?:0|[1-9][0-9]{0,15})(?:\\.[0-9]{1,4})?"))
            throw new IllegalArgumentException("CLT_CAPACITY_DECIMAL_FORMAT_INVALID");
        BigDecimal amount = new BigDecimal(text);
        if (amount.precision() > 20 || amount.scale() > 4)
            throw new IllegalArgumentException("CLT_CAPACITY_NUMBER_20_4_EXCEEDED");
        return amount;
    }
    public static BigDecimal positiveMoney(String text) {
        BigDecimal value = storedMoney(text);
        if (value.signum() <= 0) throw new IllegalArgumentException("CLT_CAPACITY_AMOUNT_MUST_BE_POSITIVE");
        return value;
    }
    public static String domain(String input) {
        if (input == null || !DOMAINS.contains(input))
            throw new IllegalArgumentException("CLT_CAPACITY_CONSUMER_DOMAIN_INVALID");
        return input;
    }
    public static String currency(String input) {
        if (input == null || !input.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("CLT_CAPACITY_CURRENCY_INVALID");
        return input;
    }
    private static LocalDate date(String text) {
        try { return text == null ? null : LocalDate.parse(text); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("CLT_CAPACITY_ORACLE_DATE_INVALID"); }
    }
    public static String decimal(BigDecimal value) {return value == null ? null : value.toPlainString();}

    public static Arithmetic calculate(Input input) {
        List<String> blockers = new ArrayList<>();
        if (!input.typeActive()) blockers.add("CLT_CAPACITY_TYPE_NOT_ACTIVE");
        if (input.sponsorRequired()) blockers.add("CLT_CAPACITY_SPONSOR_VALIDITY_NOT_VERIFIED");
        if (input.insurable()) blockers.add("CLT_CAPACITY_INSURANCE_COVERAGE_NOT_VERIFIED");
        if (input.exclusiveUsage()) blockers.add("CLT_CAPACITY_EXCLUSIVE_USAGE_CASE_NOT_VERIFIED");
        if (!input.activeReferenceStatus() || !Set.of("ACTIVE", "PLEDGED", "PARTIAL_RELEASE")
                .contains(input.collateralStatus()))
            blockers.add("CLT_CAPACITY_COLLATERAL_STATUS_NOT_ALLOCATABLE");
        LocalDate day = date(input.asOfDate());
        if (day == null) throw new IllegalArgumentException("CLT_CAPACITY_CLOCK_MISSING");
        LocalDate collateralStart = date(input.collateralValidFrom());
        LocalDate collateralEnd = date(input.collateralValidTo());
        if (collateralStart == null || day.isBefore(collateralStart)
                || collateralEnd != null && day.isAfter(collateralEnd))
            blockers.add("CLT_CAPACITY_COLLATERAL_VALIDITY_NOT_CURRENT");
        if (input.valuationId() == null) blockers.add("CLT_CAPACITY_CURRENT_VALUATION_MISSING");
        else if (!"FINAL".equals(input.valuationStatus()))
            blockers.add("CLT_CAPACITY_CURRENT_VALUATION_NOT_FINAL");
        if (!input.collateralCurrency().equals(input.valuationCurrency()))
            blockers.add("CLT_CAPACITY_VALUATION_CURRENCY_MISMATCH_NO_FX");
        LocalDate from = date(input.effectiveFrom());
        LocalDate to = date(input.effectiveTo());
        boolean period = input.valuationId() != null && "FINAL".equals(input.valuationStatus())
                && from != null && to != null && !day.isBefore(from) && !day.isAfter(to);
        if (!period) blockers.add("CLT_CAPACITY_CURRENT_VALUATION_PERIOD_UNPROVEN_OR_EXPIRED");
        if (input.eligibleValue() == null)
            blockers.add("CLT_CAPACITY_RECORDED_ELIGIBLE_VALUE_MISSING");
        else if (input.eligibleValue().signum() < 0)
            throw new IllegalArgumentException("CLT_CAPACITY_NEGATIVE_ELIGIBLE_VALUE");

        BigDecimal sum = BigDecimal.ZERO;
        long count = 0;
        boolean mixed = false;
        for (AllocationBucket bucket : input.buckets()) {
            if (bucket.activeAllocationCount() < 1 || bucket.activeAllocationCount() > 1_000_000_000L)
                throw new IllegalArgumentException("CLT_CAPACITY_ALLOCATION_COUNT_INVALID");
            // SUM() of valid NUMBER(20,4) rows can exceed an individual row's precision.
            if (bucket.allocatedAmount() == null || !bucket.allocatedAmount().matches("[0-9]{1,45}(?:\\.[0-9]{1,4})?"))
                throw new IllegalArgumentException("CLT_CAPACITY_ALLOCATION_SUM_INVALID");
            BigDecimal amount = new BigDecimal(bucket.allocatedAmount());
            count = Math.addExact(count, bucket.activeAllocationCount());
            if (!input.collateralCurrency().equals(bucket.currencyCode())) mixed = true;
            else sum = sum.add(amount);
        }
        if (mixed) blockers.add("CLT_CAPACITY_ACTIVE_ALLOCATION_FX_UNRESOLVED");
        if (count > 0 && input.eligibleValue() == null)
            blockers.add("CLT_CAPACITY_EXISTING_ALLOCATION_WITHOUT_ELIGIBLE_VALUE");
        // Arithmetic can be displayed only when ALL active allocation currencies match.
        // It is NOT the official equivalent-base use of collateral and never authorizes a new allocation.
        BigDecimal delta = input.eligibleValue() == null || mixed ? null : input.eligibleValue().subtract(sum);
        if (delta != null && delta.signum() < 0) blockers.add("CLT_CAPACITY_RECORDED_VALUE_SHORTFALL");
        // The future module-specific factors cannot be inferred from R3C's stored percentages.
        blockers.add("CLT_CAPACITY_APPROVED_FACTOR_AND_CONSUMER_EVIDENCE_REQUIRED");
        return new Arithmetic(decimal(sum), decimal(delta),
                delta == null ? null : decimal(delta.signum() < 0 ? delta.negate() : BigDecimal.ZERO),
                period, count, List.copyOf(blockers));
    }

    public static String hypotheticalShortfall(String rawResidual, String requestedAmount) {
        if (rawResidual == null) return null;
        // Aggregate values may exceed the NUMBER(20,4) bounds of individual stored rows.
        if (!rawResidual.matches("-?[0-9]{1,45}(?:\\.[0-9]{1,4})?"))
            throw new IllegalArgumentException("CLT_CAPACITY_AGGREGATE_OUT_OF_RANGE");
        BigDecimal result = new BigDecimal(rawResidual).subtract(positiveMoney(requestedAmount));
        return decimal(result.signum() < 0 ? result.negate() : BigDecimal.ZERO);
    }
}
