package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.workflow.CollateralSponsorCommitPolicy.*;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
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
 * A single Oracle transaction for sponsor posting + immutable posting receipt.
 * Default disabled. A DBA-installed approval and ledger contract is mandatory.
 * This is not an IAM change, nor a replacement for the bank's SoD policy.
 */
@Service
@ConditionalOnProperty(prefix="core-banking.collateral.prototype",name="enabled",havingValue="true")
public final class CollateralSponsorCommitService {
    public record PostedEvidence(String requestId, String collateralId, String sponsorId,
            String commitStatus, String policyRevision, String postedAt) {}
    public record Activation(boolean featureFlag, boolean approvalSchemaReady,
            boolean businessReleasePresent, boolean postingEnabled,
            String stage, List<String> remainingWork) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final CollateralIntroductionService introduction;
    private final CollateralPostingReadinessService preflight;
    private final CollateralWorkflowService workflow;
    @Value("${core-banking.collateral.r3-posting.sponsor-enabled:false}")
    private boolean featureFlag;

    public CollateralSponsorCommitService(DataSource sharedApplicationDataSource,
            CollateralIntroductionService introduction,
            CollateralPostingReadinessService preflight,
            CollateralWorkflowService workflow) {
        this.jdbc = new JdbcTemplate(sharedApplicationDataSource);
        this.jdbc.setQueryTimeout(12);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(sharedApplicationDataSource));
        this.transaction.setTimeout(25);
        this.introduction = introduction;
        this.preflight = preflight;
        this.workflow = workflow;
    }
    private static boolean scope(Authentication auth, String required) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> required.equals(a.getAuthority()));
    }
    private static void require(Authentication auth, String required) {
        if (!scope(auth,required))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "CLT_R3_SPONSOR_COMMIT_SCOPE_REQUIRED");
    }
    private static String value(Object object) {
        return object == null ? null : object.toString().trim();
    }
    private static long number(Object object) {
        if (!(object instanceof Number n))
            throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_SCHEMA_COUNT_INVALID");
        return n.longValue();
    }
    private boolean uniquePair(String table, String constraint, String left, String right) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                  SELECT c.CONSTRAINT_NAME FROM ALL_CONSTRAINTS c
                   JOIN ALL_CONS_COLUMNS cc ON cc.OWNER=c.OWNER
                    AND cc.CONSTRAINT_NAME=c.CONSTRAINT_NAME
                    AND cc.TABLE_NAME=c.TABLE_NAME
                  WHERE c.OWNER='CLT' AND c.TABLE_NAME=? AND c.CONSTRAINT_NAME=?
                    AND c.CONSTRAINT_TYPE='U' AND c.STATUS='ENABLED'
                    AND c.VALIDATED='VALIDATED'
                  GROUP BY c.CONSTRAINT_NAME
                  HAVING COUNT(*)=2 AND SUM(CASE WHEN cc.COLUMN_NAME IN (?,?) THEN 1 ELSE 0 END)=2
                )
                """,Long.class,table,constraint,left,right);
        return count != null && count==1L;
    }
    private boolean schemaReady() {
        try {
            Long count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CLT'
                    AND TABLE_NAME IN ('R3_POSTING_RELEASE','R3_POSTING_LEDGER')
                    """, Long.class);
            if (count == null || count != 2L) return false;
            Long cols = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                    AND ((TABLE_NAME='R3_POSTING_RELEASE' AND COLUMN_NAME IN
                      ('REQUEST_KIND','RELEASE_STATUS','POLICY_REVISION','BUSINESS_DECISION_REF',
                       'DATA_DECISION_REF','INTEGRATION_DECISION_REF','APPROVED_BY',
                       'EFFECTIVE_FROM','EFFECTIVE_TO'))
                    OR (TABLE_NAME='R3_POSTING_LEDGER' AND COLUMN_NAME IN
                      ('REQUEST_KIND','REQUEST_ID','BRANCH_CODE','IDEMPOTENCY_KEY',
                       'REQUEST_HASH','EXPECTED_VERSION','DOMAIN_RECORD_ID','COMMIT_STATUS',
                       'POSTED_BY','POLICY_REVISION','POSTED_AT','JUSTIFICATION')))
                    """,Long.class);
            // Not a substitute for DBA review of unique constraints; the candidate DDL is required.
            Long sequence = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT'
                    AND SEQUENCE_NAME='SEQ_CLT_R3_POSTING_LEDGER'
                    """,Long.class);
            return cols != null && cols == 21L && sequence != null && sequence == 1L
                    && uniquePair("R3_POSTING_LEDGER","UQ_CLT_R3_POST_REQ", "REQUEST_KIND", "REQUEST_ID")
                    && uniquePair("R3_POSTING_LEDGER","UQ_CLT_R3_POST_IDEMP", "BRANCH_CODE", "IDEMPOTENCY_KEY")
                    && uniquePair("COLLATERAL_SPONSOR","UQ_CLT_SPONSOR_COLL_PARTY", "COLLATERAL_ID", "PARTY_ID");
        } catch (RuntimeException ignored) { return false; }
    }
    private Release approvedRelease(boolean lock) {
        String sql = """
                SELECT POLICY_REVISION, BUSINESS_DECISION_REF, DATA_DECISION_REF,
                       INTEGRATION_DECISION_REF, APPROVED_BY
                  FROM CLT.R3_POSTING_RELEASE
                 WHERE REQUEST_KIND='SPONSOR' AND RELEASE_STATUS='APPROVED'
                   AND EFFECTIVE_FROM <= SYSTIMESTAMP
                   AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO > SYSTIMESTAMP)
                """ + (lock ? " FOR UPDATE" : "");
        List<Map<String,Object>> rows = jdbc.queryForList(sql);
        if (rows.size() != 1) return null;
        Map<String,Object> release = rows.getFirst();
        return new Release(value(release.get("POLICY_REVISION")),
                value(release.get("BUSINESS_DECISION_REF")),
                value(release.get("DATA_DECISION_REF")),
                value(release.get("INTEGRATION_DECISION_REF")),
                value(release.get("APPROVED_BY")), true);
    }
    private static boolean approved(Release release) {
        return release != null && release.approved()
                && refs(release.businessDecision()) && refs(release.dataDecision())
                && refs(release.integrationDecision()) && refs(release.revision())
                && refs(release.approvedBy());
    }
    private static boolean refs(String value) { return value != null && value.length() >= 8 && value.length() <= 120; }

    public Activation activation(Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        introduction.actor(auth);
        boolean schema = schemaReady();
        boolean bankRelease = false;
        if (schema) {
            try { bankRelease = approved(approvedRelease(false)); }
            catch (DataAccessException ignored) { bankRelease = false; }
        }
        return new Activation(featureFlag,schema,bankRelease,
                featureFlag && schema && bankRelease,
                featureFlag && schema && bankRelease ? "SPONSOR_ATOMIC_COMMIT_CANDIDATE_ENABLED"
                        : "SPONSOR_COMMIT_APPROVAL_OR_SCHEMA_NOT_READY",
                CollateralSponsorCommitPolicy.unresolvedOutsideAdd());
    }
    /** Read-only, branch-scoped receipt; must survive temporary deactivation of posting. */
    public PostedEvidence receipt(String requestId, Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        var who = introduction.actor(auth);
        BigDecimal id = new BigDecimal(CollateralWorkflowPolicy.id(requestId));
        List<Map<String,Object>> rows;
        try {
            rows = jdbc.queryForList("""
                    SELECT REQUEST_ID, COLLATERAL_ID, DOMAIN_RECORD_ID, COMMIT_STATUS,
                           POLICY_REVISION, POSTED_AT
                      FROM CLT.R3_POSTING_LEDGER
                     WHERE REQUEST_KIND='SPONSOR' AND REQUEST_ID=? AND BRANCH_CODE=?
                           AND COMMIT_STATUS='COMMITTED'
                    """,id,who.branch());
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R3_SPONSOR_COMMIT_LEDGER_UNAVAILABLE");
        }
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_SPONSOR_COMMIT_RECEIPT_NOT_FOUND");
        var recorded=rows.getFirst();
        return new PostedEvidence(value(recorded.get("REQUEST_ID")),
                value(recorded.get("COLLATERAL_ID")),value(recorded.get("DOMAIN_RECORD_ID")),
                value(recorded.get("COMMIT_STATUS")),value(recorded.get("POLICY_REVISION")),
                value(recorded.get("POSTED_AT")));
    }
    private Map<String,Object> lockedStage(BigDecimal requestId, String branch) {
        List<Map<String,Object>> rows = jdbc.queryForList("""
            SELECT * FROM CLT.COLLATERAL_SPONSOR_REQUEST
             WHERE SPONSOR_REQUEST_ID = ? AND BRANCH_CODE = ? FOR UPDATE
            """, requestId,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R3_SPONSOR_COMMIT_REQUEST_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    private void lockParent(BigDecimal collateralId, String branch) {
        List<Map<String,Object>> rows = jdbc.queryForList("""
                SELECT COLLATERAL_ID FROM CLT.COLLATERAL
                 WHERE COLLATERAL_ID = ? AND BRANCH_CODE = ? FOR UPDATE
                """,collateralId,branch);
        if (rows.size()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_SPONSOR_COMMIT_COLLATERAL_NOT_FOUND");
    }
    private Receipt replay(List<Map<String,Object>> rows, Command command,
            String actualHash, String caller) {
        if (rows.size() != 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_R3_SPONSOR_COMMIT_DUPLICATE_RECEIPT");
        Map<String,Object> saved = rows.getFirst();
        try {
            CollateralSponsorCommitPolicy.checkReplay(command,
                    value(saved.get("IDEMPOTENCY_KEY")),value(saved.get("REQUEST_HASH")),
                    actualHash,value(saved.get("POSTED_BY")));
            if (!"COMMITTED".equals(value(saved.get("COMMIT_STATUS")))
                    || !Objects.equals(command.reason().trim(), value(saved.get("JUSTIFICATION")))
                    || !Objects.equals(command.expectedVersion(), value(saved.get("EXPECTED_VERSION")))
                    || !Objects.equals(caller, value(saved.get("POSTED_BY"))))
                throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_REPLAY_OWNER_OR_STATE_CONFLICT");
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());
        }
        return new Receipt(value(saved.get("REQUEST_ID")),value(saved.get("COLLATERAL_ID")),
                value(saved.get("DOMAIN_RECORD_ID")),"COMMITTED",
                value(saved.get("POLICY_REVISION")),command.idempotencyKey(),true);
    }
    /** Physical ID is obtained in the same Oracle round-trip; identity or approved trigger can supply it. */
    private BigDecimal insertSponsor(BigDecimal collateralId, BigDecimal partyId,
            String depositNumber, String branch, String actor) {
        return jdbc.execute((ConnectionCallback<BigDecimal>) conn -> {
            try (CallableStatement call = conn.prepareCall("""
                    BEGIN INSERT INTO CLT.COLLATERAL_SPONSOR
                      (COLLATERAL_ID, PARTY_ID, DEPOSIT_NUMBER, BRANCH_CODE, CREATED_BY)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING COLLATERAL_SPONSOR_ID INTO ?; END;
                    """)) {
                call.setBigDecimal(1,collateralId);
                call.setBigDecimal(2,partyId);
                call.setString(3,depositNumber);
                call.setString(4,branch);
                call.setString(5,actor);
                call.registerOutParameter(6, Types.NUMERIC);
                call.execute();
                return call.getBigDecimal(6);
            }
        });
    }
    public Receipt commit(String requestId, Command command, Authentication auth) {
        require(auth,"SCOPE_clt.collateral.read");
        require(auth,"SCOPE_clt.collateral.review");
        var who = introduction.actor(auth);
        if (command == null) throw new IllegalArgumentException("CLT_R3_SPONSOR_COMMIT_BODY_REQUIRED");
        CollateralSponsorCommitPolicy.version(command.expectedVersion());
        CollateralSponsorCommitPolicy.key(command.idempotencyKey());
        CollateralSponsorCommitPolicy.reason(command.reason());
        if (!featureFlag || !schemaReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R3_SPONSOR_COMMIT_DBA_RELEASE_REQUIRED");
        BigDecimal id = new BigDecimal(CollateralWorkflowPolicy.id(requestId));
        return transaction.execute(tx -> {
            Map<String,Object> stage=lockedStage(id,who.branch());
            BigDecimal collateralId = CollateralSponsorCommitPolicy.decimal(stage.get("COLLATERAL_ID"),
                    "CLT_R3_SPONSOR_COMMIT_COLLATERAL_ID_INVALID");
            lockParent(collateralId,who.branch());
            String hash=value(stage.get("PAYLOAD_HASH"));
            List<Map<String,Object>> existing = jdbc.queryForList("""
                    SELECT * FROM CLT.R3_POSTING_LEDGER
                     WHERE REQUEST_KIND='SPONSOR' AND REQUEST_ID=?
                    """,id);
            if (!existing.isEmpty()) return replay(existing,command,hash,who.subject());
            Long reused = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.R3_POSTING_LEDGER
                     WHERE BRANCH_CODE=? AND IDEMPOTENCY_KEY=?
                    """,Long.class,who.branch(),command.idempotencyKey());
            if (reused==null || reused!=0L) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_SPONSOR_COMMIT_IDEMPOTENCY_ALREADY_USED");
            Release release;
            try { release=approvedRelease(true); }
            catch (DataAccessException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "CLT_R3_SPONSOR_COMMIT_RELEASE_UNAVAILABLE");
            }
            var proof=preflight.inspect("sponsor",requestId,auth);
            var facts=new StageEvidence(value(stage.get("STATUS_CODE")),
                    value(stage.get("RECORD_VERSION")),proof.stageIntegrityVerified(),
                    proof.physicalConflictScreenPassed(),
                    proof.checks().stream().anyMatch(x -> !"PASS".equals(x.result())),
                    value(stage.get("MAKER_SUBJECT")),who.subject());
            try { CollateralSponsorCommitPolicy.guard(command,facts,release,featureFlag,true,
                    scope(auth,"SCOPE_clt.collateral.review")); }
            catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN,ex.getMessage()); }
            catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());
            }
            // Revalidation inside the same transaction: no trust in a previous browser preflight.
            workflow.validateForStageDecision(CollateralWorkflowPolicy.Kind.SPONSOR,
                    collateralId.toPlainString(),id,CollateralWorkflowPolicy.Action.REVIEW,auth);
            String digest;
            try { digest=CollateralStageFingerprint.digest(
                    CollateralWorkflowPolicy.Kind.SPONSOR, collateralId.toPlainString(),stage); }
            catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_SPONSOR_COMMIT_PAYLOAD_INVALID"); }
            if (!Objects.equals(hash,digest)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_R3_SPONSOR_COMMIT_PAYLOAD_CHANGED");
            jdbc.update("""
                    INSERT INTO CLT.R3_POSTING_LEDGER
                      (LEDGER_ID, REQUEST_KIND, REQUEST_ID, COLLATERAL_ID, BRANCH_CODE,
                       IDEMPOTENCY_KEY, REQUEST_HASH, EXPECTED_VERSION, JUSTIFICATION,
                       COMMIT_STATUS, POSTED_BY, POLICY_REVISION)
                    VALUES (CLT.SEQ_CLT_R3_POSTING_LEDGER.NEXTVAL, 'SPONSOR', ?, ?, ?,
                            ?, ?, 3, ?, 'PREPARED', ?, ?)
                    """,id,collateralId,who.branch(),command.idempotencyKey(),hash,
                    command.reason().trim(),who.subject(),release.revision());
            BigDecimal sponsorId=insertSponsor(collateralId,
                    CollateralSponsorCommitPolicy.decimal(stage.get("PARTY_ID"),
                            "CLT_R3_SPONSOR_COMMIT_PARTY_ID_INVALID"),
                    value(stage.get("DEPOSIT_NUMBER")),who.branch(),who.subject());
            if (sponsorId==null || sponsorId.signum()<=0)
                throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_DOMAIN_KEY_NOT_RETURNED");
            int updated=jdbc.update("""
                    UPDATE CLT.R3_POSTING_LEDGER
                       SET DOMAIN_RECORD_ID=?, COMMIT_STATUS='COMMITTED', POSTED_AT=SYSTIMESTAMP
                     WHERE REQUEST_KIND='SPONSOR' AND REQUEST_ID=?
                       AND BRANCH_CODE=? AND COMMIT_STATUS='PREPARED'
                    """,sponsorId,id,who.branch());
            if (updated!=1) throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_LEDGER_UPDATE_FAILED");
            return new Receipt(requestId,collateralId.toPlainString(),sponsorId.toPlainString(),
                    "COMMITTED",release.revision(),command.idempotencyKey(),false);
        });
    }
}
