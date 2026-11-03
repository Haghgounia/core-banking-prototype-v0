package com.behsazan.corebanking.collateral.valuation;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels.*;
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

/** Whitelisted CLT read-only SQL; branch scope comes from verified Jwt, not query input. */
@Repository
public class CollateralValuationRepository {
    public record Parent(String collateralId, String collateralNumber, String ownerPartyId,
            String branchCode, String currencyCode, String collateralStatus,
            boolean typeActive, String currentValuationId) {}

    private static final String COLUMNS = """
            SELECT v.COLLATERAL_VALUATION_HISTORY_ID, v.COLLATERAL_ID,
                   v.VALUATION_SEQUENCE_NO, v.VALUATION_TYPE_CODE, ty.VALUATION_TYPE_NAME,
                   v.VALUATION_METHOD_CODE, mt.VALUATION_METHOD_NAME,
                   v.VALUATION_STATUS_CODE, st.VALUATION_STATUS_NAME,
                   v.VALUATION_DATE, v.VALUATION_AMOUNT, v.CURRENCY_CODE,
                   v.FORCED_SALE_VALUE, v.ELIGIBLE_VALUE, v.HAIRCUT_PERCENT,
                   v.VALUER_PARTY_ID, v.VALUER_NAME, v.VALUATION_REFERENCE,
                   v.EFFECTIVE_FROM, v.EFFECTIVE_TO,
                   v.SUPERSEDES_VALUATION_HISTORY_ID, v.FINALIZED_AT, v.FINALIZED_BY,
                   v.RECORD_VERSION
            """;
    private static final String FROM = """
              FROM CLT.COLLATERAL_VALUATION_HISTORY v
              JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = v.COLLATERAL_ID
              LEFT JOIN CLT.REF_VALUATION_TYPE ty ON ty.VALUATION_TYPE_CODE = v.VALUATION_TYPE_CODE
              LEFT JOIN CLT.REF_VALUATION_METHOD mt ON mt.VALUATION_METHOD_CODE = v.VALUATION_METHOD_CODE
              LEFT JOIN CLT.REF_VALUATION_STATUS st ON st.VALUATION_STATUS_CODE = v.VALUATION_STATUS_CODE
             WHERE v.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
            """;
    private final ObjectProvider<CollateralReferenceReaderPool> readers;
    public CollateralValuationRepository(ObjectProvider<CollateralReferenceReaderPool> readers) {
        this.readers = readers;
    }
    private JdbcTemplate jdbc() { return readers.getObject().jdbc(); }
    private static String dec(BigDecimal value) { return value == null ? null : value.toPlainString(); }
    private static String sqlDate(Date value) { return value == null ? null : value.toLocalDate().toString(); }
    private static ValuationRow row(ResultSet rs, int unused) throws SQLException {
        return new ValuationRow(dec(rs.getBigDecimal("COLLATERAL_VALUATION_HISTORY_ID")),
                dec(rs.getBigDecimal("COLLATERAL_ID")), dec(rs.getBigDecimal("VALUATION_SEQUENCE_NO")),
                rs.getString("VALUATION_TYPE_CODE"), rs.getString("VALUATION_TYPE_NAME"),
                rs.getString("VALUATION_METHOD_CODE"), rs.getString("VALUATION_METHOD_NAME"),
                rs.getString("VALUATION_STATUS_CODE"), rs.getString("VALUATION_STATUS_NAME"),
                sqlDate(rs.getDate("VALUATION_DATE")), dec(rs.getBigDecimal("VALUATION_AMOUNT")),
                rs.getString("CURRENCY_CODE"), dec(rs.getBigDecimal("FORCED_SALE_VALUE")),
                dec(rs.getBigDecimal("ELIGIBLE_VALUE")), dec(rs.getBigDecimal("HAIRCUT_PERCENT")),
                dec(rs.getBigDecimal("VALUER_PARTY_ID")), rs.getString("VALUER_NAME"),
                rs.getString("VALUATION_REFERENCE"), sqlDate(rs.getDate("EFFECTIVE_FROM")),
                sqlDate(rs.getDate("EFFECTIVE_TO")),
                dec(rs.getBigDecimal("SUPERSEDES_VALUATION_HISTORY_ID")),
                rs.getTimestamp("FINALIZED_AT") == null ? null : rs.getTimestamp("FINALIZED_AT").toLocalDateTime().toString(),
                rs.getString("FINALIZED_BY"), dec(rs.getBigDecimal("RECORD_VERSION")));
    }
    public Parent parent(String branch, String id) {
        List<Parent> rows = jdbc().query("""
                SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.PARTY_ID, c.BRANCH_CODE,
                       c.CURRENCY_CODE, c.COLLATERAL_STATUS_CODE,
                       c.CURRENT_VALUATION_HISTORY_ID, t.STATUS AS TYPE_STATUS
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID = c.COLLATERAL_TYPE_ID
                 WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
                """, (rs, n) -> new Parent(dec(rs.getBigDecimal(1)), dec(rs.getBigDecimal(2)),
                dec(rs.getBigDecimal(3)), rs.getString(4), rs.getString(5), rs.getString(6),
                "ACTIVE".equals(rs.getString(8)), dec(rs.getBigDecimal(7))),
                CollateralRegistryPolicy.id(id), CollateralRegistryPolicy.branch(branch));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_VALUATION_COLLATERAL_NOT_FOUND_OR_FORBIDDEN");
        return rows.getFirst();
    }
    public ValuationPage page(Parent parent, int page, int size) {
        CollateralRegistryPolicy.pagination(page, size);
        BigDecimal id = new BigDecimal(parent.collateralId());
        Long count = jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_VALUATION_HISTORY v
                JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = v.COLLATERAL_ID
                WHERE v.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
                """, Long.class, id, parent.branchCode());
        List<ValuationRow> items = jdbc().query(COLUMNS + FROM + """
                 ORDER BY v.VALUATION_SEQUENCE_NO DESC, v.COLLATERAL_VALUATION_HISTORY_ID DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, CollateralValuationRepository::row,
                id, parent.branchCode(), (long)page * size, size);
        return new ValuationPage(parent.collateralId(), parent.collateralNumber(),
                parent.currentValuationId(), count == null ? 0 : count, page, size,
                items, "ORACLE_CLT_OWN_BRANCH_READ_ONLY");
    }
    public ValuationRow current(Parent parent) {
        if (parent.currentValuationId() == null) return null;
        List<ValuationRow> values = jdbc().query(COLUMNS + FROM +
                " AND v.COLLATERAL_VALUATION_HISTORY_ID = ?",
                CollateralValuationRepository::row, new BigDecimal(parent.collateralId()),
                parent.branchCode(), new BigDecimal(parent.currentValuationId()));
        return values.isEmpty() ? null : values.getFirst();
    }
    public String oracleTehranDate() {
        Date value = jdbc().queryForObject("""
                SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE)) FROM DUAL
                """, Date.class);
        if (value == null) throw new IllegalStateException("CLT_VALUATION_ORACLE_CLOCK_UNAVAILABLE");
        return value.toLocalDate().toString();
    }
    public Options options(Parent parent) {
        List<Option> types = jdbc().query("""
                SELECT VALUATION_TYPE_CODE, VALUATION_TYPE_NAME FROM CLT.REF_VALUATION_TYPE
                WHERE STATUS = 'ACTIVE' ORDER BY DISPLAY_ORDER, VALUATION_TYPE_CODE
                FETCH FIRST 101 ROWS ONLY
                """, (rs,n) -> new Option(rs.getString(1), rs.getString(2)));
        List<Option> methods = jdbc().query("""
                SELECT VALUATION_METHOD_CODE, VALUATION_METHOD_NAME FROM CLT.REF_VALUATION_METHOD
                WHERE STATUS = 'ACTIVE' ORDER BY DISPLAY_ORDER, VALUATION_METHOD_CODE
                FETCH FIRST 101 ROWS ONLY
                """, (rs,n) -> new Option(rs.getString(1), rs.getString(2)));
        List<Option> currencies = jdbc().query("""
                SELECT CURRENCY_CODE, CURRENCY_NAME FROM CLT.REF_CURRENCY
                WHERE STATUS = 'ACTIVE' ORDER BY CURRENCY_CODE
                FETCH FIRST 101 ROWS ONLY
                """, (rs,n) -> new Option(rs.getString(1), rs.getString(2)));
        if (types.size() > 100 || methods.size() > 100 || currencies.size() > 100)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_VALUATION_REFERENCE_LIMIT_REQUIRES_SEARCH");
        return new Options(types, methods, currencies, "FINAL",
                "G-07_UI_APPROVED_VS_ORACLE_FINAL;G-18_INTERVAL_UNIT_AND_APPRAISABILITY_NOT_IN_CLT");
    }
    public boolean activeReference(String kind, String code) {
        String sql = switch (kind) {
            case "type" -> "SELECT COUNT(*) FROM CLT.REF_VALUATION_TYPE WHERE VALUATION_TYPE_CODE = ? AND STATUS = 'ACTIVE'";
            case "method" -> "SELECT COUNT(*) FROM CLT.REF_VALUATION_METHOD WHERE VALUATION_METHOD_CODE = ? AND STATUS = 'ACTIVE'";
            case "currency" -> "SELECT COUNT(*) FROM CLT.REF_CURRENCY WHERE CURRENCY_CODE = ? AND STATUS = 'ACTIVE'";
            default -> throw new IllegalArgumentException("CLT_VALUATION_UNKNOWN_REFERENCE_KIND");
        };
        Long result = jdbc().queryForObject(sql, Long.class, code);
        return result != null && result == 1;
    }
    public boolean currentIsFinal(Parent parent, String supersedes) {
        if (parent.currentValuationId() == null || !parent.currentValuationId().equals(supersedes)) return false;
        ValuationRow current = current(parent);
        return current != null && "FINAL".equals(current.statusCode());
    }
}
