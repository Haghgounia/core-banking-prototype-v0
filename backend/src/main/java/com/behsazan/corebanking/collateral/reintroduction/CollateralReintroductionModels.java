package com.behsazan.corebanking.collateral.reintroduction;

import java.util.List;

/** BUC-12: wire identifiers remain strings because Oracle NUMBER(19) exceeds JS integer precision. */
public final class CollateralReintroductionModels {
    private CollateralReintroductionModels() {}
    public record Readiness(boolean sourceReadable, boolean identitySchemaReady,
            boolean permitSchemaReady, boolean bankNumberAuthorityReady,
            boolean postingEnabled, boolean prototypePostingBlocked,
            String registrationStatusConfigured, String decision) {}
    public record Evidence(String previousCollateralId, String oldRegistrationNumber,
            String previousStatus, String previousVersion, String oldPartyId,
            String collateralTypeId, String branchCode, String stableAssetId,
            boolean finalReleaseRecorded, boolean statusTerminal, boolean assetVerified,
            boolean activeDuplicateFound, long activeAllocationCount,
            List<String> blockers, String decision) {}
    public record PermitView(String permitId, String status, String reviewId, String previousCollateralId, String assetId,
            String prospectivePartyId, String typeId, String statusCode,
            String approvedRegistrationNumber, String approvalReference, String ownerReverificationReference,
            String expiresAt, String postedCollateralId, String postedRegistrationNumber) {}
    public record Permits(List<PermitView> items, String source) {}
    public record FinalizeCommand(String permitId) {}
    public record Receipt(String permitId, String previousCollateralId,
            String previousRegistrationNumber, String newCollateralId,
            String newRegistrationNumber, String assetId, String reviewId,
            String status, String postedAt, boolean replayed, String decision) {}
    public record LineageItem(String collateralId, String registrationNumber,
            String previousCollateralId, String status, String linkedAt,
            boolean current, String assetId) {}
    public record Lineage(String previousCollateralId, String assetId,
            List<LineageItem> items, String decision) {}
}
