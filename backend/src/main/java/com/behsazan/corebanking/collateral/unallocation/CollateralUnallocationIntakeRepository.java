package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationIntakeModels.JournalItem;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** ONLY CLT.CLT_UNALLOC_REQ and CLT.CLT_UNALLOC_REQ_EVENT staging-table DML. */
@Repository
public class CollateralUnallocationIntakeRepository {
    public record Stored(String requestId, String collateralId, String allocationId,
            String moduleId, String consumerDomain, String clientId, String branchCode,
            String fileNumber, String amount, String currency, String releaseMode,
            String reasonReference, String requestStatus, String recordVersion,
            String expectedAllocationVersion, String createdAt, String updatedAt,
            String payloadDigest, String blockerCodes, String idempotencyKey) {}
    private static String str(BigDecimal n){return n==null?null:n.toPlainString();}
    private static final String SELECT = """
            SELECT REQUEST_ID,COLLATERAL_ID,ALLOCATION_ID,MODULE_ID,
                   CONSUMER_DOMAIN,CLIENT_ID,BRANCH_CODE,FILE_NUMBER,
                   REQUESTED_AMOUNT,CURRENCY_CODE,RELEASE_MODE,REASON_REFERENCE,
                   REQUEST_STATUS,RECORD_VERSION,EXPECTED_ALLOC_VERSION,
                   PAYLOAD_DIGEST,BLOCKER_CODES,IDEMPOTENCY_KEY,
                   TO_CHAR(CREATED_AT,'YYYY-MM-DD"T"HH24:MI:SS.FF3') AS CREATED_AT_TEXT,
                   TO_CHAR(UPDATED_AT,'YYYY-MM-DD"T"HH24:MI:SS.FF3') AS UPDATED_AT_TEXT
              FROM CLT.CLT_UNALLOC_REQ
            """;
    private static Stored map(ResultSet r,int row) throws SQLException {
        return new Stored(str(r.getBigDecimal("REQUEST_ID")),str(r.getBigDecimal("COLLATERAL_ID")),
                str(r.getBigDecimal("ALLOCATION_ID")),str(r.getBigDecimal("MODULE_ID")),
                r.getString("CONSUMER_DOMAIN"),r.getString("CLIENT_ID"),r.getString("BRANCH_CODE"),
                str(r.getBigDecimal("FILE_NUMBER")),str(r.getBigDecimal("REQUESTED_AMOUNT")),
                r.getString("CURRENCY_CODE").trim(),r.getString("RELEASE_MODE"),
                r.getString("REASON_REFERENCE"),r.getString("REQUEST_STATUS"),
                str(r.getBigDecimal("RECORD_VERSION")),str(r.getBigDecimal("EXPECTED_ALLOC_VERSION")),
                r.getString("CREATED_AT_TEXT"),r.getString("UPDATED_AT_TEXT"),
                r.getString("PAYLOAD_DIGEST"),r.getString("BLOCKER_CODES"),r.getString("IDEMPOTENCY_KEY"));
    }
    public void requireSchema(CollateralUnallocationIntakeWriterPool pool) {
        Long main=pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                 AND TABLE_NAME='CLT_UNALLOC_REQ'
                 AND COLUMN_NAME IN ('REQUEST_ID','COLLATERAL_ID','ALLOCATION_ID','MODULE_ID',
                     'CONSUMER_DOMAIN','CLIENT_ID','BRANCH_CODE','ACTOR_SUBJECT','IDEMPOTENCY_KEY',
                     'PAYLOAD_DIGEST','FILE_NUMBER','REQUESTED_AMOUNT','CURRENCY_CODE',
                     'RELEASE_MODE','REASON_REFERENCE','EXPECTED_ALLOC_VERSION','REQUEST_STATUS',
                     'BLOCKER_CODES','CREATED_AT','UPDATED_AT','RECORD_VERSION')
                """,Long.class);
        Long events=pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                 AND TABLE_NAME='CLT_UNALLOC_REQ_EVENT'
                 AND COLUMN_NAME IN ('EVENT_ID','REQUEST_ID','EVENT_CODE','ACTION_KEY',
                     'EVENT_ACTOR','EVENT_DIGEST','EVENT_AT','RECORD_VERSION','REASON_REFERENCE')
                """,Long.class);
        Long enforced=pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT'
                 AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                 AND ((TABLE_NAME='CLT_UNALLOC_REQ'
                      AND CONSTRAINT_NAME IN ('UQ_CLT_UA_REQ_KEY','CK_CLT_UA_REQ_STATUS','CK_CLT_UA_REQ_MODE'))
                   OR (TABLE_NAME='CLT_UNALLOC_REQ_EVENT'
                      AND CONSTRAINT_NAME IN ('FK_CLT_UA_EVT_REQ','UQ_CLT_UA_EVT_KEY')))
                """,Long.class);
        String unique=pool.jdbc().queryForObject("""
                SELECT LISTAGG(COLUMN_NAME,',') WITHIN GROUP (ORDER BY POSITION)
                  FROM ALL_CONS_COLUMNS WHERE OWNER='CLT' AND TABLE_NAME='CLT_UNALLOC_REQ'
                   AND CONSTRAINT_NAME='UQ_CLT_UA_REQ_KEY'
                """,String.class);
        String eventUnique=pool.jdbc().queryForObject("""
                SELECT LISTAGG(COLUMN_NAME,',') WITHIN GROUP (ORDER BY POSITION)
                  FROM ALL_CONS_COLUMNS WHERE OWNER='CLT' AND TABLE_NAME='CLT_UNALLOC_REQ_EVENT'
                   AND CONSTRAINT_NAME='UQ_CLT_UA_EVT_KEY'
                """,String.class);
        if(main==null||main!=21||events==null||events!=9||enforced==null||enforced!=5
             ||!"CONSUMER_DOMAIN,CLIENT_ID,IDEMPOTENCY_KEY".equals(unique)
             ||!"REQUEST_ID,EVENT_CODE,ACTION_KEY".equals(eventUnique))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R5B_STAGING_SCHEMA_NOT_APPROVED_OR_INCOMPLETE");
    }
    public Stored byKey(CollateralUnallocationIntakeWriterPool pool,
            String domain,String client,String key,boolean lock) {
        List<Stored> found=pool.jdbc().query(SELECT+"""
                WHERE CONSUMER_DOMAIN=? AND CLIENT_ID=? AND IDEMPOTENCY_KEY=?
                """+(lock?" FOR UPDATE WAIT 1":""),CollateralUnallocationIntakeRepository::map,
                domain,client,key);
        return found.isEmpty()?null:found.getFirst();
    }
    public Stored byId(CollateralUnallocationIntakeWriterPool pool,String requestId,
            String collateralId,String allocationId,String branch,String domain,String client,boolean lock) {
        List<Stored> found=pool.jdbc().query(SELECT+"""
                WHERE REQUEST_ID=? AND COLLATERAL_ID=? AND ALLOCATION_ID=?
                  AND BRANCH_CODE=? AND CONSUMER_DOMAIN=? AND CLIENT_ID=?
                """+(lock?" FOR UPDATE WAIT 1":""),CollateralUnallocationIntakeRepository::map,
                new BigDecimal(requestId),new BigDecimal(collateralId),new BigDecimal(allocationId),
                branch,domain,client);
        return found.isEmpty()?null:found.getFirst();
    }
    public Stored insert(CollateralUnallocationIntakeWriterPool pool,
            String collateralId,String allocationId,String moduleId,String branch,String actor,
            String client,String key,String digest,CollateralUnallocationPolicy.Normalized n,
            List<String> blockers) {
        BigDecimal id=pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_CLT_UNALLOC_REQ.NEXTVAL FROM DUAL",BigDecimal.class);
        if(id==null)throw new IllegalStateException("CLT_R5B_SEQUENCE_MISSING");
        int count=pool.jdbc().update("""
                INSERT INTO CLT.CLT_UNALLOC_REQ
                  (REQUEST_ID,COLLATERAL_ID,ALLOCATION_ID,MODULE_ID,CONSUMER_DOMAIN,
                   CLIENT_ID,BRANCH_CODE,ACTOR_SUBJECT,IDEMPOTENCY_KEY,PAYLOAD_DIGEST,
                   FILE_NUMBER,REQUESTED_AMOUNT,CURRENCY_CODE,RELEASE_MODE,REASON_REFERENCE,
                   EXPECTED_ALLOC_VERSION,REQUEST_STATUS,BLOCKER_CODES,CREATED_AT,RECORD_VERSION)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,SYSTIMESTAMP,1)
                """,id,new BigDecimal(collateralId),new BigDecimal(allocationId),
                new BigDecimal(moduleId),n.consumerDomain(),client,branch,actor,key,digest,
                new BigDecimal(n.fileNumber()),new BigDecimal(n.requestedAmount()),
                n.currencyCode(),n.releaseMode(),n.reasonReference(),
                new BigDecimal(n.expectedVersion()),CollateralUnallocationIntakePolicy.RECORDED,
                CollateralUnallocationIntakePolicy.serializeBlockers(blockers));
        if(count!=1)throw new IllegalStateException("CLT_R5B_REQUEST_INSERT_FAILED");
        appendEvent(pool,id,"REQUEST_RECORDED",key,actor,digest,BigDecimal.ONE,n.reasonReference());
        return readInserted(pool,id);
    }
    private Stored readInserted(CollateralUnallocationIntakeWriterPool pool,BigDecimal id) {
        List<Stored> found=pool.jdbc().query(SELECT+" WHERE REQUEST_ID=?",
                CollateralUnallocationIntakeRepository::map,id);
        if(found.size()!=1)throw new IllegalStateException("CLT_R5B_READBACK_FAILED");
        return found.getFirst();
    }
    private void appendEvent(CollateralUnallocationIntakeWriterPool pool,BigDecimal requestId,
            String code,String key,String actor,String digest,BigDecimal version,String reason) {
        int updated=pool.jdbc().update("""
                INSERT INTO CLT.CLT_UNALLOC_REQ_EVENT
                  (EVENT_ID,REQUEST_ID,EVENT_CODE,ACTION_KEY,EVENT_ACTOR,EVENT_DIGEST,
                   EVENT_AT,RECORD_VERSION,REASON_REFERENCE)
                VALUES (CLT.SEQ_CLT_UNALLOC_EVT.NEXTVAL,?,?,?,?,?,SYSTIMESTAMP,?,?)
                """,requestId,code,key,actor,digest,version,reason);
        if(updated!=1)throw new IllegalStateException("CLT_R5B_EVENT_INSERT_FAILED");
    }
    public String cancelEventDigest(CollateralUnallocationIntakeWriterPool pool,
            String requestId,String actionKey) {
        List<String> digests=pool.jdbc().query("""
                SELECT EVENT_DIGEST FROM CLT.CLT_UNALLOC_REQ_EVENT
                 WHERE REQUEST_ID=? AND EVENT_CODE='REQUEST_CANCELLED' AND ACTION_KEY=?
                """,(r,row)->r.getString("EVENT_DIGEST"),new BigDecimal(requestId),actionKey);
        return digests.isEmpty()?null:digests.getFirst();
    }
    public Stored cancel(CollateralUnallocationIntakeWriterPool pool,Stored stored,
            String actionKey,String actor,String digest,String reason) {
        int updated=pool.jdbc().update("""
                UPDATE CLT.CLT_UNALLOC_REQ
                   SET REQUEST_STATUS=?,UPDATED_AT=SYSTIMESTAMP,RECORD_VERSION=RECORD_VERSION+1
                 WHERE REQUEST_ID=? AND REQUEST_STATUS=? AND RECORD_VERSION=?
                """,CollateralUnallocationIntakePolicy.CANCELLED,
                new BigDecimal(stored.requestId()),CollateralUnallocationIntakePolicy.RECORDED,
                new BigDecimal(stored.recordVersion()));
        if(updated!=1)throw new CollateralUnallocationIntakePolicy.Conflict("CLT_R5B_CAS_FAILED");
        appendEvent(pool,new BigDecimal(stored.requestId()),"REQUEST_CANCELLED",actionKey,
                actor,digest,new BigDecimal(stored.recordVersion()).add(BigDecimal.ONE),reason);
        return readInserted(pool,new BigDecimal(stored.requestId()));
    }
    public List<JournalItem> journal(CollateralUnallocationIntakeWriterPool pool,
            String collateralId,String branch,int limit) {
        return pool.jdbc().query("""
                SELECT REQUEST_ID,ALLOCATION_ID,CONSUMER_DOMAIN,REQUESTED_AMOUNT,
                       CURRENCY_CODE,RELEASE_MODE,REQUEST_STATUS,
                       TO_CHAR(CREATED_AT,'YYYY-MM-DD"T"HH24:MI:SS.FF3') AS CREATED_AT_TEXT,
                       TO_CHAR(UPDATED_AT,'YYYY-MM-DD"T"HH24:MI:SS.FF3') AS UPDATED_AT_TEXT
                  FROM CLT.CLT_UNALLOC_REQ
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=?
                 ORDER BY REQUEST_ID DESC FETCH FIRST ? ROWS ONLY
                """,(r,row)->new JournalItem(str(r.getBigDecimal("REQUEST_ID")),
                str(r.getBigDecimal("ALLOCATION_ID")),r.getString("CONSUMER_DOMAIN"),
                str(r.getBigDecimal("REQUESTED_AMOUNT")),r.getString("CURRENCY_CODE").trim(),
                r.getString("RELEASE_MODE"),r.getString("REQUEST_STATUS"),
                r.getString("CREATED_AT_TEXT"),r.getString("UPDATED_AT_TEXT"),false),
                new BigDecimal(collateralId),branch,limit);
    }
}
