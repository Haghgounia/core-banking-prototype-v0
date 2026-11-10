package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralR3ValuationCommitPolicy.*;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * R3 valuation finalization candidate. A single shared DataSource and transaction:
 * guarded Stage -> append FINAL history -> prior FINAL becomes SUPERSEDED -> pointer CAS
 * -> immutable COMMITTED ledger receipt. Never recalculate historic allocations.
 * Feature OFF until bank G-07/G-18/G-09/G-10 decisions and DBA migration/Oracle UAT.
 */
@Service
@ConditionalOnProperty(prefix="core-banking.collateral.prototype", name="enabled", havingValue="true")
public final class CollateralR3ValuationCommitService {
    public record Activation(boolean featureFlag, boolean schemaReady, boolean bankReleasePresent,
            boolean postingEnabled, String stage, List<String> unresolvedPolicyGaps) {}
    public record PostedEvidence(String requestId, String collateralId, String valuationId,
            String commitStatus, String policyRevision, String previousValuationId,
            String recordedEffectiveTo, String postedAt, boolean currentAtRead,
            String allocationEligibility) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final CollateralIntroductionService introduction;
    private final CollateralR3PostingReadinessService preflight;
    private final CollateralR3WorkflowService workflow;
    @Value("${core-banking.collateral.r3-posting.valuation-enabled:false}")
    private boolean enabled;

