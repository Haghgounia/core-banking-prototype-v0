package com.behsazan.corebanking.collateral.introduction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralIntroductionReviewPolicyTest {
    @Test void validDecisions() {
        var good = CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "  بررسی کامل شد  ", "1", "a".repeat(64)));
        assertEquals("REVIEWED", good.decision());
        assertEquals("بررسی کامل شد", good.reason());
        assertEquals("RETURNED", CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("RETURNED", "نیازمند اصلاح مشخصات", "1", "a".repeat(64))).decision());
    }
    @Test void deniesSelfReview() {
        assertThrows(IllegalArgumentException.class, () ->
                CollateralIntroductionReviewPolicy.separateActors("maker", "maker"));
        assertDoesNotThrow(() -> CollateralIntroductionReviewPolicy.separateActors("maker", "checker"));
    }
    @Test void rejectsUnsafeOrIncompleteDecisions() {
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision(null, "valid reason long enough", "1", "a".repeat(64))));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("APPROVED", "reason sufficiently long", "1", "a".repeat(64))));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "short", "1", "a".repeat(64))));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("RETURNED", "invalid\ncontrol", "1", "a".repeat(64))));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "reason sufficiently long", "2", "a".repeat(64))));
    }
    @Test void versionAndStateFailClosed() {
        assertDoesNotThrow(() -> CollateralIntroductionReviewPolicy.pending("PENDING_REVIEW", "1"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.pending("REVIEWED", "1"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.pending("PENDING_REVIEW", "2"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionReviewPolicy.reviewedNeverRegisters("REGISTERED"));
    }
}
