package com.behsazan.corebanking.collateral.insurance;

import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels.CandidateRequest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralInsurancePolicyTest {
    private static CandidateRequest sample() {
        return new CandidateRequest("2", "101", "200", "DEP-123", "P-2026-001",
                "9999999999999999.9999", "1200.25", "12.40",
                "2026-11-03", "2027-11-03", null, "IRR", "ثبت بیمه مشتری");
    }
    @Test void normalizationPreservesOracleNumber20PrecisionAndId19() {
        var c = CollateralInsurancePolicy.candidate(sample());
        assertEquals("9999999999999999.9999", c.insuranceAmount());
        assertEquals("1200.25", c.receivedAmount());
        assertEquals("9999999999999999999", CollateralInsurancePolicy.id("9999999999999999999"));
        assertThrows(IllegalArgumentException.class, () -> CollateralInsurancePolicy.id("10000000000000000000"));
        assertThrows(IllegalArgumentException.class, () -> CollateralInsurancePolicy.id("1e5"));
    }
    @Test void bankInsurerNotEnabledWithoutContractedInsurerApi() {
        var x = sample();
        assertThrows(IllegalArgumentException.class, () -> CollateralInsurancePolicy.candidate(
                new CandidateRequest("1", x.companyId(), x.insuranceTypeId(), x.depositNumber(),
                        x.insuranceNumber(), x.insuranceAmount(), x.receivedAmount(), x.taxAmount(),
                        x.validFrom(), x.validTo(), null, x.currencyCode(), x.justification())));
    }
    @Test void sameTypeInclusivePeriodOverlapAndAdjacentPeriods() {
        assertTrue(CollateralInsurancePolicy.overlaps("2026-01-01", "2026-12-31", "2026-12-31", "2027-01-10"));
        assertFalse(CollateralInsurancePolicy.overlaps("2026-01-01", "2026-12-31", "2027-01-01", "2027-12-31"));
        assertTrue(CollateralInsurancePolicy.overlaps("2026-01-01", "2026-12-31", "2026-04-01", "2026-06-01"));
    }
    @Test void renewalChainRequiresExactNextDaySameParentAndUnusedPrevious() {
        assertTrue(CollateralInsurancePolicy.validRenewal("101", "201", "101", "201",
                "2026-11-02", "2026-11-03", false));
        assertFalse(CollateralInsurancePolicy.validRenewal("101", "201", "102", "201",
                "2026-11-02", "2026-11-03", false));
        assertFalse(CollateralInsurancePolicy.validRenewal("101", "201", "101", "202",
                "2026-11-02", "2026-11-03", false));
        assertFalse(CollateralInsurancePolicy.validRenewal("101", "201", "101", "201",
                "2026-11-02", "2026-11-04", false));
        assertFalse(CollateralInsurancePolicy.validRenewal("101", "201", "101", "201",
                "2026-11-02", "2026-11-03", true));
    }
    @Test void invalidPeriodAndAmountsRejectedBeforeOracle() {
        var x=sample();
        assertThrows(IllegalArgumentException.class, () -> CollateralInsurancePolicy.candidate(
                new CandidateRequest(x.insurerCode(), x.companyId(), x.insuranceTypeId(), x.depositNumber(),
                        x.insuranceNumber(), x.insuranceAmount(), x.receivedAmount(), x.taxAmount(),
                        "2026-11-03", "2026-11-03", null, x.currencyCode(), x.justification())));
        for (String amount : new String[]{"1e10", "1.00001", "-1", "10000000000000000", ""})
            assertThrows(IllegalArgumentException.class, () -> CollateralInsurancePolicy.amount(amount, true, false));
    }
    @Test void invalidReferencesAndOverlapIndependentlyBlockStaging() {
        var errors = CollateralInsurancePolicy.blockers(false, false, "DRAFT",
                false, false, false, false, true, true, false);
        assertEquals(10, errors.size());
        assertTrue(errors.contains("CLT_INSURANCE_COLLATERAL_NOT_INSURABLE"));
        assertTrue(errors.contains("CLT_INSURANCE_RENEWAL_CHAIN_INVALID"));
        assertTrue(CollateralInsurancePolicy.blockers(true, true, "ACTIVE", true, true,
                true, true, false, false, true).isEmpty());
    }
    @Test void idempotencyHashDiffersForAnyChangedField() {
        var x=CollateralInsurancePolicy.candidate(sample());
        assertEquals(64, CollateralInsurancePolicy.digest(x.canonical()).length());
        assertNotEquals(CollateralInsurancePolicy.digest(x.canonical()),
                CollateralInsurancePolicy.digest(x.canonical() + "X"));
        assertThrows(IllegalArgumentException.class, () -> CollateralInsurancePolicy.key("short"));
        assertEquals("insurance-12345678", CollateralInsurancePolicy.key("insurance-12345678"));
    }
}
