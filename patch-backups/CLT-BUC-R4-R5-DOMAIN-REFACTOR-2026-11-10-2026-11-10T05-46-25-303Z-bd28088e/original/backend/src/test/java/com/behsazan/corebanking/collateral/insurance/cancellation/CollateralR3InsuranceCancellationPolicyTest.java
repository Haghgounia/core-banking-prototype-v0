package com.behsazan.corebanking.collateral.insurance.cancellation;

import com.behsazan.corebanking.collateral.insurance.cancellation.CollateralR3InsuranceCancellationPolicy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Unit tests: bank decisions and Oracle integration remain separate qualification gates. */
class CollateralR3InsuranceCancellationPolicyTest {
    private Facts fact(long allocated,long successors,boolean cancelled,boolean ledger) {
        return new Facts(true,true,true,"ACTIVE","2026-01-01","2026-12-31","2026-11-10",
                allocated,successors,cancelled,ledger,true,true);
    }
    @Test void neverAutomaticallyCancels() {
        var r=CollateralR3InsuranceCancellationPolicy.assess(fact(0,0,false,true));
        assertTrue(r.requestCandidate());
        assertFalse(r.automaticCancellationAllowed());
        assertTrue(r.historyPreserved());
        assertEquals(3,r.checks().stream().filter(x->x.result().equals("UNKNOWN")).count());
    }
    @Test void allocationCannotBeInferredCleared() {
        var r=CollateralR3InsuranceCancellationPolicy.assess(fact(1,0,false,true));
        assertFalse(r.knownAllocationFree());
        assertTrue(r.checks().stream().anyMatch(x->x.code().equals("NO_ALLOCATION_ROWS") && x.result().equals("BLOCKED")));
    }
    @Test void cancellationAlreadyRecordedStopsStage() {
        assertFalse(CollateralR3InsuranceCancellationPolicy.assess(fact(0,0,true,true)).requestCandidate());
        assertFalse(CollateralR3InsuranceCancellationPolicy.assess(fact(0,0,false,false)).requestCandidate());
    }
    @Test void renewalRequiresIndependentDecision() {
        assertFalse(CollateralR3InsuranceCancellationPolicy.assess(fact(0,1,false,true)).requestCandidate());
    }
    @Test void onlyMakerMaySubmitOrWithdraw() {
        assertDoesNotThrow(()->CollateralR3InsuranceCancellationPolicy.requireStageTransition(
                "DRAFT","SUBMIT","MAKER_ACTOR",null,"MAKER_ACTOR",true));
        assertThrows(SecurityException.class,()->CollateralR3InsuranceCancellationPolicy.requireStageTransition(
                "DRAFT","SUBMIT","MAKER_ACTOR",null,"OTHER_ACTOR",true));
    }
    @Test void reviewerMustBeIndependent() {
        assertThrows(SecurityException.class,()->CollateralR3InsuranceCancellationPolicy.requireStageTransition(
                "SUBMITTED","REVIEW","MAKER_ACTOR",null,"MAKER_ACTOR",true));
        assertDoesNotThrow(()->CollateralR3InsuranceCancellationPolicy.requireStageTransition(
                "SUBMITTED","REVIEW","MAKER_ACTOR",null,"CHECKER_ACTOR",true));
    }
    @Test void inputValidationFailClosed() {
        assertThrows(IllegalArgumentException.class,()->CollateralR3InsuranceCancellationPolicy.key("short"));
        assertThrows(IllegalArgumentException.class,()->CollateralR3InsuranceCancellationPolicy.id("0"));
        assertThrows(IllegalArgumentException.class,()->CollateralR3InsuranceCancellationPolicy.reason("insufficient".substring(0,4)));
    }
    @Test void postingMustBeBankGated() {
        var cmd=new Command("3","clt-cancel-20261110","Business reason documented");
        var stage=new Stage("REVIEWED","MAKER_ACTOR","CHECKER_ACTOR","POSTER_ACTOR",
                "a".repeat(64),"a".repeat(64));
        var release=new Release("POLICY_REVISION_20261110","BUSINESS_DECISION_1234","DATA_DECISION_1234",
                "COVERAGE_DECISION_1234","ALLOCATION_DECISION_1234","INTEGRATION_DECISION_1234",
                "DOWNSTREAM_DECISION_1234","APPROVER_ACTOR",true);
        var impact=new Impact("IMPACT_DECISION_1234","COVERAGE_DECISION_1234",
                "ALLOCATION_DECISION_1234","RISK_APPROVER","123",true);
        var assessment=CollateralR3InsuranceCancellationPolicy.assess(fact(0,0,false,true));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCancellationPolicy.requirePosting(
                cmd,stage,release,impact,assessment,false,true,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCancellationPolicy.requirePosting(
                cmd,stage,release,impact,assessment,true,true,true,false,true));
        assertDoesNotThrow(()->CollateralR3InsuranceCancellationPolicy.requirePosting(
                cmd,stage,release,impact,assessment,true,true,true,true,true));
    }
}
