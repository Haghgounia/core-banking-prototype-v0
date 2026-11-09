package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels;
import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.workflow.CollateralR3InsuranceCommitPolicy.*;
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
 * R3 BUC-07 Stage-to-domain insurance CREATE/RENEW atomic candidate.
 * No cancellation DML: base model has no canonical cancellation status or immutable history.
 * All DML uses the application's one shared DataSource, an explicit bank release and
 * Oracle-enforced unique policy number and one-to-one predecessor contracts.
 */
@Service
@ConditionalOnProperty(prefix="core-banking.collateral.prototype", name="enabled", havingValue="true")
public final class CollateralR3InsuranceCommitService {
    public record Activation(boolean featureFlag, boolean schemaReady, boolean bankReleasePresent,
            boolean postingEnabled, String stage, List<String> unresolvedPolicyGaps) {}
    public record PostedEvidence(String requestId, String collateralId, String insuranceId,
            String commitStatus, String policyRevision, String previousInsuranceId, String postedAt,
            String coverageStatus) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final CollateralIntroductionService introduction;
    private final CollateralR3PostingReadinessService preflight;
    private final CollateralR3WorkflowService workflow;
    @Value("${core-banking.collateral.r3-posting.insurance-enabled:false}")
    private boolean enabled;

    public CollateralR3InsuranceCommitService(DataSource sharedDataSource,
            CollateralIntroductionService introduction,
            CollateralR3PostingReadinessService preflight, CollateralR3WorkflowService workflow) {
        this.jdbc = new JdbcTemplate(sharedDataSource);
        this.jdbc.setQueryTimeout(12);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(sharedDataSource));
        this.transaction.setTimeout(25);
        this.introduction = introduction;
        this.preflight = preflight;
        this.workflow = workflow;
    }
    private static boolean has(Authentication a,String authority) {
        return a != null && a.isAuthenticated() && a.getAuthorities().stream()
                .anyMatch(g -> authority.equals(g.getAuthority()));
    }
    private static void require(Authentication a,String authority) {
        if (!has(a,authority)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "CLT_R3_INSURANCE_COMMIT_AUTHORITY_REQUIRED");
    }
    private static String value(Object input) {
        return input == null ? null : input.toString().trim();
    }
    private static String number(Object input) {
        if (input == null) return null;
        return input instanceof BigDecimal n ? n.stripTrailingZeros().toPlainString() : value(input);
    }
    private static String date(Object input) {
        if (input == null) return null;
        if (input instanceof Date d) return d.toLocalDate().toString();
        if (input instanceof Timestamp t) return t.toLocalDateTime().toLocalDate().toString();
        if (input instanceof LocalDate d) return d.toString();
        if (input instanceof LocalDateTime t) return t.toLocalDate().toString();
        throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_STAGE_DATE_INVALID");
    }
    private boolean unique(String table, String name, String... columns) {
        String sql = """
                SELECT COUNT(*) FROM (
                  SELECT c.CONSTRAINT_NAME FROM ALL_CONSTRAINTS c
                  JOIN ALL_CONS_COLUMNS cc ON cc.OWNER=c.OWNER
                   AND cc.TABLE_NAME=c.TABLE_NAME AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
                  WHERE c.OWNER='CLT' AND c.TABLE_NAME=? AND c.CONSTRAINT_NAME=?
                    AND c.CONSTRAINT_TYPE='U' AND c.STATUS='ENABLED'
                    AND c.VALIDATED='VALIDATED'
                  GROUP BY c.CONSTRAINT_NAME
                  HAVING COUNT(*)=? AND SUM(CASE WHEN cc.COLUMN_NAME IN (%s) THEN 1 ELSE 0 END)=?
                )
                """.formatted(String.join(",",java.util.Collections.nCopies(columns.length,"?")));
        Object[] args = new Object[4+columns.length];
        args[0]=table; args[1]=name; args[2]=columns.length;
        System.arraycopy(columns,0,args,3,columns.length);args[args.length-1]=columns.length;
        Long found=jdbc.queryForObject(sql,Long.class,args);
        return found!=null && found==1L;
    }
    private boolean schemaReady() {
        try {
            Long tables=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CLT' AND TABLE_NAME IN
                      ('R3_INS_POSTING_RELEASE','R3_INS_POSTING_RULE','R3_INS_POSTING_LEDGER')
                    """,Long.class);
            Long columns=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT' AND (
                     (TABLE_NAME='R3_INS_POSTING_RELEASE' AND COLUMN_NAME IN
                      ('RELEASE_ID','RELEASE_STATUS','POLICY_REVISION','BUSINESS_DECISION_REF',
                       'DATA_DECISION_REF','INTEGRATION_DECISION_REF','COVERAGE_DECISION_REF',
                       'RENEWAL_DECISION_REF','CANCELLATION_IMPACT_DECISION_REF','APPROVED_BY',
                       'EFFECTIVE_FROM','EFFECTIVE_TO')) OR
                     (TABLE_NAME='R3_INS_POSTING_RULE' AND COLUMN_NAME IN
                      ('RULE_ID','COLLATERAL_TYPE_ID','INSURANCE_TYPE_ID','CURRENCY_CODE',
                       'MIN_COVERAGE_AMOUNT','POLICY_REVISION','RULE_STATUS','APPROVED_BY',
                       'EFFECTIVE_FROM','EFFECTIVE_TO')) OR
                     (TABLE_NAME='R3_INS_POSTING_LEDGER' AND COLUMN_NAME IN
                      ('LEDGER_ID','REQUEST_ID','COLLATERAL_ID','BRANCH_CODE',
                       'IDEMPOTENCY_KEY','REQUEST_HASH','EXPECTED_VERSION','JUSTIFICATION',
                       'COMMIT_STATUS','DOMAIN_RECORD_ID','PREVIOUS_INSURANCE_ID',
                       'COVERAGE_RULE_ID','POSTED_BY','POLICY_REVISION','POSTED_AT','CREATED_AT'))
                    )
                    """,Long.class);
            Long essentialConstraints=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT'
                      AND STATUS='ENABLED' AND VALIDATED='VALIDATED' AND (
                       (TABLE_NAME='R3_INS_POSTING_RELEASE' AND CONSTRAINT_NAME IN
                        ('CK_CLT_R3_INS_REL_ONE','CK_CLT_R3_INS_REL_STATUS','CK_CLT_R3_INS_REL_DATES')) OR
                       (TABLE_NAME='R3_INS_POSTING_RULE' AND CONSTRAINT_NAME IN
                        ('CK_CLT_R3_INS_RULE_AMOUNT','CK_CLT_R3_INS_RULE_STATUS')) OR
                       (TABLE_NAME='R3_INS_POSTING_LEDGER' AND CONSTRAINT_NAME IN
                        ('CK_CLT_R3_INS_POST_STATUS','CK_CLT_R3_INS_POST_COMPLETE')))
                    """,Long.class);
            Long sequence=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT'
                     AND SEQUENCE_NAME='SEQ_CLT_R3_INS_POST_LEDGER'
                    """,Long.class);
            return tables != null && tables==3L && columns!=null && columns==38L
                    && essentialConstraints!=null && essentialConstraints==7L
                    && sequence!=null && sequence==1L
                    && unique("R3_INS_POSTING_LEDGER","UQ_CLT_R3_INS_POST_REQ","REQUEST_ID")
                    && unique("R3_INS_POSTING_LEDGER","UQ_CLT_R3_INS_POST_IDEMP","BRANCH_CODE","IDEMPOTENCY_KEY")
                    && unique("R3_INS_POSTING_RULE","UQ_CLT_R3_INS_RULE_REV",
                        "COLLATERAL_TYPE_ID","INSURANCE_TYPE_ID","CURRENCY_CODE","POLICY_REVISION")
                    && unique("COLLATERAL_INSURANCE","UQ_CI_POLICY",
                        "INSURANCE_COMPANY_ID","INSURANCE_NUMBER")
                    && unique("COLLATERAL_INSURANCE","UQ_CLT_INS_RENEW_ONCE","PREVIOUS_INSURANCE_ID");
        } catch (RuntimeException ignored) { return false; }
    }
    private Release release(boolean lock) {
        String sql="""
               SELECT POLICY_REVISION, BUSINESS_DECISION_REF, DATA_DECISION_REF,
                      INTEGRATION_DECISION_REF, COVERAGE_DECISION_REF, RENEWAL_DECISION_REF,
                      CANCELLATION_IMPACT_DECISION_REF, APPROVED_BY
                 FROM CLT.R3_INS_POSTING_RELEASE
                WHERE RELEASE_ID=1 AND RELEASE_STATUS='APPROVED'
                  AND EFFECTIVE_FROM<=SYSTIMESTAMP
                  AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO>SYSTIMESTAMP)
               """ + (lock ? " FOR UPDATE" : "");
        List<Map<String,Object>> rows=jdbc.queryForList(sql);
        if (rows.size()!=1) return null;
        Map<String,Object> r=rows.getFirst();
        return new Release(value(r.get("POLICY_REVISION")),value(r.get("BUSINESS_DECISION_REF")),
                value(r.get("DATA_DECISION_REF")),value(r.get("INTEGRATION_DECISION_REF")),
                value(r.get("COVERAGE_DECISION_REF")),value(r.get("RENEWAL_DECISION_REF")),
                value(r.get("CANCELLATION_IMPACT_DECISION_REF")),value(r.get("APPROVED_BY")),true);
    }
    private static boolean validRelease(Release r) {
        try {
            CollateralR3InsuranceCommitPolicy.guard(new Command("3","readiness.20261109","Activation check"),
                    new StageEvidence("REVIEWED","3",true,true,false,"maker","posting"),
                    r,true,true,true);
            return true;
        } catch (RuntimeException ex) { return false; }
    }
    public Activation activation(Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        introduction.actor(auth);
        boolean ready=schemaReady();
        boolean approved=false;
        if (ready) try { approved=validRelease(release(false)); }
        catch (DataAccessException ignored) { approved=false; }
        return new Activation(enabled,ready,approved,enabled && ready && approved,
                enabled && ready && approved ? "INSURANCE_CREATE_RENEW_CANDIDATE_RULE_PER_REQUEST_REQUIRED"
                        : "INSURANCE_POSTING_RELEASE_OR_SCHEMA_MISSING",
                CollateralR3InsuranceCommitPolicy.unresolvedAfterRegistration());
    }
    public PostedEvidence receipt(String requestId, Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        var who=introduction.actor(auth);
        BigDecimal id=new BigDecimal(CollateralR3WorkflowPolicy.id(requestId));
        List<Map<String,Object>> rows;
        try { rows=jdbc.queryForList("""
                SELECT REQUEST_ID,COLLATERAL_ID,DOMAIN_RECORD_ID,COMMIT_STATUS,
                       POLICY_REVISION,PREVIOUS_INSURANCE_ID,POSTED_AT
                  FROM CLT.R3_INS_POSTING_LEDGER
                 WHERE REQUEST_ID=? AND BRANCH_CODE=? AND COMMIT_STATUS='COMMITTED'
                """,id,who.branch()); }
        catch (DataAccessException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R3_INSURANCE_COMMIT_LEDGER_UNAVAILABLE"); }
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_INSURANCE_COMMIT_RECEIPT_NOT_FOUND");
        Map<String,Object> r=rows.getFirst();
        return new PostedEvidence(number(r.get("REQUEST_ID")),number(r.get("COLLATERAL_ID")),
                number(r.get("DOMAIN_RECORD_ID")),value(r.get("COMMIT_STATUS")),
                value(r.get("POLICY_REVISION")),number(r.get("PREVIOUS_INSURANCE_ID")),
                value(r.get("POSTED_AT")),"RECORDED_NOT_ALLOCATION_COVERAGE_APPROVED");
    }
    private Map<String,Object> lockStage(BigDecimal id,String branch) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT * FROM CLT.COLLATERAL_INSURANCE_REQUEST
                 WHERE INSURANCE_REQUEST_ID=? AND BRANCH_CODE=? FOR UPDATE
                """,id,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_INSURANCE_COMMIT_REQUEST_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    private Map<String,Object> lockCollateral(BigDecimal id,String branch) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT c.COLLATERAL_ID,c.COLLATERAL_TYPE_ID,c.PARTY_ID
                  FROM CLT.COLLATERAL c
                 WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=? FOR UPDATE
                """,id,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_INSURANCE_COMMIT_COLLATERAL_NOT_FOUND");
        return rows.getFirst();
    }
    private record RuleRow(CoverageRule coverage, BigDecimal ruleId) {}
    private RuleRow coverage(BigDecimal type, BigDecimal insuranceType,String currency,String revision) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT RULE_ID,COLLATERAL_TYPE_ID,INSURANCE_TYPE_ID,CURRENCY_CODE,
                       MIN_COVERAGE_AMOUNT,POLICY_REVISION,APPROVED_BY
                  FROM CLT.R3_INS_POSTING_RULE
                 WHERE COLLATERAL_TYPE_ID=? AND INSURANCE_TYPE_ID=? AND CURRENCY_CODE=?
                   AND POLICY_REVISION=? AND RULE_STATUS='APPROVED'
                   AND EFFECTIVE_FROM<=SYSTIMESTAMP
                   AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO>SYSTIMESTAMP)
                 FOR UPDATE
                """,type,insuranceType,currency,revision);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_INSURANCE_COMMIT_UNIQUE_COVERAGE_RULE_REQUIRED");
        Map<String,Object> r=rows.getFirst();
        return new RuleRow(new CoverageRule(number(r.get("COLLATERAL_TYPE_ID")),
                number(r.get("INSURANCE_TYPE_ID")),value(r.get("CURRENCY_CODE")),
                number(r.get("MIN_COVERAGE_AMOUNT")),value(r.get("POLICY_REVISION")),
                value(r.get("APPROVED_BY")),true),
                CollateralR3InsuranceCommitPolicy.positiveId(r.get("RULE_ID"),
                        "CLT_R3_INSURANCE_COMMIT_RULE_ID_INVALID"));
    }
    private static CollateralInsurancePolicy.Candidate candidate(Map<String,Object> stage) {
        return CollateralInsurancePolicy.candidate(new CollateralInsuranceModels.CandidateRequest(
                value(stage.get("INSURER_CODE")),number(stage.get("INSURANCE_COMPANY_ID")),
                number(stage.get("COLLATERAL_INSURANCE_TYPE_ID")),value(stage.get("DEPOSIT_NUMBER")),
                value(stage.get("INSURANCE_NUMBER")),number(stage.get("INSURANCE_AMOUNT")),
                number(stage.get("INSURANCE_RECEIVED_AMOUNT")),number(stage.get("TAX_AMOUNT")),
                date(stage.get("VALID_FROM")),date(stage.get("VALID_TO")),
                number(stage.get("PREVIOUS_INSURANCE_ID")),value(stage.get("CURRENCY_CODE")),
                value(stage.get("JUSTIFICATION"))));
    }
    private void checkPredecessor(CollateralInsurancePolicy.Candidate c,BigDecimal collateralId,
            BigDecimal ownerPartyId, String branch) {
        if (c.previousInsuranceId() == null) return;
        BigDecimal prev=new BigDecimal(c.previousInsuranceId());
        List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT COLLATERAL_ID,PARTY_ID,COLLATERAL_INSURANCE_TYPE_ID,VALID_TO
                  FROM CLT.COLLATERAL_INSURANCE
                 WHERE COLLATERAL_INSURANCE_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=? FOR UPDATE
                """,prev,collateralId,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_INSURANCE_COMMIT_PREVIOUS_NOT_FOUND");
        Long descendants=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE WHERE PREVIOUS_INSURANCE_ID=?
                """,Long.class,prev);
        Map<String,Object> p=rows.getFirst();
        if (!Objects.equals(number(p.get("PARTY_ID")),ownerPartyId.toPlainString()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_INSURANCE_COMMIT_PREVIOUS_OWNER_MISMATCH");
        CollateralR3InsuranceCommitPolicy.requireRenewal(c,collateralId.toPlainString(),
                number(p.get("COLLATERAL_ID")),number(p.get("COLLATERAL_INSURANCE_TYPE_ID")),
                date(p.get("VALID_TO")),descendants==null ? -1L : descendants);
    }
    private Receipt replay(List<Map<String,Object>> rows, Command command, String hash,String poster) {
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_INSURANCE_COMMIT_DUPLICATE_LEDGER");
        Map<String,Object> r=rows.getFirst();
        try {
            CollateralR3InsuranceCommitPolicy.checkReplay(command,value(r.get("IDEMPOTENCY_KEY")),
                    hash,value(r.get("REQUEST_HASH")),value(r.get("JUSTIFICATION")),
                    number(r.get("EXPECTED_VERSION")),poster,value(r.get("POSTED_BY")));
            if (!"COMMITTED".equals(value(r.get("COMMIT_STATUS"))) || r.get("DOMAIN_RECORD_ID")==null)
                throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_REPLAY_INCOMPLETE");
        } catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage()); }
        return new Receipt(number(r.get("REQUEST_ID")),number(r.get("COLLATERAL_ID")),
                number(r.get("DOMAIN_RECORD_ID")),"COMMITTED",value(r.get("POLICY_REVISION")),
                command.idempotencyKey(),number(r.get("PREVIOUS_INSURANCE_ID")),true);
    }
    private BigDecimal insert(CollateralInsurancePolicy.Candidate c,BigDecimal collateralId,
            BigDecimal partyId,String branch,String actor) {
        return jdbc.execute((ConnectionCallback<BigDecimal>)conn -> {
            try (CallableStatement call=conn.prepareCall("""
                    BEGIN INSERT INTO CLT.COLLATERAL_INSURANCE (
                      COLLATERAL_ID,PARTY_ID,INSURER_CODE,INSURANCE_COMPANY_ID,
                      COLLATERAL_INSURANCE_TYPE_ID,DEPOSIT_NUMBER,INSURANCE_NUMBER,
                      INSURANCE_AMOUNT,INSURANCE_RECEIVED_AMOUNT,TAX_AMOUNT,VALID_FROM,VALID_TO,
                      PREVIOUS_INSURANCE_ID,BRANCH_CODE,CURRENCY_CODE,CREATED_BY)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    RETURNING COLLATERAL_INSURANCE_ID INTO ?; END;
                    """)) {
                call.setBigDecimal(1,collateralId);call.setBigDecimal(2,partyId);
                call.setString(3,c.insurerCode());call.setBigDecimal(4,new BigDecimal(c.companyId()));
                call.setBigDecimal(5,new BigDecimal(c.insuranceTypeId()));
                call.setString(6,c.depositNumber());call.setString(7,c.insuranceNumber());
                call.setBigDecimal(8,new BigDecimal(c.insuranceAmount()));
                call.setBigDecimal(9,new BigDecimal(c.receivedAmount()));
                if (c.taxAmount()==null) call.setNull(10,Types.NUMERIC);
                else call.setBigDecimal(10,new BigDecimal(c.taxAmount()));
                call.setDate(11,Date.valueOf(c.validFrom()));call.setDate(12,Date.valueOf(c.validTo()));
                if (c.previousInsuranceId()==null) call.setNull(13,Types.NUMERIC);
                else call.setBigDecimal(13,new BigDecimal(c.previousInsuranceId()));
                call.setString(14,branch);call.setString(15,c.currencyCode());call.setString(16,actor);
                call.registerOutParameter(17,Types.NUMERIC);call.execute();
                return call.getBigDecimal(17);
            }
        });
    }
    public Receipt commit(String requestId,Command command,Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        require(auth,"SCOPE_clt.collateral.review");
        var who=introduction.actor(auth);
        if (command==null) throw new IllegalArgumentException("CLT_R3_INSURANCE_COMMIT_BODY_REQUIRED");
        CollateralR3InsuranceCommitPolicy.version(command.expectedVersion());
        CollateralR3InsuranceCommitPolicy.key(command.idempotencyKey());
        CollateralR3InsuranceCommitPolicy.reason(command.reason());
        if (!enabled || !schemaReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R3_INSURANCE_COMMIT_DBA_RELEASE_REQUIRED");
        BigDecimal id=new BigDecimal(CollateralR3WorkflowPolicy.id(requestId));
        return transaction.execute(tx -> {
            Map<String,Object> stage=lockStage(id,who.branch());
            BigDecimal collateralId=CollateralR3InsuranceCommitPolicy.positiveId(stage.get("COLLATERAL_ID"),
                    "CLT_R3_INSURANCE_COMMIT_COLLATERAL_ID_INVALID");
            Map<String,Object> parent=lockCollateral(collateralId,who.branch());
            if (!Objects.equals(number(stage.get("PARTY_ID")),number(parent.get("PARTY_ID"))))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_INSURANCE_COMMIT_OWNER_CHANGED");
            String hash=value(stage.get("PAYLOAD_HASH"));
            List<Map<String,Object>> existing=jdbc.queryForList("""
                    SELECT * FROM CLT.R3_INS_POSTING_LEDGER WHERE REQUEST_ID=? AND BRANCH_CODE=?
                    """,id,who.branch());
            if (!existing.isEmpty()) return replay(existing,command,hash,who.subject());
            Long collision=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.R3_INS_POSTING_LEDGER
                     WHERE BRANCH_CODE=? AND IDEMPOTENCY_KEY=?
                    """,Long.class,who.branch(),command.idempotencyKey());
            if (collision==null || collision!=0L) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_INSURANCE_COMMIT_IDEMPOTENCY_REUSED");
            Release release=release(true);
            var check=preflight.inspect("insurance",requestId,auth);
            var facts=new StageEvidence(value(stage.get("STATUS_CODE")),
                    number(stage.get("RECORD_VERSION")),check.stageIntegrityVerified(),
                    check.physicalConflictScreenPassed(),
                    check.checks().stream().anyMatch(x -> !"PASS".equals(x.result())),
                    value(stage.get("MAKER_SUBJECT")),who.subject());
            try { CollateralR3InsuranceCommitPolicy.guard(command,facts,release,enabled,true,
                        has(auth,"SCOPE_clt.collateral.review")); }
            catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN,ex.getMessage()); }
            catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage()); }
            var c=candidate(stage);
            RuleRow rule=coverage(CollateralR3InsuranceCommitPolicy.positiveId(parent.get("COLLATERAL_TYPE_ID"),
                            "CLT_R3_INSURANCE_COMMIT_TYPE_ID_INVALID"),
                    new BigDecimal(c.insuranceTypeId()),c.currencyCode(),release.revision());
            try { CollateralR3InsuranceCommitPolicy.requireCoverage(rule.coverage(),
                        number(parent.get("COLLATERAL_TYPE_ID")),c,release,who.subject()); }
            catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage()); }
            // Full business revalidation within the same transaction; never trust UI preflight.
            workflow.validateForStageDecision(CollateralR3WorkflowPolicy.Kind.INSURANCE,
                    collateralId.toPlainString(),id,CollateralR3WorkflowPolicy.Action.REVIEW,auth);
            checkPredecessor(c,collateralId,
                    CollateralR3InsuranceCommitPolicy.positiveId(parent.get("PARTY_ID"),
                            "CLT_R3_INSURANCE_COMMIT_OWNER_ID_INVALID"),who.branch());
            String recalculated=CollateralR3StageFingerprint.digest(
                    CollateralR3WorkflowPolicy.Kind.INSURANCE,collateralId.toPlainString(),stage);
            if (!Objects.equals(hash,recalculated)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_INSURANCE_COMMIT_PAYLOAD_CHANGED");
            jdbc.update("""
                    INSERT INTO CLT.R3_INS_POSTING_LEDGER
                     (LEDGER_ID,REQUEST_ID,COLLATERAL_ID,BRANCH_CODE,IDEMPOTENCY_KEY,REQUEST_HASH,
                      EXPECTED_VERSION,JUSTIFICATION,COMMIT_STATUS,PREVIOUS_INSURANCE_ID,
                      COVERAGE_RULE_ID,POSTED_BY,POLICY_REVISION)
                    VALUES (CLT.SEQ_CLT_R3_INS_POST_LEDGER.NEXTVAL,?,?,?,?,?,3,?,'PREPARED',?,?,?,?)
                    """,id,collateralId,who.branch(),command.idempotencyKey(),hash,
                    command.reason().trim(),c.previousInsuranceId()==null ? null : new BigDecimal(c.previousInsuranceId()),
                    rule.ruleId(),who.subject(),release.revision());
            BigDecimal newId=insert(c,collateralId,
                    CollateralR3InsuranceCommitPolicy.positiveId(parent.get("PARTY_ID"),
                            "CLT_R3_INSURANCE_COMMIT_OWNER_ID_INVALID"),who.branch(),who.subject());
            if (newId==null || newId.signum()<=0)
                throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_RETURNING_ID_MISSING");
            int updated=jdbc.update("""
                    UPDATE CLT.R3_INS_POSTING_LEDGER
                       SET DOMAIN_RECORD_ID=?,COMMIT_STATUS='COMMITTED',POSTED_AT=SYSTIMESTAMP
                     WHERE REQUEST_ID=? AND BRANCH_CODE=? AND COMMIT_STATUS='PREPARED'
                    """,newId,id,who.branch());
            if (updated!=1) throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_LEDGER_UPDATE_FAILED");
            return new Receipt(requestId,collateralId.toPlainString(),newId.toPlainString(),
                    "COMMITTED",release.revision(),command.idempotencyKey(),
                    c.previousInsuranceId(),false);
        });
    }
}
