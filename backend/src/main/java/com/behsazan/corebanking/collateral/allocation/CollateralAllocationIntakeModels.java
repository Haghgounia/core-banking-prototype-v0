package com.behsazan.corebanking.collateral.allocation;

import java.util.List;

/** Staging request receipt. An intake receipt is never a reservation or allocation. */
public final class CollateralAllocationIntakeModels {
    private CollateralAllocationIntakeModels() {}
    public record Receipt(String requestId, String collateralId, String consumerDomain,
            String caseReference, String fileNumber, String amount, String currencyCode,
            String requestStatus, String createdAt, String recordVersion,
            int debtorCount, boolean sponsorDebtorConflict, String payloadDigest,
            boolean replayed, boolean idempotencyPersisted, boolean capacityLocked,
            boolean consumingObligationVerified, boolean debtorRosterVerified,
            boolean allocationAuthorized, List<String> blockers, String evidenceLevel) {}
    public record Readiness(boolean intakeConfigured, boolean schemaReady,
            boolean allocationWritesEnabled, boolean capacityLockImplemented,
            boolean consumerAttestationImplemented, String state) {}
    /** This projection deliberately excludes debtor names, Party IDs, and secrets. */
    public record JournalItem(String requestId, String consumerDomain, String caseReference,
            String amount, String currencyCode, String requestStatus, String createdAt,
            boolean allocationAuthorized) {}
}
