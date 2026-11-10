package com.behsazan.corebanking.collateral.reporting;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

/** BUC-16: bounded multi-domain history using only official CLT records; no synthetic events. */
@Repository
public class CollateralTimelineRepository {
    public record Event(String kind, String eventId, String occurredAt, String actor,
            String statusCode, String reference, String amount, String currencyCode) {}

    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralTimelineRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {this.reader = reader;}
    private static String num(BigDecimal v) {return v == null ? null : v.toPlainString();}
    private static String time(Timestamp v) {return v == null ? null : v.toLocalDateTime().toString();}

    public List<Event> events(String branch, String collateralId, int page, int size) {
        if (size < 1 || size > 100 || page < 0 || page > 100_000)
            throw new IllegalArgumentException("CLT_TIMELINE_PAGE_OR_SIZE_INVALID");
        return reader.getObject().jdbc().query("""
            WITH authorized AS (
                SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.CURRENCY_CODE,
                       c.CREATED_AT, c.CREATED_BY, c.COLLATERAL_STATUS_CODE
                  FROM CLT.COLLATERAL c
                 WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
            )
            SELECT e.EVENT_KIND, e.EVENT_ID, e.OCCURRED_AT, e.ACTOR,
                   e.STATUS_CODE, e.EVENT_REFERENCE, e.AMOUNT, e.CURRENCY_CODE
              FROM (
                    SELECT 'INTRODUCTION' AS EVENT_KIND, c.COLLATERAL_ID AS EVENT_ID,
                           c.CREATED_AT AS OCCURRED_AT, c.CREATED_BY AS ACTOR,
                           c.COLLATERAL_STATUS_CODE AS STATUS_CODE,
                           TO_CHAR(c.COLLATERAL_NUMBER) AS EVENT_REFERENCE,
                           CAST(NULL AS NUMBER(20,4)) AS AMOUNT, c.CURRENCY_CODE
                      FROM authorized c
                    UNION ALL
                    SELECT 'STATUS', h.COLLATERAL_STATUS_HISTORY_ID, h.CHANGED_AT,
                           h.CHANGED_BY, h.TO_STATUS_CODE,
                           NVL(h.FROM_STATUS_CODE,'INITIAL') || ' -> ' || h.TO_STATUS_CODE,
                           CAST(NULL AS NUMBER(20,4)), c.CURRENCY_CODE
                      FROM CLT.COLLATERAL_STATUS_HISTORY h
                      JOIN authorized c ON c.COLLATERAL_ID = h.COLLATERAL_ID
                    UNION ALL
                    SELECT 'VALUATION', v.COLLATERAL_VALUATION_HISTORY_ID,
                           NVL(v.FINALIZED_AT,v.CREATED_AT), NVL(v.FINALIZED_BY,v.CREATED_BY),
                           v.VALUATION_STATUS_CODE, v.VALUATION_TYPE_CODE,
                           v.ELIGIBLE_VALUE, v.CURRENCY_CODE
                      FROM CLT.COLLATERAL_VALUATION_HISTORY v
                      JOIN authorized c ON c.COLLATERAL_ID = v.COLLATERAL_ID
                    UNION ALL
                    SELECT 'INSURANCE', i.COLLATERAL_INSURANCE_ID, i.CREATED_AT,
                           i.CREATED_BY, CAST(NULL AS VARCHAR2(30 CHAR)),
                           i.INSURANCE_NUMBER, i.INSURANCE_AMOUNT, i.CURRENCY_CODE
                      FROM CLT.COLLATERAL_INSURANCE i
                      JOIN authorized c ON c.COLLATERAL_ID = i.COLLATERAL_ID
                    UNION ALL
                    SELECT 'SPONSOR', s.COLLATERAL_SPONSOR_ID, s.CREATED_AT,
                           s.CREATED_BY, CAST(NULL AS VARCHAR2(30 CHAR)),
                           TO_CHAR(s.PARTY_ID), CAST(NULL AS NUMBER(20,4)), c.CURRENCY_CODE
                      FROM CLT.COLLATERAL_SPONSOR s
                      JOIN authorized c ON c.COLLATERAL_ID = s.COLLATERAL_ID
                    UNION ALL
                    SELECT 'ALLOCATION', a.COLLATERAL_ALLOCATION_ID, a.CREATED_AT,
                           a.CREATED_BY, a.USED, TO_CHAR(a.MODULE_ID),
                           a.ALLOCATE_AMOUNT, a.CURRENCY_CODE
                      FROM CLT.COLLATERAL_ALLOCATION a
                      JOIN authorized c ON c.COLLATERAL_ID = a.COLLATERAL_ID
                    UNION ALL
                    SELECT 'ALLOCATION_RECORD_UPDATE', a.COLLATERAL_ALLOCATION_ID, a.UPDATED_AT,
                           a.UPDATED_BY, a.USED, TO_CHAR(a.MODULE_ID),
                           a.ALLOCATE_AMOUNT, a.CURRENCY_CODE
                      FROM CLT.COLLATERAL_ALLOCATION a
                      JOIN authorized c ON c.COLLATERAL_ID = a.COLLATERAL_ID
                     WHERE a.UPDATED_AT IS NOT NULL AND a.UPDATED_BY IS NOT NULL
                   ) e
             ORDER BY e.OCCURRED_AT DESC, e.EVENT_KIND, e.EVENT_ID DESC
             OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """, (rs,n) -> new Event(rs.getString("EVENT_KIND"),
                    num(rs.getBigDecimal("EVENT_ID")), time(rs.getTimestamp("OCCURRED_AT")),
                    rs.getString("ACTOR"),rs.getString("STATUS_CODE"),
                    rs.getString("EVENT_REFERENCE"),num(rs.getBigDecimal("AMOUNT")),
                    rs.getString("CURRENCY_CODE")),
                CollateralRegistryPolicy.id(collateralId),
                CollateralRegistryPolicy.branch(branch), (long)page * size, size + 1);
    }
}
