package com.behsazan.corebanking.collateral.introduction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralReviewDecisionGuardTest {
    private CollateralReviewDecisionGuard.Evidence ready() {
        return new CollateralReviewDecisionGuard.Evidence(true, true, true, true,
                true, true, false, true);
    }
    @Test void validReviewSnapshotStillDoesNotIssueRegistration() {
        assertTrue(CollateralReviewDecisionGuard.assess(ready()).reviewAllowed());
    }
    @Test void tamperingAndAuditMismatchAreRejected() {
        var a = CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
                true, false, false, true, true, true, false, true));
        assertFalse(a.reviewAllowed());
        assertTrue(a.blockers().contains("CLT_REVIEW_STORED_SNAPSHOT_TAMPERED"));
        assertTrue(a.blockers().contains("CLT_REVIEW_CREATION_AUDIT_INVALID"));
    }
    @Test void corruptedSerialChildRowsCannotBeReviewed() {
        var a = CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
                true, true, true, false, true, true, false, true));
        assertTrue(a.blockers().contains("CLT_REVIEW_SERIAL_ROWS_MISMATCH"));
    }
    @Test void stageCollisionOrUnavailableStageBlocksReview() {
        assertFalse(CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
                true,true,true,true,true,true,true,true)).reviewAllowed());
        assertFalse(CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
                true,true,true,true,true,false,false,true)).reviewAllowed());
    }
    @Test void missingCandidateIsNotFalseProofOfRegistration() {
        assertTrue(CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
                true,true,true,true,true,false,false,false)).reviewAllowed());
    }
    @Test void unavailableCifFailsClosed() {
        assertFalse(CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
                true,true,true,true,false,true,false,true)).reviewAllowed());
    }
}
