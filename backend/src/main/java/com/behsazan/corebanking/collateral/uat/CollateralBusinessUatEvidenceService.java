package com.behsazan.corebanking.collateral.uat;

import com.behsazan.corebanking.collateral.prototype.CltAnalystStageIdentity;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.uat.CollateralBusinessUatEvidencePolicy.Scenario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Analyst-only UAT evidence journal on the SHARED Oracle DataSource.
 * Never touches COLLATERAL/ALLOCATION, issues bank approval, or runs DDL.
 * No separate datasource or starter. DML is conditional on a bank-approved auxiliary schema.
 */
@Service
public class CollateralBusinessUatEvidenceService {
    private static final String CASE_SET=CollateralBusinessUatEvidencePolicy.CASE_SET;
    private static final Map<String,Set<String>> COLUMNS=Map.of(
        "CLT_BUC_UAT_RUN",Set.of("BRANCH_CODE","CASE_SET","REVISION","UPDATED_AT","UPDATED_BY"),
        "CLT_BUC_UAT_RESULT",Set.of("BRANCH_CODE","CASE_SET","SCENARIO_ID","STATUS_CODE","EVIDENCE_NOTE","UPDATED_AT","UPDATED_BY"),
        "CLT_BUC_UAT_EVENT",Set.of("EVENT_ID","BRANCH_CODE","CASE_SET","SCENARIO_ID","REVISION","OLD_STATUS","NEW_STATUS","OLD_NOTE","NEW_NOTE","CREATED_BY","CREATED_AT")
    );
    private static final String READ_RUN = "SELECT REVISION FROM CLT.CLT_BUC_UAT_RUN WHERE BRANCH_CODE=? AND CASE_SET=?";
    private static final String LOCK_RUN = READ_RUN+" FOR UPDATE WAIT 2";
    private static final String READ_ROWS = "SELECT SCENARIO_ID,STATUS_CODE,EVIDENCE_NOTE FROM CLT.CLT_BUC_UAT_RESULT WHERE BRANCH_CODE=? AND CASE_SET=? ORDER BY SCENARIO_ID";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public CollateralBusinessUatEvidenceService(DataSource dataSource) {
        this.jdbc=new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(12);
        this.tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.tx.setTimeout(25);
    }
    public record EvidenceState(String schema,boolean available,String reason,long revision,
            String checkedAt,boolean bankAcceptance,boolean oracleAttestation,
            List<Scenario> scenarios) {}
    public record EvidenceWrite(Long expectedRevision,Boolean bankAcceptance,
            Boolean oracleAttestation,List<Scenario> scenarios) {}
    public record EvidenceEvent(String eventId,String scenarioId,long revision,String fromStatus,
            String toStatus,String actor,String happenedAt) {}
    private record Actor(String subject,String branch) {}

