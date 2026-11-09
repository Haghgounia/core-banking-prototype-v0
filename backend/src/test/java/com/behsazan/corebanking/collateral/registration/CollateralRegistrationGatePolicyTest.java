package com.behsazan.corebanking.collateral.registration;

import com.behsazan.corebanking.collateral.registration.CollateralRegistrationGatePolicy.Evidence;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollateralRegistrationGatePolicyTest {
    private static Evidence sample() {
        return new Evidence("77", "19", "REVIEWED", "maker", "checker", "010",
                "a".repeat(64), true, true, true, true, List.of(), true,
                true, true, true, true, "12", "PROPERTY-123", "500000", "IRR", true, true, true, false);
    }
    @Test void allEvidenceStillRequiresBankDecisionsAndNeverRegisters() {
        var result = CollateralRegistrationGatePolicy.assess(sample());
        assertThat(result.eligibleForFinalRegistration()).isFalse();
        assertThat(result.finalRegistrationEnabled()).isFalse();
        assertThat(result.registrationNumber()).isNull();
        assertThat(result.registrationStatus()).isEqualTo("NOT_REGISTERED");
        assertThat(result.blockingCodes()).contains("G-01", "G-03", "G-12", "G-14", "G-16", "G-17", "FINAL_WRITER");
    }
    @Test void brokenSnapshotAndWrongReviewerBlock() {
        var src = sample();
        var report = CollateralRegistrationGatePolicy.assess(new Evidence(src.reviewId(),src.draftId(),
                "RETURNED", "same", "same", src.branch(), src.snapshotHash(), true,
                false, false, false, List.of("CLT_PARTY_UNAVAILABLE"), false,
                false, false, true, false, "0", null, src.estimateAmount(), src.currencyCode(), false, false, false, false));
        assertThat(report.blockingCodes()).contains("REVIEW_DECISION", "SNAPSHOT_INTEGRITY", "LIVE_CIF_CLT_RECHECK", "NUMBER19_STORAGE");
        assertThat(report.eligibleForFinalRegistration()).isFalse();
    }
    @Test void nonCountableDoesNotRequireItemRangeStorage() {
        var src = sample();
        var report = CollateralRegistrationGatePolicy.assess(new Evidence(src.reviewId(),src.draftId(),
                src.reviewStatus(), src.maker(), src.checker(), src.branch(), src.snapshotHash(),
                true,true,true,true,List.of(),true,true,true,false,false,"0",
                "ASSET-1",src.estimateAmount(),src.currencyCode(), true, true, true, false));
        assertThat(report.blockingCodes()).doesNotContain("G-03", "G-12");
        assertThat(report.blockingCodes()).contains("G-14", "G-17", "FINAL_WRITER");
    }
    @Test void missingCreationAuditRemainsBlockedEvenWithPerfectSnapshot() {
        var src = sample();
        var report = CollateralRegistrationGatePolicy.assess(new Evidence(src.reviewId(),src.draftId(),
                src.reviewStatus(),src.maker(),src.checker(),src.branch(),src.snapshotHash(),
                true,true,true,true,List.of(),true,true,true,true,true,"5",
                "ASSET-01",src.estimateAmount(),src.currencyCode(),false,true,true,false));
        assertThat(report.blockingCodes()).contains("CREATION_AUDIT", "LIVE_CIF_CLT_RECHECK");
    }
    @Test void missingChildRowsAndOtherOwnerCandidateBlockReadiness() {
        var src = sample();
        var report = CollateralRegistrationGatePolicy.assess(new Evidence(src.reviewId(),src.draftId(),
                src.reviewStatus(),src.maker(),src.checker(),src.branch(),src.snapshotHash(),
                true,true,true,true,List.of(),true,true,true,true,true,"5",
                "ASSET-01",src.estimateAmount(),src.currencyCode(),true,false,true,true));
        assertThat(report.blockingCodes()).contains("SERIAL_CHILD_READBACK", "COUNTABLE_ITEM_VALIDATION", "ASSET_STAGE_COLLISION");
    }
    @Test void unavailableStageLookupBlocksCandidateProof() {
        var src = sample();
        var report = CollateralRegistrationGatePolicy.assess(new Evidence(src.reviewId(),src.draftId(),
                src.reviewStatus(),src.maker(),src.checker(),src.branch(),src.snapshotHash(),
                true,true,true,true,List.of(),true,true,true,false,false,"0",
                "ASSET-01",src.estimateAmount(),src.currencyCode(),true,true,false,false));
        assertThat(report.blockingCodes()).contains("ASSET_STAGE_COLLISION");
    }

}
