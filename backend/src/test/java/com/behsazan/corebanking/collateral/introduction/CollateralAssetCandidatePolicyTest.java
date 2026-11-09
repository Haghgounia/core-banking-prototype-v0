package com.behsazan.corebanking.collateral.introduction;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralAssetCandidatePolicyTest {
    @Test void noIdentityIsEverCertified() {
        var result = CollateralAssetCandidatePolicy.assess("12", "ABC", true, 0, 0);
        assertFalse(result.identityVerified());
        assertFalse(result.finalRegistrationEnabled());
        assertEquals("NO_STAGE_MATCH", result.status());
    }
    @Test void stageConflictWithAnotherOwnerBlocksReviewNotFinalizes() {
        var result = CollateralAssetCandidatePolicy.assess("12", "ABC", true, 1, 1);
        assertTrue(result.otherOwnerConflict());
        assertFalse(result.eligibleForReviewSubmission());
        assertFalse(result.finalRegistrationEnabled());
    }
    @Test void sameOwnerRevisionsAreNotBlocked() {
        var result = CollateralAssetCandidatePolicy.assess("12", "ABC", true, 3, 0);
        assertEquals("SAME_OWNER_DRAFTS_FOUND", result.status());
        assertTrue(result.eligibleForReviewSubmission());
    }
    @Test void noReferenceDoesNotGuessIdentity() {
        var result = CollateralAssetCandidatePolicy.assess("12", null, true, 0, 0);
        assertEquals("NO_ASSET_REFERENCE", result.status());
        assertTrue(result.eligibleForReviewSubmission());
    }
    @Test void missingExtensionDoesNotClaimNoMatches() {
        var result = CollateralAssetCandidatePolicy.assess("12", "ABC", false, 0, 0);
        assertEquals("STAGE_EXTENSION_REQUIRED", result.status());
        assertFalse(result.identityVerified());
    }
    @Test void simpleTrimAndCaseFold() {
        assertEquals("ABC", CollateralAssetCandidatePolicy.lookup("  abC  "));
    }
    @Test void invalidAggregateRefused() {
        assertThrows(IllegalArgumentException.class, () ->
            CollateralAssetCandidatePolicy.assess("12", "A", true, 0, -1));
    }
}
