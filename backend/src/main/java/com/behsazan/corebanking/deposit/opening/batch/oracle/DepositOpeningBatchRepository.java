package com.behsazan.corebanking.deposit.opening.batch.oracle;

import com.behsazan.corebanking.deposit.opening.batch.domain.DepositOpeningBatchModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Repository
public class DepositOpeningBatchRepository {
    private static final Set<String> REFERENCE_TABLES = Set.of(
            "REF_DEP_OPEN_BATCH_SOURCE_TYPE", "REF_DEP_OPEN_BATCH_STATUS",
            "REF_DEP_OPEN_BATCH_ITEM_STATUS", "REF_DEP_OPEN_BATCH_ERROR_STAGE",
            "REF_DEP_OPEN_BATCH_ERROR_CODE"
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;
    private final String cifSchema;
    private final String productSchema;

    public DepositOpeningBatchRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-opening:DPS2}") String schema,
            @Value("${core-banking.schemas.cif:CIF}") String cifSchema,
            @Value("${core-banking.schemas.product-definition:PDL}") String productSchema
    ) {
        this.jdbc = jdbc;
        this.schema = requireIdentifier(schema);
        this.cifSchema = requireIdentifier(cifSchema);
        this.productSchema = requireIdentifier(productSchema);
    }

    public long nextBatchId() { return nextValue("SEQ_DEPOSIT_OPENING_BATCH"); }
    public long nextBatchItemId() { return nextValue("SEQ_DEPOSIT_OPENING_BATCH_ITEM"); }
    public long nextBatchErrorId() { return nextValue("SEQ_DEPOSIT_OPENING_BATCH_ERROR"); }

    public Optional<BatchHeaderView> findByIdempotencyKey(String key) {
        return queryBatch("IDEMPOTENCY_KEY = :value", key, false);
    }

    public Optional<BatchHeaderView> findBatch(long batchId) {
        return queryBatch("OPENING_BATCH_ID = :value", batchId, false);
    }

    public Optional<BatchHeaderView> lockBatch(long batchId) {
        return queryBatch("OPENING_BATCH_ID = :value", batchId, true);
    }

    private Optional<BatchHeaderView> queryBatch(String predicate, Object value, boolean lock) {
        String sql = ("""
                SELECT OPENING_BATCH_ID, BATCH_NO, IDEMPOTENCY_KEY, SOURCE_TYPE_CODE, SOURCE_REFERENCE,
                       BULK_OPENING_BASIS_CODE, LEGAL_BASIS_REFERENCE, CDD_APPROVAL_REFERENCE,
                       TOTAL_COUNT, SUCCESS_COUNT, FAILED_COUNT, BATCH_STATUS_CODE,
                       STARTED_AT, COMPLETED_AT, RECORD_VERSION
                  FROM %s.DEPOSIT_OPENING_BATCH
                 WHERE %s
                """ + (lock ? " FOR UPDATE" : "")).formatted(schema, predicate);
        List<BatchHeaderView> rows = jdbc.query(sql, new MapSqlParameterSource().addValue("value", value),
                (rs, n) -> new BatchHeaderView(
                        rs.getLong("OPENING_BATCH_ID"), rs.getString("BATCH_NO"), rs.getString("IDEMPOTENCY_KEY"),
                        rs.getString("SOURCE_TYPE_CODE"), rs.getString("SOURCE_REFERENCE"),
                        rs.getString("BULK_OPENING_BASIS_CODE"), rs.getString("LEGAL_BASIS_REFERENCE"), rs.getString("CDD_APPROVAL_REFERENCE"),
                        rs.getInt("TOTAL_COUNT"), rs.getInt("SUCCESS_COUNT"), rs.getInt("FAILED_COUNT"),
                        rs.getString("BATCH_STATUS_CODE"), localDateTime(rs.getTimestamp("STARTED_AT")),
                        localDateTime(rs.getTimestamp("COMPLETED_AT")), rs.getLong("RECORD_VERSION")
                ));
        return rows.stream().findFirst();
    }

    public int insertBatch(long batchId, String batchNo, String idempotencyKey, String sourceTypeCode,
                           String sourceReference, String bulkOpeningBasisCode, String legalBasisReference,
                           String cddApprovalReference, int totalCount, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_BATCH (
                    OPENING_BATCH_ID, BATCH_NO, IDEMPOTENCY_KEY, SOURCE_TYPE_CODE, SOURCE_REFERENCE,
                    BULK_OPENING_BASIS_CODE, LEGAL_BASIS_REFERENCE, CDD_APPROVAL_REFERENCE,
                    TOTAL_COUNT, SUCCESS_COUNT, FAILED_COUNT, BATCH_STATUS_CODE, CREATED_BY
                ) VALUES (
                    :batchId, :batchNo, :idempotencyKey, :sourceTypeCode, :sourceReference,
                    :bulkOpeningBasisCode, :legalBasisReference, :cddApprovalReference,
                    :totalCount, 0, 0, 'DRAFT', :actor
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("batchId", batchId).addValue("batchNo", batchNo)
                .addValue("idempotencyKey", idempotencyKey).addValue("sourceTypeCode", sourceTypeCode)
                .addValue("sourceReference", sourceReference)
                .addValue("bulkOpeningBasisCode", bulkOpeningBasisCode)
                .addValue("legalBasisReference", legalBasisReference)
                .addValue("cddApprovalReference", cddApprovalReference)
                .addValue("totalCount", totalCount).addValue("actor", actor));
    }

    public int insertItem(long itemId, long batchId, BatchItemCreateRequest item, int rowNo, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_BATCH_ITEM (
                    OPENING_BATCH_ITEM_ID, OPENING_BATCH_ID, ROW_NO, EXTERNAL_ROW_KEY, PARTY_ID,
                    PRODUCT_VERSION_ID, CURRENCY_CODE, OPENING_AMOUNT, ITEM_STATUS_CODE, CREATED_BY
                ) VALUES (
                    :itemId, :batchId, :rowNo, :externalRowKey, :partyId,
                    :productVersionId, :currencyCode, :openingAmount, 'PENDING', :actor
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("itemId", itemId).addValue("batchId", batchId).addValue("rowNo", rowNo)
                .addValue("externalRowKey", item.externalRowKey()).addValue("partyId", item.partyId())
                .addValue("productVersionId", item.productVersionId()).addValue("currencyCode", item.currencyCode())
                .addValue("openingAmount", item.openingAmount()).addValue("actor", actor));
    }

    public List<BatchItemView> listItems(long batchId) {
        String sql = """
                SELECT I.OPENING_BATCH_ITEM_ID, I.OPENING_BATCH_ID, I.ROW_NO, I.EXTERNAL_ROW_KEY,
                       I.PARTY_ID, I.PRODUCT_VERSION_ID, I.CURRENCY_CODE, I.OPENING_AMOUNT,
                       I.ITEM_STATUS_CODE, I.OPENING_REQUEST_ID, I.ACCOUNT_ID, I.RECORD_VERSION,
                       R.REQUEST_NO, A.ACCOUNT_NO, A.ACCOUNT_STATUS_CODE
                  FROM %s.DEPOSIT_OPENING_BATCH_ITEM I
                  LEFT JOIN %s.DEPOSIT_OPENING_REQUEST R ON R.OPENING_REQUEST_ID = I.OPENING_REQUEST_ID
                  LEFT JOIN %s.DEPOSIT_ACCOUNT A ON A.ACCOUNT_ID = I.ACCOUNT_ID
                 WHERE I.OPENING_BATCH_ID = :batchId
                 ORDER BY I.ROW_NO, I.OPENING_BATCH_ITEM_ID
                """.formatted(schema, schema, schema);
        return jdbc.query(sql, new MapSqlParameterSource("batchId", batchId), (rs, n) -> new BatchItemView(
                rs.getLong("OPENING_BATCH_ITEM_ID"), rs.getLong("OPENING_BATCH_ID"), rs.getInt("ROW_NO"),
                rs.getString("EXTERNAL_ROW_KEY"), nullableLong(rs, "PARTY_ID"), nullableLong(rs, "PRODUCT_VERSION_ID"),
                rs.getString("CURRENCY_CODE"), rs.getBigDecimal("OPENING_AMOUNT"), rs.getString("ITEM_STATUS_CODE"),
                nullableLong(rs, "OPENING_REQUEST_ID"), nullableLong(rs, "ACCOUNT_ID"), rs.getString("REQUEST_NO"),
                rs.getString("ACCOUNT_NO"), rs.getString("ACCOUNT_STATUS_CODE"), rs.getLong("RECORD_VERSION")
        ));
    }

    public Optional<BatchItemView> lockItem(long itemId) {
        String sql = """
                SELECT I.OPENING_BATCH_ITEM_ID, I.OPENING_BATCH_ID, I.ROW_NO, I.EXTERNAL_ROW_KEY,
                       I.PARTY_ID, I.PRODUCT_VERSION_ID, I.CURRENCY_CODE, I.OPENING_AMOUNT,
                       I.ITEM_STATUS_CODE, I.OPENING_REQUEST_ID, I.ACCOUNT_ID, I.RECORD_VERSION,
                       CAST(NULL AS VARCHAR2(40)) REQUEST_NO, CAST(NULL AS VARCHAR2(50)) ACCOUNT_NO,
                       CAST(NULL AS VARCHAR2(30)) ACCOUNT_STATUS_CODE
                  FROM %s.DEPOSIT_OPENING_BATCH_ITEM I
                 WHERE I.OPENING_BATCH_ITEM_ID = :itemId
                 FOR UPDATE
                """.formatted(schema);
        List<BatchItemView> rows = jdbc.query(sql, new MapSqlParameterSource("itemId", itemId), (rs, n) -> new BatchItemView(
                rs.getLong("OPENING_BATCH_ITEM_ID"), rs.getLong("OPENING_BATCH_ID"), rs.getInt("ROW_NO"),
                rs.getString("EXTERNAL_ROW_KEY"), nullableLong(rs, "PARTY_ID"), nullableLong(rs, "PRODUCT_VERSION_ID"),
                rs.getString("CURRENCY_CODE"), rs.getBigDecimal("OPENING_AMOUNT"), rs.getString("ITEM_STATUS_CODE"),
                nullableLong(rs, "OPENING_REQUEST_ID"), nullableLong(rs, "ACCOUNT_ID"), rs.getString("REQUEST_NO"),
                rs.getString("ACCOUNT_NO"), rs.getString("ACCOUNT_STATUS_CODE"), rs.getLong("RECORD_VERSION")
        ));
        return rows.stream().findFirst();
    }

    public List<BatchErrorView> listErrors(long batchId) {
        String sql = """
                SELECT E.OPENING_BATCH_ERROR_ID, E.OPENING_BATCH_ITEM_ID, E.ERROR_STAGE_CODE, E.ERROR_CODE,
                       E.FIELD_NAME, E.ERROR_MESSAGE, E.IS_RETRYABLE, E.CREATED_AT
                  FROM %s.DEPOSIT_OPENING_BATCH_ERROR E
                  JOIN %s.DEPOSIT_OPENING_BATCH_ITEM I ON I.OPENING_BATCH_ITEM_ID = E.OPENING_BATCH_ITEM_ID
                 WHERE I.OPENING_BATCH_ID = :batchId
                 ORDER BY I.ROW_NO, E.OPENING_BATCH_ERROR_ID
                """.formatted(schema, schema);
        return jdbc.query(sql, new MapSqlParameterSource("batchId", batchId), (rs, n) -> new BatchErrorView(
                rs.getLong("OPENING_BATCH_ERROR_ID"), rs.getLong("OPENING_BATCH_ITEM_ID"),
                rs.getString("ERROR_STAGE_CODE"), rs.getString("ERROR_CODE"), rs.getString("FIELD_NAME"),
                rs.getString("ERROR_MESSAGE"), rs.getInt("IS_RETRYABLE") == 1, localDateTime(rs.getTimestamp("CREATED_AT"))
        ));
    }

    public int updateBatchStatus(long batchId, String status, String actor, boolean started, boolean completed) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_BATCH
                   SET BATCH_STATUS_CODE = :status,
                       STARTED_AT = CASE WHEN :started = 1 AND STARTED_AT IS NULL THEN SYSTIMESTAMP ELSE STARTED_AT END,
                       COMPLETED_AT = CASE WHEN :completed = 1 THEN SYSTIMESTAMP ELSE NULL END,
                       UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = :actor, RECORD_VERSION = RECORD_VERSION + 1
                 WHERE OPENING_BATCH_ID = :batchId
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource().addValue("status", status).addValue("started", started ? 1 : 0)
                .addValue("completed", completed ? 1 : 0).addValue("actor", actor).addValue("batchId", batchId));
    }

    public int updateCounters(long batchId, int total, int success, int failed, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_BATCH
                   SET TOTAL_COUNT=:total, SUCCESS_COUNT=:success, FAILED_COUNT=:failed,
                       UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                 WHERE OPENING_BATCH_ID=:batchId
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource().addValue("total", total).addValue("success", success)
                .addValue("failed", failed).addValue("actor", actor).addValue("batchId", batchId));
    }

    public int updateItemStatus(long itemId, String status, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_BATCH_ITEM
                   SET ITEM_STATUS_CODE=:status, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                       RECORD_VERSION=RECORD_VERSION+1
                 WHERE OPENING_BATCH_ITEM_ID=:itemId
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource().addValue("status", status).addValue("actor", actor).addValue("itemId", itemId));
    }

    public int linkItemResult(long itemId, long requestId, long accountId, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_BATCH_ITEM
                   SET OPENING_REQUEST_ID=:requestId, ACCOUNT_ID=:accountId, ITEM_STATUS_CODE='SUCCESS',
                       UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                 WHERE OPENING_BATCH_ITEM_ID=:itemId
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource().addValue("requestId", requestId).addValue("accountId", accountId)
                .addValue("actor", actor).addValue("itemId", itemId));
    }

    public int clearErrors(long itemId, String stage) {
        return jdbc.update("DELETE FROM %s.DEPOSIT_OPENING_BATCH_ERROR WHERE OPENING_BATCH_ITEM_ID=:itemId AND ERROR_STAGE_CODE=:stage".formatted(schema),
                new MapSqlParameterSource().addValue("itemId", itemId).addValue("stage", stage));
    }

    public int insertError(long itemId, String stage, String code, String field, String message, boolean retryable, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_BATCH_ERROR (
                    OPENING_BATCH_ERROR_ID, OPENING_BATCH_ITEM_ID, ERROR_STAGE_CODE, ERROR_CODE,
                    FIELD_NAME, ERROR_MESSAGE, IS_RETRYABLE, CREATED_BY
                ) VALUES (
                    %s.SEQ_DEPOSIT_OPENING_BATCH_ERROR.NEXTVAL, :itemId, :stage, :code,
                    :field, :message, :retryable, :actor
                )
                """.formatted(schema, schema);
        return jdbc.update(sql, new MapSqlParameterSource().addValue("itemId", itemId).addValue("stage", stage)
                .addValue("code", code).addValue("field", field).addValue("message", truncate(message, 1000))
                .addValue("retryable", retryable ? 1 : 0).addValue("actor", actor));
    }

    public boolean partyExists(Long partyId) {
        if (partyId == null) return false;
        String sql = "SELECT COUNT(*) FROM %s.PARTY WHERE PARTY_ID=:id".formatted(cifSchema);
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("id", partyId), Integer.class);
        return count != null && count > 0;
    }

    public boolean productVersionExists(Long productVersionId) {
        if (productVersionId == null) return false;
        String sql = "SELECT COUNT(*) FROM %s.PRODUCT_VERSION WHERE PRODUCT_VERSION_ID=:id".formatted(productSchema);
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("id", productVersionId), Integer.class);
        return count != null && count > 0;
    }

    public boolean referenceCodeExists(String table, String column, String code) {
        String safeTable = requireIdentifier(table);
        String safeColumn = requireIdentifier(column);
        if (!REFERENCE_TABLES.contains(safeTable) || code == null || code.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM %s.%s WHERE %s=:code AND IS_ACTIVE=1 AND (VALID_FROM IS NULL OR VALID_FROM<=TRUNC(SYSDATE)) AND (VALID_TO IS NULL OR VALID_TO>=TRUNC(SYSDATE))"
                .formatted(schema, safeTable, safeColumn);
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("code", code), Integer.class);
        return count != null && count > 0;
    }

    private long nextValue(String sequence) {
        Long value = jdbc.queryForObject("SELECT %s.%s.NEXTVAL FROM DUAL".formatted(schema, requireIdentifier(sequence)), new MapSqlParameterSource(), Long.class);
        if (value == null) throw new IllegalStateException("Oracle sequence returned null: " + sequence);
        return value;
    }

    private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime localDateTime(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }
    private static String truncate(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
    private static String requireIdentifier(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_$#]*")) throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        return normalized;
    }
}
