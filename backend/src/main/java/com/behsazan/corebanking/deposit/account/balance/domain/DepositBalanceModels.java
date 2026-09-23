package com.behsazan.corebanking.deposit.account.balance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositBalanceModels {
    private DepositBalanceModels() {}

    public record BalanceSnapshot(
            long accountBalanceId,
            long accountId,
            String currencyCode,
            BigDecimal ledgerBalance,
            BigDecimal availableBalance,
            BigDecimal blockedAmount,
            BigDecimal unclearedAmount,
            BigDecimal pendingDebitAmount,
            BigDecimal pendingCreditAmount,
            Long lastSubledgerEntryId,
            OffsetDateTime balanceAsOf,
            long recordVersion
    ) {}

    public record SubledgerEntry(
            long subledgerEntryId,
            long accountId,
            Long transactionId,
            Long transactionLegId,
            int entrySequenceNo,
            String postingReference,
            String sourceEntityType,
            long sourceEntityId,
            String debitCreditCode,
            BigDecimal amount,
            String currencyCode,
            LocalDate bookingDate,
            LocalDate valueDate,
            OffsetDateTime postedAt,
            Long reversalOfEntryId,
            String glPostingReference,
            String createdBy
    ) {}

    public record BalanceReservation(
            long balanceReservationId,
            long accountId,
            Long transactionId,
            String reservationReference,
            String reservationTypeCode,
            BigDecimal amount,
            String currencyCode,
            String reservationStatusCode,
            OffsetDateTime expiresAt,
            OffsetDateTime consumedAt,
            OffsetDateTime releasedAt,
            String releaseReasonCode,
            String ownerEntityType,
            Long ownerEntityId,
            String ownerReference,
            long recordVersion
    ) {}

    public record BalanceView(
            BalanceSnapshot balance,
            List<SubledgerEntry> subledgerEntries,
            List<BalanceReservation> reservations
    ) {}

    public record PostEntryRequest(
            String debitCreditCode,
            BigDecimal amount,
            String currencyCode,
            LocalDate bookingDate,
            LocalDate valueDate,
            String postingReference,
            String sourceEntityType,
            Long sourceEntityId,
            Long reversalOfEntryId,
            String glPostingReference
    ) {}

    public record PostEntryResponse(BalanceView state, long subledgerEntryId, boolean idempotentReplay) {}

    public record CreateReservationRequest(
            String reservationReference,
            String reservationTypeCode,
            BigDecimal amount,
            String currencyCode,
            OffsetDateTime expiresAt,
            Long transactionId,
            String ownerEntityType,
            Long ownerEntityId,
            String ownerReference
    ) {}

    public record ReleaseReservationRequest(String reasonCode) {}

    public record ReservationActionResponse(BalanceView state, long balanceReservationId, boolean idempotentReplay) {}
}
