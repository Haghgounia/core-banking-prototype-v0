package com.behsazan.corebanking.collateral.insurance.cancellation;

import com.behsazan.corebanking.collateral.insurance.cancellation.CollateralR3InsuranceCancellationPolicy.*;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * G-06: branch-scoped read-only cancellation assessment, optional approved Stage
 * and independently gated append-only final event. The base insurance row and
 * past allocation records are NEVER deleted or silently changed.
 * One shared application DataSource, one Oracle transaction per state transition.
 */
@Service
@ConditionalOnProperty(prefix="core-banking.collateral.prototype", name="enabled", havingValue="true")
public final class CollateralR3InsuranceCancellationService {
    public record Readiness(String collateralId, String insuranceId, String policyRecordVersion,
            String asOf, String currentState, long allocationRows, long renewalSuccessors,
            boolean stageWritesEnabled, boolean finalEventEnabled, Assessment assessment,
            String evidenceSource, String existingCancellationEventId) {}
    public record Activation(boolean stageFeatureFlag, boolean finalFeatureFlag,
            boolean schemaReady, boolean bankReleasePresent, boolean finalCancellationEnabled,
            String status, List<String> openGates) {}
    public record NewRequest(String insuranceVersion, String reason) {}
    public record Transition(String action, String expectedVersion, String reason) {}
    public record StageRow(String requestId, String collateralId, String insuranceId,
            String status, String makerSubject, String reviewerSubject, String recordVersion,
            String createdAt, String evidenceKind) {}
    public record StagePage(long totalElements, int page, int size, List<StageRow> items) {}
    public record Receipt(String requestId, String collateralId, String insuranceId,
            String cancellationEventId, String status, String policyRevision, String postedAt,
            boolean replayed, String downstreamStatus) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final CollateralIntroductionService introduction;
    @Value("${core-banking.collateral.r3-cancellation.stage-enabled:false}")
    private boolean stageEnabled;
    @Value("${core-banking.collateral.r3-cancellation.final-enabled:false}")
    private boolean finalEnabled;

