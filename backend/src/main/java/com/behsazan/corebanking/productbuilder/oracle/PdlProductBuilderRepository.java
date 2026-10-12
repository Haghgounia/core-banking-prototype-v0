package com.behsazan.corebanking.productbuilder.oracle;

import com.behsazan.corebanking.productbuilder.application.PdlCatalog;
import com.behsazan.corebanking.productbuilder.application.ProductBuilderValidationException;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ColumnDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class PdlProductBuilderRepository {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]*");
    private static final Pattern IN_CLAUSE = Pattern.compile("(?is)([A-Z][A-Z0-9_$#]*)\\s+IN\\s*\\(([^()]*)\\)");
    private static final Set<String> SYSTEM_MANAGED = Set.of(
            "CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "RECORD_VERSION", "MIGRATED_AT", "IS_DELETED"
    );

    private final JdbcClient jdbcClient;
    private final String schemaName;
    private final Map<String, TableDescriptor> descriptorCache = new ConcurrentHashMap<>();

    public PdlProductBuilderRepository(JdbcClient jdbcClient,
                                       @Value("${core-banking.schemas.product-definition:PDL}") String schemaName) {
        this.jdbcClient = jdbcClient;
        this.schemaName = identifier(schemaName);
    }

    public String schemaName() {
        return schemaName;
    }

    public TableDescriptor descriptor(String requestedTable) {
        String table = supportedTable(requestedTable);
        return descriptorCache.computeIfAbsent(table, this::loadDescriptor);
    }

    public long count(String requestedTable) {
        String table = supportedTable(requestedTable);
        return jdbcClient.sql("SELECT COUNT(*) FROM " + qualified(table)).query(Long.class).single();
    }

    /** Fail closed if multiple versions claim IS_CURRENT for the same product. */
    public long currentProductVersionCount(long productId) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM " + qualified("PRODUCT_VERSION")
                        + " WHERE PRODUCT_ID = :productId AND IS_CURRENT = 1")
                .param("productId", productId).query(Long.class).single();
        return count == null ? 0L : count;
    }

    public long previewNextProductVersionNo(long productId) {
        return jdbcClient.sql("SELECT NVL(MAX(VERSION_NO), 0) + 1 FROM " + qualified("PRODUCT_VERSION")
                        + " WHERE PRODUCT_ID = :productId")
                .param("productId", productId)
                .query(Long.class)
                .single();
    }

    /** Guard against a version-status change racing with finalization. */
    public void lockProductVersion(long versionId) {
        jdbcClient.sql("SELECT PRODUCT_VERSION_ID FROM " + qualified("PRODUCT_VERSION")
                        + " WHERE PRODUCT_VERSION_ID = :versionId FOR UPDATE")
                .param("versionId", versionId).query(Long.class)
                .optional().orElseThrow(() -> new ProductBuilderValidationException(
                        "نسخه برای نهایی‌سازی پیدا نشد."));
    }

    /** Serialize collateral ordering per version, rather than locking the whole PDL table. */
    /** Serializes finalization per product. A second request sees the already-issued code. */
    public void lockProduct(long productId) {
        jdbcClient.sql("SELECT PRODUCT_ID FROM " + qualified("PRODUCT")
                        + " WHERE PRODUCT_ID = :productId FOR UPDATE")
                .param("productId", productId).query(Long.class)
                .optional().orElseThrow(() -> new ProductBuilderValidationException(
                        "محصول برای نهایی‌سازی پیدا نشد."));
    }

    public void lockCollateralProductVersion(long versionId) {
        jdbcClient.sql("SELECT PRODUCT_VERSION_ID FROM " + qualified("PRODUCT_VERSION")
                        + " WHERE PRODUCT_VERSION_ID = :versionId FOR UPDATE")
                .param("versionId", versionId).query(Long.class).single();
    }

    public long lockAndNextProductVersionNo(long productId) {
        jdbcClient.sql("LOCK TABLE " + qualified("PRODUCT_VERSION") + " IN SHARE ROW EXCLUSIVE MODE").update();
        return previewNextProductVersionNo(productId);
    }

    /** One row maintained by bank operations, not inferred from the OS or the Jalali year. */
    public int activeLoanProductWorkingYear() {
        Integer year = jdbcClient.sql("SELECT WORK_YEAR FROM " + schemaName
                        + ".PRODUCT_LOAN_WORK_YEAR WHERE SETTING_ID = 1 FOR UPDATE")
                .query(Integer.class).optional().orElse(null);
        if (year == null) throw new ProductBuilderValidationException(
                "سال کاری صدور کد محصول تسهیلات تنظیم نشده است؛ ابتدا PDL.PRODUCT_LOAN_WORK_YEAR را با مجوز راهبر تنظیم کنید.");
        return year;
    }

    /** This lookup is for first-step product identity, before a version/profile exists. */
    public List<Map<String, Object>> activeLoanTypesForCode() {
        return jdbcClient.sql("SELECT LOAN_TYPE_ID, LOAN_TYPE_CODE, LOAN_TYPE_NAME, LOAN_GROUP FROM "
                        + qualified("LOAN_TYPE") + " WHERE RECORD_STATUS_CODE = 'ACTIVE' ORDER BY LOAN_GROUP, LOAN_TYPE_NAME")
                .query((rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("LOAN_TYPE_ID"));
                    row.put("code", rs.getString("LOAN_TYPE_CODE"));
                    row.put("label", rs.getString("LOAN_TYPE_NAME"));
                    row.put("group", rs.getInt("LOAN_GROUP"));
                    return row;
                }).list();
    }

    public record GeneratedLoanCode(long typeId, int year, int serial, String code) { }

    /** Call only from finalization's @Transactional boundary. FOR UPDATE lock on
     *  LOAN_TYPE serializes every generator for a contract, including annual reset.
     */
    public GeneratedLoanCode allocateLoanProductCode(long typeId, String contractCode) {
        lockLoanTypeForMapping(typeId);
        int year = activeLoanProductWorkingYear();
        Integer maximum = jdbcClient.sql("SELECT NVL(MAX(SERIAL_NO), 0) FROM " + schemaName
                        + ".PRODUCT_LOAN_CODE_REGISTRY WHERE LOAN_TYPE_ID = :typeId AND WORK_YEAR = :workYear")
                .param("typeId", typeId).param("workYear", year).query(Integer.class).single();
        for (int serial = maximum + 1; serial <= 999; serial++) {
            String code = com.behsazan.corebanking.productbuilder.application.LoanProductCodePolicy.format(contractCode, year, serial);
            Long existing = jdbcClient.sql("SELECT COUNT(*) FROM " + qualified("PRODUCT") + " WHERE PRODUCT_CODE = :code")
                    .param("code", code).query(Long.class).single();
            if (existing == 0) return new GeneratedLoanCode(typeId, year, serial, code);
        }
        throw new ProductBuilderValidationException("ظرفیت ۹۹۹ کد برای این عقد در سال کاری تکمیل است.");
    }

    public void registerLoanProductCode(long productId, GeneratedLoanCode code, String actor) {
        jdbcClient.sql("INSERT INTO " + schemaName + ".PRODUCT_LOAN_CODE_REGISTRY"
                + " (PRODUCT_ID, LOAN_TYPE_ID, WORK_YEAR, SERIAL_NO, PRODUCT_CODE, CREATED_BY)"
                + " VALUES (:product, :loan, :year, :serial, :code, :actor)")
                .param("product", productId).param("loan", code.typeId())
                .param("year", code.year()).param("serial", code.serial())
                .param("code", code.code()).param("actor", actor).update();
    }

    /** Legacy products have no registry row and keep their original, immutable code. */
    public Optional<Long> registeredLoanType(long productId) {
        return jdbcClient.sql("SELECT LOAN_TYPE_ID FROM " + schemaName
                        + ".PRODUCT_LOAN_CODE_REGISTRY WHERE PRODUCT_ID = :product")
                .param("product", productId).query(Long.class).optional();
    }

    /** A single bank-approved year row locks code issuance for both domains.
     *  This eliminates family/year races without introducing a second clock.
     */
    public record GeneratedDepositCode(String family, int year, int serial, String code) { }

    public GeneratedDepositCode allocateDepositProductCode(String family) {
        int year = activeLoanProductWorkingYear(); // SELECT FOR UPDATE: serializes concurrent deposit issuance.
        Integer maximum = jdbcClient.sql("SELECT NVL(MAX(SERIAL_NO), 0) FROM " + schemaName
                        + ".PRODUCT_DEPOSIT_CODE_REGISTRY WHERE PRODUCT_FAMILY_CODE = :family AND WORK_YEAR = :workYear")
                .param("family", family).param("workYear", year).query(Integer.class).single();
        for (int serial = maximum + 1; serial <= 999; serial++) {
            String code = com.behsazan.corebanking.productbuilder.application.DepositProductCodePolicy.format(family, year, serial);
            Long used = jdbcClient.sql("SELECT COUNT(*) FROM " + qualified("PRODUCT") + " WHERE PRODUCT_CODE = :code")
                    .param("code", code).query(Long.class).single();
            if (used == 0) return new GeneratedDepositCode(family, year, serial, code);
        }
        throw new ProductBuilderValidationException("ظرفیت ۹۹۹ کد این خانواده سپرده در سال کاری تکمیل شده است.");
    }

    public void registerDepositProductCode(long productId, GeneratedDepositCode code, String actor) {
        jdbcClient.sql("INSERT INTO " + schemaName + ".PRODUCT_DEPOSIT_CODE_REGISTRY"
                + " (PRODUCT_ID, PRODUCT_FAMILY_CODE, WORK_YEAR, SERIAL_NO, PRODUCT_CODE, CREATED_BY)"
                + " VALUES (:product, :family, :year, :serial, :code, :actor)")
                .param("product", productId).param("family", code.family())
                .param("year", code.year()).param("serial", code.serial())
                .param("code", code.code()).param("actor", actor).update();
    }

    /** Existing coded deposits predate the R30 registry and remain immutable. */
    public Optional<String> registeredDepositFamily(long productId) {
        return jdbcClient.sql("SELECT PRODUCT_FAMILY_CODE FROM " + schemaName
                        + ".PRODUCT_DEPOSIT_CODE_REGISTRY WHERE PRODUCT_ID = :product")
                .param("product", productId).query(String.class).optional();
    }

    public List<String> loanEligibilityEmploymentTypes(long eligibilityRuleId) {
        return jdbcClient.sql("SELECT EMPLOYMENT_TYPE_CODE FROM " + schemaName
                        + ".LOAN_ELIGIBLE_EMPLOYMENT_TYPE WHERE ELIGIBILITY_RULE_ID = :id ORDER BY EMPLOYMENT_TYPE_CODE")
                .param("id", eligibilityRuleId).query(String.class).list();
    }

    public void replaceLoanEligibilityEmploymentTypes(long eligibilityRuleId, List<String> codes, String actor) {
        jdbcClient.sql("DELETE FROM " + schemaName + ".LOAN_ELIGIBLE_EMPLOYMENT_TYPE WHERE ELIGIBILITY_RULE_ID = :id")
                .param("id", eligibilityRuleId).update();
        for (String code : codes)
            jdbcClient.sql("INSERT INTO " + schemaName + ".LOAN_ELIGIBLE_EMPLOYMENT_TYPE"
                    + " (ELIGIBILITY_RULE_ID, EMPLOYMENT_TYPE_CODE, CREATED_BY) VALUES (:id, :code, :actor)")
                    .param("id", eligibilityRuleId).param("code", code).param("actor", actor).update();
    }

    public LocalDate currentDatabaseDate() {
        return jdbcClient.sql("SELECT TRUNC(SYSDATE) AS SYSTEM_DATE FROM DUAL")
                .query((rs, rowNum) -> rs.getDate("SYSTEM_DATE").toLocalDate())
                .single();
    }

    public LocalDateTime currentDatabaseDateTime() {
        return jdbcClient.sql("SELECT CAST(SYSTIMESTAMP AS TIMESTAMP) AS SYSTEM_DATE_TIME FROM DUAL")
                .query((rs, rowNum) -> rs.getTimestamp("SYSTEM_DATE_TIME").toLocalDateTime())
                .single();
    }

    public TablePage search(String requestedTable, String text, int page, int size,
                            String filterColumn, String filterValue) {
        TableDescriptor descriptor = descriptor(requestedTable);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        if (text != null && !text.isBlank()) {
            List<String> textColumns = descriptor.columns().stream()
                    .filter(c -> isTextType(c.dataType()))
                    .filter(c -> !"CLOB".equals(c.dataType()))
                    .map(ColumnDescriptor::name)
                    .toList();
            if (!textColumns.isEmpty()) {
                List<String> parts = textColumns.stream()
                        .map(c -> "UPPER(TO_CHAR(T." + identifier(c) + ")) LIKE :searchText")
                        .toList();
                where.add("(" + String.join(" OR ", parts) + ")");
                params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
            }
        }

        if (filterColumn != null && !filterColumn.isBlank() && filterValue != null && !filterValue.isBlank()) {
            ColumnDescriptor column = column(descriptor, filterColumn);
            where.add("T." + identifier(column.name()) + " = :filterValue");
            params.put("filterValue", databaseValue(column, filterValue));
        }

        if (descriptor.columns().stream().anyMatch(c -> c.name().equals("IS_DELETED"))) {
            where.add("NVL(T.IS_DELETED, 0) = 0");
        }

        String fromWhere = " FROM " + qualified(descriptor.tableName()) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where));
        long total = jdbcClient.sql("SELECT COUNT(*)" + fromWhere).params(params).query(Long.class).single();

        // Collateral priorities are scoped per product version. Show ascending rank,
        // not reverse insertion order; keep other PDL lists unchanged.
        String order = "LOAN_PRODUCT_COLLATERAL_RULE".equals(descriptor.tableName())
                ? " ORDER BY T.PRODUCT_VERSION_ID DESC, T.PRIORITY_NO ASC, T.LOAN_COLLATERAL_RULE_ID ASC"
                : " ORDER BY T." + identifier(descriptor.primaryKeyColumn()) + " DESC";
        String sql = "SELECT T.*" + fromWhere + order
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        Map<String, Object> pageParams = new LinkedHashMap<>(params);
        pageParams.put("offset", safePage * safeSize);
        pageParams.put("pageSize", safeSize);
        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(pageParams)
                .query((rs, rowNum) -> mapRow(rs, descriptor.columns()))
                .list();
        return new TablePage(rows, total, safePage, safeSize);
    }

    public Optional<Map<String, Object>> findById(String requestedTable, long id) {
        TableDescriptor descriptor = descriptor(requestedTable);
        String sql = "SELECT T.* FROM " + qualified(descriptor.tableName()) + " T WHERE T."
                + identifier(descriptor.primaryKeyColumn()) + " = :id";
        return jdbcClient.sql(sql).param("id", id)
                .query((rs, rowNum) -> mapRow(rs, descriptor.columns()))
                .optional();
    }

    public long insert(String requestedTable, Map<String, Object> values, String actor) {
        TableDescriptor descriptor = descriptor(requestedTable);
        ColumnDescriptor pk = column(descriptor, descriptor.primaryKeyColumn());
        long id = numericId(values.get(pk.name()));
        if (id <= 0) id = allocateId(descriptor);

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        columns.add(identifier(pk.name()));
        placeholders.add(":" + pk.name());
        params.put(pk.name(), id);

        for (ColumnDescriptor column : descriptor.columns()) {
            if (column.primaryKey() || SYSTEM_MANAGED.contains(column.name())) continue;
            if (!values.containsKey(column.name())) continue;
            Object value = values.get(column.name());
            if (isBlank(value) && hasDatabaseDefault(column.defaultValue())) continue;
            columns.add(identifier(column.name()));
            placeholders.add(":" + column.name());
            params.put(column.name(), databaseValue(column, value));
        }

        addSystemInsertColumns(descriptor, actor, columns, placeholders, params);
        validateRequiredColumns(descriptor, values, columns);
        String sql = "INSERT INTO " + qualified(descriptor.tableName()) + " (" + String.join(", ", columns)
                + ") VALUES (" + String.join(", ", placeholders) + ")";
        jdbcClient.sql(sql).params(params).update();
        return id;
    }

    public boolean update(String requestedTable, long id, Map<String, Object> values, String actor) {
        TableDescriptor descriptor = descriptor(requestedTable);
        List<String> assignments = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        for (ColumnDescriptor column : descriptor.columns()) {
            if (column.primaryKey() || SYSTEM_MANAGED.contains(column.name())) continue;
            if (!values.containsKey(column.name())) continue;
            assignments.add(identifier(column.name()) + " = :" + column.name());
            params.put(column.name(), databaseValue(column, values.get(column.name())));
        }
        if (hasColumn(descriptor, "UPDATED_AT")) assignments.add("UPDATED_AT = SYSTIMESTAMP");
        if (hasColumn(descriptor, "UPDATED_BY")) {
            assignments.add("UPDATED_BY = :UPDATED_BY");
            params.put("UPDATED_BY", actor);
        }
        if (hasColumn(descriptor, "RECORD_VERSION")) assignments.add("RECORD_VERSION = RECORD_VERSION + 1");
        if (assignments.isEmpty()) throw new ProductBuilderValidationException("No editable values supplied");
        params.put("id", id);
        String sql = "UPDATE " + qualified(descriptor.tableName()) + " SET " + String.join(", ", assignments)
                + " WHERE " + identifier(descriptor.primaryKeyColumn()) + " = :id";
        return jdbcClient.sql(sql).params(params).update() == 1;
    }

    public boolean delete(String requestedTable, long id, String actor) {
        TableDescriptor descriptor = descriptor(requestedTable);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id);
        if (hasColumn(descriptor, "IS_DELETED")) {
            List<String> assignments = new ArrayList<>(List.of("IS_DELETED = 1"));
            if (hasColumn(descriptor, "UPDATED_AT")) assignments.add("UPDATED_AT = SYSTIMESTAMP");
            if (hasColumn(descriptor, "UPDATED_BY")) {
                assignments.add("UPDATED_BY = :actor");
                params.put("actor", actor);
            }
            if (hasColumn(descriptor, "RECORD_VERSION")) assignments.add("RECORD_VERSION = RECORD_VERSION + 1");
            String sql = "UPDATE " + qualified(descriptor.tableName()) + " SET " + String.join(", ", assignments)
                    + " WHERE " + identifier(descriptor.primaryKeyColumn()) + " = :id";
            return jdbcClient.sql(sql).params(params).update() == 1;
        }
        String sql = "DELETE FROM " + qualified(descriptor.tableName()) + " WHERE "
                + identifier(descriptor.primaryKeyColumn()) + " = :id";
        return jdbcClient.sql(sql).params(params).update() == 1;
    }

    public List<SelectOption> lookup(String requestedTable, String requestedColumn, String text, int limit) {
        TableDescriptor child = descriptor(requestedTable);
        ColumnDescriptor fk = column(child, requestedColumn);
        // Some deployed PDL schemas omit the physical FK for this legacy field.
        // Its logical master is nevertheless governed by ProductBuilderService.
        boolean loanCollateral = "LOAN_PRODUCT_COLLATERAL_RULE".equals(child.tableName())
                && "LOAN_PRODUCT_COLLATERAL_ID".equals(fk.name());
        if (!loanCollateral && (!fk.foreignKey() || fk.parentTable() == null)) {
            throw new ProductBuilderValidationException("Column is not a foreign key: " + requestedColumn);
        }
        TableDescriptor parent = descriptor(loanCollateral ? "LOAN_PRODUCT_COLLATERAL" : fk.parentTable());
        // DESCRIPTION is nullable and may be blank: the collateral reference must
        // display its official PRODUCT_COLLATERAL_NAME and business code instead.
        ColumnDescriptor label = loanCollateral ? column(parent, "PRODUCT_COLLATERAL_NAME") : labelColumn(parent);
        ColumnDescriptor code = loanCollateral ? column(parent, "PRODUCT_COLLATERAL_CODE") : codeColumn(parent).orElse(label);
        // Facility operation parameters have no name column; use actual persisted periods,
        // never a status code or a synthetic reference description as the display label.
        boolean facilityParameter = "FACILITY_OPERATION_PARAMETER".equals(parent.tableName());
        String labelSql = facilityParameter
                ? "'پارامتر ' || TO_CHAR(T.OPERATION_PARAMETER_ID) || ' - سررسید گذشته ' || TO_CHAR(T.OVERDUE_DURATION) || ' / معوق ' || TO_CHAR(T.PASTDUE_DURATION)"
                : "TO_CHAR(T." + identifier(label.name()) + ")";
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            where.add("(UPPER(TO_CHAR(T." + identifier(code.name()) + ")) LIKE :q OR UPPER(" + labelSql + ") LIKE :q)");
            params.put("q", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (hasColumn(parent, "IS_ACTIVE")) where.add("NVL(T.IS_ACTIVE, 1) = 1");
        if (hasColumn(parent, "IS_DELETED")) where.add("NVL(T.IS_DELETED, 0) = 0");
        if (hasColumn(parent, "RECORD_STATUS_CODE")) where.add("T.RECORD_STATUS_CODE = 'ACTIVE'");
        params.put("limit", safeLimit);
        String sql = "SELECT T." + identifier(parent.primaryKeyColumn()) + " AS V, TO_CHAR(T."
                + identifier(code.name()) + ") AS C, " + labelSql + " AS L FROM "
                + qualified(parent.tableName()) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY T." + identifier(label.name()) + " FETCH FIRST :limit ROWS ONLY";
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) ->
                new SelectOption(rs.getObject("V"), rs.getString("C"), rs.getString("L"))).list();
    }

    public boolean loanReferenceCodeExists(String table, String column, String code, Long exceptId) {
        // Table and column names are fixed by our service allowlist, not user input.
        if (!("LOAN_TYPE".equals(table) && "LOAN_TYPE_CODE".equals(column)) &&
            !("PLAN_TYPE".equals(table) && "PLAN_TYPE_CODE".equals(column)) &&
            !("LOAN_PRODUCT_COLLATERAL".equals(table) && "PRODUCT_COLLATERAL_CODE".equals(column)))
            throw new ProductBuilderValidationException("Invalid reference key.");
        String idColumn = "LOAN_PRODUCT_COLLATERAL".equals(table) ? "LOAN_PRODUCT_COLLATERAL_ID" : table + "_ID";
        String sql = "SELECT COUNT(*) FROM " + qualified(table)
            + " WHERE UPPER(TRIM(" + column + ")) = UPPER(TRIM(:code))"
            + (exceptId == null ? "" : " AND " + idColumn + " <> :id");
        var spec = jdbcClient.sql(sql).param("code", code);
        if (exceptId != null) spec = spec.param("id", exceptId);
        Long count = spec.query(Long.class).single();
        return count != null && count > 0;
    }

    public void lockLoanTypeForMapping(long loanTypeId) {
        // Called inside ProductBuilderService's @Transactional create/update boundary.
        jdbcClient.sql("SELECT LOAN_TYPE_ID FROM " + qualified("LOAN_TYPE")
                + " WHERE LOAN_TYPE_ID = :id FOR UPDATE")
            .param("id", loanTypeId).query(Long.class).single();
    }

    public boolean loanTypePlanPairExists(long loanTypeId, long planTypeId, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + qualified("LOAN_TYPE_PLAN_TYPE")
            + " WHERE LOAN_TYPE_ID=:loan AND PLAN_TYPE_ID=:plan"
            + (exceptId == null ? "" : " AND LOAN_TYPE_PLAN_TYPE_ID <> :id");
        var spec = jdbcClient.sql(sql).param("loan", loanTypeId).param("plan", planTypeId);
        if (exceptId != null) spec = spec.param("id", exceptId);
        Long count = spec.query(Long.class).single();
        return count != null && count > 0;
    }

    /** Source-of-truth for family selection: the four business loan groups
     *  represented by ACTIVE loan contracts in the actual PDL catalog.
     *  A DISTINCT query prevents empty/truncated choices from UI paging.
     */
    public List<Integer> activeLoanGroupCodes() {
        return jdbcClient.sql("SELECT DISTINCT LOAN_GROUP FROM " + qualified("LOAN_TYPE")
                        + " WHERE RECORD_STATUS_CODE='ACTIVE' AND LOAN_GROUP BETWEEN 1 AND 4 ORDER BY LOAN_GROUP")
                .query(Integer.class).list();
    }

    /** Only active PDL loan types make a loan-group family available for NEW products. */
    public boolean activeLoanGroupExists(int groupCode) {
        if (groupCode < 1 || groupCode > 4) return false;
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM " + qualified("LOAN_TYPE")
                        + " WHERE LOAN_GROUP = :groupCode AND RECORD_STATUS_CODE = 'ACTIVE'")
                .param("groupCode", groupCode).query(Long.class).single();
        return count != null && count > 0;
    }

    public boolean loanProductHasProfiles(long productId) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM " + qualified("LOAN_PRODUCT_PROFILE") + " L JOIN "
                        + qualified("PRODUCT_VERSION") + " V ON V.PRODUCT_VERSION_ID = L.PRODUCT_VERSION_ID"
                        + " WHERE V.PRODUCT_ID = :productId")
                .param("productId", productId).query(Long.class).single();
        return count != null && count > 0;
    }

    public boolean loanTypeHasProfiles(long loanTypeId) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM " + qualified("LOAN_PRODUCT_PROFILE")
                        + " WHERE LOAN_TYPE_ID = :loanTypeId")
                .param("loanTypeId", loanTypeId).query(Long.class).single();
        return count != null && count > 0;
    }

    public boolean loanTypePlanAllowed(long loanTypeId, long planTypeId) {
        if (loanTypeId <= 0 || planTypeId <= 0) return false;
        String sql = "SELECT COUNT(*) FROM " + qualified("LOAN_TYPE_PLAN_TYPE")
                + " WHERE LOAN_TYPE_ID=:loan AND PLAN_TYPE_ID=:plan AND RECORD_STATUS_CODE='ACTIVE'";
        Integer count = jdbcClient.sql(sql).param("loan", loanTypeId).param("plan", planTypeId)
                .query(Integer.class).single();
        return count != null && count > 0;
    }

    public boolean loanEconomicSubSectionAllowed(long planTypeId, long subSectionId) {
        if (planTypeId <= 0 || subSectionId <= 0) return false;
        String sql = "SELECT COUNT(*) FROM " + qualified("ECONOMIC_SUB_SECTION") + " D JOIN "
                + qualified("ECONOMIC_SECTION") + " S ON S.ECONOMIC_SECTION_ID=D.ECONOMIC_SECTION_ID "
                + "WHERE D.ECONOMIC_SUB_SECTION_ID=:sub AND S.PLAN_TYPE_ID=:plan "
                + "AND D.RECORD_STATUS_CODE='ACTIVE' AND S.RECORD_STATUS_CODE='ACTIVE'";
        Integer count = jdbcClient.sql(sql).param("sub", subSectionId).param("plan", planTypeId)
                .query(Integer.class).single();
        return count != null && count > 0;
    }

    private TableDescriptor loadDescriptor(String table) {
        PdlCatalog.Entry entry = PdlCatalog.require(table);
        Map<String, ForeignKeyInfo> foreignKeys = loadForeignKeys(table);
        String pk = loadPrimaryKey(table);
        Map<String, List<SelectOption>> checkOptions = loadCheckOptions(table);
        String sql = "SELECT C.COLUMN_NAME, C.DATA_TYPE, C.DATA_LENGTH, C.DATA_PRECISION, C.DATA_SCALE, C.NULLABLE, "
                + "C.DATA_DEFAULT, CC.COMMENTS FROM ALL_TAB_COLUMNS C LEFT JOIN ALL_COL_COMMENTS CC "
                + "ON CC.OWNER=C.OWNER AND CC.TABLE_NAME=C.TABLE_NAME AND CC.COLUMN_NAME=C.COLUMN_NAME "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table ORDER BY C.COLUMN_ID";
        List<ColumnDescriptor> columns = jdbcClient.sql(sql).param("owner", schemaName).param("table", table)
                .query((rs, rowNum) -> {
                    String name = rs.getString("COLUMN_NAME");
                    ForeignKeyInfo fk = foreignKeys.get(name);
                    String comments = rs.getString("COMMENTS");
                    String defaultValue = safeString(rs, "DATA_DEFAULT");
                    return new ColumnDescriptor(
                            name,
                            comments == null || comments.isBlank() ? name : comments,
                            normalizeType(rs.getString("DATA_TYPE")),
                            integerOrNull(rs.getObject("DATA_LENGTH")),
                            integerOrNull(rs.getObject("DATA_PRECISION")),
                            integerOrNull(rs.getObject("DATA_SCALE")),
                            "Y".equals(rs.getString("NULLABLE")),
                            name.equals(pk),
                            fk != null,
                            fk == null ? null : fk.parentTable(),
                            fk == null ? null : fk.parentColumn(),
                            name.equals(pk) || SYSTEM_MANAGED.contains(name),
                            defaultValue == null ? null : defaultValue.trim(),
                            checkOptions.getOrDefault(name, List.of()),
                            false,
                            null
                    );
                }).list();
        if (columns.isEmpty()) throw new ProductBuilderValidationException("PDL table not visible to datasource user: " + table);
        return new TableDescriptor(schemaName, table, entry.title(), entry.packageCode(), entry.packageTitle(), pk, columns);
    }

    private String loadPrimaryKey(String table) {
        String sql = "SELECT CC.COLUMN_NAME FROM ALL_CONSTRAINTS C JOIN ALL_CONS_COLUMNS CC "
                + "ON CC.OWNER=C.OWNER AND CC.CONSTRAINT_NAME=C.CONSTRAINT_NAME "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table AND C.CONSTRAINT_TYPE='P' ORDER BY CC.POSITION";
        List<String> columns = jdbcClient.sql(sql).param("owner", schemaName).param("table", table)
                .query(String.class).list();
        if (columns.size() != 1) throw new ProductBuilderValidationException("PDL table must have one-column PK: " + table);
        return columns.getFirst();
    }

    private Map<String, ForeignKeyInfo> loadForeignKeys(String table) {
        String sql = "SELECT CC.COLUMN_NAME, P.TABLE_NAME AS PARENT_TABLE, PCC.COLUMN_NAME AS PARENT_COLUMN "
                + "FROM ALL_CONSTRAINTS C JOIN ALL_CONS_COLUMNS CC ON CC.OWNER=C.OWNER AND CC.CONSTRAINT_NAME=C.CONSTRAINT_NAME "
                + "JOIN ALL_CONSTRAINTS P ON P.OWNER=C.R_OWNER AND P.CONSTRAINT_NAME=C.R_CONSTRAINT_NAME "
                + "JOIN ALL_CONS_COLUMNS PCC ON PCC.OWNER=P.OWNER AND PCC.CONSTRAINT_NAME=P.CONSTRAINT_NAME AND PCC.POSITION=CC.POSITION "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table AND C.CONSTRAINT_TYPE='R'";
        Map<String, ForeignKeyInfo> result = new HashMap<>();
        jdbcClient.sql(sql).param("owner", schemaName).param("table", table)
                .query((rs, rowNum) -> new ForeignKeyInfo(rs.getString("COLUMN_NAME"), rs.getString("PARENT_TABLE"), rs.getString("PARENT_COLUMN")))
                .list().forEach(fk -> result.put(fk.column(), fk));
        return result;
    }

    private Map<String, List<SelectOption>> loadCheckOptions(String table) {
        String sql = "SELECT SEARCH_CONDITION_VC FROM ALL_CONSTRAINTS WHERE OWNER=:owner AND TABLE_NAME=:table "
                + "AND CONSTRAINT_TYPE='C' AND SEARCH_CONDITION_VC IS NOT NULL";
        Map<String, LinkedHashSet<String>> raw = new LinkedHashMap<>();
        jdbcClient.sql(sql).param("owner", schemaName).param("table", table).query(String.class).list().forEach(condition -> {
            Matcher matcher = IN_CLAUSE.matcher(condition);
            while (matcher.find()) {
                String column = matcher.group(1).toUpperCase(Locale.ROOT);
                for (String token : splitValues(matcher.group(2))) {
                    raw.computeIfAbsent(column, k -> new LinkedHashSet<>()).add(token);
                }
            }
        });
        Map<String, List<SelectOption>> result = new LinkedHashMap<>();
        raw.forEach((column, values) -> result.put(column, values.stream().map(this::checkOption).toList()));
        return result;
    }

    private SelectOption checkOption(String token) {
        String value = token.trim();
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1).replace("''", "'");
            return new SelectOption(value, value, value);
        }
        try {
            BigDecimal number = new BigDecimal(value);
            return new SelectOption(number, value, value);
        } catch (NumberFormatException ex) {
            return new SelectOption(value, value, value);
        }
    }

    private static List<String> splitValues(String raw) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\'' && (i + 1 >= raw.length() || raw.charAt(i + 1) != '\'')) quoted = !quoted;
            if (ch == ',' && !quoted) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
                if (ch == '\'' && quoted && i + 1 < raw.length() && raw.charAt(i + 1) == '\'') {
                    current.append(raw.charAt(++i));
                }
            }
        }
        if (!current.isEmpty()) result.add(current.toString().trim());
        return result;
    }

    private long allocateId(TableDescriptor descriptor) {
        jdbcClient.sql("LOCK TABLE " + qualified(descriptor.tableName()) + " IN SHARE ROW EXCLUSIVE MODE").update();
        String sql = "SELECT NVL(MAX(" + identifier(descriptor.primaryKeyColumn()) + "), 0) + 1 FROM "
                + qualified(descriptor.tableName());
        return jdbcClient.sql(sql).query(Long.class).single();
    }

    private void addSystemInsertColumns(TableDescriptor descriptor, String actor, List<String> columns,
                                        List<String> placeholders, Map<String, Object> params) {
        if (hasColumn(descriptor, "CREATED_AT")) {
            columns.add("CREATED_AT"); placeholders.add("SYSTIMESTAMP");
        }
        if (hasColumn(descriptor, "CREATED_BY")) {
            columns.add("CREATED_BY"); placeholders.add(":CREATED_BY"); params.put("CREATED_BY", actor);
        }
        if (hasColumn(descriptor, "RECORD_VERSION")) {
            columns.add("RECORD_VERSION"); placeholders.add("1");
        }
        if (hasColumn(descriptor, "MIGRATED_AT")) {
            columns.add("MIGRATED_AT"); placeholders.add("SYSTIMESTAMP");
        }
        if (hasColumn(descriptor, "IS_DELETED")) {
            columns.add("IS_DELETED"); placeholders.add("0");
        }
    }

    private void validateRequiredColumns(TableDescriptor descriptor, Map<String, Object> values, List<String> insertedColumns) {
        Set<String> inserted = new LinkedHashSet<>(insertedColumns);
        for (ColumnDescriptor column : descriptor.columns()) {
            if (column.primaryKey() || column.nullable() || inserted.contains(column.name()) || hasDatabaseDefault(column.defaultValue())) continue;
            if (SYSTEM_MANAGED.contains(column.name())) continue;
            if (!values.containsKey(column.name()) || isBlank(values.get(column.name()))) {
                throw new ProductBuilderValidationException("Required column is missing: " + column.name());
            }
        }
    }

    private static boolean hasDatabaseDefault(String value) {
        return value != null && !value.isBlank() && !"NULL".equalsIgnoreCase(value.trim());
    }

    private static boolean isBlank(Object value) {
        return value == null || (value instanceof String s && s.isBlank());
    }

    private Object databaseValue(ColumnDescriptor column, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) return null;
        String type = column.dataType();
        try {
            if (type.startsWith("NUMBER")) return value instanceof Number ? value : new BigDecimal(value.toString());
            if ("DATE".equals(type)) return value instanceof LocalDate d ? Date.valueOf(d) : Date.valueOf(value.toString().substring(0, 10));
            if (type.startsWith("TIMESTAMP")) {
                if (value instanceof LocalDateTime dt) return Timestamp.valueOf(dt);
                String text = value.toString().replace('T', ' ');
                if (text.length() == 10) text += " 00:00:00";
                else if (text.length() == 16) text += ":00";
                return Timestamp.valueOf(text);
            }
            return value.toString();
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw new ProductBuilderValidationException("Invalid value for " + column.name() + ": " + value);
        }
    }

    private static Map<String, Object> mapRow(ResultSet rs, List<ColumnDescriptor> columns) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (ColumnDescriptor column : columns) {
            Object value;
            String type = column.dataType();
            if ("CLOB".equals(type)) {
                Clob clob = rs.getClob(column.name());
                value = clob == null ? null : clob.getSubString(1, (int) Math.min(clob.length(), Integer.MAX_VALUE));
            } else if ("DATE".equals(type)) {
                Date date = rs.getDate(column.name());
                value = date == null ? null : date.toLocalDate().toString();
            } else if (type.startsWith("TIMESTAMP")) {
                Timestamp ts = rs.getTimestamp(column.name());
                value = ts == null ? null : ts.toLocalDateTime().toString();
            } else {
                value = rs.getObject(column.name());
            }
            row.put(column.name(), value);
        }
        return row;
    }

    private static String normalizeType(String dataType) {
        return dataType == null ? "VARCHAR2" : dataType.toUpperCase(Locale.ROOT);
    }

    private static boolean isTextType(String dataType) {
        return dataType.contains("CHAR") || dataType.contains("CLOB");
    }

    private static Integer integerOrNull(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static String safeString(ResultSet rs, String column) {
        try { return rs.getString(column); } catch (SQLException ex) { return null; }
    }

    private static long numericId(Object value) {
        if (value == null) return 0;
        try { return new BigDecimal(value.toString()).longValue(); } catch (NumberFormatException ex) { return 0; }
    }

    private static boolean hasColumn(TableDescriptor descriptor, String name) {
        return descriptor.columns().stream().anyMatch(c -> c.name().equals(name));
    }

    private static ColumnDescriptor column(TableDescriptor descriptor, String requested) {
        String name = identifier(requested);
        return descriptor.columns().stream().filter(c -> c.name().equals(name)).findFirst()
                .orElseThrow(() -> new ProductBuilderValidationException("Unknown column: " + requested));
    }

    private static ColumnDescriptor labelColumn(TableDescriptor descriptor) {
        List<String> preferred = List.of("NAME_FA", "TITLE_FA", "PRODUCT_NAME", "DESCRIPTION");
        for (String name : preferred) {
            Optional<ColumnDescriptor> found = descriptor.columns().stream().filter(c -> c.name().equals(name)).findFirst();
            if (found.isPresent()) return found.get();
        }
        return descriptor.columns().stream()
                .filter(c -> c.name().endsWith("_NAME_FA") || c.name().endsWith("_NAME") || c.name().endsWith("_TITLE"))
                .findFirst()
                .orElseGet(() -> descriptor.columns().stream().filter(c -> isTextType(c.dataType()) && !c.readOnly()).findFirst()
                        .orElse(column(descriptor, descriptor.primaryKeyColumn())));
    }

    private static Optional<ColumnDescriptor> codeColumn(TableDescriptor descriptor) {
        return descriptor.columns().stream().filter(c -> c.name().endsWith("_CODE") || c.name().equals("CODE")).findFirst();
    }

    private String supportedTable(String requested) {
        String table = identifier(requested);
        PdlCatalog.require(table);
        return table;
    }

    private String qualified(String table) {
        return schemaName + "." + identifier(table);
    }

    private static String identifier(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(normalized).matches()) throw new ProductBuilderValidationException("Invalid SQL identifier: " + value);
        return normalized;
    }

    private record ForeignKeyInfo(String column, String parentTable, String parentColumn) {}
}
