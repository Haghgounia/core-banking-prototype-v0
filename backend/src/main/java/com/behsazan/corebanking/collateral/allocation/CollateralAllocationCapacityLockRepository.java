package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.AllocationBucket;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationCapacityLockPolicy.Snapshot;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/**
 * In the R4D optional probe ONLY: parent-row FOR UPDATE and ALL following evidence reads
 * use the SAME JdbcTemplate/DataSource/Oracle transaction. No DML, no financial changes.
 * This is NOT globally serializable until every operational allocation writer follows the lock.
 */
@Repository
public class CollateralAllocationCapacityLockRepository {
    private static String dec(BigDecimal value) {return CollateralCapacityPolicy.decimal(value);}
    private static String date(Date date) {return date==null?null:date.toLocalDate().toString();}
    private static boolean flag(String value) {return "Y".equals(value);}

    /** This must be the first financial SQL statement inside the bounded transaction. */
    public String lockParent(JdbcTemplate jdbc, String collateralId, String branch) {
        var locked=jdbc.query("""
                SELECT COLLATERAL_ID, RECORD_VERSION
                  FROM CLT.COLLATERAL
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=?
                   FOR UPDATE WAIT 1
                """, (rs,index) -> dec(rs.getBigDecimal("RECORD_VERSION")),
                new BigDecimal(collateralId),branch);
        if(locked.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R4D_COLLATERAL_NOT_FOUND_IN_AUTHORIZED_BRANCH");
        return locked.getFirst();
    }

    public Snapshot inspectUnderParentLock(JdbcTemplate jdbc, String collateralId,
            String branch, String requestId, String lockedVersion) {
        List<CollateralCapacityPolicy.Input> base=jdbc.query("""
                SELECT c.COLLATERAL_STATUS_CODE,c.CURRENCY_CODE,
                       c.VALID_FROM,c.VALID_TO,c.CURRENT_VALUATION_HISTORY_ID,
                       t.STATUS AS TYPE_STATUS,t.SPONSOR_REQUIRED,t.INSURABLE,t.EXCLUSIVE_USAGE,
                       s.IS_ACTIVE_STATUS_FLAG,
                       v.VALUATION_STATUS_CODE,v.CURRENCY_CODE AS VALUATION_CURRENCY,
                       v.EFFECTIVE_FROM,v.EFFECTIVE_TO,v.ELIGIBLE_VALUE,
                       TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE)) AS CALENDAR_DATE
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=c.COLLATERAL_TYPE_ID
                  JOIN CLT.REF_COLLATERAL_STATUS s
                    ON s.COLLATERAL_STATUS_CODE=c.COLLATERAL_STATUS_CODE
                  LEFT JOIN CLT.COLLATERAL_VALUATION_HISTORY v
                    ON v.COLLATERAL_VALUATION_HISTORY_ID=c.CURRENT_VALUATION_HISTORY_ID
                   AND v.COLLATERAL_ID=c.COLLATERAL_ID
                 WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=? AND c.RECORD_VERSION=?
                """, (rs,index)->new CollateralCapacityPolicy.Input(
                    rs.getString("COLLATERAL_STATUS_CODE"),flag(rs.getString("IS_ACTIVE_STATUS_FLAG")),
                    "ACTIVE".equals(rs.getString("TYPE_STATUS")),
                    flag(rs.getString("SPONSOR_REQUIRED")),flag(rs.getString("INSURABLE")),
                    flag(rs.getString("EXCLUSIVE_USAGE")),rs.getString("CURRENCY_CODE"),
                    dec(rs.getBigDecimal("CURRENT_VALUATION_HISTORY_ID")),
                    rs.getString("VALUATION_STATUS_CODE"),rs.getString("VALUATION_CURRENCY"),
                    date(rs.getDate("EFFECTIVE_FROM")),date(rs.getDate("EFFECTIVE_TO")),
                    date(rs.getDate("CALENDAR_DATE")),date(rs.getDate("VALID_FROM")),
                    date(rs.getDate("VALID_TO")),rs.getBigDecimal("ELIGIBLE_VALUE"),List.of()),
                new BigDecimal(collateralId),branch,new BigDecimal(lockedVersion));
        if(base.size()!=1)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R4D_PARENT_SNAPSHOT_RECONCILIATION_FAILED");
        var parent=base.getFirst();
        List<AllocationBucket> allocations=jdbc.query("""
                SELECT MODULE_ID,CURRENCY_CODE,COUNT(*) AS ACTIVE_COUNT,
                       SUM(ALLOCATE_AMOUNT) AS ACTIVE_AMOUNT
                  FROM CLT.COLLATERAL_ALLOCATION
                 WHERE COLLATERAL_ID=? AND USED='YES' AND BRANCH_CODE=?
                 GROUP BY MODULE_ID,CURRENCY_CODE
                 ORDER BY MODULE_ID,CURRENCY_CODE
                 FETCH FIRST 201 ROWS ONLY
                """,(rs,index)->new AllocationBucket(dec(rs.getBigDecimal("MODULE_ID")),
                rs.getString("CURRENCY_CODE"),rs.getLong("ACTIVE_COUNT"),
                dec(rs.getBigDecimal("ACTIVE_AMOUNT"))),new BigDecimal(collateralId),branch);
        if(allocations.size()>200)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R4D_ALLOCATION_BUCKET_LIMIT_EXCEEDED");
        var roster=jdbc.queryForMap("""
                SELECT COUNT(*) AS TOTAL,
                       COUNT(DISTINCT PARTY_ID) AS UNIQUE_COUNT,
                       SUM(CASE WHEN DEBTOR_ROLE='PRIMARY' THEN 1 ELSE 0 END) AS PRIMARY_COUNT
                  FROM CLT.CLT_ALLOCATION_REQUEST_DEBTOR
                 WHERE REQUEST_ID=?
                """,new BigDecimal(requestId));
        Integer overlap=jdbc.queryForObject("""
                SELECT COUNT(DISTINCT s.PARTY_ID)
                  FROM CLT.CLT_ALLOCATION_REQUEST_DEBTOR d
                  JOIN CLT.COLLATERAL_SPONSOR s ON s.PARTY_ID=d.PARTY_ID
                 WHERE d.REQUEST_ID=? AND s.COLLATERAL_ID=?
                """,Integer.class,new BigDecimal(requestId),new BigDecimal(collateralId));
        var input=new CollateralCapacityPolicy.Input(parent.collateralStatus(),
                parent.activeReferenceStatus(),parent.typeActive(),parent.sponsorRequired(),
                parent.insurable(),parent.exclusiveUsage(),parent.collateralCurrency(),
                parent.valuationId(),parent.valuationStatus(),parent.valuationCurrency(),
                parent.effectiveFrom(),parent.effectiveTo(),parent.asOfDate(),
                parent.collateralValidFrom(),parent.collateralValidTo(),
                parent.eligibleValue(),List.copyOf(allocations));
        return new Snapshot(input,lockedVersion,((Number)roster.get("TOTAL")).intValue(),
                ((Number)roster.get("UNIQUE_COUNT")).intValue(),
                roster.get("PRIMARY_COUNT")==null?0:((Number)roster.get("PRIMARY_COUNT")).intValue(),
                overlap==null?0:overlap);
    }
}