    public CollateralR3InsuranceCancellationService(DataSource applicationDataSource,
            CollateralIntroductionService introduction) {
        this.jdbc = new JdbcTemplate(applicationDataSource);
        this.jdbc.setQueryTimeout(12);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(applicationDataSource));
        this.transaction.setTimeout(25);
        this.introduction = introduction;
    }
    private static boolean has(Authentication a, String scope) {
        return a!=null && a.isAuthenticated() && a.getAuthorities().stream()
                .anyMatch(x -> scope.equals(x.getAuthority()));
    }
    private static void require(Authentication a,String scope) {
        if (!has(a,scope)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "CLT_R3_CANCEL_AUTHORITY_REQUIRED");
    }
    private static String number(Object value) {
        if (value==null) return null;
        return value instanceof BigDecimal n ? n.stripTrailingZeros().toPlainString() : value.toString().trim();
    }
    private static String str(Object value) { return value==null ? null : value.toString().trim(); }
    private static BigDecimal dec(Object value) {
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        throw new IllegalStateException("CLT_R3_CANCEL_ORACLE_NUMBER_REQUIRED");
    }
    private static String day(Object value) {
        if (value instanceof Date d) return d.toLocalDate().toString();
        if (value instanceof Timestamp t) return t.toLocalDateTime().toLocalDate().toString();
        if (value instanceof LocalDate d) return d.toString();
        return null;
    }
    private static String instant(Object value) { return value==null ? null : value.toString(); }
    private boolean table(String name) {
        try { Long n=jdbc.queryForObject("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CLT' AND TABLE_NAME=?",
                Long.class,name); return n!=null && n==1L; }
        catch (RuntimeException ignored) { return false; }
    }
    private boolean unique(String table,String name) {
        try { Long n=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT' AND TABLE_NAME=?
                      AND CONSTRAINT_NAME=? AND CONSTRAINT_TYPE='U'
                      AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                    """,Long.class,table,name); return n!=null && n==1L; }
        catch (RuntimeException ignored) { return false; }
    }
    private boolean appendOnlyTriggersReady() {
        try {
            Long n=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TRIGGERS WHERE OWNER='CLT' AND STATUS='ENABLED'
                     AND ((TABLE_NAME='R3_INS_CANCEL_EVENT' AND TRIGGER_NAME='TRG_R3_INS_CANCEL_EVENT_APPEND')
                       OR (TABLE_NAME='R3_INS_CANCEL_AUDIT' AND TRIGGER_NAME='TRG_R3_INS_CANCEL_AUDIT_APPEND'))
                    """,Long.class);
            return n!=null && n==2L;
        } catch (RuntimeException ignored) {return false;}
    }
    private boolean statusChecksReady() {
        try {
            Long n=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT'
                      AND STATUS='ENABLED' AND VALIDATED='VALIDATED' AND (
                       (TABLE_NAME='R3_INS_CANCEL_REQUEST' AND CONSTRAINT_NAME IN
                        ('CK_R3_INS_CAN_REQ_STATE','CK_R3_INS_CAN_REQ_REVIEW','CK_R3_INS_CAN_REQ_VERS')) OR
                       (TABLE_NAME='R3_INS_CANCEL_AUDIT' AND CONSTRAINT_NAME IN
                        ('CK_R3_INS_CAN_AUD_ACT','CK_R3_INS_CAN_AUD_VERS')) OR
                       (TABLE_NAME='R3_INS_CANCEL_EVENT' AND CONSTRAINT_NAME IN
                        ('CK_R3_INS_CAN_EVENT_VERSION','CK_R3_INS_CAN_EVENT_SEPARATION')))
                    """,Long.class);
            return n!=null && n==7L;
        } catch (RuntimeException ignored) {return false;}
    }
    private boolean cancellationSchema() {
        try {
            Long tables=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CLT' AND TABLE_NAME IN
                     ('R3_INS_CANCEL_REQUEST','R3_INS_CANCEL_AUDIT','R3_INS_CANCEL_IMPACT',
                      'R3_INS_CANCEL_RELEASE','R3_INS_CANCEL_EVENT')
                    """,Long.class);
            Long sequences=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT'
                     AND SEQUENCE_NAME IN ('SEQ_R3_INS_CANCEL_REQ','SEQ_R3_INS_CANCEL_AUD',
                                           'SEQ_R3_INS_CANCEL_EVT')
                    """,Long.class);
            Long columns=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT' AND (
                     (TABLE_NAME='R3_INS_CANCEL_REQUEST' AND COLUMN_NAME IN
                      ('CANCEL_REQUEST_ID','COLLATERAL_ID','INSURANCE_ID','BRANCH_CODE','STATUS_CODE',
                       'MAKER_SUBJECT','REVIEWER_SUBJECT','REVIEW_REASON','REQUEST_REASON',
                       'REQUEST_HASH','IDEMPOTENCY_KEY','EXPECTED_INSURANCE_VERSION',
                       'RECORD_VERSION','CREATED_AT','UPDATED_AT')) OR
                     (TABLE_NAME='R3_INS_CANCEL_AUDIT' AND COLUMN_NAME IN
                      ('AUDIT_ID','CANCEL_REQUEST_ID','ACTION_CODE','ACTOR_SUBJECT','REASON',
                       'VERSION_BEFORE','VERSION_AFTER','EVENT_AT')) OR
                     (TABLE_NAME='R3_INS_CANCEL_IMPACT' AND COLUMN_NAME IN
                      ('CANCEL_REQUEST_ID','IMPACT_STATUS','DECISION_REF','COVERAGE_DECISION_REF',
                       'ALLOCATION_DECISION_REF','APPROVED_BY','REPLACEMENT_INSURANCE_ID','APPROVED_AT')) OR
                     (TABLE_NAME='R3_INS_CANCEL_RELEASE' AND COLUMN_NAME IN
                      ('RELEASE_ID','RELEASE_STATUS','POLICY_REVISION','BUSINESS_DECISION_REF',
                       'DATA_DECISION_REF','COVERAGE_DECISION_REF','ALLOCATION_DECISION_REF',
                       'INTEGRATION_DECISION_REF','DOWNSTREAM_BINDING_REF','APPROVED_BY',
                       'EFFECTIVE_FROM','EFFECTIVE_TO')) OR
                     (TABLE_NAME='R3_INS_CANCEL_EVENT' AND COLUMN_NAME IN
                      ('CANCEL_EVENT_ID','CANCEL_REQUEST_ID','COLLATERAL_ID','INSURANCE_ID',
                       'BRANCH_CODE','REQUEST_HASH','IDEMPOTENCY_KEY','REASON',
                       'REQUEST_VERSION','REVIEWED_BY','POSTED_BY','POLICY_REVISION',
                       'IMPACT_DECISION_REF','REPLACEMENT_INSURANCE_ID','POSTED_AT')))
                    """,Long.class);
            return tables!=null && tables==5 && sequences!=null && sequences==3
                    && columns!=null && columns==58
                    && unique("R3_INS_CANCEL_REQUEST","UQ_R3_INS_CAN_REQ_IDEMP")
                    && unique("R3_INS_CANCEL_EVENT","UQ_R3_INS_CAN_EVENT_POLICY")
                    && unique("R3_INS_CANCEL_EVENT","UQ_R3_INS_CAN_EVENT_REQUEST")
                    && unique("R3_INS_CANCEL_EVENT","UQ_R3_INS_CAN_EVENT_IDEMP")
                    && appendOnlyTriggersReady() && statusChecksReady();
        } catch (RuntimeException ignored) { return false; }
    }
    private void stageGate() {
        if (!stageEnabled || !cancellationSchema())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R3_CANCEL_STAGE_SCHEMA_OR_FEATURE_NOT_APPROVED");
    }
    private static String digest(String id,String policy,String version,String reason,String branch) {
        return CollateralR3InsuranceCancellationPolicy.digest(
                "CLT_R3_CANCEL_V1\u001f"+id+"\u001f"+policy+"\u001f"+version+"\u001f"+reason+"\u001f"+branch);
    }
    private Map<String,Object> policy(BigDecimal collateralId,BigDecimal insuranceId,String branch,boolean lock) {
        String sql="""
                SELECT i.COLLATERAL_INSURANCE_ID, i.COLLATERAL_ID, i.BRANCH_CODE, i.RECORD_VERSION,
                       i.COLLATERAL_INSURANCE_TYPE_ID,i.CURRENCY_CODE,i.VALID_FROM,i.VALID_TO,
                       c.COLLATERAL_STATUS_CODE, t.INSURABLE,co.STATUS AS INSURER_STATUS
                  FROM CLT.COLLATERAL_INSURANCE i
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID=i.COLLATERAL_ID
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=c.COLLATERAL_TYPE_ID
                  JOIN CLT.INSURANCE_COMPANY co ON co.INSURANCE_COMPANY_ID=i.INSURANCE_COMPANY_ID
                 WHERE i.COLLATERAL_INSURANCE_ID=? AND i.COLLATERAL_ID=?
                   AND i.BRANCH_CODE=? AND c.BRANCH_CODE=?
                """ + (lock ? " FOR UPDATE OF i.RECORD_VERSION" : "");
        List<Map<String,Object>> rows=jdbc.queryForList(sql,insuranceId,collateralId,branch,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_CANCEL_POLICY_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    private boolean eventAvailable() {
        return table("R3_INS_CANCEL_EVENT")
                && unique("R3_INS_CANCEL_EVENT","UQ_R3_INS_CAN_EVENT_POLICY")
                && appendOnlyTriggersReady();
    }
    private Readiness assess(String collateralId,String insuranceId,String branch,boolean lock) {
        BigDecimal c=CollateralR3InsuranceCancellationPolicy.numberId(collateralId);
        BigDecimal i=CollateralR3InsuranceCancellationPolicy.numberId(insuranceId);
        Map<String,Object> row=policy(c,i,branch,lock);
        Long allocation=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_ALLOCATION WHERE COLLATERAL_ID=?
                """,Long.class,c);
        Long successors=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE
                 WHERE PREVIOUS_INSURANCE_ID=?
                """,Long.class,i);
        String asOf=jdbc.queryForObject("""
                SELECT TO_CHAR(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE),'YYYY-MM-DD') FROM DUAL
                """,String.class);
        boolean events=eventAvailable();
        String eventId=null;
        if (events) {
            List<Map<String,Object>> rows=jdbc.queryForList("""
                    SELECT CANCEL_EVENT_ID FROM CLT.R3_INS_CANCEL_EVENT
                     WHERE INSURANCE_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=?
                    """,i,c,branch);
            if (rows.size()>1) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R3_CANCEL_LEDGER_DUPLICATE_INTEGRITY");
            if (!rows.isEmpty()) eventId=number(rows.getFirst().get("CANCEL_EVENT_ID"));
        }
        var facts=new Facts(true,true,true,str(row.get("COLLATERAL_STATUS_CODE")),
                day(row.get("VALID_FROM")),day(row.get("VALID_TO")),asOf,
                allocation==null?-1:allocation,successors==null?-1:successors,
                eventId!=null,events,row.get("RECORD_VERSION")!=null,
                row.get("INSURER_STATUS")!=null);
        Assessment answer=CollateralR3InsuranceCancellationPolicy.assess(facts);
        return new Readiness(collateralId,insuranceId,number(row.get("RECORD_VERSION")),
                asOf,eventId!=null ? "CANCELLATION_EVENT_RECORDED" : "LIFECYCLE_NOT_VERIFIED",
                allocation==null?-1:allocation, successors==null?-1:successors,
                stageEnabled && cancellationSchema(),false,answer,
                events ? "ORACLE_CLT_WITH_EVENT_LEDGER" : "ORACLE_CLT_BASE_ONLY_NO_CANCEL_SCHEMA",
                eventId);
    }
    public Readiness preview(String collateralId,String insuranceId,Authentication authentication) {
        require(authentication,"SCOPE_clt.collateral.read");
        var actor=introduction.actor(authentication);
        try { return assess(collateralId,insuranceId,actor.branch(),false); }
        catch (ResponseStatusException ex) { throw ex; }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R3_CANCEL_READ_OR_DICTIONARY_UNAVAILABLE",ex); }
    }
    private Release release(boolean lock) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT POLICY_REVISION,BUSINESS_DECISION_REF,DATA_DECISION_REF,
                       COVERAGE_DECISION_REF,ALLOCATION_DECISION_REF,INTEGRATION_DECISION_REF,
                       DOWNSTREAM_BINDING_REF,APPROVED_BY
                  FROM CLT.R3_INS_CANCEL_RELEASE
                 WHERE RELEASE_ID=1 AND RELEASE_STATUS='APPROVED'
                   AND EFFECTIVE_FROM<=SYSTIMESTAMP
                   AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO>SYSTIMESTAMP)
                """ + (lock ? " FOR UPDATE" : ""));
        if (rows.size()!=1) return null;
        Map<String,Object> r=rows.getFirst();
        return new Release(str(r.get("POLICY_REVISION")),str(r.get("BUSINESS_DECISION_REF")),
                str(r.get("DATA_DECISION_REF")),str(r.get("COVERAGE_DECISION_REF")),
                str(r.get("ALLOCATION_DECISION_REF")),str(r.get("INTEGRATION_DECISION_REF")),
                str(r.get("DOWNSTREAM_BINDING_REF")),str(r.get("APPROVED_BY")),true);
    }
    public Activation activation(Authentication authentication) {
        require(authentication,"SCOPE_clt.collateral.read");
        introduction.actor(authentication);
        boolean schema=cancellationSchema();
        boolean approved=false;
        if (schema) {
            try { approved=release(false)!=null; }
            catch (RuntimeException ignored) { approved=false; }
        }
        boolean ready=finalEnabled && schema && approved && has(authentication,"SCOPE_clt.collateral.cancel");
        return new Activation(stageEnabled,finalEnabled,schema,approved,ready,
                ready ? "R3_CANCEL_APPROVED_PER_REQUEST_EVIDENCE_STILL_REQUIRED" : "R3_CANCEL_RELEASE_NOT_READY",
                List.of("G-04_REPLACEMENT_AND_COVERAGE", "G-06_APPROVED_CANCELLATION_RULES",
                        "G-09_R4_DOWNSTREAM_BINDING_AND_EXTERNAL_INTEGRATION",
                        "D-07_APPROVED_EVENT_LEDGER_AND_IMPACT_MIGRATION"));
    }
    private Map<String,Object> requestRow(BigDecimal id,String branch,boolean lock) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT * FROM CLT.R3_INS_CANCEL_REQUEST
                 WHERE CANCEL_REQUEST_ID=? AND BRANCH_CODE=?
                """+(lock ? " FOR UPDATE" : ""),id,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_CANCEL_REQUEST_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    private StageRow stage(Map<String,Object> r) {
        return new StageRow(number(r.get("CANCEL_REQUEST_ID")),number(r.get("COLLATERAL_ID")),
                number(r.get("INSURANCE_ID")),str(r.get("STATUS_CODE")),
                str(r.get("MAKER_SUBJECT")),str(r.get("REVIEWER_SUBJECT")),
                number(r.get("RECORD_VERSION")),instant(r.get("CREATED_AT")),"ORACLE_CANCEL_STAGE_NOT_DOMAIN");
    }
    private void audit(BigDecimal id,String action,String actor,String reason,BigDecimal before,BigDecimal after) {
        jdbc.update("""
                INSERT INTO CLT.R3_INS_CANCEL_AUDIT (AUDIT_ID,CANCEL_REQUEST_ID,ACTION_CODE,
                  ACTOR_SUBJECT,REASON,VERSION_BEFORE,VERSION_AFTER,EVENT_AT)
                VALUES (CLT.SEQ_R3_INS_CANCEL_AUD.NEXTVAL,?,?,?,?,?,?,SYSTIMESTAMP)
                """,id,action,actor,reason,before,after);
    }
    public StageRow create(String collateralId,String insuranceId,NewRequest body,
            String key,Authentication authentication) {
        require(authentication,"SCOPE_clt.collateral.introduce");
        var who=introduction.actor(authentication);
        if (body==null) throw new IllegalArgumentException("CLT_R3_CANCEL_REQUEST_BODY_REQUIRED");
        String version=CollateralR3InsuranceCancellationPolicy.version(body.insuranceVersion());
        String reason=CollateralR3InsuranceCancellationPolicy.reason(body.reason());
        String idem=CollateralR3InsuranceCancellationPolicy.key(key);
        String collateral=CollateralR3InsuranceCancellationPolicy.id(collateralId);
        String insurance=CollateralR3InsuranceCancellationPolicy.id(insuranceId);
        stageGate();
        return transaction.execute(tx -> {
            // Parent lock serializes same-policy competing maker submissions.
            Map<String,Object> p=policy(new BigDecimal(collateral),new BigDecimal(insurance),who.branch(),true);
            if (!Objects.equals(number(p.get("RECORD_VERSION")),version))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_CANCEL_POLICY_VERSION_CHANGED");
            String hash=digest(collateral,insurance,version,reason,who.branch());
            List<Map<String,Object>> prior=jdbc.queryForList("""
                    SELECT * FROM CLT.R3_INS_CANCEL_REQUEST
                     WHERE BRANCH_CODE=? AND IDEMPOTENCY_KEY=?
                    """,who.branch(),idem);
            if (!prior.isEmpty()) {
                Map<String,Object> r=prior.getFirst();
                if (prior.size()!=1 || !Objects.equals(number(r.get("COLLATERAL_ID")),collateral)
                        || !Objects.equals(number(r.get("INSURANCE_ID")),insurance)
                        || !Objects.equals(str(r.get("REQUEST_HASH")),hash)
                        || !Objects.equals(str(r.get("MAKER_SUBJECT")),who.subject()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_CANCEL_REPLAY_KEY_CONFLICT");
                return stage(r);
            }
            Readiness current=assess(collateral,insurance,who.branch(),false);
            if (!current.assessment().requestCandidate())
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_CANCEL_EXISTING_EVENT_OR_RENEWAL_BLOCKS_REQUEST");
            Long other=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.R3_INS_CANCEL_REQUEST
                     WHERE INSURANCE_ID=? AND STATUS_CODE IN ('DRAFT','SUBMITTED','REVIEWED')
                    """,Long.class,new BigDecimal(insurance));
            if (other==null || other>0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_CANCEL_ANOTHER_PENDING_REQUEST_EXISTS");
            BigDecimal id=jdbc.queryForObject("SELECT CLT.SEQ_R3_INS_CANCEL_REQ.NEXTVAL FROM DUAL",
                    BigDecimal.class);
            if (id==null) throw new IllegalStateException("CLT_R3_CANCEL_SEQUENCE_UNAVAILABLE");
            jdbc.update("""
                    INSERT INTO CLT.R3_INS_CANCEL_REQUEST
                     (CANCEL_REQUEST_ID,COLLATERAL_ID,INSURANCE_ID,BRANCH_CODE,STATUS_CODE,
                      MAKER_SUBJECT,REQUEST_REASON,REQUEST_HASH,IDEMPOTENCY_KEY,
                      EXPECTED_INSURANCE_VERSION,RECORD_VERSION,CREATED_AT)
                    VALUES (?,?,?,?,'DRAFT',?,?,?,?,?,1,SYSTIMESTAMP)
                    """,id,new BigDecimal(collateral),new BigDecimal(insurance),who.branch(),
                    who.subject(),reason,hash,idem,new BigDecimal(version));
            audit(id,"CREATED",who.subject(),reason,BigDecimal.ZERO,BigDecimal.ONE);
            return stage(requestRow(id,who.branch(),false));
        });
    }
    public StageRow transition(String requestId,Transition body,Authentication authentication) {
        var who=introduction.actor(authentication);
        if (body==null) throw new IllegalArgumentException("CLT_R3_CANCEL_TRANSITION_BODY_REQUIRED");
        String action=body.action();
        String reason=CollateralR3InsuranceCancellationPolicy.reason(body.reason());
        BigDecimal expected=new BigDecimal(CollateralR3InsuranceCancellationPolicy.version(body.expectedVersion()));
        BigDecimal id=new BigDecimal(CollateralR3InsuranceCancellationPolicy.id(requestId));
        boolean reviewer="REVIEW".equals(action) || "REJECT".equals(action);
        require(authentication,reviewer ? "SCOPE_clt.collateral.review" : "SCOPE_clt.collateral.introduce");
        stageGate();
        return transaction.execute(tx -> {
            Map<String,Object> r=requestRow(id,who.branch(),true);
            BigDecimal v=dec(r.get("RECORD_VERSION"));
            if (v.compareTo(expected)!=0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_CANCEL_STAGE_VERSION_CONFLICT");
            CollateralR3InsuranceCancellationPolicy.requireStageTransition(
                    str(r.get("STATUS_CODE")),action,str(r.get("MAKER_SUBJECT")),
                    str(r.get("REVIEWER_SUBJECT")),who.subject(),true);
            String status=switch(action) {
                case "SUBMIT" -> "SUBMITTED"; case "WITHDRAW" -> "WITHDRAWN";
                case "REVIEW" -> "REVIEWED"; case "REJECT" -> "REJECTED";
                default -> throw new IllegalArgumentException("CLT_R3_CANCEL_ACTION_INVALID");
            };
            if ("REVIEW".equals(action) || "SUBMIT".equals(action)) {
                String collateral=number(r.get("COLLATERAL_ID"));
                String insurance=number(r.get("INSURANCE_ID"));
                // Do not approve Stage based on a stale or replaced policy.
                Readiness latest=assess(collateral,insurance,who.branch(),false);
                if (!latest.assessment().requestCandidate()
                        || !Objects.equals(latest.policyRecordVersion(),number(r.get("EXPECTED_INSURANCE_VERSION")))
                        || !Objects.equals(digest(collateral,insurance,latest.policyRecordVersion(),
                                 str(r.get("REQUEST_REASON")),who.branch()),str(r.get("REQUEST_HASH"))))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_CANCEL_REVALIDATION_FAILED");
            }
            BigDecimal next=v.add(BigDecimal.ONE);
            int updated=jdbc.update("""
                    UPDATE CLT.R3_INS_CANCEL_REQUEST SET STATUS_CODE=?,REVIEWER_SUBJECT=?,
                           REVIEW_REASON=?,RECORD_VERSION=?,UPDATED_AT=SYSTIMESTAMP
                     WHERE CANCEL_REQUEST_ID=? AND BRANCH_CODE=? AND RECORD_VERSION=?
                    """,status,reviewer?who.subject():str(r.get("REVIEWER_SUBJECT")),
                    reason,next,id,who.branch(),v);
            if (updated!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_CANCEL_STAGE_COMPARE_AND_SET_FAILED");
            audit(id,action,who.subject(),reason,v,next);
            return stage(requestRow(id,who.branch(),false));
        });
    }
    private static int page(int value,int size) {
        if (value<0 || value>10000 || size<1 || size>100)
            throw new IllegalArgumentException("CLT_R3_CANCEL_PAGE_INVALID");
        return value;
    }
    public StagePage mine(int page,int size,Authentication authentication) {
        require(authentication,"SCOPE_clt.collateral.read");
        var who=introduction.actor(authentication);
        page(page,size); stageGate();
        Long total=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.R3_INS_CANCEL_REQUEST
                 WHERE BRANCH_CODE=? AND MAKER_SUBJECT=?
                """,Long.class,who.branch(),who.subject());
        List<StageRow> items=jdbc.queryForList("""
                SELECT * FROM CLT.R3_INS_CANCEL_REQUEST
                 WHERE BRANCH_CODE=? AND MAKER_SUBJECT=?
                 ORDER BY CANCEL_REQUEST_ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """,who.branch(),who.subject(),(long)page*size,size).stream().map(this::stage).toList();
        return new StagePage(total==null?0:total,page,size,items);
    }
    public StagePage pending(int page,int size,Authentication authentication) {
        require(authentication,"SCOPE_clt.collateral.review");
        var who=introduction.actor(authentication);
        page(page,size); stageGate();
        Long total=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.R3_INS_CANCEL_REQUEST
                 WHERE BRANCH_CODE=? AND STATUS_CODE='SUBMITTED' AND MAKER_SUBJECT<>?
                """,Long.class,who.branch(),who.subject());
        List<StageRow> items=jdbc.queryForList("""
                SELECT * FROM CLT.R3_INS_CANCEL_REQUEST
                 WHERE BRANCH_CODE=? AND STATUS_CODE='SUBMITTED' AND MAKER_SUBJECT<>?
                 ORDER BY CANCEL_REQUEST_ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """,who.branch(),who.subject(),(long)page*size,size).stream().map(this::stage).toList();
        return new StagePage(total==null?0:total,page,size,items);
    }
    public Receipt receipt(String requestId,Authentication authentication) {
        require(authentication,"SCOPE_clt.collateral.read");
        var who=introduction.actor(authentication);
        String id=CollateralR3InsuranceCancellationPolicy.id(requestId);
        if (!eventAvailable()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R3_CANCEL_EVENT_LEDGER_NOT_INSTALLED");
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT * FROM CLT.R3_INS_CANCEL_EVENT
                 WHERE CANCEL_REQUEST_ID=? AND BRANCH_CODE=?
                """,new BigDecimal(id),who.branch());
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_CANCEL_RECEIPT_NOT_FOUND_OR_FORBIDDEN");
        return receipt(rows.getFirst(),false);
    }
    private Receipt receipt(Map<String,Object> r,boolean replay) {
        return new Receipt(number(r.get("CANCEL_REQUEST_ID")),number(r.get("COLLATERAL_ID")),
                number(r.get("INSURANCE_ID")),number(r.get("CANCEL_EVENT_ID")),
                "CANCELLATION_EVENT_RECORDED",str(r.get("POLICY_REVISION")),
                instant(r.get("POSTED_AT")),replay,
                "R4_COVERAGE_AND_ALLOCATION_RECONCILIATION_SEPARATELY_REQUIRED");
    }
    private Impact impact(BigDecimal id,boolean lock) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT * FROM CLT.R3_INS_CANCEL_IMPACT
                 WHERE CANCEL_REQUEST_ID=? AND IMPACT_STATUS='APPROVED'
                   AND APPROVED_AT<=SYSTIMESTAMP
                """+(lock ? " FOR UPDATE" : ""),id);
        if (rows.size()!=1) return null;
        Map<String,Object> r=rows.getFirst();
        return new Impact(str(r.get("DECISION_REF")),str(r.get("COVERAGE_DECISION_REF")),
                str(r.get("ALLOCATION_DECISION_REF")),str(r.get("APPROVED_BY")),
                number(r.get("REPLACEMENT_INSURANCE_ID")),true);
    }
    private boolean replacementValid(String collateralId,String insuranceId,String otherId,
            String insuranceType,String currency,String asOf,String branch) {
        if (otherId==null || Objects.equals(insuranceId,otherId)) return false;
        Long count=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE p
                 WHERE p.COLLATERAL_INSURANCE_ID=? AND p.COLLATERAL_ID=? AND p.BRANCH_CODE=?
                   AND p.COLLATERAL_INSURANCE_TYPE_ID=? AND p.CURRENCY_CODE=?
                   AND p.VALID_FROM<=? AND p.VALID_TO>=?
                """,Long.class,new BigDecimal(otherId),new BigDecimal(collateralId),branch,
                new BigDecimal(insuranceType),currency,Date.valueOf(asOf),Date.valueOf(asOf));
        if (count==null || count!=1L) return false;
        Long cancelled=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.R3_INS_CANCEL_EVENT WHERE INSURANCE_ID=?
                """,Long.class,new BigDecimal(otherId));
        return cancelled!=null && cancelled==0L;
    }
    public Receipt commit(String requestId,Command command,Authentication authentication) {
        require(authentication,"SCOPE_clt.collateral.cancel");
        var who=introduction.actor(authentication);
        String rid=CollateralR3InsuranceCancellationPolicy.id(requestId);
        if (command==null) throw new IllegalArgumentException("CLT_R3_CANCEL_COMMAND_REQUIRED");
        CollateralR3InsuranceCancellationPolicy.version(command.expectedVersion());
        CollateralR3InsuranceCancellationPolicy.key(command.idempotencyKey());
        CollateralR3InsuranceCancellationPolicy.reason(command.reason());
        if (!finalEnabled || !stageEnabled || !cancellationSchema())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R3_CANCEL_BANK_MIGRATION_OR_RELEASE_REQUIRED");
        return transaction.execute(tx -> {
            BigDecimal requestIdNumber=new BigDecimal(rid);
            // Look up branch-scoped request first, then lock parent collateral -> request -> policy.
            Map<String,Object> initial=requestRow(requestIdNumber,who.branch(),false);
            String collateralId=number(initial.get("COLLATERAL_ID"));
            String insuranceId=number(initial.get("INSURANCE_ID"));
            jdbc.queryForList("SELECT COLLATERAL_ID FROM CLT.COLLATERAL WHERE COLLATERAL_ID=? "
                    +"AND BRANCH_CODE=? FOR UPDATE",new BigDecimal(collateralId),who.branch());
            Map<String,Object> r=requestRow(requestIdNumber,who.branch(),true);
            if (!Objects.equals(number(r.get("COLLATERAL_ID")),collateralId)
                    || !Objects.equals(number(r.get("INSURANCE_ID")),insuranceId))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_CANCEL_REQUEST_TARGET_CHANGED");
            List<Map<String,Object>> done=jdbc.queryForList("""
                    SELECT * FROM CLT.R3_INS_CANCEL_EVENT
                     WHERE CANCEL_REQUEST_ID=? OR (BRANCH_CODE=? AND IDEMPOTENCY_KEY=?)
                    """,requestIdNumber,who.branch(),command.idempotencyKey());
            String hash=str(r.get("REQUEST_HASH"));
            if (!done.isEmpty()) {
                if (done.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_CANCEL_REPLAY_MULTIPLE_EVENTS");
                Map<String,Object> old=done.getFirst();
                var snapshot=new Stage(str(r.get("STATUS_CODE")),str(r.get("MAKER_SUBJECT")),
                        str(r.get("REVIEWER_SUBJECT")),who.subject(),hash,hash);
                Ledger ledger=new Ledger(number(old.get("CANCEL_REQUEST_ID")),
                        number(old.get("INSURANCE_ID")),number(old.get("COLLATERAL_ID")),
                        number(old.get("CANCEL_EVENT_ID")),str(old.get("IDEMPOTENCY_KEY")),
                        str(old.get("REQUEST_HASH")),str(old.get("REASON")),
                        number(old.get("REQUEST_VERSION")),str(old.get("POSTED_BY")),
                        str(old.get("POLICY_REVISION")));
                CollateralR3InsuranceCancellationPolicy.requireReplay(rid,command,snapshot,ledger,
                        hash,str(old.get("POLICY_REVISION")));
                return receipt(old,true);
            }
            BigDecimal v=dec(r.get("RECORD_VERSION"));
            if (!Objects.equals(v.toPlainString(),command.expectedVersion()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_CANCEL_VERSION_CHANGED");
            Map<String,Object> insured=policy(new BigDecimal(collateralId),new BigDecimal(insuranceId),who.branch(),true);
            if (!Objects.equals(number(insured.get("RECORD_VERSION")),number(r.get("EXPECTED_INSURANCE_VERSION"))))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_CANCEL_INSURANCE_VERSION_CHANGED");
            Release rel=release(true);
            Impact impact=impact(requestIdNumber,true);
            Readiness current=assess(collateralId,insuranceId,who.branch(),false);
            String originalHash=digest(collateralId,insuranceId,
                    number(r.get("EXPECTED_INSURANCE_VERSION")),str(r.get("REQUEST_REASON")),who.branch());
            boolean replace=impact!=null && replacementValid(collateralId,insuranceId,
                    impact.replacementInsuranceId(),number(insured.get("COLLATERAL_INSURANCE_TYPE_ID")),
                    str(insured.get("CURRENCY_CODE")),current.asOf(),who.branch());
            var stage=new Stage(str(r.get("STATUS_CODE")),str(r.get("MAKER_SUBJECT")),
                    str(r.get("REVIEWER_SUBJECT")),who.subject(),hash,originalHash);
            try {
                CollateralR3InsuranceCancellationPolicy.requirePosting(command,stage,rel,impact,
                        current.assessment(),finalEnabled,cancellationSchema(),true,replace,
                        current.allocationRows()==0);
            } catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN,ex.getMessage()); }
              catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage()); }
            Long sameKey=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.R3_INS_CANCEL_EVENT
                     WHERE BRANCH_CODE=? AND IDEMPOTENCY_KEY=?
                    """,Long.class,who.branch(),command.idempotencyKey());
            if (sameKey==null || sameKey!=0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_CANCEL_IDEMPOTENCY_COLLISION");
            BigDecimal eventId=jdbc.queryForObject(
                    "SELECT CLT.SEQ_R3_INS_CANCEL_EVT.NEXTVAL FROM DUAL",BigDecimal.class);
            if (eventId==null) throw new IllegalStateException("CLT_R3_CANCEL_EVENT_SEQUENCE_UNAVAILABLE");
            jdbc.update("""
                    INSERT INTO CLT.R3_INS_CANCEL_EVENT
                     (CANCEL_EVENT_ID,CANCEL_REQUEST_ID,COLLATERAL_ID,INSURANCE_ID,BRANCH_CODE,
                      REQUEST_HASH,IDEMPOTENCY_KEY,REASON,REQUEST_VERSION,REVIEWED_BY,POSTED_BY,
                      POLICY_REVISION,IMPACT_DECISION_REF,REPLACEMENT_INSURANCE_ID,POSTED_AT)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,SYSTIMESTAMP)
                    """,eventId,requestIdNumber,new BigDecimal(collateralId),new BigDecimal(insuranceId),
                    who.branch(),hash,command.idempotencyKey(),command.reason().trim(),v,
                    str(r.get("REVIEWER_SUBJECT")),who.subject(),rel.policyRevision(),
                    impact.decisionRef(),new BigDecimal(impact.replacementInsuranceId()));
            int changed=jdbc.update("""
                    UPDATE CLT.R3_INS_CANCEL_REQUEST SET STATUS_CODE='COMMITTED',
                        RECORD_VERSION=RECORD_VERSION+1,UPDATED_AT=SYSTIMESTAMP
                     WHERE CANCEL_REQUEST_ID=? AND BRANCH_CODE=? AND STATUS_CODE='REVIEWED'
                       AND RECORD_VERSION=?
                    """,requestIdNumber,who.branch(),v);
            if (changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_CANCEL_STAGE_COMMIT_COMPARE_AND_SET_FAILED");
            audit(requestIdNumber,"COMMITTED",who.subject(),command.reason(),v,v.add(BigDecimal.ONE));
            return receipt(jdbc.queryForMap("""
                    SELECT * FROM CLT.R3_INS_CANCEL_EVENT WHERE CANCEL_EVENT_ID=?
                    """,eventId),false);
        });
    }
}
