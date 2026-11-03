package com.behsazan.corebanking.collateral.insurance;

import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels.*;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** READ ONLY against existing 26-table CLT model; never confers allocation validity. */
@Repository
public class CollateralInsuranceRepository {
    public record Parent(String collateralId, String collateralNumber, String ownerPartyId,
            String branchCode, String collateralStatus, boolean insurable, boolean typeActive) {}
    public record Previous(String collateralId, String insuranceTypeId, String validTo, boolean renewed) {}
    private final ObjectProvider<CollateralReferenceReaderPool> readers;
    public CollateralInsuranceRepository(ObjectProvider<CollateralReferenceReaderPool> readers) {
        this.readers = readers;
    }
    private JdbcTemplate jdbc() { return readers.getObject().jdbc(); }
    private static String number(Object val) {
        if (val == null) throw new IllegalStateException("CLT_INSURANCE_ORACLE_ID_MISSING");
        return val instanceof BigDecimal b ? b.toPlainString() : val.toString();
    }
    public Parent parent(String branch, String collateralId) {
        var rows = jdbc().queryForList("""
                SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.PARTY_ID, c.BRANCH_CODE,
                       c.COLLATERAL_STATUS_CODE, t.INSURABLE, t.STATUS TYPE_STATUS
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID = c.COLLATERAL_TYPE_ID
                 WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
                """, CollateralRegistryPolicy.id(collateralId), CollateralRegistryPolicy.branch(branch));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_INSURANCE_COLLATERAL_NOT_FOUND_OR_FORBIDDEN");
        Map<String,Object> r = rows.getFirst();
        return new Parent(number(r.get("COLLATERAL_ID")), number(r.get("COLLATERAL_NUMBER")),
                number(r.get("PARTY_ID")), (String)r.get("BRANCH_CODE"),
                (String)r.get("COLLATERAL_STATUS_CODE"), "Y".equals(r.get("INSURABLE")),
                "ACTIVE".equals(r.get("TYPE_STATUS")));
    }
    public PolicyPage list(Parent parent, int page, int size) {
        CollateralRegistryPolicy.pagination(page, size);
        BigDecimal collateralId = new BigDecimal(parent.collateralId());
        Long total = jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE i
                 WHERE i.COLLATERAL_ID = ? AND i.BRANCH_CODE = ?
                """, Long.class, collateralId, parent.branchCode());
        List<PolicyRow> items = jdbc().query("""
                SELECT i.COLLATERAL_INSURANCE_ID, i.COLLATERAL_ID, i.PARTY_ID,
                       i.INSURER_CODE, i.INSURANCE_COMPANY_ID,
                       co.INSURANCE_COMPANY_NAME, i.COLLATERAL_INSURANCE_TYPE_ID,
                       ty.COLLATERAL_INSURANCE_TYPE_NAME, i.DEPOSIT_NUMBER, i.INSURANCE_NUMBER,
                       i.INSURANCE_AMOUNT, i.INSURANCE_RECEIVED_AMOUNT, i.TAX_AMOUNT,
                       i.VALID_FROM, i.VALID_TO, i.PREVIOUS_INSURANCE_ID,
                       i.BRANCH_CODE, i.CURRENCY_CODE, i.RECORD_VERSION
                  FROM CLT.COLLATERAL_INSURANCE i
                  JOIN CLT.INSURANCE_COMPANY co ON co.INSURANCE_COMPANY_ID = i.INSURANCE_COMPANY_ID
                  JOIN CLT.COLLATERAL_INSURANCE_TYPE ty
                    ON ty.COLLATERAL_INSURANCE_TYPE_ID = i.COLLATERAL_INSURANCE_TYPE_ID
                 WHERE i.COLLATERAL_ID = ? AND i.BRANCH_CODE = ?
                 ORDER BY i.COLLATERAL_INSURANCE_ID DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (rs,n) -> new PolicyRow(
                  rs.getBigDecimal("COLLATERAL_INSURANCE_ID").toPlainString(),
                  rs.getBigDecimal("COLLATERAL_ID").toPlainString(),
                  rs.getBigDecimal("PARTY_ID").toPlainString(), rs.getString("INSURER_CODE"),
                  rs.getBigDecimal("INSURANCE_COMPANY_ID").toPlainString(),
                  rs.getString("INSURANCE_COMPANY_NAME"),
                  rs.getBigDecimal("COLLATERAL_INSURANCE_TYPE_ID").toPlainString(),
                  rs.getString("COLLATERAL_INSURANCE_TYPE_NAME"), rs.getString("DEPOSIT_NUMBER"),
                  rs.getString("INSURANCE_NUMBER"), rs.getBigDecimal("INSURANCE_AMOUNT").toPlainString(),
                  rs.getBigDecimal("INSURANCE_RECEIVED_AMOUNT").toPlainString(),
                  rs.getBigDecimal("TAX_AMOUNT") == null ? null : rs.getBigDecimal("TAX_AMOUNT").toPlainString(),
                  rs.getDate("VALID_FROM").toLocalDate().toString(),
                  rs.getDate("VALID_TO").toLocalDate().toString(),
                  rs.getBigDecimal("PREVIOUS_INSURANCE_ID") == null ? null : rs.getBigDecimal("PREVIOUS_INSURANCE_ID").toPlainString(),
                  rs.getString("BRANCH_CODE"), rs.getString("CURRENCY_CODE"),
                  rs.getBigDecimal("RECORD_VERSION").toPlainString(),
                  "PERSISTED_LIFECYCLE_UNVERIFIED"),
                  collateralId, parent.branchCode(), (long)page*size, size);
        return new PolicyPage(parent.collateralId(), parent.collateralNumber(), parent.insurable(),
                "G-04_REQUIRED_INSURANCE_TYPES_NOT_PERSISTED",
                "G-06_POLICY_APPROVAL_AND_CANCELLATION_UNKNOWN", total == null ? 0 : total,
                page, size, items, "ORACLE_CLT_OWN_BRANCH");
    }
    public Options options(Parent parent) {
        if (!parent.insurable()) return new Options(List.of(), List.of(), List.of(),
                "NOT_APPLICABLE", "BANK_INSURER_API_UNAVAILABLE");
        List<Option> types = jdbc().query("""
                SELECT COLLATERAL_INSURANCE_TYPE_ID, COLLATERAL_INSURANCE_TYPE_CODE,
                       COLLATERAL_INSURANCE_TYPE_NAME
                  FROM CLT.COLLATERAL_INSURANCE_TYPE WHERE STATUS = 'ACTIVE'
                 ORDER BY COLLATERAL_INSURANCE_TYPE_NAME
                 FETCH FIRST 501 ROWS ONLY
                """, (rs,n) -> new Option(
                    rs.getBigDecimal(1).toPlainString(), rs.getString(2), rs.getString(3)));
        List<Option> companies = jdbc().query("""
                SELECT INSURANCE_COMPANY_ID, INSURANCE_COMPANY_CODE, INSURANCE_COMPANY_NAME
                  FROM CLT.INSURANCE_COMPANY WHERE STATUS = 'ACTIVE'
                 ORDER BY INSURANCE_COMPANY_NAME
                 FETCH FIRST 501 ROWS ONLY
                """, (rs,n) -> new Option(
                    rs.getBigDecimal(1).toPlainString(), rs.getString(2), rs.getString(3)));
        List<Option> currencies = jdbc().query("""
                SELECT REF_CURRENCY_ID, CURRENCY_CODE, CURRENCY_NAME
                  FROM CLT.REF_CURRENCY WHERE STATUS = 'ACTIVE'
                 ORDER BY CURRENCY_CODE
                 FETCH FIRST 501 ROWS ONLY
                """, (rs,n) -> new Option(
                    rs.getBigDecimal(1).toPlainString(), rs.getString(2), rs.getString(3)));
        // Never silently omit approved reference data when an entire selection is needed.
        if (types.size() > 500 || companies.size() > 500 || currencies.size() > 500)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_INSURANCE_REFERENCE_PAGE_LIMIT_REQUIRES_SEARCH");
        return new Options(types, companies, currencies,
                "G-04_INTRODUCTION_REQUIRED_TYPES_UNKNOWN", "BANK_INSURER_API_UNAVAILABLE");
    }
    public List<RenewalOption> renewalOptions(Parent parent, String insuranceTypeId) {
        var rows = jdbc().query("""
                SELECT i.COLLATERAL_INSURANCE_ID, i.INSURANCE_NUMBER,
                       i.COLLATERAL_INSURANCE_TYPE_ID, i.VALID_TO
                  FROM CLT.COLLATERAL_INSURANCE i
                 WHERE i.COLLATERAL_ID = ? AND i.BRANCH_CODE = ?
                   AND i.COLLATERAL_INSURANCE_TYPE_ID = ?
                   AND NOT EXISTS (SELECT 1 FROM CLT.COLLATERAL_INSURANCE child
                       WHERE child.PREVIOUS_INSURANCE_ID = i.COLLATERAL_INSURANCE_ID)
                 ORDER BY i.VALID_TO DESC, i.COLLATERAL_INSURANCE_ID DESC
                 FETCH FIRST 201 ROWS ONLY
                """, (rs,n) -> new RenewalOption(rs.getBigDecimal(1).toPlainString(),
                rs.getString(2), rs.getBigDecimal(3).toPlainString(),
                rs.getDate(4).toLocalDate().toString()),
                new BigDecimal(parent.collateralId()), parent.branchCode(), new BigDecimal(insuranceTypeId));
        if (rows.size() > 200) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_INSURANCE_RENEWAL_OPTIONS_LIMIT_REQUIRES_SEARCH");
        return rows;
    }
    public boolean activeReference(String table, String id) {
        String sql = switch (table) {
            case "company" -> "SELECT COUNT(*) FROM CLT.INSURANCE_COMPANY WHERE INSURANCE_COMPANY_ID = ? AND STATUS = 'ACTIVE'";
            case "type" -> "SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE_TYPE WHERE COLLATERAL_INSURANCE_TYPE_ID = ? AND STATUS = 'ACTIVE'";
            default -> throw new IllegalArgumentException("CLT_INSURANCE_REFERENCE_INVALID");
        };
        Long n = jdbc().queryForObject(sql, Long.class, new BigDecimal(id));
        return n != null && n == 1;
    }
    public boolean currencyActive(String code) {
        Long n = jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_CURRENCY
                 WHERE CURRENCY_CODE = ? AND STATUS = 'ACTIVE'
                """, Long.class, code);
        return n != null && n == 1;
    }
    public boolean alreadyNumber(String companyId, String insuranceNumber) {
        Long n = jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE
                 WHERE INSURANCE_COMPANY_ID = ? AND INSURANCE_NUMBER = ?
                """, Long.class, new BigDecimal(companyId), insuranceNumber);
        return n != null && n > 0;
    }
    public boolean overlaps(Parent parent, String typeId, String from, String to) {
        Long n = jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE
                 WHERE COLLATERAL_ID = ? AND BRANCH_CODE = ?
                   AND COLLATERAL_INSURANCE_TYPE_ID = ?
                   AND VALID_FROM <= ? AND VALID_TO >= ?
                """, Long.class, new BigDecimal(parent.collateralId()), parent.branchCode(),
                new BigDecimal(typeId), Date.valueOf(to), Date.valueOf(from));
        // No cancellation status in the base model; even old rows block overlapping staging.
        return n != null && n > 0;
    }
    public Previous previous(Parent parent, String previousId) {
        var rows = jdbc().query("""
                SELECT i.COLLATERAL_ID, i.COLLATERAL_INSURANCE_TYPE_ID, i.VALID_TO,
                       (SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE child
                         WHERE child.PREVIOUS_INSURANCE_ID = i.COLLATERAL_INSURANCE_ID) AS RENEWED
                  FROM CLT.COLLATERAL_INSURANCE i
                 WHERE i.COLLATERAL_INSURANCE_ID = ? AND i.COLLATERAL_ID = ?
                   AND i.BRANCH_CODE = ?
                """, (rs,n) -> new Previous(rs.getBigDecimal(1).toPlainString(),
                    rs.getBigDecimal(2).toPlainString(), rs.getDate(3).toLocalDate().toString(),
                    rs.getLong(4) > 0), new BigDecimal(previousId),
                new BigDecimal(parent.collateralId()), parent.branchCode());
        return rows.isEmpty() ? null : rows.getFirst();
    }
}
