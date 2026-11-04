package com.behsazan.corebanking.collateral.release;

import java.util.List;

/** Decimal Oracle identifiers always travel as strings to prevent JS precision loss. */
public final class CollateralFinalReleaseModels {
    private CollateralFinalReleaseModels() {}
    public record Readiness(boolean evidenceReadsEnabled, boolean finalReleaseWritesEnabled,
            boolean legalHoldAttestationIntegrated, boolean consumerClearanceIntegrated,
            boolean databaseConcurrencyProven, String decision) {}
    public record Snapshot(String collateralId, String registrationNumber, String branchCode,
            String currentStatusCode, String typeCode, String recordVersion,
            boolean releaseInquiryRequired, boolean terminalStatusReported,
            long totalAllocationCount, long activeAllocationCount,
            long crossBranchAllocationCount, long invalidUsageCount,
            long pendingTransactionCount) {}
    public record Evidence(Snapshot snapshot, List<String> blockers, String decision,
            boolean releaseAuthorized, String evidenceGrade) {}
    public record StatusEvent(String statusHistoryId, String sequenceNumber,
            String fromStatusCode, String toStatusCode, String reasonCode,
            String sourceEventCode, String sourceReference, String changedAt) {}
    public record PreviewRequest(String reasonReference) {}
    public record PreviewResult(String collateralId, String expectedVersion,
            String currentVersion, String reasonReference, String requestDigest,
            boolean durableRequestRecorded, boolean capacityReleased,
            boolean finalReleaseAuthorized, List<String> blockers, String decision) {}
}