    public CollateralR3ValuationCommitService(DataSource sharedDataSource,
            CollateralIntroductionService introduction, CollateralR3PostingReadinessService preflight,
            CollateralR3WorkflowService workflow) {
        jdbc = new JdbcTemplate(sharedDataSource);
        jdbc.setQueryTimeout(12);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(sharedDataSource));
        transaction.setTimeout(25);
        this.introduction = introduction;
        this.preflight = preflight;
        this.workflow = workflow;
    }
    private static boolean has(Authentication auth, String scope) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(x -> scope.equals(x.getAuthority()));
    }
    private static void require(Authentication auth, String scope) {
        if (!has(auth, scope))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "CLT_R3_VAL_COMMIT_AUTHORITY_REQUIRED");
    }
    private static String text(Object v) { return v == null ? null : v.toString().trim(); }
    private static String number(Object v) {
        return v == null ? null : v instanceof BigDecimal n
                ? n.stripTrailingZeros().toPlainString() : text(v);
    }
    private static String day(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Date d) return d.toLocalDate().toString();
        if (raw instanceof Timestamp t) return t.toLocalDateTime().toLocalDate().toString();
        if (raw instanceof LocalDate d) return d.toString();
        if (raw instanceof LocalDateTime d) return d.toLocalDate().toString();
        throw new IllegalStateException("CLT_R3_VAL_COMMIT_ORACLE_DATE_INVALID");
    }
    private boolean unique(String table, String name, String... columns) {
        String query = """
                SELECT COUNT(*) FROM (
                  SELECT c.CONSTRAINT_NAME FROM ALL_CONSTRAINTS c
                    JOIN ALL_CONS_COLUMNS cc ON cc.OWNER=c.OWNER
                      AND cc.TABLE_NAME=c.TABLE_NAME AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
                  WHERE c.OWNER='CLT' AND c.TABLE_NAME=? AND c.CONSTRAINT_NAME=?
                    AND c.CONSTRAINT_TYPE='U' AND c.STATUS='ENABLED' AND c.VALIDATED='VALIDATED'
                  GROUP BY c.CONSTRAINT_NAME
                  HAVING COUNT(*)=? AND SUM(CASE WHEN cc.COLUMN_NAME IN (%s) THEN 1 ELSE 0 END)=?
                )
                """.formatted(String.join(",",java.util.Collections.nCopies(columns.length,"?")));
        Object[] args = new Object[columns.length + 4];
        args[0]=table;args[1]=name;args[2]=columns.length;
        System.arraycopy(columns,0,args,3,columns.length);
        args[args.length-1]=columns.length;
        Long count=jdbc.queryForObject(query,Long.class,args);
        return count != null && count == 1;
    }
    /** Fail closed when any expected approved DB object/constraint is absent. */
    private boolean schemaReady() {
        try {
            Long tables = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CLT' AND TABLE_NAME IN
                    ('R3_VAL_POSTING_RELEASE','R3_VAL_POSTING_TYPE_RULE',
                     'R3_VAL_POSTING_VALUER_RULE','R3_VAL_POSTING_LEDGER')
                    """,Long.class);
            Long columns = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT' AND (
                     (TABLE_NAME='R3_VAL_POSTING_RELEASE' AND COLUMN_NAME IN
                      ('RELEASE_ID','RELEASE_STATUS','POLICY_REVISION','FINAL_STATUS_DECISION_REF',
                       'APPRAISABILITY_DECISION_REF','INTERVAL_DECISION_REF','BUSINESS_DATE_DECISION_REF',
                       'VALUER_AUTHORITY_DECISION_REF','VALUE_DECISION_REF','SUPERSESSION_DECISION_REF',
                       'APPROVED_BY','EFFECTIVE_FROM','EFFECTIVE_TO')) OR
                     (TABLE_NAME='R3_VAL_POSTING_TYPE_RULE' AND COLUMN_NAME IN
                      ('RULE_ID','COLLATERAL_TYPE_ID','APPRAISABLE_FLAG','INTERVAL_VALUE',
                       'INTERVAL_UNIT','BOUNDARY_MODE','ANCHOR_CODE','POLICY_REVISION',
                       'RULE_STATUS','APPROVED_BY','EFFECTIVE_FROM','EFFECTIVE_TO')) OR
                     (TABLE_NAME='R3_VAL_POSTING_VALUER_RULE' AND COLUMN_NAME IN
                      ('RULE_ID','VALUER_PARTY_ID','COLLATERAL_TYPE_ID','VALUATION_METHOD_CODE',
                       'POLICY_REVISION','RULE_STATUS','APPROVED_BY','EFFECTIVE_FROM','EFFECTIVE_TO')) OR
                     (TABLE_NAME='R3_VAL_POSTING_LEDGER' AND COLUMN_NAME IN
                      ('LEDGER_ID','REQUEST_ID','COLLATERAL_ID','BRANCH_CODE','IDEMPOTENCY_KEY',
                       'REQUEST_HASH','EXPECTED_VERSION','JUSTIFICATION','COMMIT_STATUS',
                       'DOMAIN_RECORD_ID','PREVIOUS_VALUATION_ID','TYPE_RULE_ID','VALUER_RULE_ID',
                       'POSTED_BY','POLICY_REVISION','RECORDED_EFFECTIVE_TO','POSTED_AT','CREATED_AT'))
                    )
                    """,Long.class);
            Long checks = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT'
                     AND STATUS='ENABLED' AND VALIDATED='VALIDATED' AND (
                      (TABLE_NAME='R3_VAL_POSTING_RELEASE' AND CONSTRAINT_NAME IN
                       ('CK_CLT_R3_VAL_REL_ONE','CK_CLT_R3_VAL_REL_STATUS','CK_CLT_R3_VAL_REL_DATES')) OR
                      (TABLE_NAME='R3_VAL_POSTING_TYPE_RULE' AND CONSTRAINT_NAME IN
                       ('CK_CLT_R3_VAL_TYPE_APP','CK_CLT_R3_VAL_TYPE_PERIOD','CK_CLT_R3_VAL_TYPE_STATUS')) OR
                      (TABLE_NAME='R3_VAL_POSTING_VALUER_RULE' AND CONSTRAINT_NAME='CK_CLT_R3_VAL_VALUER_STATUS') OR
                      (TABLE_NAME='R3_VAL_POSTING_LEDGER' AND CONSTRAINT_NAME IN
                       ('CK_CLT_R3_VAL_POST_STATUS','CK_CLT_R3_VAL_POST_COMPLETE')))
                    """,Long.class);
            Long sequences=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT'
                     AND SEQUENCE_NAME='SEQ_CLT_R3_VAL_POST_LEDGER'
                    """,Long.class);
            Long identity=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TAB_IDENTITY_COLS WHERE OWNER='CLT'
                     AND TABLE_NAME='COLLATERAL_VALUATION_HISTORY'
                     AND COLUMN_NAME='COLLATERAL_VALUATION_HISTORY_ID'
                    """,Long.class);
            Long fk=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT'
                      AND TABLE_NAME='COLLATERAL' AND CONSTRAINT_NAME='FK_COLL_CURRENT_VALUATION'
                      AND CONSTRAINT_TYPE='R' AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                    """,Long.class);
            return tables!=null && tables==4 && columns!=null && columns==52
                    && checks!=null && checks==9 && sequences!=null && sequences==1
                    && identity!=null && identity==1 && fk!=null && fk==1
                    && unique("R3_VAL_POSTING_TYPE_RULE","UQ_CLT_R3_VAL_TYPE_REV",
                              "COLLATERAL_TYPE_ID","POLICY_REVISION")
                    && unique("R3_VAL_POSTING_VALUER_RULE","UQ_CLT_R3_VAL_VALUER_REV",
                              "VALUER_PARTY_ID","COLLATERAL_TYPE_ID","VALUATION_METHOD_CODE","POLICY_REVISION")
                    && unique("R3_VAL_POSTING_LEDGER","UQ_CLT_R3_VAL_POST_REQ","REQUEST_ID")
                    && unique("R3_VAL_POSTING_LEDGER","UQ_CLT_R3_VAL_POST_IDEMP",
                              "BRANCH_CODE","IDEMPOTENCY_KEY")
                    && unique("COLLATERAL_VALUATION_HISTORY","UQ_COL_VAL_HIST_SEQUENCE",
                              "COLLATERAL_ID","VALUATION_SEQUENCE_NO")
                    && unique("COLLATERAL_VALUATION_HISTORY","UQ_COL_VAL_HIST_SUPERSEDES",
                              "SUPERSEDES_VALUATION_HISTORY_ID");
        } catch (RuntimeException ex) { return false; }
    }
    private Release release(boolean lock) {
        String sql="""
                SELECT POLICY_REVISION,FINAL_STATUS_DECISION_REF,APPRAISABILITY_DECISION_REF,
                       INTERVAL_DECISION_REF,BUSINESS_DATE_DECISION_REF,VALUER_AUTHORITY_DECISION_REF,
                       VALUE_DECISION_REF,SUPERSESSION_DECISION_REF,APPROVED_BY
                  FROM CLT.R3_VAL_POSTING_RELEASE
                 WHERE RELEASE_ID=1 AND RELEASE_STATUS='APPROVED'
                   AND EFFECTIVE_FROM<=SYSTIMESTAMP
                   AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO>SYSTIMESTAMP)
                """ + (lock ? " FOR UPDATE" : "");
        List<Map<String,Object>> rows=jdbc.queryForList(sql);
        if (rows.size()!=1) return null;
        Map<String,Object> r=rows.getFirst();
        return new Release(text(r.get("POLICY_REVISION")),text(r.get("FINAL_STATUS_DECISION_REF")),
                text(r.get("APPRAISABILITY_DECISION_REF")),text(r.get("INTERVAL_DECISION_REF")),
                text(r.get("BUSINESS_DATE_DECISION_REF")),text(r.get("VALUER_AUTHORITY_DECISION_REF")),
                text(r.get("VALUE_DECISION_REF")),text(r.get("SUPERSESSION_DECISION_REF")),
                text(r.get("APPROVED_BY")),true);
    }
    private static boolean validRelease(Release release) {
        try {
            CollateralR3ValuationCommitPolicy.guard(new Command("3","readiness.20261109","Activation check"),
                    new StageEvidence("REVIEWED","3",true,true,false,"maker","operator"),
                    release,true,true,true);
            return true;
        } catch (RuntimeException ex) { return false; }
    }
    public Activation activation(Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read"); introduction.actor(auth);
        boolean schema=schemaReady();boolean approved=false;
        if (schema) try { approved=validRelease(release(false)); }
        catch (DataAccessException ignored) { approved=false; }
        return new Activation(enabled,schema,approved,enabled && schema && approved,
                enabled && schema && approved ? "VALUATION_FINAL_CANDIDATE_PER_REQUEST_RULES_REQUIRED"
                        : "VALUATION_POSTING_RELEASE_OR_SCHEMA_MISSING",
                CollateralR3ValuationCommitPolicy.unresolvedAfterRegistration());
    }
    public PostedEvidence receipt(String requestId,Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        var actor=introduction.actor(auth);
        BigDecimal id=new BigDecimal(CollateralR3WorkflowPolicy.id(requestId));
        List<Map<String,Object>> rows;
        try { rows=jdbc.queryForList("""
                SELECT l.REQUEST_ID,l.COLLATERAL_ID,l.DOMAIN_RECORD_ID,l.COMMIT_STATUS,
                       l.POLICY_REVISION,l.PREVIOUS_VALUATION_ID,l.RECORDED_EFFECTIVE_TO,
                       l.POSTED_AT,c.CURRENT_VALUATION_HISTORY_ID
                  FROM CLT.R3_VAL_POSTING_LEDGER l
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID=l.COLLATERAL_ID
                     AND c.BRANCH_CODE=l.BRANCH_CODE
                 WHERE l.REQUEST_ID=? AND l.BRANCH_CODE=? AND l.COMMIT_STATUS='COMMITTED'
                """,id,actor.branch()); }
        catch (DataAccessException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R3_VAL_COMMIT_LEDGER_UNAVAILABLE"); }
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_VAL_COMMIT_RECEIPT_NOT_FOUND");
        Map<String,Object> r=rows.getFirst();
        return new PostedEvidence(number(r.get("REQUEST_ID")),number(r.get("COLLATERAL_ID")),
                number(r.get("DOMAIN_RECORD_ID")),text(r.get("COMMIT_STATUS")),
                text(r.get("POLICY_REVISION")),number(r.get("PREVIOUS_VALUATION_ID")),
                day(r.get("RECORDED_EFFECTIVE_TO")),text(r.get("POSTED_AT")),
                Objects.equals(number(r.get("DOMAIN_RECORD_ID")),number(r.get("CURRENT_VALUATION_HISTORY_ID"))),
                "NOT_DETERMINED_BY_R3_FINAL_RECEIPT");
    }
    private Map<String,Object> stage(BigDecimal id,String branch) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT * FROM CLT.COLLATERAL_VALUATION_REQUEST
                 WHERE VALUATION_REQUEST_ID=? AND BRANCH_CODE=? FOR UPDATE
                """,id,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_VAL_COMMIT_REQUEST_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    private Map<String,Object> parent(BigDecimal id,String branch) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT c.COLLATERAL_ID,c.COLLATERAL_TYPE_ID,c.PARTY_ID,c.CURRENCY_CODE,
                       c.CURRENT_VALUATION_HISTORY_ID,c.RECORD_VERSION,c.COLLATERAL_STATUS_CODE,
                       t.STATUS AS TYPE_STATUS
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=c.COLLATERAL_TYPE_ID
                 WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=? FOR UPDATE OF c.RECORD_VERSION
                """,id,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_VAL_COMMIT_COLLATERAL_CHANGED_OR_MISSING");
        return rows.getFirst();
    }
    private record TypeRuleRow(TypeRule rule,BigDecimal id) {}
    private TypeRuleRow typeRule(BigDecimal collateralTypeId,String revision) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT RULE_ID,COLLATERAL_TYPE_ID,APPRAISABLE_FLAG,INTERVAL_VALUE,
                       INTERVAL_UNIT,BOUNDARY_MODE,ANCHOR_CODE,POLICY_REVISION,APPROVED_BY
                  FROM CLT.R3_VAL_POSTING_TYPE_RULE
                 WHERE COLLATERAL_TYPE_ID=? AND POLICY_REVISION=? AND RULE_STATUS='APPROVED'
                   AND EFFECTIVE_FROM<=SYSTIMESTAMP
                   AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO>SYSTIMESTAMP)
                 FOR UPDATE
                """,collateralTypeId,revision);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_VAL_COMMIT_APPROVED_TYPE_RULE_REQUIRED");
        Map<String,Object> r=rows.getFirst();
        BigDecimal v=CollateralR3ValuationCommitPolicy.positiveId(r.get("INTERVAL_VALUE"),
                "CLT_R3_VAL_COMMIT_INTERVAL_VALUE_INVALID");
        if (v.compareTo(BigDecimal.valueOf(36500))>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R3_VAL_COMMIT_INTERVAL_TOO_LARGE");
        return new TypeRuleRow(new TypeRule(number(r.get("COLLATERAL_TYPE_ID")),
                "YES".equals(text(r.get("APPRAISABLE_FLAG"))),v.intValueExact(),
                text(r.get("INTERVAL_UNIT")),text(r.get("BOUNDARY_MODE")),
                text(r.get("ANCHOR_CODE")),text(r.get("POLICY_REVISION")),
                text(r.get("APPROVED_BY")),true),
                CollateralR3ValuationCommitPolicy.positiveId(r.get("RULE_ID"),
                        "CLT_R3_VAL_COMMIT_TYPE_RULE_ID_INVALID"));
    }
    private record ValuerRuleRow(ValuerRule rule,BigDecimal id) {}
    private ValuerRuleRow valuerRule(CollateralValuationPolicy.Candidate candidate,
            BigDecimal collateralTypeId,String revision) {
        if (candidate.valuerPartyId()==null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_VAL_COMMIT_REGISTERED_VALUER_REQUIRED");
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT RULE_ID,VALUER_PARTY_ID,COLLATERAL_TYPE_ID,VALUATION_METHOD_CODE,
                       POLICY_REVISION,APPROVED_BY
                  FROM CLT.R3_VAL_POSTING_VALUER_RULE
                 WHERE VALUER_PARTY_ID=? AND COLLATERAL_TYPE_ID=?
                   AND VALUATION_METHOD_CODE=? AND POLICY_REVISION=? AND RULE_STATUS='APPROVED'
                   AND EFFECTIVE_FROM<=SYSTIMESTAMP
                   AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO>SYSTIMESTAMP)
                 FOR UPDATE
                """,new BigDecimal(candidate.valuerPartyId()),collateralTypeId,
                candidate.methodCode(),revision);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_VAL_COMMIT_APPROVED_VALUER_REQUIRED");
        Map<String,Object> r=rows.getFirst();
        return new ValuerRuleRow(new ValuerRule(number(r.get("VALUER_PARTY_ID")),
                number(r.get("COLLATERAL_TYPE_ID")),text(r.get("VALUATION_METHOD_CODE")),
                text(r.get("POLICY_REVISION")),text(r.get("APPROVED_BY")),true),
                CollateralR3ValuationCommitPolicy.positiveId(r.get("RULE_ID"),
                        "CLT_R3_VAL_COMMIT_VALUER_RULE_ID_INVALID"));
    }
    private static CollateralValuationPolicy.Candidate candidate(Map<String,Object> r) {
        return CollateralValuationPolicy.normalize(new CollateralValuationModels.CandidateRequest(
                text(r.get("VALUATION_TYPE_CODE")),text(r.get("VALUATION_METHOD_CODE")),
                day(r.get("VALUATION_DATE")),number(r.get("VALUATION_AMOUNT")),
                text(r.get("CURRENCY_CODE")),number(r.get("FORCED_SALE_VALUE")),
                number(r.get("ELIGIBLE_VALUE")),number(r.get("HAIRCUT_PERCENT")),
                number(r.get("VALUER_PARTY_ID")),text(r.get("VALUER_NAME")),
                text(r.get("VALUATION_REFERENCE")),day(r.get("EFFECTIVE_FROM")),
                day(r.get("EFFECTIVE_TO")),number(r.get("SUPERSEDES_VALUATION_HISTORY_ID")),
                text(r.get("JUSTIFICATION"))));
    }
    private void referenceStatuses() {
        Long valid=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_VALUATION_STATUS
                 WHERE VALUATION_STATUS_CODE IN ('FINAL','SUPERSEDED') AND STATUS='ACTIVE'
                """,Long.class);
        if (valid==null || valid!=2) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_VAL_COMMIT_FINAL_STATUSES_UNAVAILABLE");
    }
    private static String asText(Object raw) { return raw instanceof Number n ? n.toString() : text(raw); }
    private LocalDate oracleTehranDate() {
        Date d=jdbc.queryForObject("""
                SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE)) FROM DUAL
                """,Date.class);
        if (d==null) throw new IllegalStateException("CLT_R3_VAL_COMMIT_CLOCK_UNAVAILABLE");
        // This calendar date is NOT the bank's official business date until approved by release.
        return d.toLocalDate();
    }
    private void predecessor(CollateralValuationPolicy.Candidate candidate,
            BigDecimal collateralId, String actualCurrent,String currency) {
        if (actualCurrent==null) {
            Long orphan=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.COLLATERAL_VALUATION_HISTORY
                     WHERE COLLATERAL_ID=? AND VALUATION_STATUS_CODE='FINAL'
                    """,Long.class,collateralId);
            if (orphan==null || orphan!=0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_VAL_COMMIT_ORPHAN_FINAL_HISTORY");
            CollateralR3ValuationCommitPolicy.requireSupersession(null,
                    candidate.supersedesValuationId(),null,null,null,null,
                    collateralId.toPlainString(),currency,candidate.effectiveFrom(),0L);
            return;
        }
        BigDecimal oldId=new BigDecimal(actualCurrent);
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT COLLATERAL_ID,VALUATION_STATUS_CODE,CURRENCY_CODE,EFFECTIVE_FROM
                  FROM CLT.COLLATERAL_VALUATION_HISTORY
                 WHERE COLLATERAL_VALUATION_HISTORY_ID=? AND COLLATERAL_ID=? FOR UPDATE
                """,oldId,collateralId);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_VAL_COMMIT_PREDECESSOR_MISSING");
        Long successors=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_VALUATION_HISTORY
                 WHERE SUPERSEDES_VALUATION_HISTORY_ID=?
                """,Long.class,oldId);
        Map<String,Object> p=rows.getFirst();
        CollateralR3ValuationCommitPolicy.requireSupersession(actualCurrent,
                candidate.supersedesValuationId(),number(p.get("COLLATERAL_ID")),
                text(p.get("VALUATION_STATUS_CODE")),text(p.get("CURRENCY_CODE")),
                day(p.get("EFFECTIVE_FROM")),collateralId.toPlainString(),currency,
                candidate.effectiveFrom(),successors==null?-1:successors);
    }
    private Receipt replay(List<Map<String,Object>> rows,Command command,
            String stageHash,String poster) {
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_VAL_COMMIT_DUPLICATE_LEDGER");
        Map<String,Object> r=rows.getFirst();
        try {
            CollateralR3ValuationCommitPolicy.checkReplay(command,text(r.get("IDEMPOTENCY_KEY")),
                    stageHash,text(r.get("REQUEST_HASH")),text(r.get("JUSTIFICATION")),
                    number(r.get("EXPECTED_VERSION")),poster,text(r.get("POSTED_BY")));
            if (!"COMMITTED".equals(text(r.get("COMMIT_STATUS"))) || r.get("DOMAIN_RECORD_ID")==null)
                throw new IllegalStateException("CLT_R3_VAL_COMMIT_REPLAY_INCOMPLETE");
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());
        }
        return new Receipt(number(r.get("REQUEST_ID")),number(r.get("COLLATERAL_ID")),
                number(r.get("DOMAIN_RECORD_ID")),"COMMITTED",text(r.get("POLICY_REVISION")),
                command.idempotencyKey(),number(r.get("PREVIOUS_VALUATION_ID")),
                day(r.get("RECORDED_EFFECTIVE_TO")),true);
    }
    private BigDecimal insertHistory(CollateralValuationPolicy.Candidate c,BigDecimal collateralId,
            long sequence,String actor) {
        return jdbc.execute((ConnectionCallback<BigDecimal>)connection -> {
            try (CallableStatement stmt=connection.prepareCall("""
                    BEGIN INSERT INTO CLT.COLLATERAL_VALUATION_HISTORY (
                       COLLATERAL_ID,VALUATION_SEQUENCE_NO,VALUATION_TYPE_CODE,VALUATION_METHOD_CODE,
                       VALUATION_STATUS_CODE,VALUATION_DATE,VALUATION_AMOUNT,CURRENCY_CODE,
                       FORCED_SALE_VALUE,ELIGIBLE_VALUE,HAIRCUT_PERCENT,VALUER_PARTY_ID,
                       VALUER_NAME,VALUATION_REFERENCE,EFFECTIVE_FROM,EFFECTIVE_TO,REMARKS,
                       SUPERSEDES_VALUATION_HISTORY_ID,FINALIZED_AT,FINALIZED_BY,CREATED_BY,RECORD_VERSION)
                    VALUES (?,?,?,?,'FINAL',?,?,?,?,?,?,?,?,?,?,?,?,?,SYSTIMESTAMP,?,?,1)
                    RETURNING COLLATERAL_VALUATION_HISTORY_ID INTO ?; END;
                    """)) {
                stmt.setBigDecimal(1,collateralId);stmt.setLong(2,sequence);
                stmt.setString(3,c.typeCode());stmt.setString(4,c.methodCode());
                stmt.setDate(5,Date.valueOf(c.valuationDate()));
                stmt.setBigDecimal(6,new BigDecimal(c.valuationAmount()));stmt.setString(7,c.currencyCode());
                if (c.forcedSaleValue()==null) stmt.setNull(8,Types.NUMERIC);
                else stmt.setBigDecimal(8,new BigDecimal(c.forcedSaleValue()));
                if (c.eligibleValue()==null) stmt.setNull(9,Types.NUMERIC);
                else stmt.setBigDecimal(9,new BigDecimal(c.eligibleValue()));
                if (c.haircutPercent()==null) stmt.setNull(10,Types.NUMERIC);
                else stmt.setBigDecimal(10,new BigDecimal(c.haircutPercent()));
                if (c.valuerPartyId()==null) stmt.setNull(11,Types.NUMERIC);
                else stmt.setBigDecimal(11,new BigDecimal(c.valuerPartyId()));
                stmt.setString(12,c.valuerName());stmt.setString(13,c.valuationReference());
                stmt.setDate(14,Date.valueOf(c.effectiveFrom()));
                if (c.effectiveTo()==null) stmt.setNull(15,Types.DATE);
                else stmt.setDate(15,Date.valueOf(c.effectiveTo()));
                stmt.setString(16,c.justification());
                if (c.supersedesValuationId()==null) stmt.setNull(17,Types.NUMERIC);
                else stmt.setBigDecimal(17,new BigDecimal(c.supersedesValuationId()));
                stmt.setString(18,actor);stmt.setString(19,actor);
                stmt.registerOutParameter(20,Types.NUMERIC);stmt.execute();
                return stmt.getBigDecimal(20);
            }
        });
    }
    public Receipt commit(String requestId,Command command,Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        require(auth,"SCOPE_clt.collateral.review");
        var actor=introduction.actor(auth);
        if (command==null) throw new IllegalArgumentException("CLT_R3_VAL_COMMIT_BODY_REQUIRED");
        CollateralR3ValuationCommitPolicy.expectedVersion(command.expectedVersion());
        CollateralR3ValuationCommitPolicy.idempotencyKey(command.idempotencyKey());
        CollateralR3ValuationCommitPolicy.reason(command.reason());
        if (!enabled || !schemaReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R3_VAL_COMMIT_DBA_RELEASE_REQUIRED");
        BigDecimal requestKey=new BigDecimal(CollateralR3WorkflowPolicy.id(requestId));
        return transaction.execute(tx -> {
            Map<String,Object> stage=stage(requestKey,actor.branch());
            BigDecimal collateralId=CollateralR3ValuationCommitPolicy.positiveId(stage.get("COLLATERAL_ID"),
                    "CLT_R3_VAL_COMMIT_COLLATERAL_ID_INVALID");
            Map<String,Object> parent=parent(collateralId,actor.branch());
            String stageHash=text(stage.get("PAYLOAD_HASH"));
            List<Map<String,Object>> existing=jdbc.queryForList("""
                    SELECT * FROM CLT.R3_VAL_POSTING_LEDGER
                     WHERE REQUEST_ID=? AND BRANCH_CODE=?
                    """,requestKey,actor.branch());
            if (!existing.isEmpty()) return replay(existing,command,stageHash,actor.subject());
            Long keyCollision=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.R3_VAL_POSTING_LEDGER
                     WHERE BRANCH_CODE=? AND IDEMPOTENCY_KEY=?
                    """,Long.class,actor.branch(),command.idempotencyKey());
            if (keyCollision==null || keyCollision!=0L) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_VAL_COMMIT_IDEMPOTENCY_KEY_ALREADY_USED");
            Release signedRelease=release(true);
            var check=preflight.inspect("valuation",requestId,auth);
            var evidence=new StageEvidence(text(stage.get("STATUS_CODE")),
                    number(stage.get("RECORD_VERSION")),check.stageIntegrityVerified(),
                    check.physicalConflictScreenPassed(),
                    check.checks().stream().anyMatch(c -> !"PASS".equals(c.result())),
                    text(stage.get("MAKER_SUBJECT")),actor.subject());
            try {
                CollateralR3ValuationCommitPolicy.guard(command,evidence,signedRelease,
                        enabled,true,has(auth,"SCOPE_clt.collateral.review"));
            } catch (SecurityException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,ex.getMessage());
            } catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());
            }
            if (!"ACTIVE".equals(text(parent.get("TYPE_STATUS")))
                    || !List.of("ACTIVE","PLEDGED","PARTIAL_RELEASE")
                    .contains(text(parent.get("COLLATERAL_STATUS_CODE"))))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_VAL_COMMIT_COLLATERAL_NOT_ELIGIBLE");
            var c=candidate(stage);
            if (!Objects.equals(stageHash,CollateralR3StageFingerprint.digest(
                    CollateralR3WorkflowPolicy.Kind.VALUATION,collateralId.toPlainString(),stage)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_VAL_COMMIT_PAYLOAD_CHANGED");
            // Revalidation is in the same database transaction and uses existing Oracle/CIF readers.
            workflow.validateForStageDecision(CollateralR3WorkflowPolicy.Kind.VALUATION,
                    collateralId.toPlainString(),requestKey,CollateralR3WorkflowPolicy.Action.REVIEW,auth);
            referenceStatuses();
            BigDecimal typeId=CollateralR3ValuationCommitPolicy.positiveId(parent.get("COLLATERAL_TYPE_ID"),
                    "CLT_R3_VAL_COMMIT_TYPE_INVALID");
            TypeRuleRow typeRule=typeRule(typeId,signedRelease.revision());
            ValuerRuleRow valuer=valuerRule(c,typeId,signedRelease.revision());
            LocalDate asOf=oracleTehranDate();
            LocalDate recordedTo;
            try {
                CollateralR3ValuationCommitPolicy.requireAmount(c,text(parent.get("CURRENCY_CODE")));
                CollateralR3ValuationCommitPolicy.requireValuer(valuer.rule(),typeId.toPlainString(),
                        c,signedRelease,actor.subject());
                recordedTo=CollateralR3ValuationCommitPolicy.requirePeriod(typeRule.rule(),
                        typeId.toPlainString(),c,signedRelease,actor.subject(),asOf);
            } catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());
            }
            String currentId=number(parent.get("CURRENT_VALUATION_HISTORY_ID"));
            try { predecessor(c,collateralId,currentId,text(parent.get("CURRENCY_CODE"))); }
            catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage()); }
            Long maximum=jdbc.queryForObject("""
                    SELECT NVL(MAX(VALUATION_SEQUENCE_NO),0)
                      FROM CLT.COLLATERAL_VALUATION_HISTORY WHERE COLLATERAL_ID=?
                    """,Long.class,collateralId);
            if (maximum==null) throw new IllegalStateException("CLT_R3_VAL_COMMIT_SEQUENCE_READ_FAILED");
            long seq=CollateralR3ValuationCommitPolicy.nextSequence(maximum);
            BigDecimal parentVersion=CollateralR3ValuationCommitPolicy.positiveId(parent.get("RECORD_VERSION"),
                    "CLT_R3_VAL_COMMIT_PARENT_VERSION_INVALID");
            jdbc.update("""
                    INSERT INTO CLT.R3_VAL_POSTING_LEDGER
                     (LEDGER_ID,REQUEST_ID,COLLATERAL_ID,BRANCH_CODE,IDEMPOTENCY_KEY,REQUEST_HASH,
                      EXPECTED_VERSION,JUSTIFICATION,COMMIT_STATUS,PREVIOUS_VALUATION_ID,
                      TYPE_RULE_ID,VALUER_RULE_ID,POSTED_BY,POLICY_REVISION,RECORDED_EFFECTIVE_TO)
                    VALUES (CLT.SEQ_CLT_R3_VAL_POST_LEDGER.NEXTVAL,?,?,?,?,?,3,?,'PREPARED',?,?,?,?,?,?)
                    """,requestKey,collateralId,actor.branch(),command.idempotencyKey(),stageHash,
                    command.reason().trim(),currentId==null ? null : new BigDecimal(currentId),
                    typeRule.id(),valuer.id(),actor.subject(),signedRelease.revision(),Date.valueOf(recordedTo));
            BigDecimal newId=insertHistory(c,collateralId,seq,actor.subject());
            if (newId==null || newId.signum()<=0)
                throw new IllegalStateException("CLT_R3_VAL_COMMIT_DOMAIN_ID_MISSING");
            if (currentId!=null) {
                int previousUpdated=jdbc.update("""
                        UPDATE CLT.COLLATERAL_VALUATION_HISTORY
                           SET VALUATION_STATUS_CODE='SUPERSEDED',UPDATED_AT=SYSTIMESTAMP,
                               UPDATED_BY=?,RECORD_VERSION=RECORD_VERSION+1
                         WHERE COLLATERAL_VALUATION_HISTORY_ID=? AND COLLATERAL_ID=?
                           AND VALUATION_STATUS_CODE='FINAL'
                        """,actor.subject(),new BigDecimal(currentId),collateralId);
                if (previousUpdated!=1)
                    throw new IllegalStateException("CLT_R3_VAL_COMMIT_PREDECESSOR_TRANSITION_FAILED");
            }
            int parentUpdated;
            if (currentId==null) {
                parentUpdated=jdbc.update("""
                        UPDATE CLT.COLLATERAL SET CURRENT_VALUATION_HISTORY_ID=?,
                               UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=?,RECORD_VERSION=RECORD_VERSION+1
                         WHERE COLLATERAL_ID=? AND BRANCH_CODE=? AND RECORD_VERSION=?
                           AND CURRENT_VALUATION_HISTORY_ID IS NULL
                        """,newId,actor.subject(),collateralId,actor.branch(),parentVersion);
            } else {
                parentUpdated=jdbc.update("""
                        UPDATE CLT.COLLATERAL SET CURRENT_VALUATION_HISTORY_ID=?,
                               UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=?,RECORD_VERSION=RECORD_VERSION+1
                         WHERE COLLATERAL_ID=? AND BRANCH_CODE=? AND RECORD_VERSION=?
                           AND CURRENT_VALUATION_HISTORY_ID=?
                        """,newId,actor.subject(),collateralId,actor.branch(),parentVersion,
                        new BigDecimal(currentId));
            }
            if (parentUpdated!=1)
                throw new IllegalStateException("CLT_R3_VAL_COMMIT_CURRENT_POINTER_CAS_FAILED");
            int completed=jdbc.update("""
                    UPDATE CLT.R3_VAL_POSTING_LEDGER
                       SET DOMAIN_RECORD_ID=?,COMMIT_STATUS='COMMITTED',POSTED_AT=SYSTIMESTAMP
                     WHERE REQUEST_ID=? AND BRANCH_CODE=? AND COMMIT_STATUS='PREPARED'
                    """,newId,requestKey,actor.branch());
            if (completed!=1) throw new IllegalStateException("CLT_R3_VAL_COMMIT_LEDGER_UPDATE_FAILED");
            return new Receipt(requestId,collateralId.toPlainString(),newId.toPlainString(),
                    "COMMITTED",signedRelease.revision(),command.idempotencyKey(),
                    currentId,recordedTo.toString(),false);
        });
    }
}
