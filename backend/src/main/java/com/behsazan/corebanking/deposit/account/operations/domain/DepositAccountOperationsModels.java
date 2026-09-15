package com.behsazan.corebanking.deposit.account.operations.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositAccountOperationsModels {
    private DepositAccountOperationsModels() {
    }

    public record AccountSummary(
            long accountId,
            String accountNo,
            long openingRequestId,
            String requestNo,
            Long primaryPartyId,
            Long productVersionId,
            String productFamilyCode,
            String currencyCode,
            BigDecimal openingAmount,
            String accountStatusCode,
            OffsetDateTime createdAt,
            OffsetDateTime activatedAt,
            long recordVersion
    ) {
    }

    public record OwnerParty(
            long partyId,
            String roleCode,
            boolean primary,
            BigDecimal ownershipPercent,
            int sequenceNo
    ) {
    }

    public record LifecycleEvent(
            long lifecycleEventId,
            String eventTypeCode,
            String fromStatusCode,
            String toStatusCode,
            String correlationId,
            OffsetDateTime eventAt,
            String eventBy
    ) {
    }

    public record AccountDetails(
            AccountSummary account,
            List<OwnerParty> owners,
            List<LifecycleEvent> lifecycleEvents
    ) {
    }

    public record AccountSearchResponse(
            List<AccountSummary> items,
            long total,
            int offset,
            int limit
    ) {
    }
}
