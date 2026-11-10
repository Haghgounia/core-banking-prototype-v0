package com.behsazan.corebanking.collateral.crossunit;

import java.util.List;

/** All Oracle numeric IDs / amounts cross the HTTP boundary as decimal strings. */
public final class CollateralCrossUnitModels {
    private CollateralCrossUnitModels() {}
    public record Readiness(String callerBranch,boolean schemaReady,boolean requestsEnabled,
            boolean bankDecisionsEnabled,boolean consumerServiceConfigured,String operationalState) {}
    public record GrantCommand(String consumerBranch, String purposeReference) {}
    public record GrantDecision(String decision, String approvalReference, String validUntilUtc) {}
    public record RevokeCommand(String reasonReference) {}
    public record Grant(String grantId,String collateralId,String ownerBranch,
            String consumerBranch,String purposeReference,String status,String sourceRecordVersion,
            String maker,String checker,String approvalReference,String validUntilUtc,
            String createdAtUtc,String decidedAtUtc,String revokedAtUtc) {}
    public record Grants(List<Grant> items) {}
    public record Eligibility(String collateralId,String registrationNumber,String ownerBranch,
            String consumerBranch,String typeCode,String typeName,String status,
            String currencyCode,String estimatedAmount,String sourceRecordVersion,
            String grantId,String grantExpiresUtc,boolean typePermission,
            boolean introductionPermission,boolean financialAllocationAuthorized,
            String evidenceLevel) {}
    public record IntentCommand(String registrationNumber,
            com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.Request allocation) {}
    public record IntentReceipt(String intentId,String collateralId,String registrationNumber,
            String grantId,String ownerBranch,String consumerBranch,String consumerDomain,
            String caseReference,String fileNumber,String amount,String currencyCode,
            int debtorCount,String requestStatus,boolean replay,boolean allocationPosted,
            String createdAtUtc,String requestDigest,List<String> operationalBlockers) {}
}
