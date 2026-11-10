package com.behsazan.corebanking.collateral.monitoring;

import com.behsazan.corebanking.collateral.monitoring.CollateralCoverageMonitoringPolicy.Snapshot;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

/** One Oracle statement per report page. No FX conversion, unknown domain joins or DML. */
@Repository
public class CollateralCoverageMonitoringRepository {
    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralCoverageMonitoringRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader = reader;
    }
    private static String num(BigDecimal v) {return v == null ? null : v.toPlainString();}
    private static String day(Date v) {return v == null ? null : v.toLocalDate().toString();}

    public List<Snapshot> page(String branch, int page, int size) {
        int safePage = CollateralCoverageMonitoringPolicy.page(page);
        int safeSize = CollateralCoverageMonitoringPolicy.size(size);
        return reader.getObject().jdbc().query("""
            SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.COLLATERAL_STATUS_CODE,
                   c.CURRENCY_CODE, c.CURRENT_VALUATION_HISTORY_ID,
                   v.VALUATION_STATUS_CODE, v.CURRENCY_CODE AS VALUATION_CURRENCY,
                   v.EFFECTIVE_FROM, v.EFFECTIVE_TO, v.ELIGIBLE_VALUE,
                   SUM(CASE WHEN a.CURRENCY_CODE = c.CURRENCY_CODE THEN a.ALLOCATE_AMOUNT ELSE 0 END)
                     AS SAME_CURRENCY_EXPOSURE,
                   SUM(CASE WHEN a.CURRENCY_CODE <> c.CURRENCY_CODE THEN 1 ELSE 0 END)
                     AS FOREIGN_CURRENCY_COUNT,
                   COUNT(*) AS ACTIVE_COUNT,
                   MAX(TO_CHAR(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE), 'YYYY-MM-DD')) AS AS_OF_DATE
              FROM CLT.COLLATERAL c
              JOIN CLT.COLLATERAL_ALLOCATION a
                ON a.COLLATERAL_ID = c.COLLATERAL_ID AND a.USED = 'YES'
              LEFT JOIN CLT.COLLATERAL_VALUATION_HISTORY v
                ON v.COLLATERAL_VALUATION_HISTORY_ID = c.CURRENT_VALUATION_HISTORY_ID
               AND v.COLLATERAL_ID = c.COLLATERAL_ID
             WHERE c.BRANCH_CODE = ?
             GROUP BY c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.COLLATERAL_STATUS_CODE,
                      c.CURRENCY_CODE, c.CURRENT_VALUATION_HISTORY_ID,
                      v.VALUATION_STATUS_CODE, v.CURRENCY_CODE,
                      v.EFFECTIVE_FROM, v.EFFECTIVE_TO, v.ELIGIBLE_VALUE
             ORDER BY c.COLLATERAL_ID DESC
             OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """, (rs,n) -> new Snapshot(
                num(rs.getBigDecimal("COLLATERAL_ID")), num(rs.getBigDecimal("COLLATERAL_NUMBER")),
                rs.getString("COLLATERAL_STATUS_CODE"),rs.getString("CURRENCY_CODE"),
                num(rs.getBigDecimal("CURRENT_VALUATION_HISTORY_ID")),rs.getString("VALUATION_STATUS_CODE"),
                rs.getString("VALUATION_CURRENCY"),day(rs.getDate("EFFECTIVE_FROM")),
                day(rs.getDate("EFFECTIVE_TO")),rs.getString("AS_OF_DATE"),
                rs.getBigDecimal("ELIGIBLE_VALUE"),rs.getBigDecimal("SAME_CURRENCY_EXPOSURE"),
                rs.getLong("FOREIGN_CURRENCY_COUNT"),rs.getLong("ACTIVE_COUNT")),
                CollateralRegistryPolicy.branch(branch), (long)safePage * safeSize, safeSize + 1);
    }
}
