package com.behsazan.corebanking.collateral.introduction;

/** Separate review projection; a REVIEWED draft is NOT a registered collateral. */
public final class CollateralIntroductionReviewModels {
    private CollateralIntroductionReviewModels() {}
    public record Review(String reviewId, String draftId, String status,
            String makerSubject, String branchCode, String submittedAt,
            String checkerSubject, String decidedAt, String decisionReason,
            String recordVersion, String registrationStatus) {}
    public record ReviewSnapshot(String reviewId,
            CollateralIntroductionModels.Draft draft, String draftHash,
            boolean finalRegistrationEnabled) {}
    public record ReviewReadiness(boolean reviewWorkflowEnabled, boolean schemaVerified,
            boolean finalRegistrationEnabled, String stage) {}
}
