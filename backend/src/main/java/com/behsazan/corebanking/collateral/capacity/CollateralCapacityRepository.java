package com.behsazan.corebanking.collateral.capacity;

import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.*;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/**
 * Whitelisted, bounded Oracle CLT reads; no dynamic SQL, DDL or DML.
 * Branch is server-verified Jwt scope. Allocation rows are grouped (no debtor/case reference leakage).
 * Multi-query evidence is NOT a serializable authorization snapshot.
 */
@Repository
public class CollateralCapacityRepository {
    public record Parent(String collateralId, String collateralNumber, String branchCode,
            String currencyCode, String collateralStatus, boolean statusReferenceActive,
            String collateralValidFrom, String collateralValidTo,
            String currentValuationId, String valuationStatus, String valuationCurrency,
            String valuationEffectiveFrom, String valuationEffectiveTo, BigDecimal eligibleValue,
            TypeParameters typeParameters) {}

    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralCapacityRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader = reader;
    }
    private JdbcTemplate jdbc() {return reader.getObject().jdbc();}
    private static String str(BigDecimal n) {return CollateralCapacityPolicy.decimal(n);}
    private static String date(Date value) {return value == null ? null : value.toLocalDate().toString();}
    private static boolean yes(String flag) {return "Y".equals(flag);}

    private static Parent parent(ResultSet rs, int unused) throws SQLException {
        TypeParameters p = new TypeParameters(rs.getString("COLLATERAL_TYPE_CODE"),
                rs.getString("TYPE_STATUS"),
                str(rs.getBigDecimal("MORTGAGE_PERCENT_LOAN")),
                str(rs.getBigDecimal("USAGE_PERCENT_LOAN")),
                str(rs.getBigDecimal("MORTGAGE_PERCENT_GUARANTEE")),
                str(rs.getBigDecimal("USAGE_PERCENT_GUARANTEE")),
                str(rs.getBigDecimal("MORTGAGE_PERCENT_LETTERCREDIT")),
                str(rs.getBigDecimal("USAGE_PERCENT_LETTERCREDIT")),
                str(rs.getBigDecimal("USAGE_PERCENT_GREENLICENSE")),
                str(rs.getBigDecimal("RESERVE_RATIO")), yes(rs.getString("SPONSOR_REQUIRED")),
                yes(rs.getString("INSURABLE")), yes(rs.getString("EXCLUSIVE_USAGE")),
                yes(rs.getString("USE_IN_OTHER_BRANCHES")));
        return new Parent(str(rs.getBigDecimal("COLLATERAL_ID")),
                str(rs.getBigDecimal("COLLATERAL_NUMBER")), rs.getString("BRANCH_CODE"),
                rs.getString("CURRENCY_CODE"), rs.getString("COLLATERAL_STATUS_CODE"),
                yes(rs.getString("IS_ACTIVE_STATUS_FLAG")),
                date(rs.getDate("VALID_FROM")), date(rs.getDate("VALID_TO")),
                str(rs.getBigDecimal("CURRENT_VALUATION_HISTORY_ID")),
                rs.getString("VALUATION_STATUS_CODE"), rs.getString("VALUATION_CURRENCY"),
                date(rs.getDate("EFFECTIVE_FROM")), date(rs.getDate("EFFECTIVE_TO")),
                rs.getBigDecimal("ELIGIBLE_VALUE"), p);
    }

    public Parent loadParent(String branch, String collateralId) {
        List<Parent> rows = jdbc().query("""
                SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.BRANCH_CODE,
                       c.CURRENCY_CODE, c.COLLATERAL_STATUS_CODE, c.VALID_FROM, c.VALID_TO,
                       c.CURRENT_VALUATION_HISTORY_ID, st.IS_ACTIVE_STATUS_FLAG,
                       t.COLLATERAL_TYPE_CODE, t.STATUS AS TYPE_STATUS,
                       t.MORTGAGE_PERCENT_LOAN, t.USAGE_PERCENT_LOAN,
                       t.MORTGAGE_PERCENT_GUARANTEE, t.USAGE_PERCENT_GUARANTEE,
                       t.MORTGAGE_PERCENT_LETTERCREDIT, t.USAGE_PERCENT_LETTERCREDIT,
                       t.USAGE_PERCENT_GREENLICENSE, t.RESERVE_RATIO,
                       t.SPONSOR_REQUIRED, t.INSURABLE, t.EXCLUSIVE_USAGE,
                       t.USE_IN_OTHER_BRANCHES,
                       v.VALUATION_STATUS_CODE, v.CURRENCY_CODE AS VALUATION_CURRENCY,
                       v.EFFECTIVE_FROM, v.EFFECTIVE_TO, v.ELIGIBLE_VALUE
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID = c.COLLATERAL_TYPE_ID
                  JOIN CLT.REF_COLLATERAL_STATUS st ON st.COLLATERAL_STATUS_CODE = c.COLLATERAL_STATUS_CODE
                  LEFT JOIN CLT.COLLATERAL_VALUATION_HISTORY v
                    ON v.COLLATERAL_VALUATION_HISTORY_ID = c.CURRENT_VALUATION_HISTORY_ID
                   AND v.COLLATERAL_ID = c.COLLATERAL_ID
                 WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
                """, CollateralCapacityRepository::parent,
                CollateralRegistryPolicy.id(collateralId), CollateralRegistryPolicy.branch(branch));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_CAPACITY_COLLATERAL_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }

    /** Used=NO is historic; only current USED=YES rows enter exposure sums. */
    public List<AllocationBucket> activeAllocations(Parent p) {
        List<AllocationBucket> rows = jdbc().query("""
                SELECT a.MODULE_ID, a.CURRENCY_CODE,
                       COUNT(*) AS ACTIVE_COUNT, SUM(a.ALLOCATE_AMOUNT) AS ACTIVE_AMOUNT
                  FROM CLT.COLLATERAL_ALLOCATION a
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = a.COLLATERAL_ID
                 WHERE a.COLLATERAL_ID = ? AND c.BRANCH_CODE = ? AND a.USED = 'YES'
                 GROUP BY a.MODULE_ID, a.CURRENCY_CODE
                 ORDER BY a.MODULE_ID, a.CURRENCY_CODE
                 FETCH FIRST 201 ROWS ONLY
                """, (rs, unused) -> new AllocationBucket(str(rs.getBigDecimal("MODULE_ID")),
                rs.getString("CURRENCY_CODE"), rs.getLong("ACTIVE_COUNT"),
                str(rs.getBigDecimal("ACTIVE_AMOUNT"))),
                new BigDecimal(p.collateralId()), p.branchCode());
        if (rows.size() > 200) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_CAPACITY_ALLOCATION_GROUP_LIMIT_REQUIRES_REVIEW");
        return rows;
    }

    /** Calendar evidence, explicitly not bank official business date. */
    public String oracleTehranDate() {
        Date value = jdbc().queryForObject("""
                SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE)) FROM DUAL
                """, Date.class);
        if (value == null) throw new IllegalStateException("CLT_CAPACITY_ORACLE_CLOCK_UNAVAILABLE");
        return value.toLocalDate().toString();
    }
}
