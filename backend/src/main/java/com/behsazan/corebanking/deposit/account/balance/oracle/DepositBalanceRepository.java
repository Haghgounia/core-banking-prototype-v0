package com.behsazan.corebanking.deposit.account.balance.oracle;

import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class DepositBalanceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public DepositBalanceRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-account:DPS2}") String schema
    ) {
        this.jdbc = jdbc;
        this.schema = requireIdentifier(schema);
    }

    public Optional<AccountFinancialRow> findAccount(long accountId, boolean lock) {
        String sql = """
                SELECT ACCOUNT_ID, OPENING_REQUEST_ID, ACCOUNT_STATUS_CODE, CURRENCY_CODE, OPENED_ON,
                       LEDGER_BALANCE, AVAILABLE_BALANCE, RECORD_VERSION
                  FROM %s.DEPOSIT_ACCOUNT
                 WHERE ACCOUNT_ID = :accountId
                """.formatted(schema) + (lock ? " FOR UPDATE" : "");
        var rows = jdbc.query(sql, new MapSqlParameterSource("accountId", accountId), (rs, n) ->
                new AccountFinancialRow(
                        rs.getLong("ACCOUNT_ID"),
                        rs.getObject("OPENING_REQUEST_ID", Long.class),
                        rs.getString("ACCOUNT_STATUS_CODE"),
                        rs.getString("CURRENCY_CODE"),
                        rs.getDate("OPENED_ON") == null ? null : rs.getDate("OPENED_ON").toLocalDate(),
                        rs.getBigDecimal("LEDGER_BALANCE"),
                        rs.getBigDecimal("AVAILABLE_BALANCE"),
                        rs.getLong("RECORD_VERSION")
                ));
        return rows.stream().findFirst();
    }

    public Optional<BalanceSnapshot> findBalance(long accountId, boolean lock) {
        String sql = """
                SELECT ACCOUNT_BALANCE_ID, ACCOUNT_ID, CURRENCY_CODE, LEDGER_BALANCE, AVAILABLE_BALANCE,
                       BLOCKED_AMOUNT, UNCLEARED_AMOUNT, PENDING_DEBIT_AMOUNT, PENDING_CREDIT_AMOUNT,
                       LAST_SUBLEDGER_ENTRY_ID, BALANCE_AS_OF, RECORD_VERSION
                  FROM %s.DEPOSIT_ACCOUNT_BALANCE
                 WHERE ACCOUNT_ID = :accountId
                """.formatted(schema) + (lock ? " FOR UPDATE" : "");
        var rows = jdbc.query(sql, new MapSqlParameterSource("accountId", accountId), (rs, n) -> mapBalance(rs));
        return rows.stream().findFirst();
    }

    public long nextBalanceId() { return next("SEQ_DEPOSIT_ACCOUNT_BALANCE"); }
    public long nextSubledgerId() { return next("SEQ_DEPOSIT_SUBLEDGER_ENTRY"); }
    public long nextReservationId() { return next("SEQ_DEPOSIT_BALANCE_RESERVATION"); }
    public long nextIdempotencyId() { return next("SEQ_DEPOSIT_OPERATION_IDEMPOTENCY"); }

    public void insertBalance(long balanceId, long accountId, String currency, BigDecimal ledger,
                              BigDecimal available, BigDecimal blocked, BigDecimal pendingDebit,
                              Long lastEntryId, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_ACCOUNT_BALANCE(
                    ACCOUNT_BALANCE_ID,ACCOUNT_ID,CURRENCY_CODE,LEDGER_BALANCE,AVAILABLE_BALANCE,
                    BLOCKED_AMOUNT,UNCLEARED_AMOUNT,PENDING_DEBIT_AMOUNT,PENDING_CREDIT_AMOUNT,
                    LAST_SUBLEDGER_ENTRY_ID,BALANCE_AS_OF,CREATED_AT,CREATED_BY,RECORD_VERSION
                ) VALUES(
                    :id,:accountId,:currency,:ledger,:available,:blocked,0,:pendingDebit,0,
                    :lastEntryId,SYSTIMESTAMP,SYSTIMESTAMP,:actor,1
                )
                """.formatted(schema);
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", balanceId).addValue("accountId", accountId).addValue("currency", currency)
                .addValue("ledger", ledger).addValue("available", available).addValue("blocked", blocked)
                .addValue("pendingDebit", pendingDebit).addValue("lastEntryId", lastEntryId).addValue("actor", actor));
    }

    public int updateBalance(long accountId, long expectedVersion, BigDecimal ledger, BigDecimal available,
                             BigDecimal blocked, BigDecimal pendingDebit, Long lastEntryId, String actor) {
        String lastEntryAssignment = lastEntryId == null ? "" : "LAST_SUBLEDGER_ENTRY_ID=:lastEntryId,";
        String sql = """
                UPDATE %s.DEPOSIT_ACCOUNT_BALANCE
                   SET LEDGER_BALANCE=:ledger,
                       AVAILABLE_BALANCE=:available,
                       BLOCKED_AMOUNT=:blocked,
                       PENDING_DEBIT_AMOUNT=:pendingDebit,
                       %s
                       BALANCE_AS_OF=SYSTIMESTAMP,
                       UPDATED_AT=SYSTIMESTAMP,
                       UPDATED_BY=:actor,
                       RECORD_VERSION=RECORD_VERSION+1
                 WHERE ACCOUNT_ID=:accountId AND RECORD_VERSION=:expectedVersion
                """.formatted(schema, lastEntryAssignment);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ledger", ledger).addValue("available", available).addValue("blocked", blocked)
                .addValue("pendingDebit", pendingDebit)
                .addValue("actor", actor).addValue("accountId", accountId).addValue("expectedVersion", expectedVersion);
        if (lastEntryId != null) params.addValue("lastEntryId", lastEntryId);
        return jdbc.update(sql, params);
    }

    public void insertSubledger(long entryId, long accountId, Long transactionId, Long transactionLegId,
                                int sequenceNo, String postingReference, String sourceEntityType, long sourceEntityId,
                                String debitCreditCode, BigDecimal amount, String currencyCode,
                                LocalDate bookingDate, LocalDate valueDate, Long reversalOfEntryId,
                                String glPostingReference, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_SUBLEDGER_ENTRY(
                    SUBLEDGER_ENTRY_ID,ACCOUNT_ID,TRANSACTION_ID,TRANSACTION_LEG_ID,ENTRY_SEQUENCE_NO,
                    POSTING_REFERENCE,SOURCE_ENTITY_TYPE,SOURCE_ENTITY_ID,DEBIT_CREDIT_CODE,AMOUNT,CURRENCY_CODE,
                    BOOKING_DATE,VALUE_DATE,POSTED_AT,REVERSAL_OF_ENTRY_ID,GL_POSTING_REFERENCE,CREATED_AT,CREATED_BY
                ) VALUES(
                    :id,:accountId,:transactionId,:transactionLegId,:sequenceNo,
                    :postingReference,:sourceEntityType,:sourceEntityId,:dc,:amount,:currency,
                    :bookingDate,:valueDate,SYSTIMESTAMP,:reversalOfEntryId,:glPostingReference,SYSTIMESTAMP,:actor
                )
                """.formatted(schema);
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", entryId).addValue("accountId", accountId)
                .addValue("transactionId", transactionId).addValue("transactionLegId", transactionLegId)
                .addValue("sequenceNo", sequenceNo).addValue("postingReference", postingReference)
                .addValue("sourceEntityType", sourceEntityType).addValue("sourceEntityId", sourceEntityId)
                .addValue("dc", debitCreditCode).addValue("amount", amount).addValue("currency", currencyCode)
                .addValue("bookingDate", bookingDate).addValue("valueDate", valueDate)
                .addValue("reversalOfEntryId", reversalOfEntryId).addValue("glPostingReference", glPostingReference)
                .addValue("actor", actor));
    }

    public Optional<SubledgerEntry> findSubledger(long entryId) {
        String sql = subledgerSelect() + " WHERE SUBLEDGER_ENTRY_ID=:id";
        var rows = jdbc.query(sql, new MapSqlParameterSource("id", entryId), (rs, n) -> mapSubledger(rs));
        return rows.stream().findFirst();
    }

    public List<SubledgerEntry> listSubledger(long accountId, int limit) {
        String sql = subledgerSelect() + " WHERE ACCOUNT_ID=:accountId ORDER BY SUBLEDGER_ENTRY_ID DESC FETCH FIRST " + Math.min(Math.max(limit, 1), 200) + " ROWS ONLY";
        return jdbc.query(sql, new MapSqlParameterSource("accountId", accountId), (rs, n) -> mapSubledger(rs));
    }

    public long activeDebitBlockHoldCount(long accountId) {
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM "+schema+".DEPOSIT_ACCOUNT_HOLD WHERE ACCOUNT_ID=:id AND HOLD_STATUS_CODE='ACTIVE' AND (VALID_TO IS NULL OR VALID_TO>SYSTIMESTAMP) AND HOLD_TYPE_CODE IN ('FULL','DEBIT_ONLY')", new MapSqlParameterSource("id", accountId), Long.class);
        return v == null ? 0 : v;
    }

    public long activeCreditBlockHoldCount(long accountId) {
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM "+schema+".DEPOSIT_ACCOUNT_HOLD WHERE ACCOUNT_ID=:id AND HOLD_STATUS_CODE='ACTIVE' AND (VALID_TO IS NULL OR VALID_TO>SYSTIMESTAMP) AND HOLD_TYPE_CODE IN ('FULL','CREDIT_ONLY')", new MapSqlParameterSource("id", accountId), Long.class);
        return v == null ? 0 : v;
    }

    public BigDecimal activePartialHoldTotal(long accountId) {
        BigDecimal v = jdbc.queryForObject("SELECT NVL(SUM(HOLD_AMOUNT),0) FROM "+schema+".DEPOSIT_ACCOUNT_HOLD WHERE ACCOUNT_ID=:id AND HOLD_STATUS_CODE='ACTIVE' AND (VALID_TO IS NULL OR VALID_TO>SYSTIMESTAMP) AND HOLD_TYPE_CODE='PARTIAL'", new MapSqlParameterSource("id", accountId), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal activeReservationTotal(long accountId) {
        BigDecimal v = jdbc.queryForObject("SELECT NVL(SUM(AMOUNT),0) FROM "+schema+".DEPOSIT_BALANCE_RESERVATION WHERE ACCOUNT_ID=:id AND RESERVATION_STATUS_CODE='ACTIVE' AND (EXPIRES_AT IS NULL OR EXPIRES_AT>SYSTIMESTAMP)", new MapSqlParameterSource("id", accountId), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    public int expireReservations(long accountId, String actor) {
        String sql = "UPDATE "+schema+".DEPOSIT_BALANCE_RESERVATION SET RESERVATION_STATUS_CODE='EXPIRED',UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE ACCOUNT_ID=:id AND RESERVATION_STATUS_CODE='ACTIVE' AND EXPIRES_AT IS NOT NULL AND EXPIRES_AT<=SYSTIMESTAMP";
        return jdbc.update(sql, new MapSqlParameterSource().addValue("actor", actor).addValue("id", accountId));
    }

    public void insertReservation(long id, long accountId, Long transactionId, String reference, String type,
                                  BigDecimal amount, String currency, OffsetDateTime expiresAt,
                                  String ownerType, Long ownerId, String ownerReference, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_BALANCE_RESERVATION(
                    BALANCE_RESERVATION_ID,ACCOUNT_ID,TRANSACTION_ID,RESERVATION_REFERENCE,RESERVATION_TYPE_CODE,
                    AMOUNT,CURRENCY_CODE,RESERVATION_STATUS_CODE,EXPIRES_AT,OWNER_ENTITY_TYPE,OWNER_ENTITY_ID,
                    OWNER_REFERENCE,CREATED_AT,CREATED_BY,RECORD_VERSION
                ) VALUES(
                    :id,:accountId,:transactionId,:reference,:type,:amount,:currency,'ACTIVE',:expiresAt,
                    :ownerType,:ownerId,:ownerReference,SYSTIMESTAMP,:actor,1
                )
                """.formatted(schema);
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id).addValue("accountId", accountId).addValue("transactionId", transactionId)
                .addValue("reference", reference).addValue("type", type).addValue("amount", amount)
                .addValue("currency", currency).addValue("expiresAt", expiresAt == null ? null : Timestamp.from(expiresAt.toInstant()))
                .addValue("ownerType", ownerType).addValue("ownerId", ownerId).addValue("ownerReference", ownerReference)
                .addValue("actor", actor));
    }

    public Optional<ReservationLockRow> lockReservation(long accountId, long reservationId) {
        String sql = "SELECT BALANCE_RESERVATION_ID,ACCOUNT_ID,RESERVATION_STATUS_CODE,AMOUNT,CURRENCY_CODE,RECORD_VERSION FROM "+schema+".DEPOSIT_BALANCE_RESERVATION WHERE ACCOUNT_ID=:accountId AND BALANCE_RESERVATION_ID=:id FOR UPDATE";
        var rows = jdbc.query(sql, new MapSqlParameterSource().addValue("accountId", accountId).addValue("id", reservationId), (rs,n) ->
                new ReservationLockRow(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getBigDecimal(4),rs.getString(5),rs.getLong(6)));
        return rows.stream().findFirst();
    }

    public int releaseReservation(long reservationId, String reason, String actor) {
        String sql = "UPDATE "+schema+".DEPOSIT_BALANCE_RESERVATION SET RESERVATION_STATUS_CODE='RELEASED',RELEASED_AT=SYSTIMESTAMP,RELEASE_REASON_CODE=:reason,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE BALANCE_RESERVATION_ID=:id AND RESERVATION_STATUS_CODE='ACTIVE'";
        return jdbc.update(sql, new MapSqlParameterSource().addValue("reason", reason).addValue("actor", actor).addValue("id", reservationId));
    }

    public List<BalanceReservation> listReservations(long accountId) {
        String sql = """
                SELECT BALANCE_RESERVATION_ID,ACCOUNT_ID,TRANSACTION_ID,RESERVATION_REFERENCE,RESERVATION_TYPE_CODE,
                       AMOUNT,CURRENCY_CODE,
                       CASE WHEN RESERVATION_STATUS_CODE='ACTIVE' AND EXPIRES_AT IS NOT NULL AND EXPIRES_AT<=SYSTIMESTAMP THEN 'EXPIRED' ELSE RESERVATION_STATUS_CODE END AS EFFECTIVE_STATUS,
                       EXPIRES_AT,CONSUMED_AT,RELEASED_AT,RELEASE_REASON_CODE,OWNER_ENTITY_TYPE,OWNER_ENTITY_ID,OWNER_REFERENCE,RECORD_VERSION
                  FROM %s.DEPOSIT_BALANCE_RESERVATION
                 WHERE ACCOUNT_ID=:accountId
                 ORDER BY CASE WHEN RESERVATION_STATUS_CODE='ACTIVE' THEN 0 ELSE 1 END,BALANCE_RESERVATION_ID DESC
                """.formatted(schema);
        return jdbc.query(sql, new MapSqlParameterSource("accountId", accountId), (rs,n) -> new BalanceReservation(
                rs.getLong(1),rs.getLong(2),rs.getObject(3,Long.class),rs.getString(4),rs.getString(5),rs.getBigDecimal(6),rs.getString(7),rs.getString(8),
                toOffsetDateTime(rs.getTimestamp(9)),toOffsetDateTime(rs.getTimestamp(10)),toOffsetDateTime(rs.getTimestamp(11)),rs.getString(12),rs.getString(13),rs.getObject(14,Long.class),rs.getString(15),rs.getLong(16)));
    }

    public Optional<IdempotencyRow> findIdempotency(String key) {
        String sql = "SELECT IDEMPOTENCY_KEY,ACCOUNT_ID,OPERATION_TYPE_CODE,PAYLOAD_HASH,PROCESSING_STATUS_CODE,RESULT_REFERENCE FROM "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY WHERE IDEMPOTENCY_KEY=:key";
        var rows = jdbc.query(sql,new MapSqlParameterSource("key",key),(r,n)->new IdempotencyRow(r.getString(1),r.getObject(2,Long.class),r.getString(3),r.getString(4),r.getString(5),r.getString(6)));
        return rows.stream().findFirst();
    }

    public void insertIdempotency(String key,long accountId,String operationType,String payloadHash,String actor,String originRequestRef,String correlation) {
        String sql = "INSERT INTO "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY(OPERATION_IDEMPOTENCY_ID,IDEMPOTENCY_KEY,ACCOUNT_ID,OPERATION_TYPE_CODE,PAYLOAD_HASH,PROCESSING_STATUS_CODE,REQUESTED_BY_USER_ID,REQUESTED_AT,ORIGIN_SYSTEM_CODE,ORIGIN_MODULE_CODE,ORIGIN_REQUEST_REF,CORRELATION_ID,RECORD_VERSION) VALUES(:id,:key,:accountId,:op,:hash,'IN_PROGRESS',:actor,SYSTIMESTAMP,'CORE_BANKING','BALANCE_SUBLEDGER',:originRequestRef,:correlation,1)";
        jdbc.update(sql,new MapSqlParameterSource().addValue("id",nextIdempotencyId()).addValue("key",key).addValue("accountId",accountId).addValue("op",operationType).addValue("hash",payloadHash).addValue("actor",actor).addValue("originRequestRef",originRequestRef).addValue("correlation",correlation));
    }

    public void completeIdempotency(String key,String resultReference) {
        jdbc.update("UPDATE "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY SET PROCESSING_STATUS_CODE='COMPLETED',RESULT_REFERENCE=:ref,COMPLETED_AT=SYSTIMESTAMP,RECORD_VERSION=RECORD_VERSION+1 WHERE IDEMPOTENCY_KEY=:key AND PROCESSING_STATUS_CODE='IN_PROGRESS'",new MapSqlParameterSource().addValue("ref",resultReference).addValue("key",key));
    }

    private long next(String sequence) {
        Long value = jdbc.getJdbcOperations().queryForObject("SELECT "+schema+"."+sequence+".NEXTVAL FROM DUAL",Long.class);
        if(value==null) throw new IllegalStateException(sequence+" returned null.");
        return value;
    }

    private String subledgerSelect() {
        return "SELECT SUBLEDGER_ENTRY_ID,ACCOUNT_ID,TRANSACTION_ID,TRANSACTION_LEG_ID,ENTRY_SEQUENCE_NO,POSTING_REFERENCE,SOURCE_ENTITY_TYPE,SOURCE_ENTITY_ID,DEBIT_CREDIT_CODE,AMOUNT,CURRENCY_CODE,BOOKING_DATE,VALUE_DATE,POSTED_AT,REVERSAL_OF_ENTRY_ID,GL_POSTING_REFERENCE,CREATED_BY FROM "+schema+".DEPOSIT_SUBLEDGER_ENTRY";
    }

    private static BalanceSnapshot mapBalance(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new BalanceSnapshot(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getBigDecimal(6),rs.getBigDecimal(7),rs.getBigDecimal(8),rs.getBigDecimal(9),rs.getObject(10,Long.class),toOffsetDateTime(rs.getTimestamp(11)),rs.getLong(12));
    }

    private static SubledgerEntry mapSubledger(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SubledgerEntry(rs.getLong(1),rs.getLong(2),rs.getObject(3,Long.class),rs.getObject(4,Long.class),rs.getInt(5),rs.getString(6),rs.getString(7),rs.getLong(8),rs.getString(9),rs.getBigDecimal(10),rs.getString(11),rs.getDate(12).toLocalDate(),rs.getDate(13).toLocalDate(),toOffsetDateTime(rs.getTimestamp(14)),rs.getObject(15,Long.class),rs.getString(16),rs.getString(17));
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp t){return t==null?null:t.toLocalDateTime().atOffset(ZoneOffset.UTC);}
    private static String requireIdentifier(String raw){String n=raw==null?"":raw.trim().toUpperCase(Locale.ROOT);if(!n.matches("[A-Z][A-Z0-9_$#]{0,127}"))throw new IllegalArgumentException("Invalid Oracle identifier: "+raw);return n;}

    public record AccountFinancialRow(long accountId,Long openingRequestId,String accountStatusCode,String currencyCode,LocalDate openedOn,BigDecimal legacyLedgerBalance,BigDecimal legacyAvailableBalance,long recordVersion){}
    public record ReservationLockRow(long reservationId,long accountId,String statusCode,BigDecimal amount,String currencyCode,long recordVersion){}
    public record IdempotencyRow(String key,Long accountId,String operationType,String payloadHash,String processingStatus,String resultReference){}
}
