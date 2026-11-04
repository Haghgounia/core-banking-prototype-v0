package com.behsazan.corebanking.collateral.unallocation;

import java.util.List;

/** Oracle NUMBER(19) and NUMBER(20,4) are JSON strings (never JavaScript numbers). */
public final class CollateralUnallocationModels {
    private CollateralUnallocationModels() {}
    public record AllocationLine(String allocationId, String collateralId, String moduleId,
            String fileNumber, String allocatedAmount, String currencyCode, boolean used,
            String recordVersion, String createdAt, String updatedAt) {}
    public record TransactionLine(String transactionId, String allocationId,
            String transactionNumber, String transactionType, String transactionStatus,
            String transactionDirection, String amount, String currencyCode,
            String effectiveAt, String originalTransactionId, String reversalReasonCode) {}
    public record Readiness(boolean historyConfigured, boolean financialUnallocationEnabled,
            boolean durableIdempotencyEnabled, boolean consumerAttestationImplemented,
            boolean finalReleaseEnabled, String state) {}
    public record PreviewRequest(String consumerDomain, String fileNumber, String requestedAmount,
            String currencyCode, String releaseMode, String reasonReference) {}
    public record Preflight(String collateralId, String allocationId, String consumerDomain,
            String fileNumber, String requestedAmount, String currencyCode, String releaseMode,
            String idempotencyKey, String payloadDigest, String allocationRecordVersion,
            boolean activeAllocation, boolean expectedVersionMatched,
            boolean domainOwnerMapped, boolean serviceClientVerified,
            boolean consumerObligationVerified, boolean idempotencyPersisted,
            boolean capacityReleased, boolean unallocationAuthorized,
            List<String> blockers, String decision, String evidenceLevel) {}
}