    private static ResponseStatusException fail(HttpStatus status,String message) {
        return new ResponseStatusException(status,message);
    }
    private Actor actor(Authentication auth,boolean write) {
        if(!(auth instanceof JwtAuthenticationToken jwt)||!jwt.isAuthenticated())
            throw fail(HttpStatus.UNAUTHORIZED,"CLT_UAT_IDENTITY_REQUIRED");
        if(auth.getAuthorities().stream().noneMatch(a -> "SCOPE_clt.collateral.read".equals(a.getAuthority())))
            throw fail(HttpStatus.FORBIDDEN,"CLT_UAT_READ_SCOPE_REQUIRED");
        Object claim=jwt.getToken().getClaims().get("branch_code");
        String subject=jwt.getToken().getSubject();
        if(!(claim instanceof String branch)||subject==null||subject.isBlank()||subject.length()>100)
            throw fail(HttpStatus.FORBIDDEN,"CLT_UAT_BRANCH_ACTOR_INVALID");
        try { CollateralRegistryPolicy.branch(branch); }
        catch(IllegalArgumentException ex){throw fail(HttpStatus.FORBIDDEN,"CLT_UAT_BRANCH_INVALID");}
        if(write) {
            boolean local=Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean("clt_local_prototype"));
            if(local) CltAnalystStageIdentity.require(auth); // ONLY self-issued loopback Stage token
            else if(auth.getAuthorities().stream().noneMatch(a ->
                    "SCOPE_clt.collateral.uat.write".equals(a.getAuthority())))
                throw fail(HttpStatus.FORBIDDEN,"CLT_UAT_WRITE_SCOPE_REQUIRED");
        }
        return new Actor(subject,branch);
    }
    public boolean schemaReady() {
        try {
            List<String> items=jdbc.query("""
                    SELECT TABLE_NAME,COLUMN_NAME FROM ALL_TAB_COLUMNS
                    WHERE OWNER='CLT' AND TABLE_NAME IN
                    ('CLT_BUC_UAT_RUN','CLT_BUC_UAT_RESULT','CLT_BUC_UAT_EVENT')
                    """,(rs,n)->rs.getString(1)+"."+rs.getString(2));
            Set<String> actual=new HashSet<>(items);
            for(var table:COLUMNS.entrySet())for(String col:table.getValue())
                if(!actual.contains(table.getKey()+"."+col))return false;
            List<String> sequence=jdbc.queryForList("""
                    SELECT SEQUENCE_NAME FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT'
                     AND SEQUENCE_NAME='CLT_BUC_UAT_EVENT_SEQ'
                    """,String.class);
            return !sequence.isEmpty();
        }catch(DataAccessException ex){return false;}
    }
    private List<Scenario> rows(Actor actor) {
        List<Scenario> loaded=jdbc.query(READ_ROWS,(rs,n)->new Scenario(
                rs.getString(1),rs.getString(2),rs.getString(3)==null?"":rs.getString(3)),actor.branch(),CASE_SET);
        return CollateralBusinessUatEvidencePolicy.fillMissing(loaded);
    }
    private long currentRevision(Actor actor,boolean lock) {
        List<Long> revisions=jdbc.query(lock?LOCK_RUN:READ_RUN,
                (rs,n)->rs.getLong(1),actor.branch(),CASE_SET);
        if(revisions.size()>1)throw fail(HttpStatus.CONFLICT,"CLT_UAT_RUN_NOT_UNIQUE");
        return revisions.isEmpty()?-1:revisions.get(0);
    }
    private EvidenceState state(boolean available,String reason,long revision,List<Scenario> scenarios) {
        return new EvidenceState(CollateralBusinessUatEvidencePolicy.CONTRACT,available,reason,
                Math.max(0,revision),Instant.now().toString(),false,false,scenarios);
    }
    public EvidenceState get(Authentication auth) {
        Actor actor=actor(auth,false);
        if(!schemaReady())return state(false,"ORACLE_UAT_SCHEMA_NOT_APPROVED_OR_UNAVAILABLE",0,List.of());
        try {
            // Read the revision twice to refuse an inconsistent cross-commit snapshot.
            for(int retry=0;retry<2;retry++) {
                long before=currentRevision(actor,false);
                List<Scenario> result=rows(actor);
                long after=currentRevision(actor,false);
                if(before==after)return state(true,"",after,result);
            }
            return state(false,"CONCURRENT_EVIDENCE_UPDATE_RETRY",0,List.of());
        }catch(DataAccessException ex){return state(false,"ORACLE_UAT_READ_UNAVAILABLE",0,List.of());}
    }
    public List<EvidenceEvent> history(Authentication auth) {
        Actor actor=actor(auth,false);
        if(!schemaReady())return List.of();
        try {
            return jdbc.query("""
                    SELECT EVENT_ID,SCENARIO_ID,REVISION,OLD_STATUS,NEW_STATUS,
                           CREATED_BY,TO_CHAR(CREATED_AT,'YYYY-MM-DD HH24:MI:SS')
                      FROM CLT.CLT_BUC_UAT_EVENT
                     WHERE BRANCH_CODE=? AND CASE_SET=?
                     ORDER BY EVENT_ID DESC FETCH FIRST 50 ROWS ONLY
                    """,(rs,n)->new EvidenceEvent(rs.getBigDecimal(1).toPlainString(),
                        rs.getString(2),rs.getLong(3),rs.getString(4),rs.getString(5),
                        rs.getString(6),rs.getString(7)),actor.branch(),CASE_SET);
        }catch(DataAccessException ex){return List.of();}
    }
    public EvidenceState put(Authentication auth,EvidenceWrite request) {
        Actor actor=actor(auth,true);
        if(request==null||!Boolean.FALSE.equals(request.bankAcceptance())
                ||!Boolean.FALSE.equals(request.oracleAttestation()))
            throw fail(HttpStatus.UNPROCESSABLE_ENTITY,"CLT_UAT_FORGED_ATTESTATION_OR_BAD_REQUEST");
        final long expected;
        final List<Scenario> incoming;
        try {
            expected=CollateralBusinessUatEvidencePolicy.revision(request.expectedRevision());
            incoming=CollateralBusinessUatEvidencePolicy.normalize(request.scenarios());
        }catch(IllegalArgumentException ex){throw fail(HttpStatus.UNPROCESSABLE_ENTITY,ex.getMessage());}
        if(!schemaReady())throw fail(HttpStatus.SERVICE_UNAVAILABLE,"CLT_UAT_APPROVED_ORACLE_TABLES_REQUIRED");
        try {
            EvidenceState answer=tx.execute(status->saveLocked(actor,expected,incoming));
            if(answer==null)throw fail(HttpStatus.SERVICE_UNAVAILABLE,"CLT_UAT_TRANSACTION_NOT_COMPLETED");
            return answer;
        }catch(DuplicateKeyException ex) {
            // Two analysts initializing the same branch-specific register must not race to overwrite it.
            throw fail(HttpStatus.CONFLICT,"CLT_UAT_CONCURRENT_RUN_INITIALIZATION");
        }catch(DataAccessException ex) {
            // Never substitute an in-memory success when Oracle rolls back.
            throw fail(HttpStatus.SERVICE_UNAVAILABLE,"CLT_UAT_ORACLE_WRITE_FAILED");
        }
    }
    private EvidenceState saveLocked(Actor actor,long expected,List<Scenario> incoming) {
        long revision=currentRevision(actor,true);
        if(revision<0) {
            if(expected!=0)throw fail(HttpStatus.CONFLICT,"CLT_UAT_EXPECTED_REVISION_CONFLICT");
            jdbc.update("""
                    INSERT INTO CLT.CLT_BUC_UAT_RUN
                    (BRANCH_CODE,CASE_SET,REVISION,UPDATED_AT,UPDATED_BY)
                    VALUES (?,?,0,SYSTIMESTAMP,?)
                    """,actor.branch(),CASE_SET,actor.subject());
            revision=currentRevision(actor,true);
        }
        if(revision!=expected)
            throw fail(HttpStatus.CONFLICT,"CLT_UAT_EXPECTED_REVISION_CONFLICT");
        List<Scenario> before=rows(actor);
        Map<String,Scenario> prior=new HashMap<>();
        before.forEach(row->prior.put(row.id(),row));
        List<Scenario> changes=new ArrayList<>();
        for(Scenario row:incoming)if(!row.equals(prior.get(row.id())))changes.add(row);
        if(changes.isEmpty())return state(true,"",revision,incoming);
        if(revision>=999_999_999L)
            throw fail(HttpStatus.CONFLICT,"CLT_UAT_REVISION_LIMIT_REACHED");
        long next=revision+1;
        // Root CAS + 32-row snapshot + immutable events are ONE Oracle transaction.
        int updated=jdbc.update("""
                UPDATE CLT.CLT_BUC_UAT_RUN SET REVISION=?,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=?
                WHERE BRANCH_CODE=? AND CASE_SET=? AND REVISION=?
                """,next,actor.subject(),actor.branch(),CASE_SET,revision);
        if(updated!=1)throw fail(HttpStatus.CONFLICT,"CLT_UAT_CAS_CONFLICT");
        jdbc.update("DELETE FROM CLT.CLT_BUC_UAT_RESULT WHERE BRANCH_CODE=? AND CASE_SET=?",
                actor.branch(),CASE_SET);
        for(Scenario row:incoming)jdbc.update("""
                INSERT INTO CLT.CLT_BUC_UAT_RESULT
                (BRANCH_CODE,CASE_SET,SCENARIO_ID,STATUS_CODE,EVIDENCE_NOTE,UPDATED_AT,UPDATED_BY)
                VALUES (?,?,?,?,?,SYSTIMESTAMP,?)
                """,actor.branch(),CASE_SET,row.id(),row.status(),row.note(),actor.subject());
        for(Scenario row:changes) {
            Scenario previous=prior.get(row.id());
            jdbc.update("""
                    INSERT INTO CLT.CLT_BUC_UAT_EVENT
                    (EVENT_ID,BRANCH_CODE,CASE_SET,SCENARIO_ID,REVISION,OLD_STATUS,NEW_STATUS,
                     OLD_NOTE,NEW_NOTE,CREATED_BY,CREATED_AT)
                    VALUES (CLT.CLT_BUC_UAT_EVENT_SEQ.NEXTVAL,?,?,?,?,?,?,?,?,?,SYSTIMESTAMP)
                    """,actor.branch(),CASE_SET,row.id(),next,previous.status(),row.status(),
                    previous.note(),row.note(),actor.subject());
        }
        return state(true,"",next,incoming);
    }
}
