package com.behsazan.corebanking.deposit.account.operations.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.BalanceReservation;
import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.BalanceSnapshot;
import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.SubledgerEntry;

public final class DepositAccountOperationsModels {
    private DepositAccountOperationsModels() {}

    public record AccountSummary(long accountId,String accountNo,long openingRequestId,String requestNo,Long primaryPartyId,Long productVersionId,String productFamilyCode,String currencyCode,BigDecimal openingAmount,String accountStatusCode,String accountName,String openingOrgUnitCode,LocalDate dormancyDate,OffsetDateTime createdAt,OffsetDateTime activatedAt,long recordVersion) {}
    /* Phase 8 compatibility: OwnerParty evolved into persistent AccountParty. */
    public record OwnerParty(long partyId,String roleCode,boolean primary,BigDecimal ownershipPercent,int sequenceNo) {}
    public record AccountParty(long accountPartyId,long partyId,String roleCode,boolean primary,BigDecimal ownershipPercent,LocalDate validFrom,LocalDate validTo,String statusCode,Long sourceOpeningPartyId) {}
    public record AccountContact(long accountContactId,String contactTypeCode,String contactValue,String purposeCode,boolean primary,LocalDate validFrom,LocalDate validTo) {}
    public record ServicingHistory(long servicingHistoryId,String changeTypeCode,String oldValue,String newValue,String reasonCode,OffsetDateTime effectiveAt,String sourceEntityType,Long sourceEntityId,String createdBy) {}
    public record LifecycleEvent(long lifecycleEventId,String eventTypeCode,String fromStatusCode,String toStatusCode,String correlationId,OffsetDateTime eventAt,String eventBy) {}
    public record AccountStatusHistory(long accountStatusHistoryId,String fromStatusCode,String toStatusCode,OffsetDateTime effectiveAt,String reasonCode,String eventReference,String createdBy) {}
    public record AccountHold(long accountHoldId,String holdTypeCode,BigDecimal holdAmount,String currencyCode,String holdReasonCode,String sourceReference,OffsetDateTime validFrom,OffsetDateTime validTo,String holdStatusCode,OffsetDateTime releasedAt,String originSystemCode,String originModuleCode,String originRequestRef,String originExecutionModeCode,String releasePolicyCode,long recordVersion) {}
    public record HoldHistory(long holdHistoryId,long accountHoldId,String actionCode,OffsetDateTime actionAt,String oldStatusCode,String newStatusCode,String reasonCode,String actionSourceSystemCode,String actionSourceModuleCode,String actionRequestRef,String correlationId,String actionExecutionModeCode,BigDecimal oldHoldAmount,BigDecimal newHoldAmount,BigDecimal releasedAmount,String createdBy) {}
    public record AccountDetails(AccountSummary account,List<AccountParty> parties,List<AccountContact> contacts,List<ServicingHistory> servicingHistory,List<LifecycleEvent> lifecycleEvents,List<AccountStatusHistory> statusHistory,List<AccountHold> holds,List<HoldHistory> holdHistory,BalanceSnapshot balance,List<SubledgerEntry> subledgerEntries,List<BalanceReservation> reservations) {}
    public record AccountSearchResponse(List<AccountSummary> items,long total,int offset,int limit) {}
}
