package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Kind;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Only a dry-run read of reviewed Stage plus physical conflict evidence; never writes to CLT. */
@Service
@ConditionalOnProperty(prefix="core-banking.collateral.prototype", name="enabled", havingValue="true")
public final class CollateralPostingReadinessService {
    private final JdbcTemplate jdbc;
    private final CollateralIntroductionService introduction;
    private final CollateralRegistryProperties registry;
    private final CollateralReferenceProperties reference;

    public CollateralPostingReadinessService(DataSource sharedDataSource,
            CollateralIntroductionService introduction, CollateralRegistryProperties registry,
            CollateralReferenceProperties reference) {
        this.jdbc = new JdbcTemplate(sharedDataSource);
        this.jdbc.setQueryTimeout(10);
        this.introduction = introduction;
        this.registry = registry;
        this.reference = reference;
    }
    private static String dec(Object value) {
        if (value == null) return null;
        return value instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString() : value.toString();
    }
    private static long count(Object value) {
        if (!(value instanceof Number number))
            throw new IllegalStateException("CLT_R3_POSTING_MISSING_ORACLE_COUNT");
        return number.longValue();
    }
    private static boolean hasScope(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream().anyMatch(
                a -> "SCOPE_clt.collateral.read".equals(a.getAuthority()));
    }
    private static String conflicts(Kind kind) {
        return switch (kind) {
            case SPONSOR -> """
                (SELECT COUNT(*) FROM CLT.COLLATERAL_SPONSOR p
                  WHERE p.COLLATERAL_ID=r.COLLATERAL_ID AND p.BRANCH_CODE=r.BRANCH_CODE
                    AND p.PARTY_ID=r.PARTY_ID)
                """;
            case INSURANCE -> """
                (SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE p
                  WHERE p.INSURANCE_COMPANY_ID=r.INSURANCE_COMPANY_ID
                    AND p.INSURANCE_NUMBER=r.INSURANCE_NUMBER)
                """;
            case VALUATION -> """
                (SELECT COUNT(*) FROM CLT.COLLATERAL_VALUATION_HISTORY p
                  WHERE p.COLLATERAL_ID=r.COLLATERAL_ID AND r.VALUATION_REFERENCE IS NOT NULL
                    AND p.VALUATION_REFERENCE=r.VALUATION_REFERENCE)
                """;
        };
    }
    // Fixed enum-supplied SQL identifiers; no client-controlled table/column name interpolation.
    static String sql(Kind kind) {
        String event = "CLT." + kind.eventTable;
        return """
            SELECT r.*, c.COLLATERAL_STATUS_CODE AS LIVE_COLLATERAL_STATUS,
                   t.STATUS AS LIVE_TYPE_STATUS,
                   c.CURRENT_VALUATION_HISTORY_ID AS LIVE_CURRENT_VALUATION_ID,
                   (SELECT COUNT(*) FROM %s e WHERE e.%s = r.%s
                      AND e.EVENT_CODE='REQUEST_CREATED' AND e.EVENT_ACTOR=r.MAKER_SUBJECT
                      AND e.SNAPSHOT_HASH=r.PAYLOAD_HASH) AS CREATED_AUDIT_COUNT,
                   (SELECT COUNT(*) FROM %s e WHERE e.%s = r.%s
                      AND e.EVENT_CODE='REQUEST_SUBMITTED' AND e.EVENT_ACTOR=r.MAKER_SUBJECT
                      AND LENGTH(TRIM(e.DECISION_REASON)) BETWEEN 8 AND 500
                      AND e.SNAPSHOT_HASH=r.PAYLOAD_HASH) AS SUBMITTED_AUDIT_COUNT,
                   (SELECT COUNT(*) FROM %s e WHERE e.%s = r.%s
                      AND e.EVENT_CODE='REQUEST_REVIEWED' AND e.EVENT_ACTOR<>r.MAKER_SUBJECT
                      AND LENGTH(TRIM(e.DECISION_REASON)) BETWEEN 8 AND 500
                      AND e.SNAPSHOT_HASH=r.PAYLOAD_HASH) AS INDEPENDENT_REVIEW_AUDIT_COUNT,
                   (SELECT COUNT(*) FROM %s e WHERE e.%s = r.%s) AS TOTAL_AUDIT_COUNT,
                   %s AS PHYSICAL_CONFLICT_COUNT
              FROM CLT.%s r
              JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID=r.COLLATERAL_ID
                   AND c.BRANCH_CODE=r.BRANCH_CODE
              JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=c.COLLATERAL_TYPE_ID
             WHERE r.%s = ? AND r.BRANCH_CODE = ?
            """.formatted(event,kind.idColumn,kind.idColumn,
                    event,kind.idColumn,kind.idColumn,
                    event,kind.idColumn,kind.idColumn,
                    event,kind.idColumn,kind.idColumn,
                    conflicts(kind),kind.table,kind.idColumn);
    }
    public CollateralPostingReadinessPolicy.Readiness inspect(String kindName,
            String requestId, Authentication authentication) {
        if (!hasScope(authentication))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_R3_POSTING_READ_SCOPE_REQUIRED");
        var actor = introduction.actor(authentication);
        if (!registry.readsEnabled() || !reference.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_R3_POSTING_READS_DISABLED");
        Kind kind = Kind.parse(kindName);
        BigDecimal requestKey = new BigDecimal(CollateralWorkflowPolicy.id(requestId));
        String branch = CollateralRegistryPolicy.branch(actor.branch());
        List<Map<String,Object>> rows;
        try {
            rows = jdbc.queryForList(sql(kind), requestKey, branch);
        } catch (DataAccessException e) {
            // Absent/unapproved Stage migration is an expected blocker, never leak SQL or identifiers.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R3_POSTING_STAGE_SCHEMA_OR_READ_UNAVAILABLE");
        }
        if (rows.size() != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "CLT_R3_POSTING_REQUEST_NOT_FOUND_OR_FORBIDDEN");
        Map<String,Object> row = rows.getFirst();
        String collateralId = dec(row.get("COLLATERAL_ID"));
        String hash = (String)row.get("PAYLOAD_HASH");
        String calculated;
        try {
            calculated = CollateralStageFingerprint.digest(kind,collateralId,row);
        } catch (RuntimeException e) {
            // Invalid/mutated Stage payload may not be renderable as a candidate; fail closed.
            calculated = null;
        }
        var facts = new CollateralPostingReadinessPolicy.Facts(
                kind,dec(row.get("REQUEST_ID")) == null ? dec(row.get(kind.idColumn)) : dec(row.get("REQUEST_ID")),
                collateralId, (String)row.get("STATUS_CODE"), dec(row.get("RECORD_VERSION")),
                hash,calculated, count(row.get("CREATED_AUDIT_COUNT")),
                count(row.get("SUBMITTED_AUDIT_COUNT")),
                count(row.get("INDEPENDENT_REVIEW_AUDIT_COUNT")),
                count(row.get("TOTAL_AUDIT_COUNT")),count(row.get("PHYSICAL_CONFLICT_COUNT")),
                (String)row.get("LIVE_COLLATERAL_STATUS"), (String)row.get("LIVE_TYPE_STATUS"),
                dec(row.get("LIVE_CURRENT_VALUATION_ID")),
                kind == Kind.VALUATION ? dec(row.get("SUPERSEDES_VALUATION_HISTORY_ID")) : null,
                kind != Kind.VALUATION || row.get("VALUATION_REFERENCE") != null
                        && !row.get("VALUATION_REFERENCE").toString().isBlank());
        return CollateralPostingReadinessPolicy.evaluate(facts);
    }
}
