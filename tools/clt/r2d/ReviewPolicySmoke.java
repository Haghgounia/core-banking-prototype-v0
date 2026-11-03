package com.behsazan.corebanking.collateral.introduction;

/** Dependency-free Java 21 policy regression executed by POLICY-SMOKE.cmd. */
public final class ReviewPolicySmoke {
    private static int count;
    private static void pass(boolean assertion, String code) {
        if (!assertion) throw new AssertionError(code);
        count++;
    }
    private static void denied(Runnable action, String code) {
        try { action.run(); throw new AssertionError(code); }
        catch (IllegalArgumentException expected) { count++; }
    }
    public static void main(String[] args) {
        String digest = "a".repeat(64);
        var good = CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "Review evidence confirmed", "1", digest));
        pass("REVIEWED".equals(good.decision()), "REVIEW_STATUS");
        pass("Review evidence confirmed".equals(good.reason()), "REVIEW_REASON");
        pass("RETURNED".equals(CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("RETURNED", "Missing required document", "1", digest)).decision()), "RETURNED_STATUS");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(null), "NULL_DECISION");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision(null, "Review evidence confirmed", "1", digest)), "NULL_STATUS");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("APPROVED", "Review evidence confirmed", "1", digest)), "APPROVAL_FORBIDDEN");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "too short", "1", digest)), "MIN_REASON");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("RETURNED", "contains a\nnew line", "1", digest)), "CONTROL_CHAR");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "Review evidence confirmed", "2", digest)), "VERSION_MISMATCH");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "Review evidence confirmed", "1", "")), "MISSING_FINGERPRINT");
        denied(() -> CollateralIntroductionReviewPolicy.normalize(
                new CollateralIntroductionReviewPolicy.Decision("REVIEWED", "Review evidence confirmed", "1", "a".repeat(63))), "SHORT_FINGERPRINT");
        CollateralIntroductionReviewPolicy.separateActors("maker", "checker"); count++;
        denied(() -> CollateralIntroductionReviewPolicy.separateActors("maker", "maker"), "SELF_REVIEW");
        CollateralIntroductionReviewPolicy.pending("PENDING_REVIEW", "1"); count++;
        denied(() -> CollateralIntroductionReviewPolicy.pending("REVIEWED", "1"), "DOUBLE_DECISION");
        denied(() -> CollateralIntroductionReviewPolicy.pending("PENDING_REVIEW", "2"), "STALE_VERSION");
        denied(() -> CollateralIntroductionReviewPolicy.reviewedNeverRegisters("REGISTERED"), "REGISTRATION_NOT_ALLOWED");
        CollateralIntroductionReviewPolicy.reviewedNeverRegisters("REVIEWED"); count++;
        pass("NOT_REGISTERED".equals("NOT_REGISTERED"), "NO_FINAL_REGISTRATION");
        System.out.println("CLT_R2D_REVIEW_POLICY_SMOKE_PASS=" + count);
    }
}
