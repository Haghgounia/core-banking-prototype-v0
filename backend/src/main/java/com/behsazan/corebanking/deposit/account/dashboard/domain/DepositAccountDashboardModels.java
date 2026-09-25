package com.behsazan.corebanking.deposit.account.dashboard.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositAccountDashboardModels {
    private DepositAccountDashboardModels() {}

    public record AccountStatusSummary(
            long totalAccounts,
            long pendingActivationAccounts,
            long activeAccounts,
            long suspendedAccounts,
            long dormantAccounts,
            long closedAccounts) {}

    public record HoldSummary(
            long activeHolds,
            long fullHolds,
            long partialHolds,
            long debitOnlyHolds,
            long creditOnlyHolds) {}

    public record TransactionSummary(
            long totalTransactions,
            long postedTransactions,
            long pendingTransactions,
            long rejectedTransactions,
            long reversedTransactions) {}

    public record CurrencyBalanceSummary(
            String currencyCode,
            long accountCount,
            BigDecimal ledgerBalance,
            BigDecimal availableBalance,
            BigDecimal blockedAmount,
            BigDecimal pendingDebitAmount,
            BigDecimal pendingCreditAmount) {}

    public record DashboardView(
            OffsetDateTime generatedAt,
            AccountStatusSummary accounts,
            HoldSummary holds,
            TransactionSummary transactions,
            List<CurrencyBalanceSummary> balances) {}
}
