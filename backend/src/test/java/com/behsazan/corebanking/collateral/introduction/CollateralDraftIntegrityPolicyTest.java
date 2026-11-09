package com.behsazan.corebanking.collateral.introduction;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralDraftIntegrityPolicyTest {
    private CollateralDraftIntegrityPolicy.Evidence evidence() {
        return new CollateralDraftIntegrityPolicy.Evidence(true, true, true, true,
                true, true, List.of());
    }
    @Test void fullyVerifiedSnapshotIsEligibleForIndependentReview() {
        var checked = CollateralDraftIntegrityPolicy.assess(evidence());
        assertTrue(checked.eligibleForReview());
        assertTrue(checked.snapshotConsistent());
        assertTrue(checked.auditConsistent());
        assertTrue(checked.currentRulesValid());
        assertTrue(checked.blockers().isEmpty());
    }
    @Test void noAuditMeansNoReviewEvenIfFingerprintMatches() {
        var checked = CollateralDraftIntegrityPolicy.assess(
                new CollateralDraftIntegrityPolicy.Evidence(true,true,true,false,true,true,List.of()));
        assertFalse(checked.eligibleForReview());
        assertTrue(checked.blockers().contains("CLT_R2_DRAFT_AUDIT_MISSING_OR_DIFFERENT"));
    }
    @Test void changedSnapshotIsBlocked() {
        var checked = CollateralDraftIntegrityPolicy.assess(
                new CollateralDraftIntegrityPolicy.Evidence(true,true,false,true,true,true,List.of()));
        assertFalse(checked.eligibleForReview());
        assertFalse(checked.snapshotConsistent());
    }
    @Test void staleCifAndSerialRulesRemainBlocked() {
        var checked = CollateralDraftIntegrityPolicy.assess(
                new CollateralDraftIntegrityPolicy.Evidence(true,true,true,true,false,false,
                    List.of("CLT_PARTY_ELIGIBILITY_NOT_VERIFIED")));
        assertFalse(checked.eligibleForReview());
        assertTrue(checked.blockers().contains("CLT_PARTY_ELIGIBILITY_NOT_VERIFIED"));
        assertTrue(checked.blockers().contains("CLT_R2_DRAFT_SERIAL_RANGES_INVALID"));
    }
    @Test void unknownEvidenceFailsClosed() {
        assertThrows(IllegalArgumentException.class, ()->CollateralDraftIntegrityPolicy.assess(null));
    }
}
