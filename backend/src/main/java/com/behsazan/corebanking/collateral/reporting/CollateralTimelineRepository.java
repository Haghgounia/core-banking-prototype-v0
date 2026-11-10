package com.behsazan.corebanking.collateral.reporting;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

/** BUC-16: bounded multi-domain history using only official CLT records; no synthetic events. */
@Repository
public class CollateralTimelineRepository {
    public record Event(String kind, String eventId, String occurredAt, String actor,
            String statusCode, String reference, String amount, String currencyCode) {}
    /** Explicit source coverage; absent Oracle history tables must not break basic BUC-16. */
    public record EventBatch(List<Event> events, boolean transactionIncluded, boolean statusHistoryIncluded) {}

    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralTimelineRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {this.reader = reader;}
    private static String num(BigDecimal v) {return v == null ? null : v.toPlainString();}
    private static String time(Timestamp v) {return v == null ? null : v.toLocalDateTime().toString();}

    private static boolean hasColumns(JdbcTemplate jdbc, String table, String... names) {
        String placeholders = String.join(",", java.util.Collections.nCopies(names.length,"?"));
        List<Object> args=new ArrayList<>();args.add(table);args.addAll(List.of(names));
        try {
            Integer count=jdbc.queryForObject("SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS "
                +"WHERE OWNER='CLT' AND TABLE_NAME=? AND COLUMN_NAME IN ("+placeholders+")",
                Integer.class,args.toArray());
            return count!=null && count==names.length;
        } catch(org.springframework.dao.DataAccessException ignored){return false;}
    }
    private static final String TRANSACTION_UNION="""
                    UNION ALL
                    SELECT 'TRANSACTION', t.COLLATERAL_TRANSACTION_ID, NVL(t.EFFECTIVE_AT,t.TRANSACTION_AT),
                           t.CREATED_BY, t.TRANSACTION_STATUS_CODE,
                           SUBSTR(t.COLLATERAL_TRANSACTION_NUMBER || ' / '
                             || t.TRANSACTION_TYPE_CODE || ' / ' || t.TRANSACTION_DIRECTION_CODE
                             || CASE WHEN t.ORIGINAL_TRANSACTION_ID IS NULL THEN ''
                                  ELSE ' / original='||TO_CHAR(t.ORIGINAL_TRANSACTION_ID) END,1,500),
                           t.TRANSACTION_AMOUNT,t.CURRENCY_CODE
                      FROM CLT.COLLATERAL_TRANSACTION t
                      JOIN authorized c ON c.COLLATERAL_ID=t.COLLATERAL_ID
            """;
    private static final String TRANSACTION_STATUS_UNION="""
                    UNION ALL
                    SELECT 'TRANSACTION_STATUS',h.COLLATERAL_TXN_STATUS_HIST_ID,h.CHANGED_AT,
                           h.CHANGED_BY,h.TO_STATUS_CODE,
                           'txn='||TO_CHAR(h.COLLATERAL_TRANSACTION_ID)||' / '
                             ||NVL(h.FROM_STATUS_CODE,'INITIAL')||' -> '||h.TO_STATUS_CODE,
                           CAST(NULL AS NUMBER(20,4)),t.CURRENCY_CODE
                      FROM CLT.COLLATERAL_TXN_STATUS_HISTORY h
                      JOIN CLT.COLLATERAL_TRANSACTION t
                        ON t.COLLATERAL_TRANSACTION_ID=h.COLLATERAL_TRANSACTION_ID
                      JOIN authorized c ON c.COLLATERAL_ID=t.COLLATERAL_ID
            """;
    public EventBatch events(String branch, String collateralId, int page, int size) {
        if (size < 1 || size > 100 || page < 0 || page > 100_000)
            throw new IllegalArgumentException("CLT_TIMELINE_PAGE_OR_SIZE_INVALID");
        JdbcTemplate jdbc=reader.getObject().jdbc();
        boolean transactions=hasColumns(jdbc,"COLLATERAL_TRANSACTION",
                "COLLATERAL_TRANSACTION_ID","COLLATERAL_ID","EFFECTIVE_AT","TRANSACTION_AT",
                "CREATED_BY","TRANSACTION_STATUS_CODE","COLLATERAL_TRANSACTION_NUMBER",
                "TRANSACTION_TYPE_CODE","TRANSACTION_DIRECTION_CODE","ORIGINAL_TRANSACTION_ID",
                "TRANSACTION_AMOUNT","CURRENCY_CODE");
        boolean statuses=transactions && hasColumns(jdbc,"COLLATERAL_TXN_STATUS_HISTORY",
                "COLLATERAL_TXN_STATUS_HIST_ID","COLLATERAL_TRANSACTION_ID","CHANGED_AT",
                "CHANGED_BY","TO_STATUS_CODE","FROM_STATUS_CODE");
        String query="""
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
            """+(transactions?TRANSACTION_UNION:"")+(statuses?TRANSACTION_STATUS_UNION:"")+"""
                   ) e
             ORDER BY e.OCCURRED_AT DESC, e.EVENT_KIND, e.EVENT_ID DESC
             OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """;
        List<Event> rows=jdbc.query(query, (rs,n) -> new Event(rs.getString("EVENT_KIND"),
                    num(rs.getBigDecimal("EVENT_ID")), time(rs.getTimestamp("OCCURRED_AT")),
                    rs.getString("ACTOR"),rs.getString("STATUS_CODE"),
                    rs.getString("EVENT_REFERENCE"),num(rs.getBigDecimal("AMOUNT")),
                    rs.getString("CURRENCY_CODE")),
                CollateralRegistryPolicy.id(collateralId),
                CollateralRegistryPolicy.branch(branch), (long)page * size, size + 1);
        return new EventBatch(rows,transactions,statuses);
    }
}
