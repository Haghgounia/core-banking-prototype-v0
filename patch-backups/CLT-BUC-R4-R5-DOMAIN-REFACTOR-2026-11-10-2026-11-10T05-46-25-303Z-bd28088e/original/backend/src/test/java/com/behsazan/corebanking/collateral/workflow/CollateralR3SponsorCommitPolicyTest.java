package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.workflow.CollateralR3SponsorCommitPolicy.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralR3SponsorCommitPolicyTest {
    private final Command command = new Command("3","sponsor.request.20261109", "Reviewed bank approval");
    private final Release release = new Release("POLICY-REVISION-001", "G05-BUSINESS-DECISION", "CLT-DBA-MIGRATION-001",
            "G09-INTEGRATION-REF", "BANK-SPONSOR-OWNER", true);
    private final StageEvidence ready = new StageEvidence("REVIEWED","3",true,true,false,"maker","reviewer");
    @Test void validStageAndApprovedBankReleasePassesPolicyOnly() {
        assertDoesNotThrow(()->CollateralR3SponsorCommitPolicy.guard(command,ready,release,true,true,true));
    }
    @Test void noBankApprovalOrUnknownSchemaCannotCommit() {
        assertThrows(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.guard(command,ready,null,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.guard(command,ready,release,true,false,true));
        assertThrows(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.guard(command,ready,release,false,true,true));
    }
    @Test void staleOrInconsistentRequestCannotCommit() {
        assertThrows(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.version("2"));
        assertThrows(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",false,true,false,"maker","reviewer"),release,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",true,true,true,"maker","reviewer"),release,true,true,true));
    }
    @Test void makerCannotPostTheirOwnRequest() {
        assertThrows(SecurityException.class,()->CollateralR3SponsorCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",true,true,false,"maker","maker"),release,true,true,true));
    }
    @Test void sameIdempotencyKeyMustMatchOriginalDigest() {
        assertDoesNotThrow(()->CollateralR3SponsorCommitPolicy.checkReplay(command,command.idempotencyKey(),"aa","aa","reviewer"));
        assertThrows(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.checkReplay(command,command.idempotencyKey(),"aa","bb","reviewer"));
    }
}
