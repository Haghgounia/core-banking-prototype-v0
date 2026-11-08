package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.AttributeInput;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.Draft;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.SerialRangeInput;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionPolicy.Normalized;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Fixed allowlisted SQL, independent CLT reader, isolated staging-only writer. */
@Repository
public class CollateralIntroductionRepository {
    public record AttributeRule(String id, String dataType, boolean required,
                                boolean repeatable, boolean manualEntryAllowed, String listValue) {}
    public record TypeRules(boolean active, boolean countable, List<AttributeRule> attributes) {}

    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    private final JsonMapper json;
    public CollateralIntroductionRepository(ObjectProvider<CollateralReferenceReaderPool> reader, JsonMapper json) {
        this.reader = reader;
        this.json = json;
    }
    private JdbcTemplate db() { return reader.getObject().jdbc(); }

    public TypeRules rules(String typeId) {
        List<String> types = db().query("""
                SELECT t.HAS_MULTIPLICITY
                  FROM CLT.COLLATERAL_TYPE t
                  JOIN CLT.COLLATERAL_GROUP g ON g.COLLATERAL_GROUP_ID = t.COLLATERAL_GROUP_ID
                 WHERE t.COLLATERAL_TYPE_ID = ? AND t.STATUS = 'ACTIVE' AND g.STATUS = 'ACTIVE'
                """, (rs, i) -> rs.getString(1), new BigDecimal(typeId));
        if (types.isEmpty()) return new TypeRules(false, false, List.of());
        List<AttributeRule> attrs = db().query("""
                SELECT d.COLLATERAL_ATTRIBUTE_DEFINITION_ID, d.DATA_TYPE_CODE,
                       a.IS_REQUIRED, d.IS_REPEATABLE_FLAG, a.MANUAL_ENTRY_ALLOWED_FLAG, d.LIST_VALUE
                  FROM CLT.COLLATERAL_TYPE_ATTRIBUTE a
                  JOIN CLT.COLLATERAL_ATTRIBUTE_DEFINITION d
                    ON d.COLLATERAL_ATTRIBUTE_DEFINITION_ID = a.COLLATERAL_ATTRIBUTE_DEFINITION_ID
                 WHERE a.COLLATERAL_TYPE_ID = ? AND a.STATUS = 'ACTIVE' AND d.STATUS = 'ACTIVE'
                 ORDER BY a.DISPLAY_ORDER, a.COLLATERAL_TYPE_ATTRIBUTE_ID
                """, (rs, i) -> new AttributeRule(rs.getBigDecimal(1).toPlainString(),
                rs.getString(2), "Y".equals(rs.getString(3)), "Y".equals(rs.getString(4)),
                "Y".equals(rs.getString(5)), rs.getString(6)), new BigDecimal(typeId));
        return new TypeRules(true, "Y".equals(types.getFirst()), List.copyOf(attrs));
    }
    public boolean currencyActive(String currencyCode) {
        Integer n = db().queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_CURRENCY
                 WHERE CURRENCY_CODE = ? AND STATUS = 'ACTIVE'
                """, Integer.class, currencyCode);
        return n != null && n == 1;
    }

    /** The opt-in staging schema is not created by a launcher or installer. */
    public boolean draftSchemaReady(CollateralIntroductionWriterPool pool) {
        Integer tables = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TABLES
                 WHERE OWNER = 'CLT' AND TABLE_NAME IN
                       ('CLT_INTRODUCTION_DRAFT', 'CLT_INTRODUCTION_DRAFT_AUDIT')
                """, Integer.class);
        Integer sequence = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_SEQUENCES
                 WHERE SEQUENCE_OWNER = 'CLT' AND SEQUENCE_NAME = 'SEQ_CLT_INTRODUCTION_DRAFT'
                """, Integer.class);
        Integer columns = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'CLT_INTRODUCTION_DRAFT'
                   AND COLUMN_NAME IN ('DRAFT_ID', 'MAKER_SUBJECT', 'BRANCH_CODE',
                     'PARTY_ID', 'COLLATERAL_TYPE_ID', 'ESTIMATE_AMOUNT', 'CURRENCY_CODE',
                     'VALID_FROM', 'VALID_TO', 'DEPOSIT_NUMBER', 'REMARKS', 'ATTRIBUTES_JSON',
                     'IDEMPOTENCY_KEY', 'REQUEST_HASH', 'CORRELATION_ID', 'STATUS',
                     'CREATED_AT', 'CREATED_BY', 'RECORD_VERSION')
                """, Integer.class);
        Integer auditColumns = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'CLT_INTRODUCTION_DRAFT_AUDIT'
                   AND COLUMN_NAME IN ('DRAFT_ID','EVENT_CODE','EVENT_SUBJECT',
                     'BRANCH_CODE','REQUEST_HASH','CORRELATION_ID')
                """, Integer.class);
        if (!Integer.valueOf(2).equals(tables) || !Integer.valueOf(1).equals(sequence)
                || !Integer.valueOf(19).equals(columns) || !Integer.valueOf(6).equals(auditColumns)) return false;
        extended(pool); // 0 or 2 extension columns is valid; exactly 1 fails closed.
        return true;
    }

    // Old R2B writer deployments continue to work for legacy (non-range) drafts.
    // The two R2C columns are optional and require an independently approved migration.
    private boolean extended(CollateralIntroductionWriterPool pool) {
        Integer count = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'CLT_INTRODUCTION_DRAFT'
                   AND COLUMN_NAME IN ('ASSET_REFERENCE', 'ITEM_RANGES_JSON')
                """, Integer.class);
        if (count == null || count == 1) throw new IllegalStateException("CLT_R2C_INCOMPLETE_STAGE_MIGRATION");
        return count == 2;
    }
    private String selectDraft(boolean hasExtension) {
        String extras = hasExtension
                ? ", ASSET_REFERENCE, ITEM_RANGES_JSON"
                : ", CAST(NULL AS VARCHAR2(200)) AS ASSET_REFERENCE, "
                  + "CAST(NULL AS CLOB) AS ITEM_RANGES_JSON";
        return """
                SELECT DRAFT_ID, STATUS, PARTY_ID, COLLATERAL_TYPE_ID,
                       ESTIMATE_AMOUNT, CURRENCY_CODE, VALID_FROM, VALID_TO,
                       DEPOSIT_NUMBER, REMARKS, ATTRIBUTES_JSON, BRANCH_CODE,
                       CREATED_AT, RECORD_VERSION
                """ + extras + " FROM CLT.CLT_INTRODUCTION_DRAFT ";
    }
    public Draft find(CollateralIntroductionWriterPool pool, String draftId,
                      String maker, String branch) {
        List<Draft> records = pool.jdbc().query(selectDraft(extended(pool))
                + " WHERE DRAFT_ID = ? AND MAKER_SUBJECT = ? AND BRANCH_CODE = ?",
                (rs, i) -> toDraft(rs), new BigDecimal(draftId), maker, branch);
        return records.isEmpty() ? null : records.getFirst();
    }
    public Draft findByKey(CollateralIntroductionWriterPool pool, String key,
                           String maker, String branch) {
        List<Draft> records = pool.jdbc().query(selectDraft(extended(pool))
                + " WHERE IDEMPOTENCY_KEY = ? AND MAKER_SUBJECT = ? AND BRANCH_CODE = ?",
                (rs, i) -> toDraft(rs), key, maker, branch);
        return records.isEmpty() ? null : records.getFirst();
    }
    /** Reviewer can inspect the immutable draft only inside their branch, never their own. */
    public Draft findForReviewer(CollateralIntroductionWriterPool pool, String reviewId,
                                 String branch, String checker) {
        List<Draft> records = pool.jdbc().query(selectDraft(extended(pool)) + """
                 WHERE DRAFT_ID = (
                      SELECT r.DRAFT_ID FROM CLT.CLT_INTRODUCTION_REVIEW r
                       WHERE r.REVIEW_ID = ? AND r.BRANCH_CODE = ? AND r.MAKER_SUBJECT <> ?)
                """, (rs, i) -> toDraft(rs), new BigDecimal(reviewId), branch, checker);
        return records.isEmpty() ? null : records.getFirst();
    }
    public String previousHash(CollateralIntroductionWriterPool pool, String draftId) {
        return pool.jdbc().queryForObject(
                "SELECT REQUEST_HASH FROM CLT.CLT_INTRODUCTION_DRAFT WHERE DRAFT_ID = ?",
                String.class, new BigDecimal(draftId));
    }
    public Draft insert(CollateralIntroductionWriterPool pool, Normalized request, String maker,
                        String branch, String key, String hash, String correlation) {
        BigDecimal newId = pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_CLT_INTRODUCTION_DRAFT.NEXTVAL FROM DUAL", BigDecimal.class);
        if (newId == null) throw new IllegalStateException("CLT_INTRODUCTION_SEQUENCE_UNAVAILABLE");
        String attrs;
        try { attrs = json.writeValueAsString(request.attributes()); }
        catch (Exception e) { throw new IllegalStateException("CLT_INTRODUCTION_JSON_SERIALIZATION_UNAVAILABLE", e); }
        String rangeJson;
        try { rangeJson = json.writeValueAsString(request.itemRanges()); }
        catch (Exception e) { throw new IllegalStateException("CLT_R2C_SERIAL_JSON_UNAVAILABLE", e); }
        boolean hasExtension = extended(pool);
        if (!hasExtension && (request.assetReference() != null || !request.itemRanges().isEmpty()))
            throw new IllegalStateException("CLT_R2C_STAGE_EXTENSION_NOT_APPLIED");
        pool.jdbc().update(con -> {
            String sql = hasExtension ? """
                    INSERT INTO CLT.CLT_INTRODUCTION_DRAFT
                    (DRAFT_ID, MAKER_SUBJECT, BRANCH_CODE, PARTY_ID, COLLATERAL_TYPE_ID,
                     ESTIMATE_AMOUNT, CURRENCY_CODE, VALID_FROM, VALID_TO, DEPOSIT_NUMBER,
                     REMARKS, ATTRIBUTES_JSON, ASSET_REFERENCE, ITEM_RANGES_JSON,
                     IDEMPOTENCY_KEY, REQUEST_HASH, CORRELATION_ID,
                     STATUS, CREATED_BY, RECORD_VERSION)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)
                    """ : """
                    INSERT INTO CLT.CLT_INTRODUCTION_DRAFT
                    (DRAFT_ID, MAKER_SUBJECT, BRANCH_CODE, PARTY_ID, COLLATERAL_TYPE_ID,
                     ESTIMATE_AMOUNT, CURRENCY_CODE, VALID_FROM, VALID_TO, DEPOSIT_NUMBER,
                     REMARKS, ATTRIBUTES_JSON, IDEMPOTENCY_KEY, REQUEST_HASH, CORRELATION_ID,
                     STATUS, CREATED_BY, RECORD_VERSION)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)
                    """;
            var ps = con.prepareStatement(sql);
            ps.setBigDecimal(1, newId);
            ps.setString(2, maker);
            ps.setString(3, branch);
            ps.setBigDecimal(4, new BigDecimal(request.partyId()));
            ps.setBigDecimal(5, new BigDecimal(request.collateralTypeId()));
            ps.setBigDecimal(6, new BigDecimal(request.estimateAmount()));
            ps.setString(7, request.currencyCode());
            ps.setDate(8, Date.valueOf(request.validFrom()));
            if (request.validTo() == null) ps.setNull(9, java.sql.Types.DATE);
            else ps.setDate(9, Date.valueOf(request.validTo()));
            ps.setString(10, request.depositNumber());
            ps.setString(11, request.remarks());
            ps.setCharacterStream(12, new StringReader(attrs), attrs.length());
            int offset = 0;
            if (hasExtension) {
                ps.setString(13, request.assetReference());
                ps.setCharacterStream(14, new StringReader(rangeJson), rangeJson.length());
                offset = 2;
            }
            ps.setString(13 + offset, key);
            ps.setString(14 + offset, hash);
            ps.setString(15 + offset, correlation);
            ps.setString(16 + offset, "DRAFT");
            ps.setString(17 + offset, maker);
            return ps;
        });
        pool.jdbc().update("""
                INSERT INTO CLT.CLT_INTRODUCTION_DRAFT_AUDIT
                    (DRAFT_ID, EVENT_CODE, EVENT_SUBJECT, BRANCH_CODE, REQUEST_HASH, CORRELATION_ID)
                VALUES (?, 'DRAFT_CREATED', ?, ?, ?, ?)
                """, newId, maker, branch, hash, correlation);
        return find(pool, newId.toPlainString(), maker, branch);
    }
    private Draft toDraft(ResultSet rs) throws SQLException {
        List<AttributeInput> attrs;
        try {
            AttributeInput[] parsed = json.readValue(rs.getString("ATTRIBUTES_JSON"), AttributeInput[].class);
            attrs = List.of(parsed);
        } catch (Exception ex) { throw new IllegalStateException("CLT_INTRODUCTION_DRAFT_JSON_INVALID", ex); }
        List<SerialRangeInput> serialRanges;
        try {
            String itemJson = rs.getString("ITEM_RANGES_JSON");
            if (itemJson == null || itemJson.isBlank()) serialRanges = List.of();
            else serialRanges = List.of(json.readValue(itemJson, SerialRangeInput[].class));
        } catch (Exception ex) { throw new IllegalStateException("CLT_R2C_SERIAL_JSON_INVALID", ex); }
        Date end = rs.getDate("VALID_TO");
        return new Draft(rs.getBigDecimal("DRAFT_ID").toPlainString(), rs.getString("STATUS"),
                rs.getBigDecimal("PARTY_ID").toPlainString(),
                rs.getBigDecimal("COLLATERAL_TYPE_ID").toPlainString(),
                rs.getBigDecimal("ESTIMATE_AMOUNT").stripTrailingZeros().toPlainString(),
                rs.getString("CURRENCY_CODE").strip(), rs.getDate("VALID_FROM").toLocalDate().toString(),
                end == null ? null : end.toLocalDate().toString(), rs.getString("DEPOSIT_NUMBER"),
                rs.getString("REMARKS"), attrs, rs.getString("BRANCH_CODE"),
                rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(),
                rs.getBigDecimal("RECORD_VERSION").toPlainString(), "ORACLE_CLT_DRAFT_ONLY",
                rs.getString("ASSET_REFERENCE"), serialRanges);
    }
}
