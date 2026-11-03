package com.behsazan.corebanking.collateral.allocation;

import java.util.List;

/** All Oracle NUMBER(19) identifiers and NUMBER(20,4) money are JSON strings. */
public final class CollateralAllocationRequestModels {
    private CollateralAllocationRequestModels() {}

    public record Debtor(String partyId, String role) {}
    public record Request(String consumerDomain, String caseReference, String fileNumber,
            String amount, String currencyCode, List<Debtor> debtors) {}

    /** A diagnostic, never an allocation authorization or a durable idempotency receipt. */
    public record Preflight(String collateralId, String consumerDomain, String caseReference,
            String fileNumber, String amount, String currencyCode, String requestKey,
            String payloadDigest, String callerEvidence, String arithmeticShortfall,
            boolean sponsorDebtorConflict, boolean debtorRosterVerified,
            boolean consumingObligationVerified, boolean idempotencyPersisted,
            boolean capacityLocked, boolean allocationAuthorized,
            List<String> blockers, String decision, String evidenceLevel) {}
}
