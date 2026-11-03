package com.behsazan.corebanking.collateral.valuation;

import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels.RequestPage;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels.RequestRecord;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Optional staging-only request and append-only event; NO base history/pointer mutation. */
@Repository
public class CollateralValuationRequestRepository {
    public record Keyed(RequestRecord request, String hash) {}
    private static final String SELECT = """
            SELECT VALUATION_REQUEST_ID, COLLATERAL_ID, VALUATION_TYPE_CODE,
                   VALUATION_METHOD_CODE, VALUATION_DATE, VALUATION_AMOUNT,
                   CURRENCY_CODE, SUPERSEDES_VALUATION_HISTORY_ID, STATUS_CODE,
                   BRANCH_CODE, MAKER_SUBJECT, CREATED_AT, RECORD_VERSION
              FROM CLT.COLLATERAL_VALUATION_REQUEST
            """;
    private static RequestRecord row(ResultSet rs, int n) throws SQLException {
        return new RequestRecord(rs.getBigDecimal(1).toPlainString(),
                rs.getBigDecimal(2).toPlainString(), rs.getString(3), rs.getString(4),
                rs.getDate(5).toLocalDate().toString(), rs.getBigDecimal(6).toPlainString(),
                rs.getString(7), rs.getBigDecimal(8) == null ? null : rs.getBigDecimal(8).toPlainString(),
                rs.getString(9), rs.getString(10), rs.getString(11),
                rs.getTimestamp(12).toLocalDateTime().toString(),
                rs.getBigDecimal(13).toPlainString(), "STAGING_ONLY");
    }
    public void requireSchema(CollateralValuationWriterPool pool) {
        Long requestCols = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL_VALUATION_REQUEST'
                   AND COLUMN_NAME IN ('VALUATION_REQUEST_ID','COLLATERAL_ID',
                   'VALUATION_TYPE_CODE','VALUATION_METHOD_CODE','VALUATION_DATE',
                   'VALUATION_AMOUNT','CURRENCY_CODE','FORCED_SALE_VALUE','ELIGIBLE_VALUE',
                   'HAIRCUT_PERCENT','VALUER_PARTY_ID','VALUER_NAME','VALUATION_REFERENCE',
                   'EFFECTIVE_FROM','EFFECTIVE_TO','SUPERSEDES_VALUATION_HISTORY_ID',
                   'JUSTIFICATION','STATUS_CODE','BRANCH_CODE','MAKER_SUBJECT',
                   'IDEMPOTENCY_KEY','PAYLOAD_HASH','CREATED_AT','RECORD_VERSION')
                """, Long.class);
        Long eventCols = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL_VALUATION_REQUEST_EVENT'
                   AND COLUMN_NAME IN ('VALUATION_REQUEST_EVENT_ID','VALUATION_REQUEST_ID',
                   'EVENT_CODE','EVENT_ACTOR','CORRELATION_ID','SNAPSHOT_HASH','EVENT_AT')
                """, Long.class);
        if (requestCols == null || requestCols != 24 || eventCols == null || eventCols != 7)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_VALUATION_STAGING_SCHEMA_NOT_READY");
    }
    public Keyed byKey(CollateralValuationWriterPool pool, String branch, String maker, String key) {
        List<Keyed> found = pool.jdbc().query(SELECT.replace("FROM CLT.COLLATERAL_VALUATION_REQUEST",
                ", PAYLOAD_HASH FROM CLT.COLLATERAL_VALUATION_REQUEST") +
                " WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ? AND IDEMPOTENCY_KEY = ?",
                (rs,n) -> new Keyed(row(rs,n), rs.getString("PAYLOAD_HASH")), branch, maker, key);
        return found.isEmpty() ? null : found.getFirst();
    }
    public RequestRecord create(CollateralValuationWriterPool pool,
            CollateralValuationPolicy.Candidate c, CollateralValuationRepository.Parent parent,
            String branch, String actor, String key, String hash, String correlation) {
        BigDecimal id = pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_COLLATERAL_VALUATION_REQUEST.NEXTVAL FROM DUAL", BigDecimal.class);
        if (id == null) throw new IllegalStateException("CLT_VALUATION_SEQUENCE_UNAVAILABLE");
        pool.jdbc().update("""
                INSERT INTO CLT.COLLATERAL_VALUATION_REQUEST
                (VALUATION_REQUEST_ID, COLLATERAL_ID, VALUATION_TYPE_CODE, VALUATION_METHOD_CODE,
                 VALUATION_DATE, VALUATION_AMOUNT, CURRENCY_CODE, FORCED_SALE_VALUE,
                 ELIGIBLE_VALUE, HAIRCUT_PERCENT, VALUER_PARTY_ID, VALUER_NAME,
                 VALUATION_REFERENCE, EFFECTIVE_FROM, EFFECTIVE_TO,
                 SUPERSEDES_VALUATION_HISTORY_ID, JUSTIFICATION, STATUS_CODE,
                 BRANCH_CODE, MAKER_SUBJECT, IDEMPOTENCY_KEY, PAYLOAD_HASH,
                 CREATED_AT, RECORD_VERSION)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        'DRAFT', ?, ?, ?, ?, SYSTIMESTAMP, 1)
                """, id, new BigDecimal(parent.collateralId()), c.typeCode(), c.methodCode(),
                Date.valueOf(c.valuationDate()), new BigDecimal(c.valuationAmount()), c.currencyCode(),
                c.forcedSaleValue() == null ? null : new BigDecimal(c.forcedSaleValue()),
                c.eligibleValue() == null ? null : new BigDecimal(c.eligibleValue()),
                c.haircutPercent() == null ? null : new BigDecimal(c.haircutPercent()),
                c.valuerPartyId() == null ? null : new BigDecimal(c.valuerPartyId()),
                c.valuerName(), c.valuationReference(), Date.valueOf(c.effectiveFrom()),
                c.effectiveTo() == null ? null : Date.valueOf(c.effectiveTo()),
                c.supersedesValuationId() == null ? null : new BigDecimal(c.supersedesValuationId()),
                c.justification(), branch, actor, key, hash);
        pool.jdbc().update("""
                INSERT INTO CLT.COLLATERAL_VALUATION_REQUEST_EVENT
                (VALUATION_REQUEST_EVENT_ID, VALUATION_REQUEST_ID, EVENT_CODE,
                 EVENT_ACTOR, CORRELATION_ID, SNAPSHOT_HASH, EVENT_AT)
                VALUES (CLT.SEQ_COLLATERAL_VALUATION_REQUEST_EVENT.NEXTVAL,
                        ?, 'REQUEST_CREATED', ?, ?, ?, SYSTIMESTAMP)
                """, id, actor, correlation, hash);
        List<RequestRecord> results = pool.jdbc().query(SELECT + " WHERE VALUATION_REQUEST_ID = ?",
                CollateralValuationRequestRepository::row, id);
        if (results.size() != 1) throw new IllegalStateException("CLT_VALUATION_STAGING_READBACK_MISSING");
        return results.getFirst();
    }
    public RequestPage mine(CollateralValuationWriterPool pool, String branch, String maker,
            int page, int size) {
        Long total = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_VALUATION_REQUEST
                 WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ?
                """, Long.class, branch, maker);
        List<RequestRecord> rows = pool.jdbc().query(SELECT + """
                WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ?
                ORDER BY VALUATION_REQUEST_ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, CollateralValuationRequestRepository::row, branch, maker, (long)page*size, size);
        return new RequestPage(total == null ? 0 : total, page, size, rows);
    }
}
