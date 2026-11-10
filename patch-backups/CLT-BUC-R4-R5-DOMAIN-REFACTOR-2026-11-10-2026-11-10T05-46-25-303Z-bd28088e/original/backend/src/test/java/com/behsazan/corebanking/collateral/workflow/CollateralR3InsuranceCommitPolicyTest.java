package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels;
import com.behsazan.corebanking.collateral.workflow.CollateralR3InsuranceCommitPolicy.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralR3InsuranceCommitPolicyTest {
    private final Command command=new Command("3","insurance.idempotency.key.01","Valid bank-approved reason");
    private final Release release=new Release("INS-POLICY-20261109","G04-BUSINESS-DECISION",
            "DBA-APPROVAL-DOC-2026","G09-INTEGRATION-DECISION","G04-COVERAGE-DECISION",
            "G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-REF","BANK-BUSINESS-OWNER",true);
    private final StageEvidence stage=new StageEvidence("REVIEWED","3",true,true,false,"maker","poster");
    private CollateralInsurancePolicy.Candidate candidate(String previous,String start,String amount) {
        return CollateralInsurancePolicy.candidate(new CollateralInsuranceModels.CandidateRequest(
                "2","10","42","12345","INS-2026-10",amount,"0",null,
                start,"2027-12-01",previous,"IRR","Valid insurance certificate on file"));
    }
    @Test void featureSchemaAndBankDecisionMustAllBeApproved() {
        assertDoesNotThrow(()->CollateralR3InsuranceCommitPolicy.guard(command,stage,release,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.guard(command,stage,release,false,true,true));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.guard(command,stage,release,true,false,true));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.guard(command,stage,null,true,true,true));
    }
    @Test void makerCannotCommitAndOnlyReviewedStagePasses() {
        assertThrows(SecurityException.class,()->CollateralR3InsuranceCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",true,true,false,"maker","maker"),release,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.guard(command,
                new StageEvidence("SUBMITTED","2",true,true,false,"maker","poster"),release,true,true,true));
    }
    @Test void approvedCoverageIsPerTypeAndMinAmountAndRevision() {
        var rule=new CoverageRule("24","42","IRR","1000000","INS-POLICY-20261109","BANK-RULE-OWNER",true);
        assertDoesNotThrow(()->CollateralR3InsuranceCommitPolicy.requireCoverage(rule,"24",candidate(null,"2026-11-10","1000000"),release,"poster"));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(rule,"24",candidate(null,"2026-11-10","999999"),release,"poster"));
    }
    @Test void sameInsuranceCanNeverHaveSecondSuccessor() {
        assertDoesNotThrow(()->CollateralR3InsuranceCommitPolicy.requireRenewal(candidate("77","2026-11-10","1000000"),"55","55","42","2026-11-09",0));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(candidate("77","2026-11-10","1000000"),"55","55","42","2026-11-09",1));
    }
    @Test void replayNeedsSamePayloadActorVersionAndReason() {
        assertDoesNotThrow(()->CollateralR3InsuranceCommitPolicy.checkReplay(command,command.idempotencyKey(),"abc","abc",command.reason(),"3","poster","poster"));
        assertThrows(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.checkReplay(command,command.idempotencyKey(),"abc","xyz",command.reason(),"3","poster","poster"));
    }
}
