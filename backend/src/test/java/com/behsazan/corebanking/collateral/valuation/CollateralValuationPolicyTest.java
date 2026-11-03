package com.behsazan.corebanking.collateral.valuation;

import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels.CandidateRequest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralValuationPolicyTest {
    private CandidateRequest sample() {
        return new CandidateRequest("PERIODIC","MARKET_COMPARISON","2026-11-03", "300000000.25",
                "IRR",null,"240000000","10.50",null,"کارشناس ارزیاب","EV-R3C-001",
                "2026-11-03","2027-11-03","500","درخواست تجدید ارزیابی");
    }
    @Test void exactMoneyAndIdentifierNeverUseDouble() {
        var c = CollateralValuationPolicy.normalize(sample());
        assertEquals("300000000.25",c.valuationAmount());
        assertEquals("240000000",c.eligibleValue());
        assertEquals("500",c.supersedesValuationId());
        assertEquals("9999999999999999999",CollateralValuationPolicy.id("9999999999999999999"));
    }
    @Test void oracleFinalNotUiApprovedOrSuperseded() {
        assertTrue(CollateralValuationPolicy.recordedPeriod("3","3","FINAL",
                "2026-01-01","2026-12-31","2026-11-03",true).withinRecordedPeriod());
        for (String status : new String[]{"APPROVED","DRAFT","SUBMITTED","SUPERSEDED"})
            assertFalse(CollateralValuationPolicy.recordedPeriod("3","3",status,
                "2026-01-01","2026-12-31","2026-11-03",true).withinRecordedPeriod());
    }
    @Test void expiredDoesNotInvalidatePastAllocationsAutomatically() {
        var record = CollateralValuationPolicy.recordedPeriod("3","3","FINAL",
                "2026-01-01","2026-11-03","2026-11-04",true);
        assertEquals("EXPIRED", record.state());
        assertFalse(record.withinRecordedPeriod());
        // This policy never writes to allocation/collateral tables.
    }
    @Test void missingEndAndPointerAreUnknownNotValid() {
        assertFalse(CollateralValuationPolicy.recordedPeriod("3","3","FINAL",
                "2026-01-01",null,"2026-11-03",true).withinRecordedPeriod());
        assertFalse(CollateralValuationPolicy.recordedPeriod(null,null,null,
                null,null,"2026-11-03",false).withinRecordedPeriod());
    }
    @Test void futureAndCurrencyMismatchAreNotValid() {
        assertFalse(CollateralValuationPolicy.recordedPeriod("3","3","FINAL",
                "2026-11-04","2027-12-31","2026-11-03",true).withinRecordedPeriod());
        assertFalse(CollateralValuationPolicy.recordedPeriod("3","3","FINAL",
                "2026-01-01","2027-12-31","2026-11-03",false).withinRecordedPeriod());
    }
    @Test void revaluationRequiresCurrentFinalPointer() {
        assertTrue(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,
                "ACTIVE","500",null,false).contains("CLT_VALUATION_CURRENT_MUST_BE_REFERENCED"));
        assertTrue(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,
                "ACTIVE","500","499",false).contains("CLT_VALUATION_SUPERSEDES_NOT_CURRENT_FINAL"));
    }
    @Test void invalidAmountCalendarAndHaircutRejected() {
        for (String money : new String[]{"1e9","-3","10000000000000000","1.12345"})
            assertThrows(IllegalArgumentException.class, () -> withAmount(money));
        assertThrows(IllegalArgumentException.class, () -> CollateralValuationPolicy.date("2026-02-29"));
        assertThrows(IllegalArgumentException.class, () -> CollateralValuationPolicy.id("10000000000000000000"));
    }
    private void withAmount(String money) {
        var x=sample();
        CollateralValuationPolicy.normalize(new CandidateRequest(x.typeCode(),x.methodCode(),x.valuationDate(),
                money,x.currencyCode(),x.forcedSaleValue(),x.eligibleValue(),x.haircutPercent(),
                x.valuerPartyId(),x.valuerName(),x.valuationReference(),x.effectiveFrom(),x.effectiveTo(),
                x.supersedesValuationId(),x.justification()));
    }
    @Test void exactDigestChangesWithPayload() {
        var c=CollateralValuationPolicy.normalize(sample());
        assertEquals(64,CollateralValuationPolicy.digest(c.canonical()).length());
        assertNotEquals(CollateralValuationPolicy.digest(c.canonical()),
                CollateralValuationPolicy.digest(c.canonical()+"Z"));
    }
    @Test void springRepositoryClassesRemainProxyable() {
        assertFalse(java.lang.reflect.Modifier.isFinal(CollateralValuationRepository.class.getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isFinal(CollateralValuationRequestRepository.class.getModifiers()));
    }

}
