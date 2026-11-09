package com.behsazan.corebanking.collateral.lifecycle;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralR3LifecycleEvidencePolicyTest {
    private static final LocalDate NOW = LocalDate.of(2026, 11, 9);
    private static CollateralR3LifecycleEvidencePolicy.Facts sample(String pointer, String row,
            String state, LocalDate from, LocalDate to, String currency) {
        return new CollateralR3LifecycleEvidencePolicy.Facts("712", "1405123000001",
                "ACTIVE", "ACTIVE", "IRR", true, true,
                12, 4, 6, pointer, row, state, currency, from, to, NOW);
    }
    @Test void countersNeverImplyApproval() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "99", "FINAL",
                NOW.minusDays(10), NOW.plusDays(10), "IRR"));
        assertEquals(12, evidence.sponsor().recordedRows());
        assertEquals(4, evidence.insurance().recordedRows());
        assertEquals(6, evidence.valuation().recordedRows());
        assertEquals("NOT_VERIFIED", evidence.sponsor().validity());
        assertEquals("NOT_VERIFIED", evidence.insurance().validity());
        assertTrue(evidence.valuation().withinRecordedEffectivePeriod());
        assertFalse(evidence.valuation().eligibleForNewAllocation());
        assertFalse(evidence.eligibleForNewAllocation());
        assertFalse(evidence.finalRegistrationEnabled());
    }
    @Test void missingCurrentPointerFailsClosed() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample(null, null, null, null, null, null));
        assertTrue(evidence.valuation().evidenceBlockers().contains("CLT_R3_CURRENT_VALUATION_POINTER_MISSING"));
    }
    @Test void stalePointerFailsClosed() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "100", "FINAL",
                NOW.minusDays(2), NOW.plusDays(2), "IRR"));
        assertEquals("NOT_PROVEN", evidence.valuation().recordedPeriodState());
    }
    @Test void wrongStatusFailsClosed() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "99", "APPROVED",
                NOW.minusDays(2), NOW.plusDays(2), "IRR"));
        assertTrue(evidence.valuation().evidenceBlockers().contains("CLT_R3_CURRENT_VALUATION_NOT_FINAL"));
    }
    @Test void expiredPeriodIsEvidenceNotAuthorization() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "99", "FINAL",
                NOW.minusDays(20), NOW.minusDays(1), "IRR"));
        assertEquals("EXPIRED_RECORDED_PERIOD", evidence.valuation().recordedPeriodState());
        assertFalse(evidence.valuation().withinRecordedEffectivePeriod());
    }
    @Test void unknownEffectiveToIsNotAssumedValid() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "99", "FINAL",
                NOW.minusDays(20), null, "IRR"));
        assertTrue(evidence.valuation().evidenceBlockers().contains("CLT_R3_VALUATION_EFFECTIVE_TO_UNKNOWN"));
    }
    @Test void futurePeriodNotCurrent() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "99", "FINAL",
                NOW.plusDays(1), NOW.plusDays(10), "IRR"));
        assertFalse(evidence.valuation().withinRecordedEffectivePeriod());
    }
    @Test void currencyMismatchNeverConvertsSilently() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "99", "FINAL",
                NOW.minusDays(1), NOW.plusDays(1), "USD"));
        assertTrue(evidence.valuation().evidenceBlockers().contains("CLT_R3_CURRENT_VALUATION_CURRENCY_MISMATCH"));
    }
    @Test void unknownPolicyUnitsRemainOpen() {
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(sample("99", "99", "FINAL",
                NOW.minusDays(1), NOW.plusDays(1), "IRR"));
        assertTrue(evidence.unresolvedApprovalGates().contains("G-18"));
        assertTrue(evidence.valuation().policyGaps().stream().anyMatch(x->x.startsWith("G-18")));
    }
    @Test void typeFlagsAreNotApproval() {
        var f = new CollateralR3LifecycleEvidencePolicy.Facts("712", "100",
                "ACTIVE", "ACTIVE", "IRR", false, false, 3, 6, 0,
                null, null, null, null, null, null, NOW);
        var evidence = CollateralR3LifecycleEvidencePolicy.evaluate(f);
        assertEquals("NOT_REQUIRED_BY_TYPE", evidence.sponsor().applicability());
        assertEquals("NOT_INSURABLE_TYPE", evidence.insurance().applicability());
        assertFalse(evidence.eligibleForNewAllocation());
    }
    @Test void invalidCountsFailClosed() {
        var f = new CollateralR3LifecycleEvidencePolicy.Facts("712", "100",
                "ACTIVE", "ACTIVE", "IRR", true, true, -1, 6, 0,
                null, null, null, null, null, null, NOW);
        assertThrows(IllegalArgumentException.class, () -> CollateralR3LifecycleEvidencePolicy.evaluate(f));
    }
}
