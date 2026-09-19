package com.behsazan.corebanking.deposit.account.oracle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class DepositAccountRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String openingSchema;
    private final String accountSchema;

    public DepositAccountRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-opening:DPS2}") String openingSchema,
            @Value("${core-banking.schemas.deposit-account:DPS2}") String accountSchema
    ) {
        this.jdbc = jdbc;
        this.openingSchema = requireIdentifier(openingSchema);
        this.accountSchema = requireIdentifier(accountSchema);
    }

    public Optional<OpeningLink> lockOpening(long openingRequestId) {
        String sql = """
                SELECT OPENING_REQUEST_ID, REQUEST_NO, PRODUCT_VERSION_ID, CURRENCY_CODE,
                       OPENING_AMOUNT, OWNERSHIP_TYPE_CODE, REQUESTED_OPENING_DATE,
                       ACTIVATION_STATUS_CODE, ACTIVATION_DEADLINE_AT,
                       REQUEST_STATUS_CODE, CREATED_ACCOUNT_ID
                  FROM %s.DEPOSIT_OPENING_REQUEST
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   FOR UPDATE
                """.formatted(openingSchema);
        return queryOpening(sql, openingRequestId);
    }

    public Optional<OpeningLink> findOpening(long openingRequestId) {
        String sql = """
                SELECT OPENING_REQUEST_ID, REQUEST_NO, PRODUCT_VERSION_ID, CURRENCY_CODE,
                       OPENING_AMOUNT, OWNERSHIP_TYPE_CODE, REQUESTED_OPENING_DATE,
                       ACTIVATION_STATUS_CODE, ACTIVATION_DEADLINE_AT,
                       REQUEST_STATUS_CODE, CREATED_ACCOUNT_ID
                  FROM %s.DEPOSIT_OPENING_REQUEST
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                """.formatted(openingSchema);
        return queryOpening(sql, openingRequestId);
    }

    private Optional<OpeningLink> queryOpening(String sql, long openingRequestId) {
        List<OpeningLink> rows = jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new OpeningLink(
                        rs.getLong("OPENING_REQUEST_ID"),
                        rs.getString("REQUEST_NO"),
                        rs.getObject("PRODUCT_VERSION_ID", Long.class),
                        rs.getString("CURRENCY_CODE"),
                        rs.getBigDecimal("OPENING_AMOUNT"),
                        rs.getString("OWNERSHIP_TYPE_CODE"),
                        rs.getDate("REQUESTED_OPENING_DATE") == null ? null : rs.getDate("REQUESTED_OPENING_DATE").toLocalDate(),
                        rs.getString("ACTIVATION_STATUS_CODE"),
                        toOffsetDateTime(rs.getTimestamp("ACTIVATION_DEADLINE_AT")),
                        rs.getString("REQUEST_STATUS_CODE"),
                        rs.getObject("CREATED_ACCOUNT_ID", Long.class)
                ));
        return rows.stream().findFirst();
    }

    public Optional<AccountRow> findAccount(long accountId) {
        return queryAccount(accountId, false);
    }

    public Optional<AccountRow> lockAccount(long accountId) {
        return queryAccount(accountId, true);
    }

    private Optional<AccountRow> queryAccount(long accountId, boolean lock) {
        String sql = """
                SELECT ACCOUNT_ID, ACCOUNT_NO, OPENING_REQUEST_ID, PRODUCT_VERSION_ID,
                       OPENED_PRODUCT_VERSION_ID, CURRENT_PRODUCT_VERSION_ID, OWNERSHIP_TYPE_CODE,
                       CURRENCY_CODE, OPENING_AMOUNT, OPENED_ON, ACCOUNT_STATUS_CODE,
                       ACTIVATION_DEADLINE_AT, ACTIVATION_POLICY_VERSION,
                       LEDGER_BALANCE, AVAILABLE_BALANCE, DEBIT_CAPABILITY_CODE,
                       CREATED_AT, ACTIVATED_AT
                  FROM %s.DEPOSIT_ACCOUNT
                 WHERE ACCOUNT_ID = :accountId
                """.formatted(accountSchema) + (lock ? " FOR UPDATE" : "");
        List<AccountRow> rows = jdbc.query(sql,
                new MapSqlParameterSource().addValue("accountId", accountId, Types.NUMERIC),
                (rs, rowNum) -> new AccountRow(
                        rs.getLong("ACCOUNT_ID"),
                        rs.getString("ACCOUNT_NO"),
                        rs.getLong("OPENING_REQUEST_ID"),
                        rs.getObject("PRODUCT_VERSION_ID", Long.class),
                        rs.getObject("OPENED_PRODUCT_VERSION_ID", Long.class),
                        rs.getObject("CURRENT_PRODUCT_VERSION_ID", Long.class),
                        rs.getString("OWNERSHIP_TYPE_CODE"),
                        rs.getString("CURRENCY_CODE"),
                        rs.getBigDecimal("OPENING_AMOUNT"),
                        rs.getDate("OPENED_ON") == null ? null : rs.getDate("OPENED_ON").toLocalDate(),
                        rs.getString("ACCOUNT_STATUS_CODE"),
                        toOffsetDateTime(rs.getTimestamp("ACTIVATION_DEADLINE_AT")),
                        rs.getString("ACTIVATION_POLICY_VERSION"),
                        rs.getBigDecimal("LEDGER_BALANCE"),
                        rs.getBigDecimal("AVAILABLE_BALANCE"),
                        rs.getString("DEBIT_CAPABILITY_CODE"),
                        toOffsetDateTime(rs.getTimestamp("CREATED_AT")),
                        toOffsetDateTime(rs.getTimestamp("ACTIVATED_AT"))
                ));
        return rows.stream().findFirst();
    }

    public long nextAccountId() {
        return nextValue("SEQ_DEPOSIT_ACCOUNT");
    }

    public long nextLifecycleEventId() {
        return nextValue("SEQ_DEP_ACCOUNT_LIFECYCLE_EVT");
    }

    public int insertAccount(
            long accountId,
            String accountNo,
            OpeningLink opening,
            String actor
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_ACCOUNT (
                    ACCOUNT_ID, ACCOUNT_NO, OPENING_REQUEST_ID, PRODUCT_VERSION_ID,
                    OPENED_PRODUCT_VERSION_ID, CURRENT_PRODUCT_VERSION_ID, OWNERSHIP_TYPE_CODE,
                    CURRENCY_CODE, OPENING_AMOUNT, OPENED_ON, ACCOUNT_STATUS_CODE,
                    ACTIVATION_DEADLINE_AT, ACTIVATION_POLICY_VERSION,
                    LEDGER_BALANCE, AVAILABLE_BALANCE, DEBIT_CAPABILITY_CODE,
                    CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :accountId, :accountNo, :openingRequestId, :productVersionId,
                    :productVersionId, :productVersionId, :ownershipTypeCode,
                    :currencyCode, :openingAmount, :openedOn, 'PENDING_ACTIVATION',
                    COALESCE(:activationDeadlineAt, SYSTIMESTAMP + INTERVAL '7' DAY), 'ACT-GATE-2026.09-v5',
                    0, 0, 'DISABLED_PENDING_ACTIVATION',
                    SYSTIMESTAMP, :actor, 1
                )
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("accountNo", accountNo, Types.VARCHAR)
                .addValue("openingRequestId", opening.openingRequestId(), Types.NUMERIC)
                .addValue("productVersionId", opening.productVersionId(), Types.NUMERIC)
                .addValue("ownershipTypeCode", opening.ownershipTypeCode(), Types.VARCHAR)
                .addValue("currencyCode", opening.currencyCode(), Types.VARCHAR)
                .addValue("openingAmount", opening.openingAmount(), Types.NUMERIC)
                .addValue("openedOn", opening.requestedOpeningDate(), Types.DATE)
                .addValue("activationDeadlineAt", opening.activationDeadlineAt() == null ? null : Timestamp.from(opening.activationDeadlineAt().toInstant()), Types.TIMESTAMP)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int linkCreatedAccount(long openingRequestId, long accountId, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_REQUEST r
                   SET CREATED_ACCOUNT_ID = :accountId,
                       ACTIVATION_STATUS_CODE = 'PENDING_READINESS',
                       ACTIVATION_DEADLINE_AT = COALESCE(r.ACTIVATION_DEADLINE_AT,
                           (SELECT a.ACTIVATION_DEADLINE_AT FROM %s.DEPOSIT_ACCOUNT a WHERE a.ACCOUNT_ID = :accountId)),
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND CREATED_ACCOUNT_ID IS NULL
                """.formatted(openingSchema, accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int activateAccount(long accountId, String debitCapabilityCode, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_ACCOUNT
                   SET ACCOUNT_STATUS_CODE = 'ACTIVE',
                       ACTIVATED_AT = SYSTIMESTAMP,
                       DEBIT_CAPABILITY_CODE = :debitCapabilityCode,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE ACCOUNT_ID = :accountId
                   AND ACCOUNT_STATUS_CODE = 'PENDING_ACTIVATION'
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("debitCapabilityCode", debitCapabilityCode, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int completeOpening(long openingRequestId, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_REQUEST
                   SET REQUEST_STATUS_CODE = 'COMPLETED',
                       ACTIVATION_STATUS_CODE = 'ACTIVATED',
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND REQUEST_STATUS_CODE = 'APPROVED'
                """.formatted(openingSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int updateActivationStatus(long openingRequestId, String activationStatusCode, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_REQUEST
                   SET ACTIVATION_STATUS_CODE = :activationStatusCode,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                """.formatted(openingSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("activationStatusCode", activationStatusCode, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public boolean hasUnresolvedDebitCapabilityChecks(long openingRequestId) {
        String sql = """
                SELECT COUNT(*)
                  FROM %s.DEPOSIT_OPENING_CHECK c
                 WHERE c.OPENING_REQUEST_ID = :openingRequestId
                   AND c.BLOCKING_SCOPE_CODE = 'DEBIT_CAPABILITY'
                   AND c.ATTEMPT_NO = (
                       SELECT MAX(x.ATTEMPT_NO)
                         FROM %s.DEPOSIT_OPENING_CHECK x
                        WHERE x.OPENING_REQUEST_ID = c.OPENING_REQUEST_ID
                          AND x.CHECK_CODE = c.CHECK_CODE
                   )
                   AND c.RESULT_STATUS_CODE NOT IN ('PASS','WAIVED','NOT_APPLICABLE')
                """.formatted(openingSchema, openingSchema);
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                Integer.class);
        return count != null && count > 0;
    }

    public int unresolvedRequiredAccountCreationChecks(long openingRequestId) {
        String sql = """
                SELECT COUNT(*)
                  FROM %s.REF_DEP_OPEN_CHECK r
                 WHERE r.IS_ACTIVE = 1
                   AND r.DEFAULT_REQUIRED_FLAG = 1
                   AND r.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM %s.DEPOSIT_OPENING_CHECK c
                        WHERE c.OPENING_REQUEST_ID = :openingRequestId
                          AND c.CHECK_CODE = r.CHECK_CODE
                          AND c.ATTEMPT_NO = (
                              SELECT MAX(x.ATTEMPT_NO)
                                FROM %s.DEPOSIT_OPENING_CHECK x
                               WHERE x.OPENING_REQUEST_ID = c.OPENING_REQUEST_ID
                                 AND x.CHECK_CODE = c.CHECK_CODE
                          )
                          AND c.RESULT_STATUS_CODE IN ('PASS','WAIVED','NOT_APPLICABLE')
                   )
                """.formatted(openingSchema, openingSchema, openingSchema);
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                Integer.class);
        return count == null ? 0 : count;
    }

    public boolean openingFinancialPlanReady(long openingRequestId) {
        String sql = """
                SELECT
                  (SELECT COUNT(*)
                     FROM %s.DEPOSIT_OPENING_FUNDING f
                    WHERE f.OPENING_REQUEST_ID = :openingRequestId) AS FUNDING_COUNT,
                  (SELECT NVL(SUM(f.FUNDING_AMOUNT),0)
                     FROM %s.DEPOSIT_OPENING_FUNDING f
                    WHERE f.OPENING_REQUEST_ID = :openingRequestId) AS FUNDING_TOTAL,
                  (SELECT COUNT(*)
                     FROM %s.DEPOSIT_OPENING_OBLIGATION o
                    WHERE o.OPENING_REQUEST_ID = :openingRequestId) AS OBLIGATION_COUNT,
                  (SELECT COUNT(*)
                     FROM %s.DEPOSIT_OPENING_OBLIGATION o
                    WHERE o.OPENING_REQUEST_ID = :openingRequestId
                      AND o.OBLIGATION_TYPE_CODE = 'INITIAL_BALANCE') AS INITIAL_BALANCE_COUNT,
                  (SELECT NVL(SUM(o.FINAL_AMOUNT),0)
                     FROM %s.DEPOSIT_OPENING_OBLIGATION o
                    WHERE o.OPENING_REQUEST_ID = :openingRequestId) AS OBLIGATION_TOTAL
                FROM dual
                """.formatted(openingSchema, openingSchema, openingSchema, openingSchema, openingSchema);
        return Boolean.TRUE.equals(jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> rs.getInt("FUNDING_COUNT") > 0
                        && rs.getInt("OBLIGATION_COUNT") > 0
                        && rs.getInt("INITIAL_BALANCE_COUNT") > 0
                        && rs.getBigDecimal("FUNDING_TOTAL").compareTo(rs.getBigDecimal("OBLIGATION_TOTAL")) >= 0));
    }

    public int updateBalances(long accountId, BigDecimal ledgerBalance, BigDecimal availableBalance, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_ACCOUNT
                   SET LEDGER_BALANCE = :ledgerBalance,
                       AVAILABLE_BALANCE = :availableBalance,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE ACCOUNT_ID = :accountId
                   AND ACCOUNT_STATUS_CODE = 'PENDING_ACTIVATION'
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("ledgerBalance", ledgerBalance, Types.NUMERIC)
                .addValue("availableBalance", availableBalance, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int insertOpeningCompletionHistory(long openingRequestId, String actor, String correlationId) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_STATUS_HISTORY (
                    OPENING_REQUEST_ID, FROM_STATUS_CODE, TO_STATUS_CODE,
                    CHANGE_NOTE, CHANGED_BY, CORRELATION_ID, CREATED_BY
                ) VALUES (
                    :openingRequestId, 'APPROVED', 'COMPLETED',
                    'Deposit account activated', :actor, :correlationId, :actor
                )
                """.formatted(openingSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR)
                .addValue("correlationId", correlationId, Types.VARCHAR));
    }

    public int insertLifecycleEvent(
            long eventId,
            long accountId,
            long openingRequestId,
            String eventTypeCode,
            String fromStatusCode,
            String toStatusCode,
            String actor,
            String correlationId
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT (
                    LIFECYCLE_EVENT_ID, ACCOUNT_ID, OPENING_REQUEST_ID,
                    EVENT_TYPE_CODE, FROM_STATUS_CODE, TO_STATUS_CODE,
                    CORRELATION_ID, EVENT_AT, EVENT_BY, CREATED_AT, CREATED_BY
                ) VALUES (
                    :eventId, :accountId, :openingRequestId,
                    :eventTypeCode, :fromStatusCode, :toStatusCode,
                    :correlationId, SYSTIMESTAMP, :actor, SYSTIMESTAMP, :actor
                )
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("eventId", eventId, Types.NUMERIC)
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("eventTypeCode", eventTypeCode, Types.VARCHAR)
                .addValue("fromStatusCode", fromStatusCode, Types.VARCHAR)
                .addValue("toStatusCode", toStatusCode, Types.VARCHAR)
                .addValue("correlationId", correlationId, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    private long nextValue(String sequenceName) {
        String safeSequence = requireIdentifier(sequenceName);
        return jdbc.getJdbcOperations().queryForObject(
                "SELECT " + accountSchema + "." + safeSequence + ".NEXTVAL FROM DUAL",
                Long.class
        );
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static String requireIdentifier(String raw) {
        if (raw == null) throw new IllegalArgumentException("Oracle identifier is required.");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_$#]{0,127}")) {
            throw new IllegalArgumentException("Invalid Oracle identifier: " + raw);
        }
        return normalized;
    }

    public record OpeningLink(
            long openingRequestId,
            String requestNo,
            Long productVersionId,
            String currencyCode,
            BigDecimal openingAmount,
            String ownershipTypeCode,
            java.time.LocalDate requestedOpeningDate,
            String activationStatusCode,
            OffsetDateTime activationDeadlineAt,
            String requestStatusCode,
            Long createdAccountId
    ) {
    }

    public record AccountRow(
            long accountId,
            String accountNo,
            long openingRequestId,
            Long productVersionId,
            Long openedProductVersionId,
            Long currentProductVersionId,
            String ownershipTypeCode,
            String currencyCode,
            BigDecimal openingAmount,
            java.time.LocalDate openedOn,
            String accountStatusCode,
            OffsetDateTime activationDeadlineAt,
            String activationPolicyVersion,
            BigDecimal ledgerBalance,
            BigDecimal availableBalance,
            String debitCapabilityCode,
            OffsetDateTime createdAt,
            OffsetDateTime activatedAt
    ) {
    }

}
