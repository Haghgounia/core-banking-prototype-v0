package com.behsazan.corebanking.collateral.sponsor;

import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorModels.RequestPage;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorModels.RequestRecord;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** R3A-only staging persistence; no INSERT/UPDATE/DELETE on COLLATERAL_SPONSOR. */
@Repository
public class CollateralSponsorRequestRepository {
    private static final String COLUMNS = """
            SELECT SPONSOR_REQUEST_ID, COLLATERAL_ID, PARTY_ID, DEPOSIT_NUMBER,
                   JUSTIFICATION, STATUS_CODE, BRANCH_CODE, MAKER_SUBJECT,
                   CREATED_AT, RECORD_VERSION
              FROM CLT.COLLATERAL_SPONSOR_REQUEST
            """;
    private static final RowMapper<RequestRecord> MAPPER = (rs, ignored) -> row(rs);

    private static RequestRecord row(ResultSet rs) throws SQLException {
        return new RequestRecord(rs.getBigDecimal("SPONSOR_REQUEST_ID").toPlainString(),
                rs.getBigDecimal("COLLATERAL_ID").toPlainString(),
                rs.getBigDecimal("PARTY_ID").toPlainString(),
                rs.getString("DEPOSIT_NUMBER"), rs.getString("JUSTIFICATION"),
                rs.getString("STATUS_CODE"), rs.getString("BRANCH_CODE"),
                rs.getString("MAKER_SUBJECT"),
                rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(),
                rs.getBigDecimal("RECORD_VERSION").toPlainString(), "STAGING_ONLY");
    }
    public void requireSchema(CollateralSponsorWriterPool pool) {
        Long columns = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL_SPONSOR_REQUEST'
                   AND COLUMN_NAME IN ('SPONSOR_REQUEST_ID','COLLATERAL_ID','PARTY_ID',
                     'DEPOSIT_NUMBER','JUSTIFICATION','STATUS_CODE','BRANCH_CODE',
                     'MAKER_SUBJECT','IDEMPOTENCY_KEY','PAYLOAD_HASH','CREATED_AT','RECORD_VERSION')
                """, Long.class);
        Long events = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL_SPONSOR_REQUEST_EVENT'
                   AND COLUMN_NAME IN ('SPONSOR_REQUEST_EVENT_ID','SPONSOR_REQUEST_ID',
                     'EVENT_CODE','EVENT_ACTOR','CORRELATION_ID','SNAPSHOT_HASH','EVENT_AT')
                """, Long.class);
        if (columns == null || columns != 12 || events == null || events != 7)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_SPONSOR_STAGING_SCHEMA_NOT_READY");
    }
    public record Keyed(RequestRecord request, String hash) {}
    public Keyed byKey(CollateralSponsorWriterPool pool, String branch, String actor, String key) {
        var rows = pool.jdbc().query(COLUMNS.replace("FROM CLT.COLLATERAL_SPONSOR_REQUEST",
                ", PAYLOAD_HASH FROM CLT.COLLATERAL_SPONSOR_REQUEST")
                + " WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ? AND IDEMPOTENCY_KEY = ?",
                (rs, n) -> new Keyed(row(rs), rs.getString("PAYLOAD_HASH")), branch, actor, key);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public RequestRecord create(CollateralSponsorWriterPool pool,
            CollateralSponsorPolicy.Candidate value, String collateralId,
            String branch, String actor, String key, String hash, String trace) {
        BigDecimal id = pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_COLLATERAL_SPONSOR_REQUEST.NEXTVAL FROM DUAL", BigDecimal.class);
        if (id == null) throw new IllegalStateException("CLT_SPONSOR_SEQUENCE_UNAVAILABLE");
        pool.jdbc().update("""
                INSERT INTO CLT.COLLATERAL_SPONSOR_REQUEST
                (SPONSOR_REQUEST_ID, COLLATERAL_ID, PARTY_ID, DEPOSIT_NUMBER,
                 JUSTIFICATION, STATUS_CODE, BRANCH_CODE, MAKER_SUBJECT,
                 IDEMPOTENCY_KEY, PAYLOAD_HASH, CREATED_AT, RECORD_VERSION)
                VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, SYSTIMESTAMP, 1)
                """, id, new BigDecimal(collateralId), new BigDecimal(value.partyId()),
                value.depositNumber(), value.justification(), branch, actor, key, hash);
        pool.jdbc().update("""
                INSERT INTO CLT.COLLATERAL_SPONSOR_REQUEST_EVENT
                (SPONSOR_REQUEST_EVENT_ID, SPONSOR_REQUEST_ID, EVENT_CODE,
                 EVENT_ACTOR, CORRELATION_ID, SNAPSHOT_HASH, EVENT_AT)
                VALUES (CLT.SEQ_COLLATERAL_SPONSOR_REQUEST_EVENT.NEXTVAL,
                        ?, 'REQUEST_CREATED', ?, ?, ?, SYSTIMESTAMP)
                """, id, actor, trace, hash);
        List<RequestRecord> rows = pool.jdbc().query(COLUMNS + " WHERE SPONSOR_REQUEST_ID = ?", MAPPER, id);
        if (rows.size() != 1) throw new IllegalStateException("CLT_SPONSOR_REQUEST_READBACK_MISSING");
        return rows.getFirst();
    }
    public RequestPage mine(CollateralSponsorWriterPool pool, String branch, String actor,
            int page, int size) {
        Long total = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_SPONSOR_REQUEST
                 WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ?
                """, Long.class, branch, actor);
        List<RequestRecord> rows = pool.jdbc().query(COLUMNS + """
                WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ?
                ORDER BY SPONSOR_REQUEST_ID DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, MAPPER, branch, actor, (long)page*size, size);
        return new RequestPage(total == null ? 0 : total, page, size, rows);
    }
}
