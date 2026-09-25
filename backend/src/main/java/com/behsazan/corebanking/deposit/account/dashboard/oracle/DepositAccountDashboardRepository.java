package com.behsazan.corebanking.deposit.account.dashboard.oracle;

import com.behsazan.corebanking.deposit.account.dashboard.domain.DepositAccountDashboardModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class DepositAccountDashboardRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public DepositAccountDashboardRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-account:DPS2}") String schema) {
        this.jdbc = jdbc;
        this.schema = requireIdentifier(schema);
    }

    public AccountStatusSummary accounts() {
        String sql = """
            SELECT COUNT(*) TOTAL_ACCOUNTS,
                   SUM(CASE WHEN ACCOUNT_STATUS_CODE='PENDING_ACTIVATION' THEN 1 ELSE 0 END) PENDING_ACTIVATION_ACCOUNTS,
                   SUM(CASE WHEN ACCOUNT_STATUS_CODE='ACTIVE' THEN 1 ELSE 0 END) ACTIVE_ACCOUNTS,
                   SUM(CASE WHEN ACCOUNT_STATUS_CODE='SUSPENDED' THEN 1 ELSE 0 END) SUSPENDED_ACCOUNTS,
                   SUM(CASE WHEN ACCOUNT_STATUS_CODE='DORMANT' THEN 1 ELSE 0 END) DORMANT_ACCOUNTS,
                   SUM(CASE WHEN ACCOUNT_STATUS_CODE='CLOSED' THEN 1 ELSE 0 END) CLOSED_ACCOUNTS
              FROM %s.DEPOSIT_ACCOUNT
            """.formatted(schema);
        return jdbc.queryForObject(sql, Map.of(), (r,n) -> new AccountStatusSummary(
                r.getLong("TOTAL_ACCOUNTS"),
                r.getLong("PENDING_ACTIVATION_ACCOUNTS"),
                r.getLong("ACTIVE_ACCOUNTS"),
                r.getLong("SUSPENDED_ACCOUNTS"),
                r.getLong("DORMANT_ACCOUNTS"),
                r.getLong("CLOSED_ACCOUNTS")));
    }

    public HoldSummary holds() {
        String sql = """
            SELECT COUNT(*) ACTIVE_HOLDS,
                   SUM(CASE WHEN HOLD_TYPE_CODE='FULL' THEN 1 ELSE 0 END) FULL_HOLDS,
                   SUM(CASE WHEN HOLD_TYPE_CODE='PARTIAL' THEN 1 ELSE 0 END) PARTIAL_HOLDS,
                   SUM(CASE WHEN HOLD_TYPE_CODE='DEBIT_ONLY' THEN 1 ELSE 0 END) DEBIT_ONLY_HOLDS,
                   SUM(CASE WHEN HOLD_TYPE_CODE='CREDIT_ONLY' THEN 1 ELSE 0 END) CREDIT_ONLY_HOLDS
              FROM %s.DEPOSIT_ACCOUNT_HOLD
             WHERE HOLD_STATUS_CODE='ACTIVE'
            """.formatted(schema);
        return jdbc.queryForObject(sql, Map.of(), (r,n) -> new HoldSummary(
                r.getLong("ACTIVE_HOLDS"),
                r.getLong("FULL_HOLDS"),
                r.getLong("PARTIAL_HOLDS"),
                r.getLong("DEBIT_ONLY_HOLDS"),
                r.getLong("CREDIT_ONLY_HOLDS")));
    }

    public TransactionSummary transactions() {
        String sql = """
            SELECT COUNT(*) TOTAL_TRANSACTIONS,
                   SUM(CASE WHEN TRANSACTION_STATUS_CODE='POSTED' THEN 1 ELSE 0 END) POSTED_TRANSACTIONS,
                   SUM(CASE WHEN TRANSACTION_STATUS_CODE IN ('INITIATED','VALIDATED','PENDING_AUTH','AUTHORIZED') THEN 1 ELSE 0 END) PENDING_TRANSACTIONS,
                   SUM(CASE WHEN TRANSACTION_STATUS_CODE='REJECTED' THEN 1 ELSE 0 END) REJECTED_TRANSACTIONS,
                   SUM(CASE WHEN TRANSACTION_STATUS_CODE='REVERSED' THEN 1 ELSE 0 END) REVERSED_TRANSACTIONS
              FROM %s.DEPOSIT_TRANSACTION
            """.formatted(schema);
        return jdbc.queryForObject(sql, Map.of(), (r,n) -> new TransactionSummary(
                r.getLong("TOTAL_TRANSACTIONS"),
                r.getLong("POSTED_TRANSACTIONS"),
                r.getLong("PENDING_TRANSACTIONS"),
                r.getLong("REJECTED_TRANSACTIONS"),
                r.getLong("REVERSED_TRANSACTIONS")));
    }

    public List<CurrencyBalanceSummary> balances() {
        String sql = """
            SELECT CURRENCY_CODE,
                   COUNT(*) ACCOUNT_COUNT,
                   NVL(SUM(LEDGER_BALANCE),0) LEDGER_BALANCE,
                   NVL(SUM(AVAILABLE_BALANCE),0) AVAILABLE_BALANCE,
                   NVL(SUM(BLOCKED_AMOUNT),0) BLOCKED_AMOUNT,
                   NVL(SUM(PENDING_DEBIT_AMOUNT),0) PENDING_DEBIT_AMOUNT,
                   NVL(SUM(PENDING_CREDIT_AMOUNT),0) PENDING_CREDIT_AMOUNT
              FROM %s.DEPOSIT_ACCOUNT_BALANCE
             GROUP BY CURRENCY_CODE
             ORDER BY CURRENCY_CODE
            """.formatted(schema);
        return jdbc.query(sql, Map.of(), (r,n) -> new CurrencyBalanceSummary(
                r.getString("CURRENCY_CODE"),
                r.getLong("ACCOUNT_COUNT"),
                nz(r.getBigDecimal("LEDGER_BALANCE")),
                nz(r.getBigDecimal("AVAILABLE_BALANCE")),
                nz(r.getBigDecimal("BLOCKED_AMOUNT")),
                nz(r.getBigDecimal("PENDING_DEBIT_AMOUNT")),
                nz(r.getBigDecimal("PENDING_CREDIT_AMOUNT"))));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String requireIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_$#]*")) {
            throw new IllegalArgumentException("Oracle schema identifier نامعتبر است.");
        }
        return value.toUpperCase();
    }
}
