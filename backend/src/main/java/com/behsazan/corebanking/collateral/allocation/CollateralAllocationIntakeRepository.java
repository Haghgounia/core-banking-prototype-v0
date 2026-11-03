package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.Debtor;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestPolicy.Normalized;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationIntakeModels.JournalItem;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Writes ONLY 3 proposed CLT_ALLOCATION_REQUEST* staging tables in one JDBC transaction. */
@Repository
public class CollateralAllocationIntakeRepository {
    public record Stored(String id, String collateralId, String consumerDomain,
            String caseReference, String fileNumber, String amount, String currency,
            String requestStatus, String createdAt, String version, int debtorCount,
            boolean sponsorConflict, String digest) {}
    private static final String SELECT = """
            SELECT REQUEST_ID,COLLATERAL_ID,CONSUMER_DOMAIN,CASE_REFERENCE,FILE_NUMBER,
                   AMOUNT,CURRENCY_CODE,REQUEST_STATUS,RECORD_VERSION,DEBTOR_COUNT,
                   SPONSOR_CONFLICT,PAYLOAD_DIGEST,
                   TO_CHAR(CREATED_AT,'YYYY-MM-DD\"T\"HH24:MI:SS.FF3') AS CREATED_AT_TEXT
              FROM CLT.CLT_ALLOCATION_REQUEST
            """;
    private static Stored map(ResultSet rs, int row) throws SQLException {
        return new Stored(rs.getBigDecimal("REQUEST_ID").toPlainString(),
                rs.getBigDecimal("COLLATERAL_ID").toPlainString(),
                rs.getString("CONSUMER_DOMAIN"),rs.getString("CASE_REFERENCE"),
                rs.getBigDecimal("FILE_NUMBER").toPlainString(),
                rs.getBigDecimal("AMOUNT").toPlainString(), rs.getString("CURRENCY_CODE"),
                rs.getString("REQUEST_STATUS"),rs.getString("CREATED_AT_TEXT"),
                rs.getBigDecimal("RECORD_VERSION").toPlainString(),
                rs.getInt("DEBTOR_COUNT"),rs.getInt("SPONSOR_CONFLICT") == 1,
                rs.getString("PAYLOAD_DIGEST"));
    }
    public void requireSchema(CollateralAllocationIntakeWriterPool pool) {
        Long main = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                   AND TABLE_NAME='CLT_ALLOCATION_REQUEST'
                   AND COLUMN_NAME IN ('REQUEST_ID','COLLATERAL_ID','CONSUMER_DOMAIN',
                      'CLIENT_ID','BRANCH_CODE','ACTOR_SUBJECT','IDEMPOTENCY_KEY',
                      'PAYLOAD_DIGEST','CASE_REFERENCE','FILE_NUMBER','AMOUNT',
                      'CURRENCY_CODE','REQUEST_STATUS','DEBTOR_COUNT','SPONSOR_CONFLICT',
                      'CREATED_AT','RECORD_VERSION')
                """, Long.class);
        Long debtors = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                  AND TABLE_NAME='CLT_ALLOCATION_REQUEST_DEBTOR'
                  AND COLUMN_NAME IN ('REQUEST_ID','PARTY_ID','DEBTOR_ROLE')
                """, Long.class);
        Long events = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                  AND TABLE_NAME='CLT_ALLOCATION_REQUEST_EVENT'
                  AND COLUMN_NAME IN ('EVENT_ID','REQUEST_ID','EVENT_CODE',
                      'EVENT_ACTOR','PAYLOAD_DIGEST','EVENT_AT')
                """, Long.class);
        Long unique = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT'
                  AND TABLE_NAME='CLT_ALLOCATION_REQUEST'
                  AND CONSTRAINT_NAME='UQ_CLT_ALLOC_REQ_IDEMPOTENCY'
                  AND CONSTRAINT_TYPE='U' AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                """, Long.class);
        String identityColumns = pool.jdbc().queryForObject("""
                SELECT LISTAGG(COLUMN_NAME, ',') WITHIN GROUP (ORDER BY POSITION)
                  FROM ALL_CONS_COLUMNS
                 WHERE OWNER='CLT' AND TABLE_NAME='CLT_ALLOCATION_REQUEST'
                   AND CONSTRAINT_NAME='UQ_CLT_ALLOC_REQ_IDEMPOTENCY'
                """, String.class);
        Long enforced = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT'
                   AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                   AND ((TABLE_NAME='CLT_ALLOCATION_REQUEST'
                         AND CONSTRAINT_NAME='CK_CLT_ALLOC_REQ_STATUS')
                     OR (TABLE_NAME='CLT_ALLOCATION_REQUEST_DEBTOR'
                         AND CONSTRAINT_NAME='FK_CLT_ALLOC_REQ_DEBTOR_REQ')
                     OR (TABLE_NAME='CLT_ALLOCATION_REQUEST_EVENT'
                         AND CONSTRAINT_NAME='FK_CLT_ALLOC_REQ_EVT_REQUEST'))
                """, Long.class);
        if (main == null || main != 17 || debtors == null || debtors != 3 ||
                events == null || events != 6 || unique == null || unique != 1 ||
                !"CONSUMER_DOMAIN,CLIENT_ID,IDEMPOTENCY_KEY".equals(identityColumns) ||
                enforced == null || enforced != 3)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_ALLOCATION_INTAKE_SCHEMA_NOT_APPROVED_OR_INCOMPLETE");
    }
    public Stored byKey(CollateralAllocationIntakeWriterPool pool, String domain,
            String client, String key, boolean lock) {
        List<Stored> rows = pool.jdbc().query(SELECT + """
                WHERE CONSUMER_DOMAIN=? AND CLIENT_ID=? AND IDEMPOTENCY_KEY=?
                """ + (lock ? " FOR UPDATE" : ""), CollateralAllocationIntakeRepository::map,
                domain, client, key);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Stored byId(CollateralAllocationIntakeWriterPool pool, String requestId,
            String collateralId, String branch, String domain, String client) {
        List<Stored> rows = pool.jdbc().query(SELECT + """
                WHERE REQUEST_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=?
                  AND CONSUMER_DOMAIN=? AND CLIENT_ID=?
                """, CollateralAllocationIntakeRepository::map,
                new BigDecimal(requestId), new BigDecimal(collateralId), branch, domain, client);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Stored insert(CollateralAllocationIntakeWriterPool pool, String collateralId,
            String branch, String subject, String client, String key, String digest,
            Normalized normalized, boolean sponsorConflict) {
        BigDecimal requestId = pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_CLT_ALLOCATION_REQUEST.NEXTVAL FROM DUAL",BigDecimal.class);
        if (requestId == null) throw new IllegalStateException("CLT_ALLOCATION_INTAKE_SEQUENCE_UNAVAILABLE");
        int updated = pool.jdbc().update("""
                INSERT INTO CLT.CLT_ALLOCATION_REQUEST
                  (REQUEST_ID,COLLATERAL_ID,CONSUMER_DOMAIN,CLIENT_ID,BRANCH_CODE,
                   ACTOR_SUBJECT,IDEMPOTENCY_KEY,PAYLOAD_DIGEST,CASE_REFERENCE,
                   FILE_NUMBER,AMOUNT,CURRENCY_CODE,REQUEST_STATUS,DEBTOR_COUNT,
                   SPONSOR_CONFLICT,CREATED_AT,RECORD_VERSION)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,SYSTIMESTAMP,1)
                """,requestId,new BigDecimal(collateralId),normalized.consumerDomain(),client,
                branch,subject,key,digest,normalized.caseReference(),
                new BigDecimal(normalized.fileNumber()),new BigDecimal(normalized.amount()),
                normalized.currencyCode(),CollateralAllocationIntakePolicy.STATUS,
                normalized.debtors().size(), sponsorConflict ? 1 : 0);
        if (updated != 1) throw new IllegalStateException("CLT_ALLOCATION_INTAKE_INSERT_FAILED");
        for (Debtor debtor : normalized.debtors())
            pool.jdbc().update("""
                    INSERT INTO CLT.CLT_ALLOCATION_REQUEST_DEBTOR
                      (REQUEST_ID,PARTY_ID,DEBTOR_ROLE) VALUES (?,?,?)
                    """,requestId,new BigDecimal(debtor.partyId()),debtor.role());
        pool.jdbc().update("""
                INSERT INTO CLT.CLT_ALLOCATION_REQUEST_EVENT
                  (EVENT_ID,REQUEST_ID,EVENT_CODE,EVENT_ACTOR,PAYLOAD_DIGEST,EVENT_AT)
                VALUES (CLT.SEQ_CLT_ALLOCATION_REQUEST_EVENT.NEXTVAL,?,
                        'REQUEST_RECORDED',?,?,SYSTIMESTAMP)
                """,requestId,subject,digest);
        List<Stored> rows = pool.jdbc().query(SELECT + " WHERE REQUEST_ID=?",
                CollateralAllocationIntakeRepository::map, requestId);
        if (rows.size() != 1) throw new IllegalStateException("CLT_ALLOCATION_INTAKE_READBACK_MISSING");
        return rows.getFirst();
    }
    public List<JournalItem> journal(CollateralAllocationIntakeWriterPool pool,
            String collateralId, String branch, int limit) {
        return pool.jdbc().query("""
                SELECT REQUEST_ID,CONSUMER_DOMAIN,CASE_REFERENCE,AMOUNT,CURRENCY_CODE,
                       REQUEST_STATUS,
                       TO_CHAR(CREATED_AT,'YYYY-MM-DD\"T\"HH24:MI:SS.FF3') AS CREATED_AT_TEXT
                  FROM CLT.CLT_ALLOCATION_REQUEST
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=?
                 ORDER BY REQUEST_ID DESC FETCH FIRST ? ROWS ONLY
                """,(rs,row) -> new JournalItem(rs.getBigDecimal("REQUEST_ID").toPlainString(),
                rs.getString("CONSUMER_DOMAIN"),rs.getString("CASE_REFERENCE"),
                rs.getBigDecimal("AMOUNT").toPlainString(),rs.getString("CURRENCY_CODE"),
                rs.getString("REQUEST_STATUS"),rs.getString("CREATED_AT_TEXT"), false),
                new BigDecimal(collateralId),branch,limit);
    }
}
