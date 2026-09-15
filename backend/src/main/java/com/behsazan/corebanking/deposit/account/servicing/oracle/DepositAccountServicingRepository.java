package com.behsazan.corebanking.deposit.account.servicing.oracle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class DepositAccountServicingRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String accountSchema;

    public DepositAccountServicingRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-account:DPS2}") String accountSchema
    ) {
        this.jdbc = jdbc;
        this.accountSchema = requireIdentifier(accountSchema);
    }

    public Optional<AccountLockRow> lockAccount(long accountId) {
        String sql = """
                SELECT ACCOUNT_ID, OPENING_REQUEST_ID, ACCOUNT_STATUS_CODE, RECORD_VERSION
                  FROM %s.DEPOSIT_ACCOUNT
                 WHERE ACCOUNT_ID=:accountId
                   FOR UPDATE
                """.formatted(accountSchema);
        List<AccountLockRow> rows = jdbc.query(
                sql,
                new MapSqlParameterSource().addValue("accountId", accountId, Types.NUMERIC),
                (rs, rowNum) -> new AccountLockRow(
                        rs.getLong("ACCOUNT_ID"),
                        rs.getLong("OPENING_REQUEST_ID"),
                        rs.getString("ACCOUNT_STATUS_CODE"),
                        rs.getLong("RECORD_VERSION")
                )
        );
        return rows.stream().findFirst();
    }

    public int closeAccount(long accountId, long expectedRecordVersion, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_ACCOUNT
                   SET ACCOUNT_STATUS_CODE='CLOSED',
                       UPDATED_AT=SYSTIMESTAMP,
                       UPDATED_BY=:actor,
                       RECORD_VERSION=RECORD_VERSION+1
                 WHERE ACCOUNT_ID=:accountId
                   AND ACCOUNT_STATUS_CODE='ACTIVE'
                   AND RECORD_VERSION=:expectedRecordVersion
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("expectedRecordVersion", expectedRecordVersion, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public long nextLifecycleEventId() {
        Long value = jdbc.getJdbcOperations().queryForObject(
                "SELECT " + accountSchema + ".SEQ_DEP_ACCOUNT_LIFECYCLE_EVT.NEXTVAL FROM DUAL",
                Long.class
        );
        if (value == null) throw new IllegalStateException("SEQ_DEP_ACCOUNT_LIFECYCLE_EVT returned null.");
        return value;
    }

    public int insertCloseEvent(
            long lifecycleEventId,
            long accountId,
            long openingRequestId,
            String actor,
            String correlationId
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT (
                    LIFECYCLE_EVENT_ID, ACCOUNT_ID, OPENING_REQUEST_ID,
                    EVENT_TYPE_CODE, FROM_STATUS_CODE, TO_STATUS_CODE,
                    CORRELATION_ID, EVENT_AT, EVENT_BY, CREATED_AT, CREATED_BY
                ) VALUES (
                    :lifecycleEventId, :accountId, :openingRequestId,
                    'CLOSE', 'ACTIVE', 'CLOSED',
                    :correlationId, SYSTIMESTAMP, :actor, SYSTIMESTAMP, :actor
                )
                """.formatted(accountSchema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("lifecycleEventId", lifecycleEventId, Types.NUMERIC)
                .addValue("accountId", accountId, Types.NUMERIC)
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("correlationId", correlationId, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    private static String requireIdentifier(String raw) {
        if (raw == null) throw new IllegalArgumentException("Oracle identifier is required.");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_$#]{0,127}")) {
            throw new IllegalArgumentException("Invalid Oracle identifier: " + raw);
        }
        return normalized;
    }

    public record AccountLockRow(
            long accountId,
            long openingRequestId,
            String accountStatusCode,
            long recordVersion
    ) {
    }
}
