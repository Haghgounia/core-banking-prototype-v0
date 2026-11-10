package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.workflow.CollateralSponsorCommitPolicy.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralSponsorCommitPolicyTest {
    private final Command command = new Command("3","sponsor.request.20261109", "Reviewed bank approval");
    private final Release release = new Release("POLICY-REVISION-001", "G05-BUSINESS-DECISION", "CLT-DBA-MIGRATION-001",
            "G09-INTEGRATION-REF", "BANK-SPONSOR-OWNER", true);
    private final StageEvidence ready = new StageEvidence("REVIEWED","3",true,true,false,"maker","reviewer");
    @Test void validStageAndApprovedBankReleasePassesPolicyOnly() {
        assertDoesNotThrow(()->CollateralSponsorCommitPolicy.guard(command,ready,release,true,true,true));
    }
    @Test void noBankApprovalOrUnknownSchemaCannotCommit() {
        assertThrows(IllegalStateException.class,()->CollateralSponsorCommitPolicy.guard(command,ready,null,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralSponsorCommitPolicy.guard(command,ready,release,true,false,true));
        assertThrows(IllegalStateException.class,()->CollateralSponsorCommitPolicy.guard(command,ready,release,false,true,true));
    }
    @Test void staleOrInconsistentRequestCannotCommit() {
        assertThrows(IllegalArgumentException.class,()->CollateralSponsorCommitPolicy.version("2"));
        assertThrows(IllegalStateException.class,()->CollateralSponsorCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",false,true,false,"maker","reviewer"),release,true,true,true));
        assertThrows(IllegalStateException.class,()->CollateralSponsorCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",true,true,true,"maker","reviewer"),release,true,true,true));
    }
    @Test void makerCannotPostTheirOwnRequest() {
        assertThrows(SecurityException.class,()->CollateralSponsorCommitPolicy.guard(command,
                new StageEvidence("REVIEWED","3",true,true,false,"maker","maker"),release,true,true,true));
    }
    @Test void sameIdempotencyKeyMustMatchOriginalDigest() {
        assertDoesNotThrow(()->CollateralSponsorCommitPolicy.checkReplay(command,command.idempotencyKey(),"aa","aa","reviewer"));
        assertThrows(IllegalStateException.class,()->CollateralSponsorCommitPolicy.checkReplay(command,command.idempotencyKey(),"aa","bb","reviewer"));
    }
}
