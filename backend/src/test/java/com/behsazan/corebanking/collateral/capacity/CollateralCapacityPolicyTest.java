package com.behsazan.corebanking.collateral.capacity;

import static org.junit.jupiter.api.Assertions.*;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.AllocationBucket;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** R4A regression: arithmetic evidence, currency isolation and permanent fail-closed authorization. */
class CollateralCapacityPolicyTest {
    private static CollateralCapacityPolicy.Input input(String status, String valuation,
            String validTo, BigDecimal eligible, List<AllocationBucket> allocations) {
        return new CollateralCapacityPolicy.Input(status, true, true,
                false, false, false, "IRR", "20", valuation, "IRR",
                "2026-01-01", validTo, "2026-11-03", "2025-01-01", null, eligible, allocations);
    }
    private static AllocationBucket bucket(String currency, String amount) {
        return new AllocationBucket("123", currency, 1, amount);
    }
    @Test void shortfallDoesNotDeleteHistoricalAllocations() {
        var result = CollateralCapacityPolicy.calculate(input("PLEDGED", "FINAL", "2027-01-01",
                new BigDecimal("50000000"), List.of(bucket("IRR", "60000000"))));
        assertEquals("60000000", result.sameCurrencyAllocated());
        assertEquals("-10000000", result.residual());
        assertEquals("10000000", result.shortfall());
        assertEquals(1, result.activeCount());
        assertTrue(result.blockers().contains("CLT_CAPACITY_RECORDED_VALUE_SHORTFALL"));
    }
    @Test void normalResidualIsNotAuthorization() {
        var result = CollateralCapacityPolicy.calculate(input("ACTIVE", "FINAL", "2027-01-01",
                new BigDecimal("100000000"), List.of(bucket("IRR", "60000000"))));
        assertEquals("40000000", result.residual());
        assertEquals("0", result.shortfall());
        assertTrue(result.blockers().contains("CLT_CAPACITY_APPROVED_FACTOR_AND_CONSUMER_EVIDENCE_REQUIRED"));
    }
    @Test void mixedCurrenciesNeverAddOrImplyFreeCapacity() {
        var result = CollateralCapacityPolicy.calculate(input("ACTIVE", "FINAL", "2027-01-01",
                new BigDecimal("200"), List.of(bucket("IRR", "20"), bucket("USD", "40"))));
        assertNull(result.residual());
        assertNull(result.shortfall());
        assertEquals("20", result.sameCurrencyAllocated());
        assertEquals(2, result.activeCount());
    }
    @Test void expiredValuationDoesNotEraseRecordedValue() {
        var result = CollateralCapacityPolicy.calculate(input("ACTIVE", "FINAL", "2026-01-01",
                new BigDecimal("200"), List.of(bucket("IRR", "300"))));
        assertFalse(result.periodProven());
        assertEquals("100", result.shortfall());
        assertTrue(result.blockers().contains("CLT_CAPACITY_CURRENT_VALUATION_PERIOD_UNPROVEN_OR_EXPIRED"));
    }
    @Test void noValuationMeansNoInventedValue() {
        var result = CollateralCapacityPolicy.calculate(input("ACTIVE", null, null,
                null, List.of(bucket("IRR", "100"))));
        assertNull(result.residual());
        assertTrue(result.blockers().contains("CLT_CAPACITY_RECORDED_ELIGIBLE_VALUE_MISSING"));
    }
    @Test void requestPrecisionAndDomainAreStrict() {
        assertEquals(new BigDecimal("0.0001"), CollateralCapacityPolicy.positiveMoney("0.0001"));
        assertThrows(IllegalArgumentException.class, () -> CollateralCapacityPolicy.positiveMoney("0"));
        assertThrows(IllegalArgumentException.class, () -> CollateralCapacityPolicy.storedMoney("0.00001"));
        assertThrows(IllegalArgumentException.class, () -> CollateralCapacityPolicy.storedMoney("10000000000000000"));
        assertThrows(IllegalArgumentException.class, () -> CollateralCapacityPolicy.storedMoney("10e7"));
        assertThrows(IllegalArgumentException.class, () -> CollateralCapacityPolicy.domain("OTHER"));
        assertThrows(IllegalArgumentException.class, () -> CollateralCapacityPolicy.currency("IRR' OR 1=1"));
    }
    @Test void scenariosOnlyCompareRawValues() {
        assertEquals("10000000", CollateralCapacityPolicy.hypotheticalShortfall("-8000000", "2000000"));
        assertEquals("0", CollateralCapacityPolicy.hypotheticalShortfall("60000000", "10000000"));
        assertNull(CollateralCapacityPolicy.hypotheticalShortfall(null, "100"));
    }
    @Test void statusAndNonFinalAreDenied() {
        var result = CollateralCapacityPolicy.calculate(input("SUSPENDED", "DRAFT", "2027-01-01",
                new BigDecimal("100"), List.of()));
        assertTrue(result.blockers().contains("CLT_CAPACITY_COLLATERAL_STATUS_NOT_ALLOCATABLE"));
        assertTrue(result.blockers().contains("CLT_CAPACITY_CURRENT_VALUATION_NOT_FINAL"));
    }
    @Test void sponsorInsuranceAndExclusiveFlagsNeverAssumedSatisfied() {
        var current = new CollateralCapacityPolicy.Input("ACTIVE", true, true, true, true, true,
                "IRR", "15", "FINAL", "IRR", "2026-01-01", "2027-01-01",
                "2026-11-03", "2025-01-01", null, BigDecimal.ONE, List.of());
        var blockers = CollateralCapacityPolicy.calculate(current).blockers();
        assertTrue(blockers.contains("CLT_CAPACITY_SPONSOR_VALIDITY_NOT_VERIFIED"));
        assertTrue(blockers.contains("CLT_CAPACITY_INSURANCE_COVERAGE_NOT_VERIFIED"));
        assertTrue(blockers.contains("CLT_CAPACITY_EXCLUSIVE_USAGE_CASE_NOT_VERIFIED"));
    }
}
