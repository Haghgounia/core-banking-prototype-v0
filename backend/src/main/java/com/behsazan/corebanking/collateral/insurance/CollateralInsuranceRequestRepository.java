package com.behsazan.corebanking.collateral.insurance;

import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels.RequestPage;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels.RequestRecord;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Staging-only persistent request + immutable event, never writes base COLLATERAL_INSURANCE. */
@Repository
public class CollateralInsuranceRequestRepository {
    public record Keyed(RequestRecord request, String hash) {}
    private static final String SELECT = """
            SELECT INSURANCE_REQUEST_ID, COLLATERAL_ID, PARTY_ID, INSURANCE_NUMBER,
                   COLLATERAL_INSURANCE_TYPE_ID, INSURANCE_COMPANY_ID, VALID_FROM, VALID_TO,
                   STATUS_CODE, BRANCH_CODE, MAKER_SUBJECT, CREATED_AT, RECORD_VERSION
              FROM CLT.COLLATERAL_INSURANCE_REQUEST
            """;
    private static RequestRecord row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RequestRecord(rs.getBigDecimal("INSURANCE_REQUEST_ID").toPlainString(),
                rs.getBigDecimal("COLLATERAL_ID").toPlainString(),
                rs.getBigDecimal("PARTY_ID").toPlainString(),
                rs.getString("INSURANCE_NUMBER"),
                rs.getBigDecimal("COLLATERAL_INSURANCE_TYPE_ID").toPlainString(),
                rs.getBigDecimal("INSURANCE_COMPANY_ID").toPlainString(),
                rs.getDate("VALID_FROM").toLocalDate().toString(),
                rs.getDate("VALID_TO").toLocalDate().toString(), rs.getString("STATUS_CODE"),
                rs.getString("BRANCH_CODE"), rs.getString("MAKER_SUBJECT"),
                rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(),
                rs.getBigDecimal("RECORD_VERSION").toPlainString(), "STAGING_ONLY");
    }
    public void requireSchema(CollateralInsuranceWriterPool pool) {
        Long requestCols = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL_INSURANCE_REQUEST'
                   AND COLUMN_NAME IN ('INSURANCE_REQUEST_ID','COLLATERAL_ID','PARTY_ID',
                   'INSURER_CODE','INSURANCE_COMPANY_ID','COLLATERAL_INSURANCE_TYPE_ID',
                   'DEPOSIT_NUMBER','INSURANCE_NUMBER','INSURANCE_AMOUNT',
                   'INSURANCE_RECEIVED_AMOUNT','TAX_AMOUNT','VALID_FROM','VALID_TO',
                   'PREVIOUS_INSURANCE_ID','CURRENCY_CODE','JUSTIFICATION',
                   'STATUS_CODE','BRANCH_CODE','MAKER_SUBJECT','IDEMPOTENCY_KEY',
                   'PAYLOAD_HASH','CREATED_AT','RECORD_VERSION')
                """, Long.class);
        Long eventCols = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL_INSURANCE_REQUEST_EVENT'
                   AND COLUMN_NAME IN ('INSURANCE_REQUEST_EVENT_ID','INSURANCE_REQUEST_ID',
                   'EVENT_CODE','EVENT_ACTOR','CORRELATION_ID','SNAPSHOT_HASH','EVENT_AT')
                """, Long.class);
        if (requestCols == null || requestCols != 23 || eventCols == null || eventCols != 7)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_INSURANCE_STAGING_SCHEMA_NOT_READY");
    }
    public Keyed byKey(CollateralInsuranceWriterPool pool, String branch, String subject, String key) {
        var rows = pool.jdbc().query(SELECT.replace("FROM CLT.COLLATERAL_INSURANCE_REQUEST",
                ", PAYLOAD_HASH FROM CLT.COLLATERAL_INSURANCE_REQUEST")
                + " WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ? AND IDEMPOTENCY_KEY = ?",
                (rs,n) -> new Keyed(row(rs), rs.getString("PAYLOAD_HASH")), branch, subject, key);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public RequestRecord create(CollateralInsuranceWriterPool pool,
            CollateralInsurancePolicy.Candidate value, CollateralInsuranceRepository.Parent parent,
            String branch, String actor, String idem, String hash, String correlation) {
        BigDecimal id = pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_COLLATERAL_INSURANCE_REQUEST.NEXTVAL FROM DUAL", BigDecimal.class);
        if (id == null) throw new IllegalStateException("CLT_INSURANCE_SEQUENCE_UNAVAILABLE");
        pool.jdbc().update("""
                INSERT INTO CLT.COLLATERAL_INSURANCE_REQUEST
                (INSURANCE_REQUEST_ID, COLLATERAL_ID, PARTY_ID, INSURER_CODE,
                 INSURANCE_COMPANY_ID, COLLATERAL_INSURANCE_TYPE_ID, DEPOSIT_NUMBER,
                 INSURANCE_NUMBER, INSURANCE_AMOUNT, INSURANCE_RECEIVED_AMOUNT, TAX_AMOUNT,
                 VALID_FROM, VALID_TO, PREVIOUS_INSURANCE_ID, CURRENCY_CODE, JUSTIFICATION,
                 STATUS_CODE, BRANCH_CODE, MAKER_SUBJECT, IDEMPOTENCY_KEY,
                 PAYLOAD_HASH, CREATED_AT, RECORD_VERSION)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?,
                        SYSTIMESTAMP, 1)
                """, id, new BigDecimal(parent.collateralId()), new BigDecimal(parent.ownerPartyId()),
                value.insurerCode(), new BigDecimal(value.companyId()),
                new BigDecimal(value.insuranceTypeId()), value.depositNumber(),
                value.insuranceNumber(), new BigDecimal(value.insuranceAmount()),
                new BigDecimal(value.receivedAmount()),
                value.taxAmount() == null ? null : new BigDecimal(value.taxAmount()),
                Date.valueOf(value.validFrom()), Date.valueOf(value.validTo()),
                value.previousInsuranceId() == null ? null : new BigDecimal(value.previousInsuranceId()),
                value.currencyCode(), value.justification(), branch, actor, idem, hash);
        pool.jdbc().update("""
                INSERT INTO CLT.COLLATERAL_INSURANCE_REQUEST_EVENT
                (INSURANCE_REQUEST_EVENT_ID, INSURANCE_REQUEST_ID, EVENT_CODE,
                 EVENT_ACTOR, CORRELATION_ID, SNAPSHOT_HASH, EVENT_AT)
                VALUES (CLT.SEQ_COLLATERAL_INSURANCE_REQUEST_EVENT.NEXTVAL,
                        ?, 'REQUEST_CREATED', ?, ?, ?, SYSTIMESTAMP)
                """, id, actor, correlation, hash);
        List<RequestRecord> rows = pool.jdbc().query(SELECT + " WHERE INSURANCE_REQUEST_ID = ?",
                (rs,n) -> row(rs), id);
        if (rows.size() != 1) throw new IllegalStateException("CLT_INSURANCE_STAGING_READBACK_MISSING");
        return rows.getFirst();
    }
    public RequestPage mine(CollateralInsuranceWriterPool pool, String branch, String maker, int page, int size) {
        Long total = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE_REQUEST
                 WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ?
                """, Long.class, branch, maker);
        List<RequestRecord> items = pool.jdbc().query(SELECT + """
                WHERE BRANCH_CODE = ? AND MAKER_SUBJECT = ?
                ORDER BY INSURANCE_REQUEST_ID DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (rs,n) -> row(rs), branch, maker, (long)page*size, size);
        return new RequestPage(total == null ? 0 : total, page, size, items);
    }
}
