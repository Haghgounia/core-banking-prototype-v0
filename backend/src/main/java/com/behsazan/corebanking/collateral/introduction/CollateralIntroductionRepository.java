package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.AttributeInput;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.Draft;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.DraftPage;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.DraftSummary;
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
    public record TypeRules(boolean active, boolean countable, boolean countabilityConfigured,
                            boolean hasMultiplicity, List<AttributeRule> attributes) {}
    private record TypeFlags(String multiplicity, String countable) {}

    /** G-12: IS_COUNTABLE_FLAG is independent from HAS_MULTIPLICITY.
     * A missing/null countability decision is never inferred from multiplicity. */
    public boolean countabilityColumnReady() {
        Integer n = db().queryForObject("""
            SELECT COUNT(*) FROM ALL_TAB_COLUMNS
             WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL_TYPE'
               AND COLUMN_NAME = 'IS_COUNTABLE_FLAG'
            """, Integer.class);
        return Integer.valueOf(1).equals(n);
    }


    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    private final JsonMapper json;
    public CollateralIntroductionRepository(ObjectProvider<CollateralReferenceReaderPool> reader, JsonMapper json) {
        this.reader = reader;
        this.json = json;
    }
    private JdbcTemplate db() { return reader.getObject().jdbc(); }

    public TypeRules rules(String typeId) {
        String countabilityExpression = countabilityColumnReady()
                ? "t.IS_COUNTABLE_FLAG" : "CAST(NULL AS CHAR(1))";
        List<TypeFlags> types = db().query("""
                SELECT t.HAS_MULTIPLICITY,
                """ + countabilityExpression + """ 
                  FROM CLT.COLLATERAL_TYPE t
                  JOIN CLT.COLLATERAL_GROUP g ON g.COLLATERAL_GROUP_ID = t.COLLATERAL_GROUP_ID
                 WHERE t.COLLATERAL_TYPE_ID = ? AND t.STATUS = 'ACTIVE' AND g.STATUS = 'ACTIVE'
                """, (rs, i) -> new TypeFlags(rs.getString(1), rs.getString(2)),
                new BigDecimal(typeId));
        if (types.isEmpty()) return new TypeRules(false, false, false, false, List.of());
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
        var decision = CollateralCountabilityPolicy.evaluate(types.getFirst().countable(),
                types.getFirst().multiplicity());
        return new TypeRules(true, decision.countable(), decision.configured(),
                decision.hasMultiplicity(), List.copyOf(attrs));
    }
    /** G-17: verify actual Oracle NUMBER(19,0) metadata rather than trusting XMI alone. */
    public int collateralNumberObservedPrecision() {
        List<Integer> result = db().query("""
                SELECT DATA_TYPE, DATA_PRECISION, DATA_SCALE
                  FROM ALL_TAB_COLUMNS
                 WHERE OWNER='CLT' AND TABLE_NAME='COLLATERAL'
                   AND COLUMN_NAME='COLLATERAL_NUMBER'
                """, (rs, i) -> {
                    String kind = rs.getString(1);
                    int precision = rs.getInt(2);
                    if (rs.wasNull()) return -1;
                    int scale = rs.getInt(3);
                    if (rs.wasNull() || !"NUMBER".equals(kind) || scale != 0) return -1;
                    return precision;
                });
        return result.size() == 1 ? result.getFirst() : -1;
    }

    /** Authoritative business-facing type code, not the numeric TYPE_ID. */
    public String activeTypeCode(String typeId) {
        List<String> rows = db().query("""
                SELECT COLLATERAL_TYPE_CODE FROM CLT.COLLATERAL_TYPE
                 WHERE COLLATERAL_TYPE_ID = ? AND STATUS = 'ACTIVE'
                """, (rs, i) -> rs.getString(1), new BigDecimal(typeId));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** Pre-save G-14 comparison: no other owner's ID, record, or branch is disclosed. */
    public CandidateCounts preflightStageCandidateMatches(CollateralIntroductionWriterPool pool,
            String typeId, String partyId, String assetReference) {
        if (!extended(pool)) throw new IllegalStateException("G14_STAGE_EXTENSION_REQUIRED");
        String normalized = CollateralAssetCandidatePolicy.lookup(assetReference);
        if (normalized == null) return new CandidateCounts(0, 0);
        List<CandidateCounts> matches = pool.jdbc().query("""
                SELECT COUNT(*) AS TOTAL_COUNT,
                       COALESCE(SUM(CASE WHEN PARTY_ID = ? THEN 1 ELSE 0 END), 0) AS SAME_OWNER
                  FROM CLT.CLT_INTRODUCTION_DRAFT
                 WHERE COLLATERAL_TYPE_ID = ? AND STATUS = 'DRAFT'
                   AND ASSET_REFERENCE IS NOT NULL
                   AND UPPER(TRIM(ASSET_REFERENCE)) = ?
                """, (rs, i) -> new CandidateCounts(rs.getLong("SAME_OWNER"),
                        rs.getLong("TOTAL_COUNT") - rs.getLong("SAME_OWNER")),
                new BigDecimal(partyId), new BigDecimal(typeId), normalized);
        return matches.isEmpty() ? new CandidateCounts(0, 0) : matches.getFirst();
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
    /** No attempt to infer a verified asset from stage text. No Party or branch identifiers are returned. */
    public record CandidateCounts(long sameOwner, long otherOwner) {}
    public boolean candidateColumnReady(CollateralIntroductionWriterPool pool) {
        return extended(pool);
    }
    public CandidateCounts stageCandidateMatches(CollateralIntroductionWriterPool pool,
            Draft current) {
        if (!extended(pool)) throw new IllegalStateException("CLT_ASSET_STAGE_EXTENSION_REQUIRED");
        String normalized = CollateralAssetCandidatePolicy.lookup(current.assetReference());
        if (normalized == null) return new CandidateCounts(0, 0);
        List<CandidateCounts> matches = pool.jdbc().query("""
                SELECT COUNT(*) AS TOTAL_COUNT,
                       COALESCE(SUM(CASE WHEN PARTY_ID = ? THEN 1 ELSE 0 END), 0) AS SAME_OWNER
                  FROM CLT.CLT_INTRODUCTION_DRAFT
                 WHERE COLLATERAL_TYPE_ID = ?
                   AND DRAFT_ID <> ?
                   AND STATUS = 'DRAFT'
                   AND ASSET_REFERENCE IS NOT NULL
                   AND UPPER(TRIM(ASSET_REFERENCE)) = ?
                """, (rs, i) -> new CandidateCounts(rs.getLong("SAME_OWNER"),
                        rs.getLong("TOTAL_COUNT") - rs.getLong("SAME_OWNER")),
                new BigDecimal(current.partyId()), new BigDecimal(current.collateralTypeId()),
                new BigDecimal(current.draftId()), normalized);
        return matches.isEmpty() ? new CandidateCounts(0, 0) : matches.getFirst();
    }

    /** G-03: immutable child table. Columns are required as a group. */
    public boolean serialRangeTableReady(CollateralIntroductionWriterPool pool) {
        Integer tables = pool.jdbc().queryForObject("""
            SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CLT'
             AND TABLE_NAME='CLT_INTRODUCTION_DRAFT_SERIAL_RANGE'
            """, Integer.class);
        if (!Integer.valueOf(1).equals(tables)) return false;
        Integer columns = pool.jdbc().queryForObject("""
            SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
             AND TABLE_NAME='CLT_INTRODUCTION_DRAFT_SERIAL_RANGE'
             AND COLUMN_NAME IN ('DRAFT_ID','ROW_NO','SERIAL_FROM','SERIAL_TO',
                'TOTAL_AMOUNT_IRR','CREATED_AT','CREATED_BY','RECORD_VERSION')
            """, Integer.class);
        return Integer.valueOf(8).equals(columns);
    }

    /** No fake success for JSON-only countable drafts. Historical rows need a reviewed backfill. */
    public boolean persistedRangesMatch(CollateralIntroductionWriterPool pool,
            String draftId, List<SerialRangeInput> snapshot) {
        if (snapshot.isEmpty()) {
            if (!serialRangeTableReady(pool)) return true;
        } else if (!serialRangeTableReady(pool)) return false;
        List<SerialRangeInput> rows = pool.jdbc().query("""
                SELECT SERIAL_FROM,SERIAL_TO,TOTAL_AMOUNT_IRR
                  FROM CLT.CLT_INTRODUCTION_DRAFT_SERIAL_RANGE
                 WHERE DRAFT_ID=? ORDER BY ROW_NO
                """, (rs, i) -> new SerialRangeInput(rs.getString(1), rs.getString(2),
                    rs.getBigDecimal(3).toPlainString()), new BigDecimal(draftId));
        return rows.equals(snapshot);
    }

    /** Child rows inserted within the same transaction as the draft and creation audit. */
    public void insertSerialRanges(CollateralIntroductionWriterPool pool,
            BigDecimal draftId, List<SerialRangeInput> ranges, String maker) {
        if (ranges.isEmpty()) return;
        if (!serialRangeTableReady(pool))
            throw new IllegalStateException("G03_SERIAL_RANGE_MIGRATION_REQUIRED");
        for (int i=0; i<ranges.size(); i++) {
            SerialRangeInput row = ranges.get(i);
            int inserted = pool.jdbc().update("""
                INSERT INTO CLT.CLT_INTRODUCTION_DRAFT_SERIAL_RANGE
                  (DRAFT_ID,ROW_NO,SERIAL_FROM,SERIAL_TO,TOTAL_AMOUNT_IRR,CREATED_BY,RECORD_VERSION)
                VALUES (?,?,?,?,?,?,1)
                """, draftId, i+1, row.serialFrom(), row.serialTo(),
                    new BigDecimal(row.totalAmount()), maker);
            if (inserted != 1) throw new IllegalStateException("G03_SERIAL_RANGE_INSERT_FAILED");
        }
    }

    /** Only own drafts, scoped by trusted server-side maker/branch; no user-supplied SQL filters. */
    public DraftPage listMine(CollateralIntroductionWriterPool pool, String maker, String branch,
                              int page, int size) {
        int offset = Math.multiplyExact(page, size);
        Long count = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.CLT_INTRODUCTION_DRAFT
                 WHERE MAKER_SUBJECT = ? AND BRANCH_CODE = ?
                """, Long.class, maker, branch);
        List<DraftSummary> rows = pool.jdbc().query("""
                SELECT DRAFT_ID, STATUS, PARTY_ID, COLLATERAL_TYPE_ID,
                       ESTIMATE_AMOUNT, CURRENCY_CODE, CREATED_AT, RECORD_VERSION
                  FROM CLT.CLT_INTRODUCTION_DRAFT
                 WHERE MAKER_SUBJECT = ? AND BRANCH_CODE = ?
                 ORDER BY CREATED_AT DESC, DRAFT_ID DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (rs, rowNum) -> new DraftSummary(
                    rs.getBigDecimal("DRAFT_ID").toPlainString(), rs.getString("STATUS"),
                    rs.getBigDecimal("PARTY_ID").toPlainString(),
                    rs.getBigDecimal("COLLATERAL_TYPE_ID").toPlainString(),
                    rs.getBigDecimal("ESTIMATE_AMOUNT").stripTrailingZeros().toPlainString(),
                    rs.getString("CURRENCY_CODE").strip(),
                    rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(),
                    rs.getBigDecimal("RECORD_VERSION").toPlainString()),
                maker, branch, offset, size);
        return new DraftPage(List.copyOf(rows), page, size, count == null ? 0L : count);
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
    /** A matching, unique immutable creation audit is mandatory before submitting a review. */
    public boolean matchingCreationAudit(CollateralIntroductionWriterPool pool, String draftId,
                                         String maker, String branch, String digest) {
        Integer matched = pool.jdbc().queryForObject("""
                SELECT COUNT(*)
                  FROM CLT.CLT_INTRODUCTION_DRAFT_AUDIT a
                  JOIN CLT.CLT_INTRODUCTION_DRAFT d ON d.DRAFT_ID = a.DRAFT_ID
                 WHERE d.DRAFT_ID = ? AND d.MAKER_SUBJECT = ? AND d.BRANCH_CODE = ?
                   AND d.REQUEST_HASH = ? AND a.REQUEST_HASH = d.REQUEST_HASH
                   AND a.EVENT_CODE = 'DRAFT_CREATED'
                   AND a.EVENT_SUBJECT = d.MAKER_SUBJECT AND a.BRANCH_CODE = d.BRANCH_CODE
                """, Integer.class, new BigDecimal(draftId), maker, branch, digest);
        return Integer.valueOf(1).equals(matched);
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
        if (!request.itemRanges().isEmpty() && !serialRangeTableReady(pool))
            throw new IllegalStateException("G03_SERIAL_RANGE_MIGRATION_REQUIRED");
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
        insertSerialRanges(pool, newId, request.itemRanges(), maker);
        if (!persistedRangesMatch(pool, newId.toPlainString(), request.itemRanges()))
            throw new IllegalStateException("G03_SERIAL_RANGE_READBACK_MISMATCH");
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
