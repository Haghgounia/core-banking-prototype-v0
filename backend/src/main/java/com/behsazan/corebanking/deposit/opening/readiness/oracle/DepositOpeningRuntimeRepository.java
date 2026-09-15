package com.behsazan.corebanking.deposit.opening.readiness.oracle;

import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.ProductVersionContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Repository
public class DepositOpeningRuntimeRepository {
    private static final Set<String> REFERENCE_TABLES = Set.of(
            "REF_DEP_OPEN_REQUEST_TYPE", "REF_DEP_OPEN_OWNERSHIP_TYPE", "REF_DEP_OPEN_CHANNEL",
            "REF_DEP_OPEN_SOURCE_OF_FUNDS", "REF_DEP_OPEN_PURPOSE", "REF_DEP_OPEN_REQUEST_STATUS",
            "REF_DEP_OPEN_PARTY_ROLE", "REF_DEP_OPEN_SIGNATURE_RULE", "REF_DEP_OPEN_SIGNATORY_ROLE",
            "REF_DEP_OPEN_VERIFICATION_STATUS", "REF_DEP_OPEN_OPERATION", "REF_DEP_OPEN_ACCESS_ROLE",
            "REF_DEP_OPEN_CHANNEL_SCOPE", "REF_DEP_OPEN_ACTION_STATUS", "REF_DEP_OPEN_DELEGATION_TYPE",
            "REF_DEP_OPEN_AUTHORITY_SCOPE", "REF_DEP_OPEN_BENEFICIARY_TYPE", "REF_DEP_OPEN_TERM_UNIT",
            "REF_DEP_OPEN_MATURITY_ACTION", "REF_DEP_OPEN_INSTRUCTION_SOURCE", "REF_DEP_OPEN_PROFIT_CALC_METHOD",
            "REF_DEP_OPEN_DAY_COUNT_BASIS", "REF_DEP_OPEN_FREQUENCY", "REF_DEP_OPEN_PAYMENT_DAY_RULE",
            "REF_DEP_OPEN_FIRST_PAYMENT_RULE", "REF_DEP_OPEN_HOLIDAY_ADJUSTMENT", "REF_DEP_OPEN_PAYMENT_DESTINATION",
            "REF_DEP_OPEN_WITHDRAWAL_MEDIA", "REF_DEP_OPEN_SERVICE", "REF_DEP_OPEN_PAYMENT_INSTRUMENT",
            "REF_DEP_OPEN_PRICING_OVERRIDE_TYPE", "REF_DEP_OPEN_AUTHORITY_LEVEL", "REF_DEP_OPEN_TAX_RESIDENCY",
            "REF_DEP_OPEN_TAX_STATUS_SOURCE", "REF_DEP_OPEN_ENROLLMENT_STATUS", "REF_DEP_OPEN_FUNDING_METHOD",
            "REF_DEP_OPEN_FUNDING_STATUS", "REF_DEP_OPEN_CHECK", "REF_DEP_OPEN_CHECK_TYPE",
            "REF_DEP_OPEN_CHECK_RESULT", "REF_DEP_OPEN_DOCUMENT_TYPE", "REF_DEP_OPEN_DOCUMENT_STATUS",
            "REF_DEP_OPEN_ACCEPTANCE_SOURCE", "REF_DEP_OPEN_ACCEPTANCE_STATUS", "REF_DEP_OPEN_DECISION",
            "REF_DEP_OPEN_DECISION_REASON", "REF_DEP_OPEN_BATCH_SOURCE_TYPE", "REF_DEP_OPEN_BATCH_STATUS",
            "REF_DEP_OPEN_BATCH_ITEM_STATUS", "REF_DEP_OPEN_BATCH_ERROR_STAGE", "REF_DEP_OPEN_BATCH_ERROR_CODE"
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;
    private final String cifSchema;
    private final String productSchema;

    public DepositOpeningRuntimeRepository(
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

    public String schema() { return schema; }
    public String cifSchema() { return cifSchema; }
    public String productSchema() { return productSchema; }

    public boolean referenceCodeExists(String table, String column, String code) {
        String safeTable = requireIdentifier(table);
        String safeColumn = requireIdentifier(column);
        if (!REFERENCE_TABLES.contains(safeTable) || code == null || code.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM %s.%s WHERE %s=:code AND IS_ACTIVE=1 "
                .formatted(schema, safeTable, safeColumn)
                + "AND (VALID_FROM IS NULL OR VALID_FROM<=TRUNC(SYSDATE)) "
                + "AND (VALID_TO IS NULL OR VALID_TO>=TRUNC(SYSDATE))";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("code", code), Integer.class);
        return count != null && count > 0;
    }

    public boolean partyExists(Long partyId) {
        if (partyId == null || partyId <= 0) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM %s.PARTY WHERE PARTY_ID=:id".formatted(cifSchema),
                new MapSqlParameterSource("id", partyId), Integer.class);
        return count != null && count > 0;
    }

    public Optional<ProductVersionContract> productVersion(Long productVersionId) {
        if (productVersionId == null || productVersionId <= 0) return Optional.empty();
        Set<String> versionColumns = columns(productSchema, "PRODUCT_VERSION");
        Set<String> productColumns = columns(productSchema, "PRODUCT");
        if (!versionColumns.contains("PRODUCT_VERSION_ID") || !versionColumns.contains("PRODUCT_ID")
                || !productColumns.contains("PRODUCT_ID")) return Optional.empty();

        String sql = "SELECT PV.PRODUCT_VERSION_ID, PV.PRODUCT_ID, "
                + selectText(productColumns, "P", "PRODUCT_CLASS_CODE") + " PRODUCT_CLASS_CODE, "
                + selectText(productColumns, "P", "PRODUCT_FAMILY_CODE") + " PRODUCT_FAMILY_CODE, "
                + selectText(productColumns, "P", "DEFAULT_CURRENCY_CODE") + " DEFAULT_CURRENCY_CODE, "
                + selectText(versionColumns, "PV", "VERSION_STATUS_CODE") + " VERSION_STATUS_CODE, "
                + selectText(versionColumns, "PV", "ORIGINATION_STATUS_CODE") + " ORIGINATION_STATUS_CODE, "
                + selectText(versionColumns, "PV", "RECORD_STATUS_CODE") + " RECORD_STATUS_CODE, "
                + selectNumber(versionColumns, "PV", "IS_CURRENT") + " IS_CURRENT, "
                + selectDate(versionColumns, "PV", "VALID_FROM") + " VALID_FROM, "
                + selectDate(versionColumns, "PV", "VALID_TO") + " VALID_TO "
                + "FROM %s.PRODUCT_VERSION PV JOIN %s.PRODUCT P ON P.PRODUCT_ID=PV.PRODUCT_ID "
                .formatted(productSchema, productSchema)
                + "WHERE PV.PRODUCT_VERSION_ID=:id";
        List<ProductVersionContract> rows = jdbc.query(sql, new MapSqlParameterSource("id", productVersionId), (rs, n) -> {
            Date from = rs.getDate("VALID_FROM");
            Date to = rs.getDate("VALID_TO");
            Integer current = rs.getObject("IS_CURRENT") == null ? null : rs.getInt("IS_CURRENT");
            return new ProductVersionContract(
                    rs.getLong("PRODUCT_VERSION_ID"), rs.getLong("PRODUCT_ID"),
                    rs.getString("PRODUCT_CLASS_CODE"), rs.getString("PRODUCT_FAMILY_CODE"),
                    rs.getString("DEFAULT_CURRENCY_CODE"), rs.getString("VERSION_STATUS_CODE"),
                    rs.getString("ORIGINATION_STATUS_CODE"), rs.getString("RECORD_STATUS_CODE"),
                    current, from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate()
            );
        });
        return rows.stream().findFirst();
    }

    public boolean tableExists(String owner, String table) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER=:owner AND TABLE_NAME=:table",
                new MapSqlParameterSource().addValue("owner", requireIdentifier(owner)).addValue("table", requireIdentifier(table)),
                Integer.class);
        return count != null && count > 0;
    }

    public boolean sequenceExists(String owner, String sequence) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER=:owner AND SEQUENCE_NAME=:name",
                new MapSqlParameterSource().addValue("owner", requireIdentifier(owner)).addValue("name", requireIdentifier(sequence)),
                Integer.class);
        return count != null && count > 0;
    }

    public boolean triggerEnabled(String owner, String trigger) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ALL_TRIGGERS WHERE OWNER=:owner AND TRIGGER_NAME=:name AND STATUS='ENABLED'",
                new MapSqlParameterSource().addValue("owner", requireIdentifier(owner)).addValue("name", requireIdentifier(trigger)),
                Integer.class);
        return count != null && count > 0;
    }

    public boolean uniqueIndexExists(String owner, String indexName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ALL_INDEXES WHERE OWNER=:owner AND INDEX_NAME=:name AND UNIQUENESS='UNIQUE'",
                new MapSqlParameterSource().addValue("owner", requireIdentifier(owner)).addValue("name", requireIdentifier(indexName)),
                Integer.class);
        return count != null && count > 0;
    }

    public long nextBatchId() {
        Long value = jdbc.queryForObject("SELECT %s.SEQ_DEPOSIT_OPENING_BATCH.NEXTVAL FROM DUAL".formatted(schema), new MapSqlParameterSource(), Long.class);
        if (value == null) throw new IllegalStateException("Oracle sequence returned null");
        return value;
    }

    public int insertProbeBatch(long batchId, String idempotencyKey) {
        return jdbc.update("""
                INSERT INTO %s.DEPOSIT_OPENING_BATCH (
                    OPENING_BATCH_ID, BATCH_NO, IDEMPOTENCY_KEY, SOURCE_TYPE_CODE, SOURCE_REFERENCE,
                    TOTAL_COUNT, SUCCESS_COUNT, FAILED_COUNT, BATCH_STATUS_CODE, CREATED_BY
                ) VALUES (:id, :batchNo, :key, 'MANUAL', 'PHASE7_ROLLBACK_PROBE', 0, 0, 0, 'DRAFT', 'PHASE7_PROBE')
                """.formatted(schema), new MapSqlParameterSource()
                .addValue("id", batchId).addValue("batchNo", "PROBE-" + batchId).addValue("key", idempotencyKey));
    }

    public int countBatchByIdempotency(String idempotencyKey) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM %s.DEPOSIT_OPENING_BATCH WHERE IDEMPOTENCY_KEY=:key".formatted(schema),
                new MapSqlParameterSource("key", idempotencyKey), Integer.class);
        return count == null ? 0 : count;
    }

    private Set<String> columns(String owner, String table) {
        String sql = "SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER=:owner AND TABLE_NAME=:table";
        return new HashSet<>(jdbc.query(sql,
                new MapSqlParameterSource().addValue("owner", owner).addValue("table", requireIdentifier(table)),
                (rs, n) -> rs.getString("COLUMN_NAME")));
    }

    private static String selectText(Set<String> columns, String alias, String column) {
        return columns.contains(column) ? alias + "." + column : "CAST(NULL AS VARCHAR2(100))";
    }
    private static String selectNumber(Set<String> columns, String alias, String column) {
        return columns.contains(column) ? alias + "." + column : "CAST(NULL AS NUMBER)";
    }
    private static String selectDate(Set<String> columns, String alias, String column) {
        return columns.contains(column) ? alias + "." + column : "CAST(NULL AS DATE)";
    }

    private static String requireIdentifier(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_$#]*")) throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        return normalized;
    }
}
