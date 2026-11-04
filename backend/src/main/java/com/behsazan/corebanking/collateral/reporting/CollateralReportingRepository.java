package com.behsazan.corebanking.collateral.reporting;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.reporting.CollateralReportingModels.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Fixed, read-only SQL. Every collateral-owned business read is scoped to JWT branch. */
@Repository
public class CollateralReportingRepository {
    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralReportingRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader = reader;
    }
    private JdbcTemplate db() { return reader.getObject().jdbc(); }
    private static String number(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
    private static String time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime().toString();
    }

    public List<StatusCount> statuses(String branch) {
        return db().query("""
                SELECT c.COLLATERAL_STATUS_CODE, COUNT(*) AS STATUS_COUNT
                  FROM CLT.COLLATERAL c
                 WHERE c.BRANCH_CODE = ?
                 GROUP BY c.COLLATERAL_STATUS_CODE
                 ORDER BY c.COLLATERAL_STATUS_CODE
                 FETCH FIRST 101 ROWS ONLY
                """, (rs,n) -> new StatusCount(rs.getString("COLLATERAL_STATUS_CODE"),
                        rs.getLong("STATUS_COUNT")), branch);
    }

    public List<ActiveExposure> exposures(String branch) {
        return db().query("""
                SELECT a.CURRENCY_CODE, COUNT(*) AS ACTIVE_COUNT,
                       SUM(a.ALLOCATE_AMOUNT) AS ACTIVE_AMOUNT
                  FROM CLT.COLLATERAL_ALLOCATION a
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = a.COLLATERAL_ID
                 WHERE c.BRANCH_CODE = ? AND a.USED = 'YES'
                 GROUP BY a.CURRENCY_CODE
                 ORDER BY a.CURRENCY_CODE
                 FETCH FIRST 101 ROWS ONLY
                """, (rs,n) -> new ActiveExposure(rs.getString("CURRENCY_CODE"),
                        rs.getLong("ACTIVE_COUNT"),
                        CollateralReportingPolicy.amount(rs.getBigDecimal("ACTIVE_AMOUNT"))), branch);
    }

    public List<StatusEvent> statusEvents(String branch, int limit) {
        return db().query("""
                SELECT h.COLLATERAL_STATUS_HISTORY_ID, c.COLLATERAL_NUMBER,
                       h.FROM_STATUS_CODE, h.TO_STATUS_CODE, h.CHANGED_AT
                  FROM CLT.COLLATERAL_STATUS_HISTORY h
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = h.COLLATERAL_ID
                 WHERE c.BRANCH_CODE = ?
                 ORDER BY h.CHANGED_AT DESC, h.COLLATERAL_STATUS_HISTORY_ID DESC
                 FETCH FIRST ? ROWS ONLY
                """, (rs,n) -> new StatusEvent(number(rs.getBigDecimal("COLLATERAL_STATUS_HISTORY_ID")),
                        number(rs.getBigDecimal("COLLATERAL_NUMBER")),
                        rs.getString("FROM_STATUS_CODE"), rs.getString("TO_STATUS_CODE"),
                        time(rs.getTimestamp("CHANGED_AT"))), branch,
                        CollateralReportingPolicy.eventLimit(limit));
    }

    /** The catalog is a CLT technical reference, not an HQ performance attestation. */
    public ReferenceSummary references() {
        Long types = db().queryForObject("SELECT COUNT(*) FROM CLT.COLLATERAL_TYPE WHERE STATUS = 'ACTIVE'", Long.class);
        Long groups = db().queryForObject("SELECT COUNT(*) FROM CLT.COLLATERAL_GROUP WHERE STATUS = 'ACTIVE'", Long.class);
        return new ReferenceSummary("ORACLE_CLT_REFERENCE", "GLOBAL_TECHNICAL_CATALOG",
                CollateralReportingPolicy.count(types), CollateralReportingPolicy.count(groups), false);
    }
}
