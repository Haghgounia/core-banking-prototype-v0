package com.behsazan.corebanking.collateral.introduction;

import java.util.List;

/** Draft intake is not a registered COLLATERAL; the registration number is never issued here. */
public final class CollateralIntroductionModels {
    private CollateralIntroductionModels() {}

    public record AttributeInput(String attributeId, String value) {}
    public record SerialRangeInput(String serialFrom, String serialTo, String totalAmount) {}
    public record Request(String partyId, String collateralTypeId,
                          String estimateAmount, String currencyCode,
                          String validFrom, String validTo,
                          String depositNumber, String remarks,
                          List<AttributeInput> attributes, String assetReference,
                          List<SerialRangeInput> itemRanges) {
        public Request(String partyId, String collateralTypeId, String estimateAmount,
                       String currencyCode, String validFrom, String validTo,
                       String depositNumber, String remarks, List<AttributeInput> attributes) {
            this(partyId, collateralTypeId, estimateAmount, currencyCode, validFrom, validTo,
                 depositNumber, remarks, attributes, null, List.of());
        }
    }
    public record Validation(boolean eligibleForDraft, boolean eligibleForFinalRegistration,
                             List<String> blockers, String partyId, String collateralTypeId,
                             int configuredAttributeCount, String state) {}
    public record Draft(String draftId, String status, String partyId, String collateralTypeId,
                        String estimateAmount, String currencyCode, String validFrom,
                        String validTo, String depositNumber, String remarks,
                        List<AttributeInput> attributes, String branchCode,
                        String createdAt, String recordVersion, String source,
                        String assetReference, List<SerialRangeInput> itemRanges) {}
    /** Server-side bounded view of immutable drafts owned by the current maker and branch. */
    public record DraftSummary(String draftId, String status, String partyId,
                               String collateralTypeId, String estimateAmount,
                               String currencyCode, String createdAt, String recordVersion) {}
    public record DraftPage(List<DraftSummary> items, int page, int size, long totalElements) {}
    /** Source-of-truth inspection of a stored Oracle stage snapshot; no final write. */
    public record DraftIntegrity(String draftId, boolean eligibleForReview,
            boolean snapshotConsistent, boolean auditConsistent, boolean currentRulesValid,
            int serialItemCount, String serialTotalAmountIRR,
            List<String> blockers, String stage) {}
    /** Advisory stage candidate comparison, never an authoritative asset-identity verdict. */
    public record AssetCandidateInspection(String draftId, String assetReference,
            long sameOwnerDrafts, boolean otherOwnerConflict,
            boolean eligibleForReviewSubmission, boolean identityVerified,
            boolean finalRegistrationEnabled, String status, String explanation,
            String comparisonScope) {}
    /** Independent business flags and stage range persistence; no implicit default. */
    public record TypeCountability(String collateralTypeId, boolean active,
            boolean hasMultiplicity, Boolean isCountable, boolean configured,
            boolean rangeTableReady, boolean finalRegistrationEnabled,
            String configurationSource) {}
    public record SerialRangePersistence(String draftId, boolean childTableReady,
            boolean snapshotMatchesChildRows, int rangeRowCount, int itemCount,
            String totalAmountIRR, String persistenceStage) {}
    /** Candidate-only G-14 check; no definitive asset directory source exists yet. */
    public record AssetIdentityPreflight(boolean stageCompared, boolean candidatePresent,
                 boolean crossOwnerDraftConflict, long sameOwnerDrafts,
                 boolean stableIdentityVerified, String source, List<String> blockers) {}
    public record RegistrationReview(boolean eligibleForDraft, boolean eligibleForFinalRegistration,
                 List<String> draftBlockers, List<String> finalizationBlockers,
                 int serialRangeRows, String serialItemCount, String serialTotalAmountIRR,
                 String numberPolicy, String persistenceTarget,
                 CollateralNumberingFeasibilityPolicy.Assessment numberingFeasibility,
                 AssetIdentityPreflight assetIdentityPreflight) {}
    public record Readiness(boolean registryReadsEnabled, boolean draftWritesEnabled,
                            boolean finalRegistrationEnabled, boolean migrationAutomatic,
                            String stage, String numberPolicy) {}
}
