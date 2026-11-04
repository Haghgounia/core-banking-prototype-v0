package com.behsazan.corebanking.collateral.unallocation;

import java.util.List;

/** Stage-only API: no record in this response is a financial unallocation. */
public final class CollateralUnallocationIntakeModels {
    private CollateralUnallocationIntakeModels() {}
    public record Readiness(boolean intakeEnabled, boolean stagingSchemaReady,
            boolean durableIdempotencyEnabled, boolean financialUnallocationEnabled,
            boolean capacityReleased, boolean finalReleaseEnabled, String state) {}
    public record Receipt(String requestId, String collateralId, String allocationId,
            String consumerDomain, String fileNumber, String requestedAmount,
            String currencyCode, String releaseMode, String reasonReference,
            String requestStatus, String recordVersion, String expectedAllocationVersion,
            String createdAt, String updatedAt, String payloadDigest,
            boolean idempotentReplay, boolean requestPersisted,
            boolean financialUnallocationAuthorized, boolean capacityReleased,
            List<String> blockers, String evidenceLevel) {}
    public record JournalItem(String requestId, String allocationId, String consumerDomain,
            String requestedAmount, String currencyCode, String releaseMode,
            String requestStatus, String createdAt, String updatedAt,
            boolean financialUnallocationAuthorized) {}
    public record CancelRequest(String consumerDomain, String reasonReference) {}
}
