package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorModels;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorPolicy;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorProperties;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorService;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels;
import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceProperties;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceService;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationProperties;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationService;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy.Action;
import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy.Kind;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.server.ResponseStatusException;

/**
 * Branch-scoped Oracle workflow over existing R3 Stage requests.
 * REVIEWED means screened in Stage, never bank approval or domain registration.
 * SQL identifiers are supplied exclusively by a fixed enum (not client input).
 */
@Service
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public final class CollateralR3WorkflowService {
    public record Readiness(boolean stageTransitionsEnabled, boolean finalRegistrationEnabled,
            boolean independentReviewerAvailable, String stage) {}
    public record StageRow(String requestId, String collateralId, String status,
            String makerSubject, String recordVersion, String createdAt, String source) {}
    public record PendingPage(long totalElements, int page, int size, List<StageRow> items) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final CollateralIntroductionService introduction;
    private final CollateralSponsorService sponsor;
    private final CollateralInsuranceService insurance;
    private final CollateralValuationService valuation;
    private final CollateralSponsorProperties sponsorProperties;
    private final CollateralInsuranceProperties insuranceProperties;
    private final CollateralValuationProperties valuationProperties;

    public CollateralR3WorkflowService(DataSource applicationDataSource,
            CollateralIntroductionService introduction,
            CollateralSponsorService sponsor, CollateralInsuranceService insurance,
            CollateralValuationService valuation,
            CollateralSponsorProperties sponsorProperties,
            CollateralInsuranceProperties insuranceProperties,
            CollateralValuationProperties valuationProperties) {
        this.jdbc = new JdbcTemplate(applicationDataSource);
        this.jdbc.setQueryTimeout(10);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(applicationDataSource));
        this.transaction.setTimeout(20);
        this.introduction = introduction;
        this.sponsor = sponsor;
        this.insurance = insurance;
        this.valuation = valuation;
        this.sponsorProperties = sponsorProperties;
        this.insuranceProperties = insuranceProperties;
        this.valuationProperties = valuationProperties;
    }
    private static boolean authority(Authentication auth, String scope) {
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> scope.equals(a.getAuthority()));
    }
    private static void require(Authentication auth, String scope) {
        if (!authority(auth, scope))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_R3_WORKFLOW_SCOPE_REQUIRED");
    }
    private boolean enabled(Kind kind) {
        return switch (kind) {
            case SPONSOR -> sponsorProperties.requestWritesEnabled();
            case INSURANCE -> insuranceProperties.requestWritesEnabled();
            case VALUATION -> valuationProperties.requestWritesEnabled();
        };
    }
    private static String table(Kind kind) { return "CLT." + kind.table; }
    private static String eventTable(Kind kind) { return "CLT." + kind.eventTable; }
    private static String idColumn(Kind kind) { return kind.idColumn; }
    private static String n(Object number) {
        return number == null ? null : ((BigDecimal) number).stripTrailingZeros().toPlainString();
    }
    private static String s(Object text) { return text == null ? null : text.toString().trim(); }
    private static String day(Object date) {
        if (date == null) return null;
        if (date instanceof Date value) return value.toLocalDate().toString();
        if (date instanceof Timestamp value) return value.toLocalDateTime().toLocalDate().toString();
        if (date instanceof LocalDate value) return value.toString();
        if (date instanceof LocalDateTime value) return value.toLocalDate().toString();
        throw new IllegalArgumentException("CLT_R3_WORKFLOW_ORACLE_DATE_INVALID");
    }

    private static String statusConstraint(Kind kind) {
        return switch (kind) {
            case SPONSOR -> "CK_CLT_SPONSOR_REQ_STATUS";
            case INSURANCE -> "CK_CIR_STATUS";
            case VALUATION -> "CK_CLT_VAL_REQUEST_STATUS";
        };
    }
    private static String eventConstraint(Kind kind) {
        return switch (kind) {
            case SPONSOR -> "CK_CLT_SPONSOR_EVENT_CODE";
            case INSURANCE -> "CK_CIRE_CODE";
            case VALUATION -> "CK_CLT_VAL_REQ_EVENT_CODE";
        };
    }
    private boolean schemaReady(Kind kind) {
        if (!enabled(kind)) return false;
        try {
            Integer reasonColumn = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                     WHERE OWNER = 'CLT' AND TABLE_NAME = ? AND COLUMN_NAME = 'DECISION_REASON'
                    """, Integer.class, kind.eventTable);
            // The old candidate migrations allow only DRAFT/REQUEST_CREATED. Reject them
            // even when someone has manually added DECISION_REASON without changing checks.
            Integer statusCheck = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS
                     WHERE OWNER = 'CLT' AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?
                       AND STATUS = 'ENABLED' AND SEARCH_CONDITION_VC LIKE '%SUBMITTED%'
                       AND SEARCH_CONDITION_VC LIKE '%REVIEWED%'
                    """, Integer.class, kind.table, statusConstraint(kind));
            Integer eventCheck = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS
                     WHERE OWNER = 'CLT' AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?
                       AND STATUS = 'ENABLED' AND SEARCH_CONDITION_VC LIKE '%REQUEST_SUBMITTED%'
                       AND SEARCH_CONDITION_VC LIKE '%REQUEST_REVIEWED%'
                    """, Integer.class, kind.eventTable, eventConstraint(kind));
            return Integer.valueOf(1).equals(reasonColumn)
                    && Integer.valueOf(1).equals(statusCheck)
                    && Integer.valueOf(1).equals(eventCheck);
        } catch (RuntimeException ex) { return false; }
    }
    private void requireSchema(Kind kind) {
        if (!schemaReady(kind))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R3_WORKFLOW_APPROVED_STAGE_MIGRATION_REQUIRED");
    }
    public Readiness readiness(String kindName, Authentication auth) {
        require(auth, "SCOPE_clt.collateral.read");
        introduction.actor(auth);
        Kind kind = Kind.parse(kindName);
        boolean ready = schemaReady(kind);
        return new Readiness(ready, false, authority(auth, "SCOPE_clt.collateral.review"),
                ready ? "R3_STAGE_REVIEW_ONLY_NO_FINAL_DOMAIN_DML"
                        : "R3_WORKFLOW_MIGRATION_NOT_READY");
    }
    private StageRow row(Map<String,Object> data) {
        return new StageRow(n(data.get("REQUEST_ID")), n(data.get("COLLATERAL_ID")),
                s(data.get("STATUS_CODE")), s(data.get("MAKER_SUBJECT")),
                n(data.get("RECORD_VERSION")), s(data.get("CREATED_AT")), "STAGING_ONLY");
    }
    private String select(Kind kind) {
        return "SELECT " + kind.idColumn + " AS REQUEST_ID, COLLATERAL_ID, STATUS_CODE, "
                + "MAKER_SUBJECT, RECORD_VERSION, CREATED_AT, PAYLOAD_HASH FROM " + table(kind);
    }
    private Map<String,Object> locked(Kind kind, BigDecimal id, String branch) {
        List<Map<String,Object>> rows = jdbc.queryForList(select(kind) + " WHERE "
                + kind.idColumn + " = ? AND BRANCH_CODE = ? FOR UPDATE", id, branch);
        if (rows.size() != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CLT_R3_WORKFLOW_REQUEST_NOT_FOUND");
        return rows.getFirst();
    }
    private String payloadDigest(Kind kind, BigDecimal id, String collateralId) {
        // Recreate the identical normalized canonical payload from the Oracle Stage row,
        // not from browser-supplied fields, and compare with immutable creation evidence.
        Map<String,Object> source = jdbc.queryForMap("SELECT * FROM " + table(kind)
                + " WHERE " + kind.idColumn + " = ?", id);
        return switch (kind) {
            case SPONSOR -> {
                var candidate = CollateralSponsorPolicy.candidate(n(source.get("PARTY_ID")),
                        s(source.get("DEPOSIT_NUMBER")), s(source.get("JUSTIFICATION")));
                yield CollateralSponsorPolicy.digest(collateralId + "\u001f" + candidate.canonical());
            }
            case INSURANCE -> {
                var request = new CollateralInsuranceModels.CandidateRequest(
                        s(source.get("INSURER_CODE")), n(source.get("INSURANCE_COMPANY_ID")),
                        n(source.get("COLLATERAL_INSURANCE_TYPE_ID")), s(source.get("DEPOSIT_NUMBER")),
                        s(source.get("INSURANCE_NUMBER")), n(source.get("INSURANCE_AMOUNT")),
                        n(source.get("INSURANCE_RECEIVED_AMOUNT")), n(source.get("TAX_AMOUNT")),
                        day(source.get("VALID_FROM")), day(source.get("VALID_TO")),
                        n(source.get("PREVIOUS_INSURANCE_ID")), s(source.get("CURRENCY_CODE")),
                        s(source.get("JUSTIFICATION")));
                var candidate = CollateralInsurancePolicy.candidate(request);
                yield CollateralInsurancePolicy.digest(collateralId + "\u001f" + candidate.canonical());
            }
            case VALUATION -> {
                var request = new CollateralValuationModels.CandidateRequest(
                        s(source.get("VALUATION_TYPE_CODE")), s(source.get("VALUATION_METHOD_CODE")),
                        day(source.get("VALUATION_DATE")), n(source.get("VALUATION_AMOUNT")),
                        s(source.get("CURRENCY_CODE")), n(source.get("FORCED_SALE_VALUE")),
                        n(source.get("ELIGIBLE_VALUE")), n(source.get("HAIRCUT_PERCENT")),
                        n(source.get("VALUER_PARTY_ID")), s(source.get("VALUER_NAME")),
                        s(source.get("VALUATION_REFERENCE")), day(source.get("EFFECTIVE_FROM")),
                        day(source.get("EFFECTIVE_TO")), n(source.get("SUPERSEDES_VALUATION_HISTORY_ID")),
                        s(source.get("JUSTIFICATION")));
                var candidate = CollateralValuationPolicy.normalize(request);
                yield CollateralValuationPolicy.digest(collateralId + "\u001f" + candidate.canonical());
            }
        };
    }
    private void validateForStageDecision(Kind kind, String collateralId, BigDecimal id,
            Action action, Authentication auth) {
        // Positive Stage review must re-read current Oracle/CIF evidence, not trust submit-time checks.
        // The reviewer has its own scope; it must not need maker's introduce authority.
        boolean review = action == Action.REVIEW;
        Map<String,Object> source = jdbc.queryForMap("SELECT * FROM " + table(kind)
                + " WHERE " + kind.idColumn + " = ?", id);
        boolean valid = switch (kind) {
            case SPONSOR -> {
                var candidate = new CollateralSponsorModels.CandidateRequest(n(source.get("PARTY_ID")),
                        s(source.get("DEPOSIT_NUMBER")), s(source.get("JUSTIFICATION")));
                yield (review ? sponsor.screenForReview(collateralId, candidate, auth)
                        : sponsor.validate(collateralId, candidate, auth)).eligibleForRequest();
            }
            case INSURANCE -> {
                var candidate = new CollateralInsuranceModels.CandidateRequest(
                            s(source.get("INSURER_CODE")), n(source.get("INSURANCE_COMPANY_ID")),
                            n(source.get("COLLATERAL_INSURANCE_TYPE_ID")), s(source.get("DEPOSIT_NUMBER")),
                            s(source.get("INSURANCE_NUMBER")), n(source.get("INSURANCE_AMOUNT")),
                            n(source.get("INSURANCE_RECEIVED_AMOUNT")), n(source.get("TAX_AMOUNT")),
                            day(source.get("VALID_FROM")), day(source.get("VALID_TO")),
                            n(source.get("PREVIOUS_INSURANCE_ID")), s(source.get("CURRENCY_CODE")),
                            s(source.get("JUSTIFICATION")));
                yield (review ? insurance.screenForReview(collateralId, candidate, auth)
                        : insurance.validate(collateralId, candidate, auth)).eligibleForRequest();
            }
            case VALUATION -> {
                var candidate = new CollateralValuationModels.CandidateRequest(
                            s(source.get("VALUATION_TYPE_CODE")), s(source.get("VALUATION_METHOD_CODE")),
                            day(source.get("VALUATION_DATE")), n(source.get("VALUATION_AMOUNT")),
                            s(source.get("CURRENCY_CODE")), n(source.get("FORCED_SALE_VALUE")),
                            n(source.get("ELIGIBLE_VALUE")), n(source.get("HAIRCUT_PERCENT")),
                            n(source.get("VALUER_PARTY_ID")), s(source.get("VALUER_NAME")),
                            s(source.get("VALUATION_REFERENCE")), day(source.get("EFFECTIVE_FROM")),
                            day(source.get("EFFECTIVE_TO")), n(source.get("SUPERSEDES_VALUATION_HISTORY_ID")),
                            s(source.get("JUSTIFICATION")));
                yield (review ? valuation.screenForReview(collateralId, candidate, auth)
                        : valuation.validate(collateralId, candidate, auth)).eligibleForStagingRequest();
            }
        };
        if (!valid) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                review ? "CLT_R3_WORKFLOW_REVIEW_REVALIDATION_BLOCKED"
                        : "CLT_R3_WORKFLOW_SUBMISSION_REVALIDATION_BLOCKED");
    }
    public StageRow transition(String kindName, String requestId,
            CollateralR3WorkflowController.Transition body, String correlation,
            Authentication auth) {
        var who = introduction.actor(auth);
        Kind kind = Kind.parse(kindName);
        String normalizedId = CollateralR3WorkflowPolicy.id(requestId);
        if (body == null) throw new IllegalArgumentException("CLT_R3_WORKFLOW_BODY_REQUIRED");
        Action action;
        try { action = Action.valueOf(body.action()); }
        catch (RuntimeException e) { throw new IllegalArgumentException("CLT_R3_WORKFLOW_ACTION_INVALID", e); }
        BigDecimal version = CollateralR3WorkflowPolicy.version(body.expectedVersion());
        String reason = CollateralR3WorkflowPolicy.reason(body.reason());
        String trace = CollateralR3WorkflowPolicy.correlation(correlation);
        boolean reviewer = action == Action.REVIEW || action == Action.REJECT;
        require(auth, reviewer ? "SCOPE_clt.collateral.review" : "SCOPE_clt.collateral.introduce");
        requireSchema(kind);
        BigDecimal id = new BigDecimal(normalizedId);
        return transaction.execute(tx -> {
            Map<String,Object> saved = locked(kind, id, who.branch());
            BigDecimal actualVersion = (BigDecimal) saved.get("RECORD_VERSION");
            if (actualVersion.compareTo(version) != 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_WORKFLOW_STALE_RECORD_VERSION");
            boolean maker = who.subject().equals(saved.get("MAKER_SUBJECT"));
            final String next;
            try { next = CollateralR3WorkflowPolicy.next(s(saved.get("STATUS_CODE")), action,
                    maker, authority(auth, "SCOPE_clt.collateral.review")); }
            catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
            } catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
            }
            String collateralId = n(saved.get("COLLATERAL_ID"));
            String hash = s(saved.get("PAYLOAD_HASH"));
            if (hash == null || !hash.equals(payloadDigest(kind, id, collateralId)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_WORKFLOW_PAYLOAD_FINGERPRINT_CHANGED");
            Integer evidence = jdbc.queryForObject("SELECT COUNT(*) FROM " + eventTable(kind)
                    + " WHERE " + kind.idColumn + " = ? AND EVENT_CODE = 'REQUEST_CREATED' "
                    + "AND SNAPSHOT_HASH = ?", Integer.class, id, hash);
            if (!Integer.valueOf(1).equals(evidence))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_R3_WORKFLOW_CREATION_AUDIT_MISSING_OR_INVALID");
            if (CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(action))
                validateForStageDecision(kind, collateralId, id, action, auth);
            int updated = jdbc.update("UPDATE " + table(kind)
                    + " SET STATUS_CODE = ?, RECORD_VERSION = RECORD_VERSION + 1 WHERE "
                    + kind.idColumn + " = ? AND BRANCH_CODE = ? AND STATUS_CODE = ? AND RECORD_VERSION = ?",
                    next, id, who.branch(), saved.get("STATUS_CODE"), version);
            if (updated != 1)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_R3_WORKFLOW_CONCURRENT_CHANGE");
            jdbc.update("INSERT INTO " + eventTable(kind)
                    + " (" + kind.eventIdColumn + ", " + kind.idColumn
                    + ", EVENT_CODE, EVENT_ACTOR, CORRELATION_ID, SNAPSHOT_HASH, "
                    + "DECISION_REASON, EVENT_AT) VALUES (CLT." + kind.eventSequence
                    + ".NEXTVAL, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)",
                    id, CollateralR3WorkflowPolicy.event(action), who.subject(), trace, hash, reason);
            Map<String,Object> after = jdbc.queryForMap(select(kind) + " WHERE "
                    + kind.idColumn + " = ? AND BRANCH_CODE = ?", id, who.branch());
            return row(after);
        });
    }
    public PendingPage pending(String kindName, int page, int size, Authentication auth) {
        require(auth, "SCOPE_clt.collateral.review");
        var who = introduction.actor(auth);
        Kind kind = Kind.parse(kindName);
        CollateralRegistryPolicy.pagination(page, size);
        requireSchema(kind);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + table(kind)
                + " WHERE BRANCH_CODE = ? AND STATUS_CODE = 'SUBMITTED' AND MAKER_SUBJECT <> ?",
                Long.class, who.branch(), who.subject());
        List<StageRow> rows = jdbc.query(select(kind)
                        + " WHERE BRANCH_CODE = ? AND STATUS_CODE = 'SUBMITTED' "
                        + "AND MAKER_SUBJECT <> ? ORDER BY " + kind.idColumn
                        + " DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (rs, index) -> new StageRow(rs.getBigDecimal("REQUEST_ID").toPlainString(),
                        rs.getBigDecimal("COLLATERAL_ID").toPlainString(),
                        rs.getString("STATUS_CODE"), rs.getString("MAKER_SUBJECT"),
                        rs.getBigDecimal("RECORD_VERSION").toPlainString(),
                        rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(), "STAGING_ONLY"),
                who.branch(), who.subject(), (long) page * size, size);
        return new PendingPage(total == null ? 0L : total, page, size, rows);
    }
}
