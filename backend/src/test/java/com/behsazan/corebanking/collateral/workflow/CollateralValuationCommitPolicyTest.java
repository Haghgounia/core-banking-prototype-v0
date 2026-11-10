package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralValuationCommitPolicy.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralValuationCommitPolicyTest {
    private final Command command=new Command("3","valuation.final.20261109.01","Documented valuation approval");
    private final Release release=new Release("VAL-20261109-POLICY","G07-FINAL-MAPPING",
            "G18-APPRAISAL-DECISION","G18-INTERVAL-DECISION","G18-TEHRAN-DATE-DOC",
            "G09-VALUER-AUTH-REF","G10-VALUE-POLICY-REF","G07-SUPERSESSION-REF",
            "BANK-APPROVER-01",true);
    private final StageEvidence stage=new StageEvidence("REVIEWED","3",true,true,false,"maker","poster");
    private final TypeRule rule=new TypeRule("24",true,1,"MONTH","EXCLUSIVE_NEXT_START",
            "EFFECTIVE_FROM","VAL-20261109-POLICY","RULE-APPROVER-01",true);
    private final ValuerRule valuer=new ValuerRule("42","24","MARKET","VAL-20261109-POLICY",
            "VALUER-APPROVER-01",true);
    private CollateralValuationPolicy.Candidate candidate(String amount,String until,String prior) {
        return CollateralValuationPolicy.normalize(new CollateralValuationModels.CandidateRequest(
                "INITIAL","MARKET","2026-10-20",amount,"IRR","900000","750000","25",
                "42","Certified Valuer","APPRAISAL-REF-2026-01","2026-10-21",until,prior,
                "Documented value for the collateral"));
    }
    @Test void approvedReleaseAndIndependentPosterAreMandatory() {
        assertDoesNotThrow(()->CollateralValuationCommitPolicy.guard(command,stage,release,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.guard(command,stage,null,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.guard(command,stage,release,false,true,true));
        assertThrows(SecurityException.class,()->CollateralValuationCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",true,true,false,"maker","maker"),release,true,true,true));
    }
    @Test void approvedIntervalControlsEffectiveToWithoutInventedDefault() {
        assertEquals(LocalDate.parse("2026-11-20"),CollateralValuationCommitPolicy.requirePeriod(
                rule,"24",candidate("1000000","2026-11-20",null),release,"poster",LocalDate.parse("2026-11-09")));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.requirePeriod(
                rule,"24",candidate("1000000","2026-11-21",null),release,"poster",LocalDate.parse("2026-11-09")));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.requirePeriod(
                null,"24",candidate("1000000","2026-11-20",null),release,"poster",LocalDate.parse("2026-11-09")));
    }
    @Test void valuerAndValueMustBeIndependentlyApproved() {
        var c=candidate("1000000","2026-11-20",null);
        assertDoesNotThrow(()->CollateralValuationCommitPolicy.requireValuer(valuer,"24",c,release,"poster"));
        assertDoesNotThrow(()->CollateralValuationCommitPolicy.requireAmount(c,"IRR"));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.requireValuer(
                valuer,"24",c,release,"VALUER-APPROVER-01"));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.requireAmount(c,"USD"));
    }
    @Test void supersessionRequiresActualCurrentFinalAndNoSuccessor() {
        assertDoesNotThrow(()->CollateralValuationCommitPolicy.requireSupersession(
                "5","5","1","FINAL","IRR","2026-10-01","1","IRR","2026-10-21",0));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.requireSupersession(
                "5","4","1","FINAL","IRR","2026-10-01","1","IRR","2026-10-21",0));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.requireSupersession(
                "5","5","1","FINAL","IRR","2026-10-01","1","IRR","2026-10-21",1));
    }
    @Test void immutableReplayNeedsSameActorHashReasonAndVersion() {
        assertDoesNotThrow(()->CollateralValuationCommitPolicy.checkReplay(command,command.idempotencyKey(),
                "same","same",command.reason(),"3","poster","poster"));
        assertThrows(IllegalStateException.class,()->CollateralValuationCommitPolicy.checkReplay(command,command.idempotencyKey(),
                "changed","same",command.reason(),"3","poster","poster"));
    }
}
