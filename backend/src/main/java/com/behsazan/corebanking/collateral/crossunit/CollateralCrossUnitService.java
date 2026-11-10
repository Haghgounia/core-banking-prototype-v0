package com.behsazan.corebanking.collateral.crossunit;

import com.behsazan.corebanking.collateral.crossunit.CollateralCrossUnitModels.*;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestPolicy;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationConsumerBinding;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestRepository;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityService;
import com.behsazan.corebanking.collateral.prototype.CltAnalystStageIdentity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
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
 * BUC-15: bank-approved type + introduction-specific cross-unit authorization.
 * Shared Oracle DataSource, per-collateral/consumer-branch grants, independent
 * maker/checker, non-disclosing inquiry, durable Stage intent, no allocation DML.
 * No bank permission is ever inferred from an HTTP parameter or local actor.
 */
@Service
public final class CollateralCrossUnitService {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final Environment environment;
    private final CollateralAllocationConsumerBinding consumers;
    private final CollateralAllocationRequestRepository sponsors;
    private final CollateralCapacityService capacity;
    private record Actor(String subject,String branch,JwtAuthenticationToken jwt,boolean local) {}
    private record Parent(String collateralId,String registration,String ownerBranch,
            String typeCode,String typeName,String status,String currency,String estimate,
            String version,boolean typePermission,boolean active) {}
    private record Proof(Parent parent,Grant grant) {}
    private record ExistingIntent(String id,String collateralId,String registration,
            String grantId,String ownerBranch,String consumerBranch,String consumerDomain,
            String caseReference,String fileNumber,String amount,String currency,
            int debtorCount,String status,String created,String digest) {}
    private static final String GRANT_SELECT = """
            SELECT GRANT_ID,COLLATERAL_ID,OWNER_BRANCH,CONSUMER_BRANCH,PURPOSE_REFERENCE,
                   GRANT_STATUS,SOURCE_RECORD_VERSION,MAKER_SUBJECT,CHECKER_SUBJECT,
                   APPROVAL_REFERENCE,TO_CHAR(VALID_UNTIL_UTC,'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS EXPIRES,
                   TO_CHAR(CREATED_AT_UTC,'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS CREATED,
                   TO_CHAR(DECIDED_AT_UTC,'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS DECIDED,
                   TO_CHAR(REVOKED_AT_UTC,'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS REVOKED
              FROM CLT.CLT_CROSS_UNIT_GRANT
            """;
    private static final String INTENT_SELECT = """
            SELECT i.INTENT_ID,i.COLLATERAL_ID,i.REGISTRATION_NUMBER,i.GRANT_ID,
                   i.OWNER_BRANCH,i.CONSUMER_BRANCH,i.CONSUMER_DOMAIN,i.CASE_REFERENCE,
                   i.FILE_NUMBER,i.AMOUNT,i.CURRENCY_CODE,i.DEBTOR_COUNT,
                   i.INTENT_STATUS,i.PAYLOAD_DIGEST,
                   TO_CHAR(i.CREATED_AT_UTC,'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS CREATED
              FROM CLT.CLT_CROSS_UNIT_INTENT i
            """;
    public CollateralCrossUnitService(DataSource source,Environment environment,
            CollateralAllocationConsumerBinding consumers,
            CollateralAllocationRequestRepository sponsors,CollateralCapacityService capacity) {
        db=new JdbcTemplate(source);db.setQueryTimeout(12);
        tx=new TransactionTemplate(new DataSourceTransactionManager(source));tx.setTimeout(25);
        this.environment=environment;this.consumers=consumers;
        this.sponsors=sponsors;this.capacity=capacity;
    }
    private static String num(BigDecimal v){return v==null?null:v.toPlainString();}
    private static String value(ResultSet rs,String column)throws SQLException{return num(rs.getBigDecimal(column));}
    private static ResponseStatusException error(HttpStatus code,String reason){return new ResponseStatusException(code,reason);}
    private boolean localMode(){return environment.getProperty("core-banking.collateral.prototype.enabled",Boolean.class,false);}
    private boolean requestOn(){return environment.getProperty("core-banking.collateral.cross-unit.requests-enabled",Boolean.class,false);}
    private boolean decisionsOn(){return environment.getProperty("core-banking.collateral.cross-unit.bank-decisions-enabled",Boolean.class,false);}
    private Actor actor(Authentication authentication,String scope,boolean prohibitLocal) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())
            throw error(HttpStatus.UNAUTHORIZED,"CLT_CROSS_UNIT_SIGNED_AUTHENTICATION_REQUIRED");
        if (jwt.getAuthorities().stream().noneMatch(a->scope.equals(a.getAuthority())))
            throw error(HttpStatus.FORBIDDEN,"CLT_CROSS_UNIT_REQUIRED_SCOPE_MISSING");
        boolean local=Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean("clt_local_prototype"))
                || "CLT_UNIFIED_LOCAL_DEV".equals(jwt.getName());
        if (prohibitLocal && (local || localMode()))
            throw error(HttpStatus.FORBIDDEN,"CLT_CROSS_UNIT_LOCAL_BANK_DECISION_DENIED");
        Object branch=jwt.getToken().getClaims().get("branch_code");
        if (!(branch instanceof String checked))
            throw error(HttpStatus.FORBIDDEN,"CLT_CROSS_UNIT_VERIFIED_BRANCH_REQUIRED");
        try {return new Actor(CollateralCrossUnitPolicy.subject(jwt.getToken().getSubject()),
                CollateralCrossUnitPolicy.branch(checked),jwt,local);}
        catch (IllegalArgumentException ex){throw error(HttpStatus.FORBIDDEN,ex.getMessage());}
    }
    private static Grant grant(ResultSet rs,int row)throws SQLException {
        return new Grant(value(rs,"GRANT_ID"),value(rs,"COLLATERAL_ID"),
                rs.getString("OWNER_BRANCH"),rs.getString("CONSUMER_BRANCH"),
                rs.getString("PURPOSE_REFERENCE"),rs.getString("GRANT_STATUS"),
                value(rs,"SOURCE_RECORD_VERSION"),rs.getString("MAKER_SUBJECT"),
                rs.getString("CHECKER_SUBJECT"),rs.getString("APPROVAL_REFERENCE"),
                rs.getString("EXPIRES"),rs.getString("CREATED"),rs.getString("DECIDED"),
                rs.getString("REVOKED"));
    }
    private static ExistingIntent intent(ResultSet rs,int row)throws SQLException {
        return new ExistingIntent(value(rs,"INTENT_ID"),value(rs,"COLLATERAL_ID"),
                value(rs,"REGISTRATION_NUMBER"),value(rs,"GRANT_ID"),
                rs.getString("OWNER_BRANCH"),rs.getString("CONSUMER_BRANCH"),
                rs.getString("CONSUMER_DOMAIN"),rs.getString("CASE_REFERENCE"),
                value(rs,"FILE_NUMBER"),value(rs,"AMOUNT"),rs.getString("CURRENCY_CODE"),
                rs.getInt("DEBTOR_COUNT"),rs.getString("INTENT_STATUS"),
                rs.getString("CREATED"),rs.getString("PAYLOAD_DIGEST"));
    }
    private static Parent parent(ResultSet rs,int unused)throws SQLException {
        return new Parent(value(rs,"COLLATERAL_ID"),value(rs,"COLLATERAL_NUMBER"),
                rs.getString("OWNER_BRANCH"),rs.getString("COLLATERAL_TYPE_CODE"),
                rs.getString("COLLATERAL_TYPE_NAME"),rs.getString("COLLATERAL_STATUS_CODE"),
                rs.getString("CURRENCY_CODE"),value(rs,"ESTIMATE_AMOUNT"),
                value(rs,"RECORD_VERSION"),"Y".equals(rs.getString("USE_IN_OTHER_BRANCHES")),
                "Y".equals(rs.getString("IS_ACTIVE_STATUS_FLAG"))
                    && "Y".equals(rs.getString("PERIOD_VALID"))
                    && "ACTIVE".equals(rs.getString("TYPE_STATUS")));
    }
    private static final String PARENT_SELECT = """
            SELECT c.COLLATERAL_ID,c.COLLATERAL_NUMBER,c.BRANCH_CODE AS OWNER_BRANCH,
                   c.COLLATERAL_STATUS_CODE,c.CURRENCY_CODE,c.ESTIMATE_AMOUNT,c.RECORD_VERSION,
                   t.COLLATERAL_TYPE_CODE,t.COLLATERAL_TYPE_NAME,t.STATUS AS TYPE_STATUS,
                   t.USE_IN_OTHER_BRANCHES,s.IS_ACTIVE_STATUS_FLAG,
                   CASE WHEN (c.VALID_FROM IS NULL OR c.VALID_FROM<=TRUNC(SYSDATE))
                         AND (c.VALID_TO IS NULL OR c.VALID_TO>=TRUNC(SYSDATE))
                        THEN 'Y' ELSE 'N' END AS PERIOD_VALID
              FROM CLT.COLLATERAL c
              JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=c.COLLATERAL_TYPE_ID
              JOIN CLT.REF_COLLATERAL_STATUS s ON s.COLLATERAL_STATUS_CODE=c.COLLATERAL_STATUS_CODE
            """;
    private Parent ownerCollateral(String id,String branch,boolean lock) {
        List<Parent> rows=db.query(PARENT_SELECT+" WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=?"
                +(lock?" FOR UPDATE OF c.RECORD_VERSION":""),
                CollateralCrossUnitService::parent,new BigDecimal(CollateralCrossUnitPolicy.id(id)),branch);
        if(rows.size()!=1)throw error(HttpStatus.NOT_FOUND,"CLT_CROSS_UNIT_COLLATERAL_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    private Proof proof(String registration,String branch,boolean lock) {
        String query=PARENT_SELECT.replace("SELECT c.COLLATERAL_ID,", "SELECT g.GRANT_ID, g.VALID_UNTIL_UTC, c.COLLATERAL_ID,") + """
                 JOIN CLT.CLT_CROSS_UNIT_GRANT g ON g.COLLATERAL_ID=c.COLLATERAL_ID
                WHERE c.COLLATERAL_NUMBER=? AND g.CONSUMER_BRANCH=?
                  AND c.BRANCH_CODE<>? AND g.OWNER_BRANCH=c.BRANCH_CODE
                  AND g.GRANT_STATUS='APPROVED'
                  AND g.SOURCE_RECORD_VERSION=c.RECORD_VERSION
                  AND g.VALID_UNTIL_UTC>SYS_EXTRACT_UTC(SYSTIMESTAMP)
                  AND t.USE_IN_OTHER_BRANCHES='Y' AND t.STATUS='ACTIVE'
                  AND s.IS_ACTIVE_STATUS_FLAG='Y'
                  AND (c.VALID_FROM IS NULL OR c.VALID_FROM<=TRUNC(SYSDATE))
                  AND (c.VALID_TO IS NULL OR c.VALID_TO>=TRUNC(SYSDATE))
                """ +(lock?" FOR UPDATE OF c.RECORD_VERSION,g.GRANT_STATUS":"");
        List<Proof> result=db.query(query,(rs,i)->{
            Parent p=parent(rs,i);
            Grant g=new Grant(value(rs,"GRANT_ID"),p.collateralId(),p.ownerBranch(),branch,
                    null,"APPROVED",p.version(),null,null,null,
                    rs.getTimestamp("VALID_UNTIL_UTC").toInstant().toString(),null,null,null);
            return new Proof(p,g);
        },new BigDecimal(CollateralCrossUnitPolicy.id(registration)),branch,branch);
        if(result.size()!=1)throw error(HttpStatus.NOT_FOUND,"CLT_CROSS_UNIT_NOT_FOUND_OR_NOT_AUTHORIZED");
        return result.getFirst();
    }
    private boolean columns(String table,String... cols){
        StringBuilder q=new StringBuilder("SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT' AND TABLE_NAME=? AND COLUMN_NAME IN (");
        Object[] args=new Object[cols.length+1];args[0]=table;
        for(int i=0;i<cols.length;i++){if(i>0)q.append(',');q.append('?');args[i+1]=cols[i];}
        q.append(')');
        Integer found=db.queryForObject(q.toString(),Integer.class,args);
        return found!=null && found==cols.length;
    }
    private boolean constraint(String table,String name,String type){
        Integer n=db.queryForObject("""
                SELECT COUNT(*) FROM ALL_CONSTRAINTS
                 WHERE OWNER='CLT' AND TABLE_NAME=? AND CONSTRAINT_NAME=?
                   AND CONSTRAINT_TYPE=? AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                """,Integer.class,table,name,type);
        return n!=null && n==1;
    }
    private boolean index(String name){
        Integer n=db.queryForObject("""
                SELECT COUNT(*) FROM ALL_INDEXES WHERE OWNER='CLT' AND INDEX_NAME=?
                  AND STATUS='VALID' AND UNIQUENESS='UNIQUE'
                """,Integer.class,name);
        return n!=null && n==1;
    }
    private boolean sequence(String name){
        Integer n=db.queryForObject("""
                SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT' AND SEQUENCE_NAME=?
                """,Integer.class,name);
        return n!=null&&n==1;
    }
    private boolean schemaReady(){
        try {
            return columns("CLT_CROSS_UNIT_GRANT","GRANT_ID","COLLATERAL_ID","OWNER_BRANCH",
                    "CONSUMER_BRANCH","SOURCE_RECORD_VERSION","PURPOSE_REFERENCE","GRANT_STATUS",
                    "MAKER_SUBJECT","CHECKER_SUBJECT","REQUEST_KEY","REQUEST_DIGEST",
                    "DECISION_KEY","APPROVAL_REFERENCE","VALID_UNTIL_UTC","CREATED_AT_UTC",
                    "DECIDED_AT_UTC","REVOKED_AT_UTC")
                && columns("CLT_CROSS_UNIT_GRANT_EVENT","EVENT_ID","GRANT_ID","EVENT_CODE",
                    "EVENT_ACTOR","EVENT_REFERENCE","EVENT_AT_UTC")
                && columns("CLT_CROSS_UNIT_INTENT","INTENT_ID","COLLATERAL_ID","REGISTRATION_NUMBER",
                    "GRANT_ID","OWNER_BRANCH","CONSUMER_BRANCH","ACTOR_SUBJECT","CLIENT_ID",
                    "REQUEST_KEY","PAYLOAD_DIGEST","CONSUMER_DOMAIN","CASE_REFERENCE",
                    "FILE_NUMBER","AMOUNT","CURRENCY_CODE","DEBTOR_COUNT","INTENT_STATUS","CREATED_AT_UTC")
                && columns("CLT_CROSS_UNIT_INTENT_DEBTOR","INTENT_ID","PARTY_ID","DEBTOR_ROLE")
                && constraint("CLT_CROSS_UNIT_GRANT","PK_CLT_CU_GRANT","P")
                && constraint("CLT_CROSS_UNIT_GRANT","UQ_CLT_CU_GRANT_KEY","U")
                && constraint("CLT_CROSS_UNIT_INTENT","UQ_CLT_CU_INTENT_KEY","U")
                && constraint("CLT_CROSS_UNIT_INTENT_DEBTOR","PK_CLT_CU_INTENT_DEBTOR","P")
                && constraint("CLT_CROSS_UNIT_GRANT","CK_CLT_CU_GRANT_STATUS","C")
                && constraint("CLT_CROSS_UNIT_INTENT","CK_CLT_CU_INTENT_STATUS","C")
                && index("UQ_CLT_CU_GRANT_ACTIVE")
                && sequence("SEQ_CLT_CU_GRANT") && sequence("SEQ_CLT_CU_GRANT_EVENT")
                && sequence("SEQ_CLT_CU_INTENT");
        } catch (DataAccessException ex) {return false;}
    }
    private void requireSchema(){if(!schemaReady())throw error(HttpStatus.SERVICE_UNAVAILABLE,
            "CLT_CROSS_UNIT_APPROVED_ORACLE_SCHEMA_REQUIRED");}
    private void requireRequests(){if(!requestOn())throw error(HttpStatus.SERVICE_UNAVAILABLE,
            "CLT_CROSS_UNIT_REQUESTS_DISABLED");requireSchema();}
    private void requireDecisions(){if(!decisionsOn() || localMode())throw error(HttpStatus.SERVICE_UNAVAILABLE,
            "CLT_CROSS_UNIT_BANK_DECISIONS_DISABLED");requireSchema();}
    public Readiness readiness(Authentication auth){
        Actor a=actor(auth,"SCOPE_clt.collateral.read",false);
        boolean ready=schemaReady();
        return new Readiness(a.branch(),ready,requestOn(),decisionsOn()&&!localMode(),false,
                ready?"PERMISSION_WORKFLOW_AVAILABLE_NO_FINANCIAL_POSTING":
                      "G13_INTRODUCTION_PERMISSION_SCHEMA_NOT_APPROVED");
    }
    public Grants grants(String id,Authentication auth){
        Actor a=actor(auth,"SCOPE_clt.collateral.read",false);
        requireSchema();ownerCollateral(id,a.branch(),false);
        List<Grant> list=db.query(GRANT_SELECT+" WHERE COLLATERAL_ID=? AND OWNER_BRANCH=?"
                +" ORDER BY GRANT_ID DESC FETCH FIRST 100 ROWS ONLY",
                CollateralCrossUnitService::grant,new BigDecimal(id),a.branch());
        return new Grants(List.copyOf(list));
    }
    private Grant grantByRequest(Actor a,String requestKey,boolean lock){
        List<Grant> results=db.query(GRANT_SELECT+" WHERE OWNER_BRANCH=? AND MAKER_SUBJECT=? AND REQUEST_KEY=?"
                +(lock?" FOR UPDATE":""),CollateralCrossUnitService::grant,
                a.branch(),a.subject(),requestKey);
        return results.isEmpty()?null:results.getFirst();
    }
    private Grant grantById(String collateral,String owner,String grantId,boolean lock){
        List<Grant> rows=db.query(GRANT_SELECT+" WHERE GRANT_ID=? AND COLLATERAL_ID=? AND OWNER_BRANCH=?"
                +(lock?" FOR UPDATE":""),CollateralCrossUnitService::grant,
                new BigDecimal(CollateralCrossUnitPolicy.id(grantId)),
                new BigDecimal(CollateralCrossUnitPolicy.id(collateral)),owner);
        if(rows.size()!=1)throw error(HttpStatus.NOT_FOUND,"CLT_CROSS_UNIT_GRANT_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    private void grantEvent(String id,String event,String subject,String reference){
        db.update("""
            INSERT INTO CLT.CLT_CROSS_UNIT_GRANT_EVENT
              (EVENT_ID,GRANT_ID,EVENT_CODE,EVENT_ACTOR,EVENT_REFERENCE,EVENT_AT_UTC)
            VALUES (CLT.SEQ_CLT_CU_GRANT_EVENT.NEXTVAL,?,?,?,?,SYS_EXTRACT_UTC(SYSTIMESTAMP))
            """,new BigDecimal(id),event,subject,reference);
    }
    public Grant requestGrant(String collateral,GrantCommand command,String rawKey,Authentication auth){
        Actor actor=actor(auth,"SCOPE_clt.collateral.read",false);
        if(command==null)throw new IllegalArgumentException("CLT_CROSS_UNIT_GRANT_BODY_REQUIRED");
        String id=CollateralCrossUnitPolicy.id(collateral);
        String target=CollateralCrossUnitPolicy.branch(command.consumerBranch());
        String ref=CollateralCrossUnitPolicy.reference(command.purposeReference());
        CollateralCrossUnitPolicy.otherBranch(actor.branch(),target);
        String key=CollateralCrossUnitPolicy.requestKey(rawKey);
        String payload=CollateralCrossUnitPolicy.hash(List.of(id,actor.branch(),actor.subject(),target,ref));
        requireRequests();
        try {return saveGrant(actor,id,target,ref,key,payload);}
        catch (DuplicateKeyException ex) {
            Grant found=grantByRequest(actor,key,false);
            if(found!=null && found.collateralId().equals(id) && found.consumerBranch().equals(target)
                    && found.purposeReference().equals(ref))return found;
            throw error(HttpStatus.CONFLICT,"CLT_CROSS_UNIT_GRANT_DUPLICATE_OR_ACTIVE_PERMISSION");
        }
    }
    private Grant saveGrant(Actor a,String id,String target,String reference,String key,String digest){
        Grant r=tx.execute(s->{
            // Single lock order for BUC-09/11/15: COLLATERAL -> GRANT/INTENT.
            // Never lock the grant before its parent (Oracle deadlock prevention).
            Parent p=ownerCollateral(id,a.branch(),true);
            Grant old=grantByRequest(a,key,true);
            if(old!=null) {
                if(!id.equals(old.collateralId()) || !target.equals(old.consumerBranch())
                        || !reference.equals(old.purposeReference()))
                    throw error(HttpStatus.CONFLICT,"CLT_CROSS_UNIT_GRANT_IDEMPOTENCY_CONFLICT");
                return old;
            }
            if(!p.typePermission()||!p.active())throw error(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLT_CROSS_UNIT_TYPE_OR_COLLATERAL_NOT_PERMITTED");
            BigDecimal grantId=db.queryForObject("SELECT CLT.SEQ_CLT_CU_GRANT.NEXTVAL FROM DUAL",BigDecimal.class);
            if(grantId==null)throw new IllegalStateException("CLT_CROSS_UNIT_GRANT_SEQUENCE_UNAVAILABLE");
            int count=db.update("""
                    INSERT INTO CLT.CLT_CROSS_UNIT_GRANT
                      (GRANT_ID,COLLATERAL_ID,OWNER_BRANCH,CONSUMER_BRANCH,
                       SOURCE_RECORD_VERSION,PURPOSE_REFERENCE,GRANT_STATUS,
                       MAKER_SUBJECT,REQUEST_KEY,REQUEST_DIGEST,CREATED_AT_UTC)
                    VALUES (?,?,?,?,?,?,'PENDING',?,?,?,SYS_EXTRACT_UTC(SYSTIMESTAMP))
                    """,grantId,new BigDecimal(id),a.branch(),target,new BigDecimal(p.version()),
                    reference,a.subject(),key,digest);
            if(count!=1)throw new IllegalStateException("CLT_CROSS_UNIT_GRANT_INSERT_MISSING");
            grantEvent(grantId.toPlainString(),"REQUESTED",a.subject(),reference);
            return grantById(id,a.branch(),grantId.toPlainString(),false);
        });
        if(r==null)throw new IllegalStateException("CLT_CROSS_UNIT_GRANT_TX_EMPTY");
        return r;
    }
    public Grant decide(String collateral,String grantId,GrantDecision command,String requestKey,Authentication auth){
        Actor a=actor(auth,"SCOPE_clt.crossunit.approve",true);
        if(command==null || !List.of("APPROVE","REJECT").contains(command.decision()))
            throw new IllegalArgumentException("CLT_CROSS_UNIT_DECISION_INVALID");
        String approvalRef=CollateralCrossUnitPolicy.reference(command.approvalReference());
        String key=CollateralCrossUnitPolicy.requestKey(requestKey);
        Instant until="APPROVE".equals(command.decision())
                ?CollateralCrossUnitPolicy.expiry(command.validUntilUtc(),Instant.now()):null;
        requireDecisions();
        Grant r=tx.execute(s->{
            // Same parent-first lock order as financial posting and final release.
            Parent p=ownerCollateral(collateral,a.branch(),true);
            Grant current=grantById(collateral,a.branch(),grantId,true);
            if(!"PENDING".equals(current.status()))throw error(HttpStatus.CONFLICT,
                    "CLT_CROSS_UNIT_GRANT_ALREADY_DECIDED");
            CollateralCrossUnitPolicy.checker(current.maker(),a.subject());
            if(!p.version().equals(current.sourceRecordVersion()) || !p.typePermission() || !p.active())
                throw error(HttpStatus.CONFLICT,"CLT_CROSS_UNIT_GRANT_SOURCE_CHANGED_OR_DISABLED");
            String status="APPROVE".equals(command.decision())?"APPROVED":"REJECTED";
            int count=db.update("""
                UPDATE CLT.CLT_CROSS_UNIT_GRANT
                   SET GRANT_STATUS=?,CHECKER_SUBJECT=?,DECISION_KEY=?,
                       APPROVAL_REFERENCE=?,VALID_UNTIL_UTC=?,
                       DECIDED_AT_UTC=SYS_EXTRACT_UTC(SYSTIMESTAMP)
                 WHERE GRANT_ID=? AND GRANT_STATUS='PENDING'
                """,status,a.subject(),key,approvalRef,
                until==null?null:Timestamp.from(until),new BigDecimal(grantId));
            if(count!=1)throw error(HttpStatus.CONFLICT,"CLT_CROSS_UNIT_DECISION_CONCURRENT_CONFLICT");
            grantEvent(grantId,status,a.subject(),approvalRef);
            return grantById(collateral,a.branch(),grantId,false);
        });
        if(r==null)throw new IllegalStateException("CLT_CROSS_UNIT_DECISION_TX_EMPTY");
        return r;
    }
    public Grant revoke(String collateral,String grantId,RevokeCommand command,Authentication auth){
        Actor a=actor(auth,"SCOPE_clt.crossunit.approve",true);
        if(command==null)throw new IllegalArgumentException("CLT_CROSS_UNIT_REVOKE_BODY_REQUIRED");
        String reason=CollateralCrossUnitPolicy.reference(command.reasonReference());
        requireDecisions();
        Grant result=tx.execute(s->{
            ownerCollateral(collateral,a.branch(),true); // parent BEFORE grant, always
            Grant current=grantById(collateral,a.branch(),grantId,true);
            if(!"APPROVED".equals(current.status()))throw error(HttpStatus.CONFLICT,
                    "CLT_CROSS_UNIT_ONLY_APPROVED_GRANTS_REVOKABLE");
            int count=db.update("""
                UPDATE CLT.CLT_CROSS_UNIT_GRANT SET GRANT_STATUS='REVOKED',
                    REVOKED_AT_UTC=SYS_EXTRACT_UTC(SYSTIMESTAMP)
                 WHERE GRANT_ID=? AND GRANT_STATUS='APPROVED'
                """,new BigDecimal(grantId));
            if(count!=1)throw error(HttpStatus.CONFLICT,"CLT_CROSS_UNIT_REVOKE_CONCURRENT_CONFLICT");
            grantEvent(grantId,"REVOKED",a.subject(),reason);
            return grantById(collateral,a.branch(),grantId,false);
        });
        if(result==null)throw new IllegalStateException("CLT_CROSS_UNIT_REVOKE_TX_EMPTY");
        return result;
    }
    private static Eligibility eligibility(Proof p,String consuming) {
        Parent c=p.parent();Grant g=p.grant();
        return new Eligibility(c.collateralId(),c.registration(),c.ownerBranch(),
                consuming,c.typeCode(),c.typeName(),c.status(),c.currency(),
                c.estimate(),c.version(),g.grantId(),g.validUntilUtc(),true,true,false,
                "APPROVED_TYPE_AND_PER_INTRODUCTION_GRANT_READ_ONLY_NOT_FINANCIAL_ALLOCATION");
    }
    public Eligibility inquire(String registration,Authentication auth){
        Actor a=actor(auth,"SCOPE_clt.collateral.read",false);
        requireSchema();
        // Never reveal whether a foreign-branch collateral exists if either permission is absent.
        return eligibility(proof(CollateralCrossUnitPolicy.id(registration),a.branch(),false),a.branch());
    }
    private ExistingIntent intentByKey(String branch,String client,String key,boolean lock){
        List<ExistingIntent> rows=db.query(INTENT_SELECT+" WHERE i.CONSUMER_BRANCH=? AND i.CLIENT_ID=? AND i.REQUEST_KEY=?"
                +(lock?" FOR UPDATE":""),CollateralCrossUnitService::intent,branch,client,key);
        return rows.isEmpty()?null:rows.getFirst();
    }
    private static IntentReceipt receipt(ExistingIntent r,boolean replay){
        boolean posted="POSTED".equals(r.status());
        return new IntentReceipt(r.id(),r.collateralId(),r.registration(),r.grantId(),
                r.ownerBranch(),r.consumerBranch(),r.consumerDomain(),r.caseReference(),
                r.fileNumber(),r.amount(),r.currency(),r.debtorCount(),r.status(),
                replay,posted,r.created(),r.digest(),posted ? List.of(
                "BUC09_ATOMIC_POSTING_COMPLETED_USE_FINANCIAL_LEDGER_AS_AUTHORITY") : List.of(
                "G09_CONSUMER_OBLIGATION_NOT_ATTESTED_BY_CLT",
                "G02_BANK_PERMIT_FOR_ATOMIC_ALLOCATION_REQUIRED",
                "G16_CROSS_DOMAIN_ATOMIC_CAPACITY_LOCK_NOT_COMPLETED",
                "CLT_CROSS_UNIT_STAGE_NOT_FINANCIAL_ALLOCATION"));
    }
    public IntentReceipt stageAnalyst(IntentCommand command,String key,Authentication auth){
        Actor a=actor(CltAnalystStageIdentity.require(auth),"SCOPE_clt.collateral.read",false);
        return stage(command,key,a,CltAnalystStageIdentity.clientId());
    }
    public IntentReceipt stageConsumer(IntentCommand command,String key,Authentication auth){
        Actor a=actor(auth,"SCOPE_clt.allocation.request",true);
        if(command==null || command.allocation()==null)
            throw new IllegalArgumentException("CLT_CROSS_UNIT_INTENT_REQUIRED");
        String client;
        try {client=consumers.authenticatedClient(a.jwt(),
                CollateralAllocationRequestPolicy.normalize(command.allocation()).consumerDomain());}
        catch(SecurityException ex){throw error(HttpStatus.FORBIDDEN,ex.getMessage());}
        return stage(command,key,a,client);
    }
    private IntentReceipt stage(IntentCommand command,String rawKey,Actor a,String client){
        if(command==null || command.allocation()==null)
            throw new IllegalArgumentException("CLT_CROSS_UNIT_INTENT_REQUIRED");
        var n=CollateralAllocationRequestPolicy.normalize(command.allocation());
        String registration=CollateralCrossUnitPolicy.id(command.registrationNumber());
        String key=CollateralCrossUnitPolicy.requestKey(rawKey);
        requireRequests();
        // Grant and source collateral are locked and revalidated inside the Stage transaction.
        try {return insertIntent(registration,n,a,client,key);}
        catch(DuplicateKeyException ex){
            ExistingIntent existing=intentByKey(a.branch(),client,key,false);
            if(existing==null)throw error(HttpStatus.CONFLICT,"CLT_CROSS_UNIT_INTENT_RACE_CONFLICT");
            if(!registration.equals(existing.registration()))
                throw error(HttpStatus.CONFLICT,"CLT_CROSS_UNIT_INTENT_REPLAY_REGISTRATION_DIFFERENT");
            String digest=CollateralAllocationRequestPolicy.digest(a.branch(),client,
                    existing.collateralId(),n);
            String submitted=CollateralCrossUnitPolicy.hash(List.of(
                    existing.grantId(),existing.registration(),existing.ownerBranch(),digest));
            try{CollateralCrossUnitPolicy.sameDigest(existing.digest(),submitted);}
            catch(IllegalArgumentException ignored){throw error(HttpStatus.CONFLICT,
                    "CLT_CROSS_UNIT_INTENT_IDEMPOTENCY_CONFLICT");}
            return receipt(existing,true);
        }
    }
    private IntentReceipt insertIntent(String registration,CollateralAllocationRequestPolicy.Normalized n,
            Actor a,String client,String key){
        IntentReceipt result=tx.execute(s->{
            ExistingIntent prior=intentByKey(a.branch(),client,key,true);
            if(prior!=null){
                String digest=CollateralAllocationRequestPolicy.digest(a.branch(),client,prior.collateralId(),n);
                String requestHash=CollateralCrossUnitPolicy.hash(List.of(
                        prior.grantId(),registration,prior.ownerBranch(),digest));
                if(!registration.equals(prior.registration()))throw error(HttpStatus.CONFLICT,
                        "CLT_CROSS_UNIT_INTENT_REPLAY_REGISTRATION_DIFFERENT");
                try{CollateralCrossUnitPolicy.sameDigest(prior.digest(),requestHash);}
                catch(IllegalArgumentException ex){throw error(HttpStatus.CONFLICT,
                        "CLT_CROSS_UNIT_INTENT_IDEMPOTENCY_CONFLICT");}
                return receipt(prior,true);
            }
            Proof p=proof(registration,a.branch(),true);
            if(!n.currencyCode().equals(p.parent().currency()))
                throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CLT_CROSS_UNIT_FX_POLICY_NOT_APPROVED");
            if(!sponsors.sponsorDebtorMatches(p.parent().collateralId(),
                    p.parent().ownerBranch(),n.debtors()).isEmpty())
                throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CLT_CROSS_UNIT_SPONSOR_DEBTOR_CONFLICT");
            // Read-only capacity evidence; explicitly NOT an authorization or a reservation.
            capacity.evidenceForVerifiedBranch(p.parent().collateralId(),p.parent().ownerBranch());
            String original=CollateralAllocationRequestPolicy.digest(a.branch(),client,
                    p.parent().collateralId(),n);
            String digest=CollateralCrossUnitPolicy.hash(List.of(p.grant().grantId(),
                    registration,p.parent().ownerBranch(),original));
            BigDecimal id=db.queryForObject("SELECT CLT.SEQ_CLT_CU_INTENT.NEXTVAL FROM DUAL",BigDecimal.class);
            if(id==null)throw new IllegalStateException("CLT_CROSS_UNIT_INTENT_SEQUENCE_UNAVAILABLE");
            db.update("""
                INSERT INTO CLT.CLT_CROSS_UNIT_INTENT
                 (INTENT_ID,COLLATERAL_ID,REGISTRATION_NUMBER,GRANT_ID,OWNER_BRANCH,
                  CONSUMER_BRANCH,ACTOR_SUBJECT,CLIENT_ID,REQUEST_KEY,PAYLOAD_DIGEST,
                  CONSUMER_DOMAIN,CASE_REFERENCE,FILE_NUMBER,AMOUNT,CURRENCY_CODE,
                  DEBTOR_COUNT,INTENT_STATUS,CREATED_AT_UTC)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,
                        'RECORDED_NOT_ALLOCATED',SYS_EXTRACT_UTC(SYSTIMESTAMP))
                """,id,new BigDecimal(p.parent().collateralId()),new BigDecimal(registration),
                new BigDecimal(p.grant().grantId()),p.parent().ownerBranch(),a.branch(),a.subject(),
                client,key,digest,n.consumerDomain(),n.caseReference(),
                new BigDecimal(n.fileNumber()),new BigDecimal(n.amount()),n.currencyCode(),
                n.debtors().size());
            for(Debtor d:n.debtors())db.update("""
                INSERT INTO CLT.CLT_CROSS_UNIT_INTENT_DEBTOR(INTENT_ID,PARTY_ID,DEBTOR_ROLE)
                VALUES (?,?,?)
                """,id,new BigDecimal(d.partyId()),d.role());
            ExistingIntent stored=intentByKey(a.branch(),client,key,false);
            if(stored==null)throw new IllegalStateException("CLT_CROSS_UNIT_INTENT_READBACK_MISSING");
            return receipt(stored,false);
        });
        if(result==null)throw new IllegalStateException("CLT_CROSS_UNIT_INTENT_TX_EMPTY");
        return result;
    }
    public List<IntentReceipt> intents(Authentication auth){
        Actor a=actor(auth,"SCOPE_clt.collateral.read",false);
        requireSchema();
        return db.query(INTENT_SELECT+" WHERE i.CONSUMER_BRANCH=? AND i.ACTOR_SUBJECT=?"
                +" ORDER BY i.INTENT_ID DESC FETCH FIRST 30 ROWS ONLY",
                (rs,i)->receipt(intent(rs,i),false),a.branch(),a.subject());
    }
}
