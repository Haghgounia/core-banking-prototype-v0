package com.behsazan.corebanking.collateral.lifecycle;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Cross-domain R3 evidence: a single branch-scoped Oracle SELECT on the application's DataSource. */
@Service
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public final class CollateralLifecycleEvidenceService {
    private final JdbcTemplate jdbc;
    private final CollateralIntroductionService introduction;
    private final CollateralRegistryProperties registry;
    private final CollateralReferenceProperties reference;

    public CollateralLifecycleEvidenceService(DataSource applicationDataSource,
            CollateralIntroductionService introduction, CollateralRegistryProperties registry,
            CollateralReferenceProperties reference) {
        jdbc = new JdbcTemplate(applicationDataSource);
        jdbc.setQueryTimeout(10);
        this.introduction = introduction;
        this.registry = registry;
        this.reference = reference;
    }

    private static String decimal(BigDecimal number) {
        return number == null ? null : number.toPlainString();
    }
    private static boolean oracleYesNo(String value, String field) {
        if ("Y".equals(value)) return true;
        if ("N".equals(value)) return false;
        throw new IllegalStateException("CLT_R3_EVIDENCE_INVALID_ORACLE_FLAG_" + field);
    }
    private static LocalDate date(Date value) {
        return value == null ? null : value.toLocalDate();
    }
    private static CollateralLifecycleEvidencePolicy.Facts facts(ResultSet rs, int unused)
            throws SQLException {
        return new CollateralLifecycleEvidencePolicy.Facts(
                decimal(rs.getBigDecimal("COLLATERAL_ID")),
                decimal(rs.getBigDecimal("COLLATERAL_NUMBER")),
                rs.getString("COLLATERAL_STATUS_CODE"), rs.getString("TYPE_STATUS"),
                rs.getString("COLLATERAL_CURRENCY"),
                oracleYesNo(rs.getString("SPONSOR_REQUIRED"), "SPONSOR_REQUIRED"),
                oracleYesNo(rs.getString("INSURABLE"), "INSURABLE"), rs.getLong("SPONSOR_ROWS"),
                rs.getLong("INSURANCE_ROWS"), rs.getLong("VALUATION_ROWS"),
                decimal(rs.getBigDecimal("CURRENT_VALUATION_HISTORY_ID")),
                decimal(rs.getBigDecimal("CURRENT_ROW_ID")), rs.getString("CURRENT_STATUS_CODE"),
                rs.getString("CURRENT_CURRENCY"), date(rs.getDate("EFFECTIVE_FROM")),
                date(rs.getDate("EFFECTIVE_TO")), date(rs.getDate("AS_OF_DATE")));
    }

    public CollateralLifecycleEvidencePolicy.Evidence evidence(String collateralId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(
                    a -> "SCOPE_clt.collateral.read".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_R3_EVIDENCE_READ_SCOPE_REQUIRED");
        }
        var actor = introduction.actor(authentication);
        if (!registry.readsEnabled() || !reference.readsEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R3_EVIDENCE_READS_DISABLED");
        }
        BigDecimal id = CollateralRegistryPolicy.id(collateralId);
        String branch = CollateralRegistryPolicy.branch(actor.branch());
        // One Oracle statement gives a statement-level consistent view of recorded rows.
        // A count is NOT an approval, and a physical FINAL status is NOT an allocation decision.
        List<CollateralLifecycleEvidencePolicy.Facts> rows = jdbc.query("""
                SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.COLLATERAL_STATUS_CODE,
                       c.CURRENCY_CODE AS COLLATERAL_CURRENCY, t.STATUS AS TYPE_STATUS,
                       t.SPONSOR_REQUIRED, t.INSURABLE,
                       c.CURRENT_VALUATION_HISTORY_ID,
                       (SELECT COUNT(*) FROM CLT.COLLATERAL_SPONSOR s
                         WHERE s.COLLATERAL_ID = c.COLLATERAL_ID AND s.BRANCH_CODE = c.BRANCH_CODE)
                            AS SPONSOR_ROWS,
                       (SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE i
                         WHERE i.COLLATERAL_ID = c.COLLATERAL_ID AND i.BRANCH_CODE = c.BRANCH_CODE)
                            AS INSURANCE_ROWS,
                       (SELECT COUNT(*) FROM CLT.COLLATERAL_VALUATION_HISTORY vh
                         WHERE vh.COLLATERAL_ID = c.COLLATERAL_ID) AS VALUATION_ROWS,
                       v.COLLATERAL_VALUATION_HISTORY_ID AS CURRENT_ROW_ID,
                       v.VALUATION_STATUS_CODE AS CURRENT_STATUS_CODE,
                       v.CURRENCY_CODE AS CURRENT_CURRENCY, v.EFFECTIVE_FROM, v.EFFECTIVE_TO,
                       TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE)) AS AS_OF_DATE
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID = c.COLLATERAL_TYPE_ID
                  LEFT JOIN CLT.COLLATERAL_VALUATION_HISTORY v
                    ON v.COLLATERAL_VALUATION_HISTORY_ID = c.CURRENT_VALUATION_HISTORY_ID
                   AND v.COLLATERAL_ID = c.COLLATERAL_ID
                 WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
                """, CollateralLifecycleEvidenceService::facts, id, branch);
        if (rows.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "CLT_R3_EVIDENCE_COLLATERAL_NOT_FOUND_OR_FORBIDDEN");
        }
        return CollateralLifecycleEvidencePolicy.evaluate(rows.getFirst());
    }
}
