package com.behsazan.corebanking.collateral.registry;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryModels.Page;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Fixed CLT allowlisted SQL; branch is always server-derived from validated JWT claims. */
@Repository
public class CollateralRegistryRepository {
    private final ObjectProvider<CollateralReferenceReaderPool> reader;

    private static final String BASE = """
          FROM CLT.COLLATERAL c
          JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID = c.COLLATERAL_TYPE_ID
          LEFT JOIN CLT.COLLATERAL_GROUP g ON g.COLLATERAL_GROUP_ID = t.COLLATERAL_GROUP_ID
          LEFT JOIN CLT.REF_COLLATERAL_STATUS s ON s.COLLATERAL_STATUS_CODE = c.COLLATERAL_STATUS_CODE
         WHERE c.BRANCH_CODE = ?
        """;
    private static final String COLUMNS = """
        SELECT c.COLLATERAL_ID, c.COLLATERAL_NUMBER, c.PARTY_ID,
               c.COLLATERAL_TYPE_ID, t.COLLATERAL_TYPE_NAME,
               g.COLLATERAL_GROUP_CODE, g.COLLATERAL_GROUP_NAME,
               c.COLLATERAL_STATUS_CODE, s.COLLATERAL_STATUS_NAME, c.ESTIMATE_AMOUNT, c.MORTGAGE_AMOUNT,
               c.INSURANCE_AMOUNT, c.VALID_FROM, c.VALID_TO,
               c.MULTIPLICITY, c.USAGE_CONDITION, c.USAGE_TERM,
               c.DEPOSIT_NUMBER, c.BRANCH_CODE, c.REMARKS, c.CURRENCY_CODE,
               c.CURRENT_VALUATION_HISTORY_ID, c.RECORD_VERSION
        """;

