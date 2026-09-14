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
                       OPENING_AMOUNT, REQUEST_STATUS_CODE, CREATED_ACCOUNT_ID
                  FROM %s.DEPOSIT_OPENING_REQUEST
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   FOR UPDATE
                """.formatted(openingSchema);
        return queryOpening(sql, openingRequestId);
    }

    public Optional<OpeningLink> findOpening(long openingRequestId) {
        String sql = """
                SELECT OPENING_REQUEST_ID, REQUEST_NO, PRODUCT_VERSION_ID, CURRENCY_CODE,
                       OPENING_AMOUNT, REQUEST_STATUS_CODE, CREATED_ACCOUNT_ID
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
                       CURRENCY_CODE, OPENING_AMOUNT, ACCOUNT_STATUS_CODE,
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
                        rs.getString("CURRENCY_CODE"),
                        rs.getBigDecimal("OPENING_AMOUNT"),
                        rs.getString("ACCOUNT_STATUS_CODE"),
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
                    CURRENCY_CODE, OPENING_AMOUNT, ACCOUNT_STATUS_CODE,
                    CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :accountId, :accountNo, :openingRequestId, :productVersionId,
                    :currencyCode, :openingAmount, 'PENDING_ACTIVATION',
                    SYSTIMESTAMP, :actor, 1
                )
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("accountNo", accountNo, Types.VARCHAR)
                .addValue("openingRequestId", opening.openingRequestId(), Types.NUMERIC)
                .addValue("productVersionId", opening.productVersionId(), Types.NUMERIC)
                .addValue("currencyCode", opening.currencyCode(), Types.VARCHAR)
                .addValue("openingAmount", opening.openingAmount(), Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int linkCreatedAccount(long openingRequestId, long accountId, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_REQUEST
                   SET CREATED_ACCOUNT_ID = :accountId,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND CREATED_ACCOUNT_ID IS NULL
                """.formatted(openingSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int activateAccount(long accountId, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_ACCOUNT
                   SET ACCOUNT_STATUS_CODE = 'ACTIVE',
                       ACTIVATED_AT = SYSTIMESTAMP,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE ACCOUNT_ID = :accountId
                   AND ACCOUNT_STATUS_CODE = 'PENDING_ACTIVATION'
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int completeOpening(long openingRequestId, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_REQUEST
                   SET REQUEST_STATUS_CODE = 'COMPLETED',
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
            String requestStatusCode,
            Long createdAccountId
    ) {
    }

    public record AccountRow(
            long accountId,
            String accountNo,
            long openingRequestId,
            Long productVersionId,
            String currencyCode,
            BigDecimal openingAmount,
            String accountStatusCode,
            OffsetDateTime createdAt,
            OffsetDateTime activatedAt
    ) {
    }
}
