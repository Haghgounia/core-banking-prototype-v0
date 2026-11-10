package com.behsazan.corebanking.collateral.release;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.release.CollateralReleasePostingModels.*;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.server.ResponseStatusException;

/**
 * BUC-11 final status mutation. The bank-owned, independently approved permit is
 * not created/approved by any endpoint here. Oracle transaction covers root CAS,
 * append-only history and durable permit consumption. Never alters allocations.
 */
@Service
public final class CollateralReleasePostingService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Environment env;
    public CollateralReleasePostingService(DataSource dataSource, Environment env) {
        this.jdbc=new JdbcTemplate(dataSource);
        this.tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.tx.setTimeout(25);
        this.env=env;
    }
    private record Actor(String subject,String branch) {}
    private record LockedCollateral(String id,String branch,String status,String version) {}
    private record Permit(String id,String branch,String status,String sourceStatus,String targetStatus,
            String baseVersion,String approvalRef,String legalRef,String consumerRef,String unallocationRef,
            String maker,String checker,LocalDateTime expiresAt,String key,String poster,
            String postedVersion,String historyId,LocalDateTime postedAt) {}
    private record StatusHistory(String id,long sequence,String status) {}
    private static String n(BigDecimal number){return number==null?null:number.toPlainString();}
    private static LocalDateTime at(Timestamp stamp){return stamp==null?null:stamp.toLocalDateTime();}
    private static BigDecimal num(String s){return new BigDecimal(s);}
    private static ResponseStatusException stop(HttpStatus status,String code){return new ResponseStatusException(status,code);}
    private Actor actor(Authentication auth,boolean reviewer){
        if (!(auth instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())
            throw stop(HttpStatus.UNAUTHORIZED,"RELEASE_SIGNED_PRINCIPAL_REQUIRED");
        if(reviewer && (jwt.getToken().getClaims().containsKey("clt_local_prototype")
                || "CLT_UNIFIED_LOCAL_DEV".equals(jwt.getToken().getSubject())))
            throw stop(HttpStatus.FORBIDDEN,"RELEASE_LOCAL_PROTOTYPE_POSTING_NOT_AUTHORIZED");
        String required=reviewer?"SCOPE_clt.collateral.review":"SCOPE_clt.collateral.read";
        if(auth.getAuthorities().stream().noneMatch(a->required.equals(a.getAuthority())))
            throw stop(HttpStatus.FORBIDDEN,"RELEASE_SCOPE_REQUIRED");
        Object claim=jwt.getToken().getClaims().get("branch_code");
        if(!(claim instanceof String branch))throw stop(HttpStatus.FORBIDDEN,"RELEASE_BRANCH_REQUIRED");
        try {return new Actor(CollateralReleasePostingPolicy.actor(jwt.getToken().getSubject()),
                CollateralRegistryPolicy.branch(branch));}
        catch(IllegalArgumentException e){throw stop(HttpStatus.FORBIDDEN,"RELEASE_ACTOR_BRANCH_INVALID");}
    }
    private boolean flag(){return env.getProperty("core-banking.collateral.final-release.posting-enabled",Boolean.class,false);}
    private boolean prototype(){return env.getProperty("core-banking.collateral.prototype.enabled",Boolean.class,false)
            || env.getProperty("CLT_LOCAL_UAT_READONLY_ENABLED",Boolean.class,false);}
    private String target(){return env.getProperty("core-banking.collateral.final-release.target-status-code","").strip();}
    private String source(){return env.getProperty("core-banking.collateral.final-release.source-status-code","").strip();}
    private boolean configured(){
        try{CollateralReleasePostingPolicy.code(target());CollateralReleasePostingPolicy.code(source());
            return !target().equals(source());}catch(IllegalArgumentException e){return false;}
    }
    private static final String[] PERMIT_COLUMNS={"RELEASE_PERMIT_ID","COLLATERAL_ID","BRANCH_CODE",
            "BASE_RECORD_VERSION","FROM_STATUS_CODE","TO_STATUS_CODE","APPROVAL_REFERENCE",
            "LEGAL_CLEARANCE_REFERENCE","CONSUMER_CLEARANCE_REFERENCE","UNALLOCATION_REFERENCE",
            "MAKER_SUBJECT","CHECKER_SUBJECT","PERMIT_STATUS","VALID_UNTIL","IDEMPOTENCY_KEY",
            "POSTER_SUBJECT","POSTED_AT","POSTED_RECORD_VERSION","STATUS_HISTORY_ID"};
    private boolean schemaReady(){
        try{
            Integer count=jdbc.queryForObject("""
                    SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS
                     WHERE OWNER='CLT' AND TABLE_NAME='CLT_FINAL_RELEASE_PERMIT'
                       AND COLUMN_NAME IN ('RELEASE_PERMIT_ID','COLLATERAL_ID','BRANCH_CODE',
                        'BASE_RECORD_VERSION','FROM_STATUS_CODE','TO_STATUS_CODE','APPROVAL_REFERENCE',
                        'LEGAL_CLEARANCE_REFERENCE','CONSUMER_CLEARANCE_REFERENCE','UNALLOCATION_REFERENCE',
                        'MAKER_SUBJECT','CHECKER_SUBJECT','PERMIT_STATUS','VALID_UNTIL','IDEMPOTENCY_KEY',
                        'POSTER_SUBJECT','POSTED_AT','POSTED_RECORD_VERSION','STATUS_HISTORY_ID')
                    """,Integer.class);
            Integer constraints=jdbc.queryForObject("""
                    SELECT COUNT(DISTINCT CONSTRAINT_NAME) FROM ALL_CONSTRAINTS
                     WHERE OWNER='CLT' AND TABLE_NAME='CLT_FINAL_RELEASE_PERMIT'
                       AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                       AND CONSTRAINT_NAME IN ('FK_CLT_REL_PERMIT_PARENT',
                          'FK_CLT_REL_PERMIT_TARGET','FK_CLT_REL_PERMIT_HISTORY',
                          'UQ_CLT_REL_PERMIT_VER','UQ_CLT_REL_PERMIT_IDEM',
                          'CK_CLT_REL_PERMIT_STATUS','CK_CLT_REL_PERMIT_ACTORS',
                          'CK_CLT_REL_PERMIT_POST')
                    """,Integer.class);
            return Integer.valueOf(PERMIT_COLUMNS.length).equals(count)
                    && Integer.valueOf(8).equals(constraints);
        }catch(DataAccessException ex){return false;}
    }
    public Readiness readiness(Authentication auth){
        actor(auth,false);
        boolean present=schemaReady(),settings=configured();
        boolean ready=flag() && !prototype() && present && settings;
        return new Readiness(ready,present,settings,prototype(),
                ready?"BANK_PERMIT_AND_TRANSACTIONAL_EVIDENCE_REQUIRED":
                    "RELEASE_POSTING_DISABLED_OR_MIGRATION_OR_APPROVED_STATUS_MISSING");
    }
    private void assertReady(){
        if(prototype())throw stop(HttpStatus.FORBIDDEN,"RELEASE_PROTOTYPE_FINANCIAL_WRITE_BLOCKED");
        if(!flag())throw stop(HttpStatus.SERVICE_UNAVAILABLE,"RELEASE_POSTING_DISABLED");
        if(!configured())throw stop(HttpStatus.SERVICE_UNAVAILABLE,"RELEASE_BANK_STATUS_MAPPING_MISSING");
        if(!schemaReady())throw stop(HttpStatus.SERVICE_UNAVAILABLE,"RELEASE_BANK_PERMIT_SCHEMA_MISSING");
    }
    private LockedCollateral collateral(String collateralId,String branch,boolean lock){
        List<LockedCollateral> rows=jdbc.query("""
                SELECT c.COLLATERAL_ID,c.BRANCH_CODE,c.COLLATERAL_STATUS_CODE,c.RECORD_VERSION
                  FROM CLT.COLLATERAL c WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=?
                """+(lock?" FOR UPDATE OF c.RECORD_VERSION":""),
                (r,i)->new LockedCollateral(n(r.getBigDecimal(1)),r.getString(2),r.getString(3),
                        n(r.getBigDecimal(4))),num(collateralId),branch);
        if(rows.size()!=1)throw stop(HttpStatus.NOT_FOUND,"RELEASE_COLLATERAL_NOT_FOUND");
        return rows.getFirst();
    }
    private static final String PERMIT_SELECT="""
            SELECT RELEASE_PERMIT_ID,BRANCH_CODE,PERMIT_STATUS,FROM_STATUS_CODE,
                   TO_STATUS_CODE,BASE_RECORD_VERSION,APPROVAL_REFERENCE,LEGAL_CLEARANCE_REFERENCE,
                   CONSUMER_CLEARANCE_REFERENCE,UNALLOCATION_REFERENCE,MAKER_SUBJECT,CHECKER_SUBJECT,
                   VALID_UNTIL,IDEMPOTENCY_KEY,POSTER_SUBJECT,POSTED_RECORD_VERSION,
                   STATUS_HISTORY_ID,POSTED_AT
              FROM CLT.CLT_FINAL_RELEASE_PERMIT
            """;
    private static Permit mapPermit(java.sql.ResultSet r,int i)throws java.sql.SQLException{
        return new Permit(n(r.getBigDecimal(1)),r.getString(2),r.getString(3),r.getString(4),
                r.getString(5),n(r.getBigDecimal(6)),r.getString(7),r.getString(8),r.getString(9),
                r.getString(10),r.getString(11),r.getString(12),at(r.getTimestamp(13)),
                r.getString(14),r.getString(15),n(r.getBigDecimal(16)),
                n(r.getBigDecimal(17)),at(r.getTimestamp(18)));
    }
    private Permit permit(String permitId,String collateralId,String branch,boolean lock){
        List<Permit> rows=jdbc.query(PERMIT_SELECT+" WHERE RELEASE_PERMIT_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=?"
                        +(lock?" FOR UPDATE":""),CollateralReleasePostingService::mapPermit,
                num(permitId),num(collateralId),branch);
        if(rows.size()!=1)throw stop(HttpStatus.NOT_FOUND,"RELEASE_APPROVAL_NOT_FOUND");
        return rows.getFirst();
    }
    public Permits permits(String rawId,Authentication auth){
        Actor who=actor(auth,false);
        String id=CollateralReleasePostingPolicy.decimalId(rawId);
        collateral(id,who.branch(),false);
        if(!schemaReady())return new Permits(List.of(),"BANK_PERMIT_SCHEMA_NOT_INSTALLED");
        List<PermitView> rows=jdbc.query("""
                SELECT RELEASE_PERMIT_ID,PERMIT_STATUS,FROM_STATUS_CODE,TO_STATUS_CODE,
                       BASE_RECORD_VERSION,APPROVAL_REFERENCE,VALID_UNTIL,POSTED_AT,POSTED_RECORD_VERSION
                  FROM CLT.CLT_FINAL_RELEASE_PERMIT
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=?
                   AND PERMIT_STATUS IN ('APPROVED','POSTED')
                 ORDER BY RELEASE_PERMIT_ID DESC FETCH FIRST 20 ROWS ONLY
                """,(r,i)->new PermitView(n(r.getBigDecimal(1)),r.getString(2),r.getString(3),
                r.getString(4),n(r.getBigDecimal(5)),r.getString(6),
                String.valueOf(at(r.getTimestamp(7))),String.valueOf(at(r.getTimestamp(8))),
                n(r.getBigDecimal(9))),num(id),who.branch());
        return new Permits(rows,"BANK_APPROVED_ORACLE_RELEASE_PERMITS");
    }
    private long[] allocationCounts(String collateralId,String branch){
        return Objects.requireNonNull(jdbc.queryForObject("""
                SELECT COUNT(CASE WHEN USED='YES' THEN 1 END),
                       COUNT(CASE WHEN BRANCH_CODE<>? THEN 1 END),
                       COUNT(CASE WHEN USED IS NULL OR USED NOT IN ('YES','NO') THEN 1 END)
                  FROM CLT.COLLATERAL_ALLOCATION WHERE COLLATERAL_ID=?
                """,(r,i)->new long[]{r.getLong(1),r.getLong(2),r.getLong(3)},
                branch,num(collateralId)));
    }
    private long unresolvedTransactions(String collateralId){
        return Objects.requireNonNull(jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_TRANSACTION
                 WHERE COLLATERAL_ID=? AND (TRANSACTION_STATUS_CODE IS NULL
                       OR TRANSACTION_STATUS_CODE NOT IN
                       ('COMPLETED','POSTED','REJECTED','CANCELLED'))
                """,Long.class,num(collateralId)));
    }
    private record CrossUnitPermissionEvidence(long openGrants, boolean schemaInstalled) {}
    /** No cross-branch grant can remain PENDING/APPROVED when the collateral is finalized.
     *  Absence of the CLT_CROSS_UNIT_GRANT table is valid only if BUC-15 was never provisioned.
     *  A partially installed dictionary fails closed rather than silently returning zero. */
    private CrossUnitPermissionEvidence crossUnitPermissions(String collateralId) {
        try {
            Integer tables=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TABLES
                     WHERE OWNER='CLT' AND TABLE_NAME='CLT_CROSS_UNIT_GRANT'
                    """,Integer.class);
            if(Integer.valueOf(0).equals(tables))return new CrossUnitPermissionEvidence(0,false);
            if(!Integer.valueOf(1).equals(tables))
                throw stop(HttpStatus.SERVICE_UNAVAILABLE,"RELEASE_CROSS_UNIT_DICTIONARY_UNAVAILABLE");
            Integer columns=jdbc.queryForObject("""
                    SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS
                     WHERE OWNER='CLT' AND TABLE_NAME='CLT_CROSS_UNIT_GRANT'
                       AND COLUMN_NAME IN ('COLLATERAL_ID','GRANT_STATUS','OWNER_BRANCH')
                    """,Integer.class);
            if(!Integer.valueOf(3).equals(columns))
                throw stop(HttpStatus.SERVICE_UNAVAILABLE,"RELEASE_CROSS_UNIT_SCHEMA_INCOMPLETE");
            Long opened=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.CLT_CROSS_UNIT_GRANT
                     WHERE COLLATERAL_ID=? AND GRANT_STATUS IN ('PENDING','APPROVED')
                    """,Long.class,num(collateralId));
            if(opened==null)throw stop(HttpStatus.SERVICE_UNAVAILABLE,"RELEASE_GRANT_CHECK_UNAVAILABLE");
            return new CrossUnitPermissionEvidence(opened,true);
        }catch(DataAccessException ex){throw stop(HttpStatus.SERVICE_UNAVAILABLE,
                "RELEASE_CROSS_UNIT_GRANT_EVIDENCE_UNAVAILABLE");}
    }
    /** Analyst can inspect the same Oracle preconditions checked by bank-gated posting.
     *  This endpoint has no mutation, no bank permit issuance and no cross-branch disclosure. */
    public LifecycleInterlocks lifecycle(String rawId,Authentication auth) {
        Actor who=actor(auth,false);
        String id=CollateralReleasePostingPolicy.decimalId(rawId);
        collateral(id,who.branch(),false);
        long[] a=allocationCounts(id,who.branch());
        long transactions=unresolvedTransactions(id);
        var grants=crossUnitPermissions(id);
        boolean clear=a[0]==0 && a[1]==0 && a[2]==0
                && transactions==0 && grants.openGrants()==0;
        return new LifecycleInterlocks(id,a[0],a[1],a[2],transactions,
                grants.openGrants(),grants.schemaInstalled(),clear,
                clear?"CAPACITY_AND_CROSS_UNIT_INTERLOCKS_CLEAR_LEGAL_APPROVAL_STILL_REQUIRED":
                    "RELEASE_HAS_ACTIVE_ALLOCATION_TRANSACTION_OR_CROSS_UNIT_PERMISSION");
    }
    private StatusHistory lastHistory(String collateralId){
        List<StatusHistory> rows=jdbc.query("""
                SELECT COLLATERAL_STATUS_HISTORY_ID,STATUS_SEQUENCE_NO,TO_STATUS_CODE
                  FROM CLT.COLLATERAL_STATUS_HISTORY WHERE COLLATERAL_ID=?
                 ORDER BY STATUS_SEQUENCE_NO DESC,COLLATERAL_STATUS_HISTORY_ID DESC
                 FETCH FIRST 1 ROW ONLY
                """,(r,i)->new StatusHistory(n(r.getBigDecimal(1)),r.getLong(2),r.getString(3)),
                num(collateralId));
        return rows.isEmpty()?new StatusHistory(null,0,null):rows.getFirst();
    }
    private boolean targetApproved(String status){
        Integer c=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_COLLATERAL_STATUS
                 WHERE COLLATERAL_STATUS_CODE=? AND STATUS='ACTIVE'
                   AND IS_TERMINAL_STATUS_FLAG='Y' AND IS_ACTIVE_STATUS_FLAG='N'
                """,Integer.class,status);
        return Integer.valueOf(1).equals(c);
    }
    private static String timestamp(LocalDateTime value){return value==null?null:value.toString();}
    private Receipt receipt(String collateralId,Permit p,boolean replay){
        return new Receipt(collateralId,p.id(),p.status(),p.sourceStatus(),p.targetStatus(),
                p.baseVersion(),p.postedVersion(),p.historyId(),p.approvalRef(),
                timestamp(p.postedAt()),replay,false,"COLLATERAL_FINAL_RELEASE_STATUS_POSTED");
    }
    public Receipt commit(String rawId,FinalizeCommand command,String ifMatch,String idem,Authentication auth){
        Actor who=actor(auth,true);
        String id=CollateralReleasePostingPolicy.decimalId(rawId);
        if(command==null)throw stop(HttpStatus.BAD_REQUEST,"RELEASE_PERMIT_ID_REQUIRED");
        String permitId=CollateralReleasePostingPolicy.decimalId(command.permitId());
        String expected=CollateralReleasePostingPolicy.etag(ifMatch);
        String key=CollateralReleasePostingPolicy.key(idem);
        assertReady();
        try {return Objects.requireNonNull(tx.execute(status->{
            LockedCollateral locked=collateral(id,who.branch(),true);
            Permit p=permit(permitId,id,who.branch(),true);
            if("POSTED".equals(p.status())){
                if(key.equals(p.key()) && who.subject().equals(p.poster())
                        && p.postedVersion()!=null && p.historyId()!=null && expected.equals(p.baseVersion()))
                    return receipt(id,p,true);
                throw stop(HttpStatus.CONFLICT,"RELEASE_PERMIT_ALREADY_CONSUMED");
            }
            long[] alloc=allocationCounts(id,who.branch());
            long unresolved=unresolvedTransactions(id);
            CrossUnitPermissionEvidence crossUnit=crossUnitPermissions(id);
            if(crossUnit.openGrants()!=0)
                throw stop(HttpStatus.CONFLICT,"RELEASE_CROSS_UNIT_GRANTS_NOT_CLOSED");
            StatusHistory history=lastHistory(id);
            LocalDateTime now=jdbc.queryForObject("SELECT CAST(SYSTIMESTAMP AS TIMESTAMP) FROM DUAL",
                    (r,i)->at(r.getTimestamp(1)));
            var facts=new CollateralReleasePostingPolicy.Facts(expected,locked.version(),p.baseVersion(),
                    locked.status(),source(),p.targetStatus(),target(),targetApproved(p.targetStatus()),
                    locked.branch(),p.branch(),alloc[0],alloc[1],alloc[2],unresolved,p.status(),
                    p.maker(),p.checker(),who.subject(),p.approvalRef(),p.legalRef(),p.consumerRef(),
                    p.unallocationRef(),p.expiresAt(),now,history.status(),history.sequence());
            List<String> blockers=CollateralReleasePostingPolicy.blockers(facts);
            if(!blockers.isEmpty())throw stop(HttpStatus.CONFLICT,"RELEASE_DENIED_"+blockers.getFirst());
            int updated=jdbc.update("""
                    UPDATE CLT.COLLATERAL
                       SET COLLATERAL_STATUS_CODE=?,RECORD_VERSION=RECORD_VERSION+1,
                           UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=?
                     WHERE COLLATERAL_ID=? AND BRANCH_CODE=? AND RECORD_VERSION=?
                       AND COLLATERAL_STATUS_CODE=?
                    """,p.targetStatus(),who.subject(),num(id),who.branch(),num(locked.version()),locked.status());
            if(updated!=1)throw stop(HttpStatus.CONFLICT,"RELEASE_PARENT_CAS_LOST");
            String newVersion=new BigDecimal(locked.version()).add(BigDecimal.ONE).toPlainString();
            KeyHolder keys=new GeneratedKeyHolder();
            jdbc.update(connection->{
                PreparedStatement ps=connection.prepareStatement("""
                    INSERT INTO CLT.COLLATERAL_STATUS_HISTORY
                       (COLLATERAL_ID,STATUS_SEQUENCE_NO,FROM_STATUS_CODE,TO_STATUS_CODE,
                        STATUS_REASON_CODE,STATUS_REASON_TEXT,SOURCE_EVENT_CODE,SOURCE_REFERENCE,
                        CHANGED_AT,CHANGED_BY,PREVIOUS_STATUS_HISTORY_ID,CREATED_BY,RECORD_VERSION)
                    VALUES (?,?,?,?,?,?,?,?,SYSTIMESTAMP,?,?,?,1)
                    """,new String[]{"COLLATERAL_STATUS_HISTORY_ID"});
                ps.setBigDecimal(1,num(id));ps.setLong(2,history.sequence()+1);
                ps.setString(3,locked.status());ps.setString(4,p.targetStatus());
                ps.setString(5,"FINAL_RELEASE_APPROVED");
                ps.setString(6,"BUC11: bank-approved final release; permit="+p.id());
                ps.setString(7,"BUC11_FINAL_RELEASE");ps.setString(8,p.approvalRef());
                ps.setString(9,who.subject());
                if(history.id()==null)ps.setNull(10,java.sql.Types.NUMERIC);
                else ps.setBigDecimal(10,num(history.id()));
                ps.setString(11,who.subject());return ps;
            },keys);
            Number generated=keys.getKey();
            if(generated==null)throw new IllegalStateException("RELEASE_STATUS_HISTORY_KEY_NOT_RETURNED");
            String historyId=new BigDecimal(generated.toString()).toPlainString();
            int permitUpdated=jdbc.update("""
                    UPDATE CLT.CLT_FINAL_RELEASE_PERMIT
                       SET PERMIT_STATUS='POSTED',IDEMPOTENCY_KEY=?,POSTER_SUBJECT=?,
                           POSTED_AT=SYSTIMESTAMP,POSTED_RECORD_VERSION=?,STATUS_HISTORY_ID=?
                     WHERE RELEASE_PERMIT_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=?
                       AND PERMIT_STATUS='APPROVED' AND BASE_RECORD_VERSION=?
                    """,key,who.subject(),num(newVersion),num(historyId),num(p.id()),num(id),
                    who.branch(),num(expected));
            if(permitUpdated!=1)throw stop(HttpStatus.CONFLICT,"RELEASE_PERMIT_POSTING_RACE");
            return receipt(id,permit(p.id(),id,who.branch(),false),false);
        }));}
        catch(org.springframework.dao.DuplicateKeyException ex){throw stop(HttpStatus.CONFLICT,"RELEASE_CONCURRENT_OR_DUPLICATE_POSTING");}
    }
}