    public CollateralRegistryRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader = reader;
    }
    private JdbcTemplate db() { return reader.getObject().jdbc(); }

    public Page list(String branch, String registrationNumber, int page, int size) {
        return list(branch, registrationNumber, null, page, size);
    }

    /** R6I: the optional group filter is parameterized and always subordinate to the server branch. */
    public Page list(String branch, String registrationNumber, String groupCode, int page, int size) {
        return list(branch, registrationNumber, groupCode, null, null, null, page, size);
    }

    /** R2 consolidated: all criteria are exact, bound values, under the authenticated branch. */
    public Page list(String branch, String registrationNumber, String groupCode,
                     String partyId, String typeId, String statusCode, int page, int size) {
        CollateralRegistryPolicy.pagination(page, size);
        boolean hasNumber = registrationNumber != null && !registrationNumber.isBlank();
        boolean hasGroup = groupCode != null && !groupCode.isBlank();
        boolean hasParty = partyId != null && !partyId.isBlank();
        boolean hasType = typeId != null && !typeId.isBlank();
        boolean hasStatus = statusCode != null && !statusCode.isBlank();
        String filter = (hasNumber ? " AND c.COLLATERAL_NUMBER = ?" : "")
                + (hasGroup ? " AND g.COLLATERAL_GROUP_CODE = ? AND g.STATUS = 'ACTIVE'" : "")
                + (hasParty ? " AND c.PARTY_ID = ?" : "")
                + (hasType ? " AND c.COLLATERAL_TYPE_ID = ?" : "")
                + (hasStatus ? " AND c.COLLATERAL_STATUS_CODE = ?" : "");
        List<Object> parameters = new ArrayList<>();
        parameters.add(CollateralRegistryPolicy.branch(branch));
        if (hasNumber) parameters.add(CollateralRegistryPolicy.id(registrationNumber));
        if (hasGroup) parameters.add(CollateralRegistryPolicy.groupCode(groupCode));
        if (hasParty) parameters.add(CollateralRegistryPolicy.id(partyId));
        if (hasType) parameters.add(CollateralRegistryPolicy.id(typeId));
        if (hasStatus) parameters.add(CollateralRegistryPolicy.statusCode(statusCode));
        JdbcTemplate jdbc = db();
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + BASE + filter,
                Long.class, parameters.toArray());
        List<Object> paged = new ArrayList<>(parameters);
        paged.add((long) page * size);
        paged.add(size);
        List<Map<String, Object>> items = jdbc.queryForList(COLUMNS + BASE + filter
                + " ORDER BY c.COLLATERAL_ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                paged.toArray()).stream().map(CollateralRegistryRepository::normalize).toList();
        return new Page(items, total == null ? 0 : total, page, size, "ORACLE_CLT_OWN_BRANCH");
    }

    public Map<String, Object> byRegistration(String branch, String number) {
        return single(COLUMNS + BASE + " AND c.COLLATERAL_NUMBER = ?",
                CollateralRegistryPolicy.branch(branch), CollateralRegistryPolicy.id(number));
    }

    public Map<String, Object> byId(String branch, String id) {
        return single(COLUMNS + BASE + " AND c.COLLATERAL_ID = ?",
                CollateralRegistryPolicy.branch(branch), CollateralRegistryPolicy.id(id));
    }

    private Map<String, Object> single(String query, Object... args) {
        List<Map<String, Object>> rows = db().queryForList(query, args);
        // Unauthorized and missing items are deliberately indistinguishable.
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_COLLATERAL_NOT_FOUND");
        return normalize(rows.getFirst());
    }

    /** The group catalog is global technical reference data, never a collateral ownership list. */
    public List<Map<String, Object>> activeGroups() {
        return db().queryForList("""
                SELECT g.COLLATERAL_GROUP_ID, g.COLLATERAL_GROUP_CODE,
                       g.COLLATERAL_GROUP_NAME, g.STATUS
                  FROM CLT.COLLATERAL_GROUP g
                 WHERE g.STATUS = 'ACTIVE'
                 ORDER BY g.COLLATERAL_GROUP_CODE, g.COLLATERAL_GROUP_ID
                 FETCH FIRST 200 ROWS ONLY
                """).stream().map(CollateralRegistryRepository::normalize).toList();
    }

    public List<Map<String, Object>> activeStatuses() {
        return db().queryForList("""
                SELECT s.COLLATERAL_STATUS_CODE, s.COLLATERAL_STATUS_NAME,
                       s.IS_ACTIVE_STATUS_FLAG, s.IS_TERMINAL_STATUS_FLAG
                  FROM CLT.REF_COLLATERAL_STATUS s
                 WHERE s.STATUS = 'ACTIVE'
                 ORDER BY s.DISPLAY_ORDER, s.COLLATERAL_STATUS_CODE
                 FETCH FIRST 100 ROWS ONLY
                """).stream().map(CollateralRegistryRepository::normalize).toList();
    }

    public List<Map<String, Object>> activeTypes() {
        return db().queryForList("""
                SELECT t.COLLATERAL_TYPE_ID, t.COLLATERAL_TYPE_CODE, t.COLLATERAL_TYPE_NAME,
                       t.COLLATERAL_GROUP_ID, g.COLLATERAL_GROUP_CODE, g.COLLATERAL_GROUP_NAME, t.HAS_MULTIPLICITY,
                       t.INSURABLE, t.SPONSOR_REQUIRED, t.STATUS
                  FROM CLT.COLLATERAL_TYPE t
                  JOIN CLT.COLLATERAL_GROUP g ON g.COLLATERAL_GROUP_ID = t.COLLATERAL_GROUP_ID
                 WHERE t.STATUS = 'ACTIVE' AND g.STATUS = 'ACTIVE'
                 ORDER BY t.COLLATERAL_GROUP_ID, t.COLLATERAL_TYPE_CODE
                 FETCH FIRST 200 ROWS ONLY
                """).stream().map(CollateralRegistryRepository::normalize).toList();
    }

    public List<Map<String, Object>> typeAttributes(String typeId) {
        BigDecimal id = CollateralRegistryPolicy.id(typeId);
        // Do not leak configuration of retired types.
        return db().queryForList("""
                SELECT a.COLLATERAL_ATTRIBUTE_DEFINITION_ID,
                       d.COLLATERAL_ATTRIBUTE_DEFINITION_CODE,
                       d.COLLATERAL_ATTRIBUTE_DEFINITION_NAME,
                       d.DATA_TYPE_CODE, d.LIST_VALUE,
                       a.DISPLAY_ORDER, a.IS_REQUIRED, a.MANUAL_ENTRY_ALLOWED_FLAG
                  FROM CLT.COLLATERAL_TYPE_ATTRIBUTE a
                  JOIN CLT.COLLATERAL_ATTRIBUTE_DEFINITION d
                    ON d.COLLATERAL_ATTRIBUTE_DEFINITION_ID = a.COLLATERAL_ATTRIBUTE_DEFINITION_ID
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID = a.COLLATERAL_TYPE_ID
                 WHERE a.COLLATERAL_TYPE_ID = ? AND t.STATUS = 'ACTIVE'
                   AND a.STATUS = 'ACTIVE' AND d.STATUS = 'ACTIVE'
                 ORDER BY a.DISPLAY_ORDER, a.COLLATERAL_TYPE_ATTRIBUTE_ID
                 FETCH FIRST 200 ROWS ONLY
                """, id).stream().map(CollateralRegistryRepository::normalize).toList();
    }

    static Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> safe = new LinkedHashMap<>();
        row.forEach((name, value) -> {
            Object v = value;
            if (v instanceof BigDecimal b) v = b.toPlainString();
            else if (v instanceof Timestamp t) v = t.toLocalDateTime().toString();
            else if (v instanceof java.sql.Date d) v = d.toLocalDate().toString();
            safe.put(name, v);
        });
        return safe;
    }
}
