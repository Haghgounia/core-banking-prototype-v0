package com.behsazan.corebanking.collateral.sponsor;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorModels.SponsorPage;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorModels.SponsorRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Only reads from the approved 26-table CLT physical model; no sponsor DML. */
@Repository
public class CollateralSponsorRepository {
    public record Parent(String collateralId, String collateralNumber, String ownerPartyId,
            String collateralStatus, String sponsorRequired, String typeStatus, String branchCode) {}
    private final ObjectProvider<CollateralReferenceReaderPool> reader;

    public CollateralSponsorRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader = reader;
    }
    private JdbcTemplate jdbc() { return reader.getObject().jdbc(); }
    private static String decimal(Object value) {
        if (value instanceof BigDecimal n) return n.toPlainString();
        if (value == null) throw new IllegalStateException("CLT_SPONSOR_REQUIRED_ORACLE_VALUE_MISSING");
        return value.toString();
    }
    public Parent parent(String branch, String collateralId) {
        BigDecimal id = CollateralRegistryPolicy.id(collateralId);
        String unit = CollateralRegistryPolicy.branch(branch);
        List<Map<String,Object>> rows = jdbc().queryForList("""
            SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.PARTY_ID,
                   c.COLLATERAL_STATUS_CODE, c.BRANCH_CODE, t.SPONSOR_REQUIRED, t.STATUS TYPE_STATUS
              FROM CLT.COLLATERAL c
              JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID = c.COLLATERAL_TYPE_ID
             WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
            """, id, unit);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_SPONSOR_COLLATERAL_NOT_FOUND_OR_FORBIDDEN");
        Map<String,Object> row = rows.getFirst();
        return new Parent(decimal(row.get("COLLATERAL_ID")), decimal(row.get("COLLATERAL_NUMBER")),
                decimal(row.get("PARTY_ID")), String.valueOf(row.get("COLLATERAL_STATUS_CODE")),
                String.valueOf(row.get("SPONSOR_REQUIRED")), String.valueOf(row.get("TYPE_STATUS")),
                String.valueOf(row.get("BRANCH_CODE")));
    }
    public boolean registered(String branch, String collateralId, String partyId) {
        Long count = jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_SPONSOR s
                JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = s.COLLATERAL_ID
                WHERE s.COLLATERAL_ID = ? AND s.PARTY_ID = ?
                  AND c.BRANCH_CODE = ? AND s.BRANCH_CODE = ?
                """, Long.class, new BigDecimal(collateralId), new BigDecimal(partyId), branch, branch);
        // Existing physical rows have no cancellation status. Treat every match as a duplicate.
        return count != null && count > 0;
    }
    public SponsorPage list(Parent parent, int page, int size) {
        CollateralRegistryPolicy.pagination(page, size);
        BigDecimal id = new BigDecimal(parent.collateralId());
        String branch = parent.branchCode();
        JdbcTemplate db = jdbc();
        Long total = db.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_SPONSOR s
                WHERE s.COLLATERAL_ID = ? AND s.BRANCH_CODE = ?
                """, Long.class, id, branch);
        List<SponsorRow> items = db.query("""
                SELECT s.COLLATERAL_SPONSOR_ID, s.COLLATERAL_ID, s.PARTY_ID,
                       s.DEPOSIT_NUMBER, s.BRANCH_CODE, s.CREATED_AT, s.RECORD_VERSION
                  FROM CLT.COLLATERAL_SPONSOR s
                 WHERE s.COLLATERAL_ID = ? AND s.BRANCH_CODE = ?
                 ORDER BY s.COLLATERAL_SPONSOR_ID DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (rs, rowNum) -> new SponsorRow(
                    rs.getBigDecimal("COLLATERAL_SPONSOR_ID").toPlainString(),
                    rs.getBigDecimal("COLLATERAL_ID").toPlainString(),
                    rs.getBigDecimal("PARTY_ID").toPlainString(),
                    rs.getString("DEPOSIT_NUMBER"), rs.getString("BRANCH_CODE"),
                    rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(),
                    rs.getBigDecimal("RECORD_VERSION").toPlainString(),
                    "PERSISTED_LIFECYCLE_UNVERIFIED"), id, branch, (long)page*size, size);
        return new SponsorPage(parent.collateralId(), parent.collateralNumber(),
                "Y".equals(parent.sponsorRequired()), "G-11_MINIMUM_COUNT_NOT_IN_CLT_DDL",
                null, total == null ? 0 : total, page, size, items, "ORACLE_CLT_OWN_BRANCH");
    }
}
