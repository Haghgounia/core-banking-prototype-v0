package com.behsazan.corebanking.collateral.registration;

import java.util.List;

/** Inspection of a reviewed staging draft. No registration number or collateral row is issued. */
public final class CollateralRegistrationReadinessModels {
    private CollateralRegistrationReadinessModels() {}

    public record Check(String code, String labelFa, String status, String evidence) {}

    public record Report(String reviewId, String draftId, String reviewStatus,
            String branchCode, String snapshotHash, String assetReferenceCandidate,
            String countableItemCount, String estimateAmount, String currencyCode,
            String registrationNumber, String registrationStatus,
            boolean finalRegistrationEnabled, boolean eligibleForFinalRegistration,
            List<Check> checks, List<String> blockingCodes, String stage) {}
}
