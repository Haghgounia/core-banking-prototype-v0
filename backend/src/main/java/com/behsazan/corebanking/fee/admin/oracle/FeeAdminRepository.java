package com.behsazan.corebanking.fee.admin.oracle;

import com.behsazan.corebanking.fee.admin.application.FeeAdminCatalog;
import com.behsazan.corebanking.fee.admin.application.FeeAdminValidationException;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.ColumnDescriptor;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.SelectOption;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.TableDescriptor;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.TablePage;
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
public class FeeAdminRepository {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]*");
    private static final Pattern SIMPLE_IN = Pattern.compile("(?is)^\\s*\\(?\\s*([A-Z][A-Z0-9_$#]*)\\s+IN\\s*\\((.+)\\)\\s*\\)?\\s*$");
    private static final Pattern COMMENT_SUFFIX = Pattern.compile("\\s*-\\s*ستون فیزیکی Oracle\\s+[A-Z0-9_]+\\.?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> SYSTEM_MANAGED = Set.of(
            "CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "RECORD_VERSION"
    );

    private static final Map<String, String> DOMAIN_BY_COLUMN = Map.ofEntries(
            Map.entry("FEE_TYPE_CODE", "FEE_TYPE"),
            Map.entry("DEFAULT_BEARER_TYPE_CODE", "CHARGE_BEARER_TYPE"),
            Map.entry("DEFAULT_DEBIT_CREDIT_CODE", "DEBIT_CREDIT_INDICATOR"),
            Map.entry("DEFAULT_DIRECTION_CODE", "ADJUSTMENT_DIRECTION"),
            Map.entry("PAYMENT_METHOD_CODE", "FEE_PAYMENT_METHOD"),
            Map.entry("APPLICATION_TIMING_TYPE_CODE", "APPLICATION_TIMING_MODALITY"),
            Map.entry("CALCULATION_STRATEGY_CODE", "CALCULATION_STRATEGY"),
            Map.entry("BASIS_TYPE_CODE", "CALCULATION_BASIS"),
            Map.entry("TIER_STRATEGY_CODE", "CALCULATION_STRATEGY"),
            Map.entry("TIER_BASIS_CODE", "TIER_BASIS"),
            Map.entry("ROUNDING_MODE_CODE", "ROUNDING_MODE"),
            Map.entry("PARTY_TYPE_CODE", "PARTY_TYPE"),
            Map.entry("CUSTOMER_SEGMENT_CODE", "CUSTOMER_SEGMENT"),
            Map.entry("SEGMENT_CODE", "CUSTOMER_SEGMENT"),
            Map.entry("CUSTOMER_GROUP_CODE", "CUSTOMER_GROUP"),
            Map.entry("PRODUCT_TYPE_CODE", "PRODUCT_TYPE"),
            Map.entry("FEATURE_TYPE_CODE", "PRODUCT_FEATURE_TYPE"),
            Map.entry("PRICING_FEATURE_TYPE_CODE", "PRICING_FEATURE_TYPE"),
            Map.entry("REQUIREMENT_TYPE_CODE", "REQUIREMENT_TYPE"),
            Map.entry("ORIGIN_CODE", "ARRANGEMENT_ORIGIN"),
            Map.entry("COMPONENT_TYPE_CODE", "COMPONENT_TYPE"),
            Map.entry("REFUNDABILITY_CODE", "REFUNDABILITY"),
            Map.entry("ALLOCATION_METHOD_CODE", "ALLOCATION_METHOD"),
            Map.entry("ALLOCATION_BASIS_CODE", "ALLOCATION_BASIS"),
            Map.entry("SETTLEMENT_METHOD_CODE", "SETTLEMENT_METHOD"),
            Map.entry("OVERRIDE_TYPE_CODE", "OVERRIDE_TYPE"),
            Map.entry("REVERSAL_TYPE_CODE", "REVERSAL_TYPE"),
            Map.entry("EVIDENCE_TYPE_CODE", "EVIDENCE_TYPE"),
            Map.entry("NODE_TYPE_CODE", "RULE_NODE_TYPE"),
            Map.entry("DATA_TYPE_CODE", "INPUT_DATA_TYPE"),
            Map.entry("UNIT_CODE", "UNIT"),
            Map.entry("BOUND_UNIT_CODE", "UNIT"),
            Map.entry("CHANNEL_CODE", "CHANNEL"),
            Map.entry("ACTIVITY_CODE", "ACTIVITY"),
            Map.entry("TRANSACTION_TYPE_CODE", "TRANSACTION_TYPE"),
            Map.entry("EVENT_TYPE_CODE", "TRANSACTION_TYPE"),
            Map.entry("CURRENCY_CODE", "CURRENCY"),
            Map.entry("DEFAULT_CURRENCY_CODE", "CURRENCY"),
            Map.entry("DEFINITION_CURRENCY_CODE", "CURRENCY"),
            Map.entry("FEE_CURRENCY_CODE", "CURRENCY"),
            Map.entry("BASIS_CURRENCY_CODE", "CURRENCY"),
            Map.entry("POSTING_CURRENCY_CODE", "CURRENCY"),
            Map.entry("REVERSAL_CURRENCY_CODE", "CURRENCY"),
            Map.entry("CONVERTED_CURRENCY_CODE", "CURRENCY"),
            Map.entry("FROM_CURRENCY_CODE", "CURRENCY"),
            Map.entry("TO_CURRENCY_CODE", "CURRENCY"),
            Map.entry("ACCOUNT_TYPE_CODE", "ACCOUNT_TYPE"),
            Map.entry("COLLECTION_MODE_CODE", "COLLECTION_MODE"),
            Map.entry("FAILURE_ACTION_CODE", "FAILURE_ACTION"),
            Map.entry("STACKING_MODE_CODE", "STACKING_MODE"),
            Map.entry("ACCOUNT_ROLE_CODE", "ACCOUNT_ROLE"),
            Map.entry("CHARGE_ACCOUNT_ROLE_CODE", "ACCOUNT_ROLE"),
            Map.entry("DEBIT_ACCOUNT_ROLE_CODE", "ACCOUNT_ROLE"),
            Map.entry("CREDIT_ACCOUNT_ROLE_CODE", "ACCOUNT_ROLE"),
            Map.entry("BENEFICIARY_ROLE_CODE", "BENEFICIARY_ROLE"),
            Map.entry("INVOLVEMENT_ROLE_CODE", "INVOLVEMENT_ROLE"),
            Map.entry("MODALITY_TYPE_CODE", "MODALITY_TYPE"),
            Map.entry("POLICY_TYPE_CODE", "POLICY_TYPE"),
            Map.entry("VERSION_TYPE_CODE", "VERSION_TYPE"),
            Map.entry("FEE_PLAN_TYPE_CODE", "FEE_PLAN_TYPE"),
            Map.entry("VALUE_TYPE_CODE", "INPUT_DATA_TYPE"),
            Map.entry("DEFAULT_BEARER_ROLE_CODE", "INVOLVEMENT_ROLE"),
            Map.entry("CURRENCY_MODALITY_TYPE_CODE", "CURRENCY_MODALITY"),
            Map.entry("POSTING_MODALITY_CODE", "POSTING_MODALITY"),
            Map.entry("POSTING_MODALITY_TYPE_CODE", "POSTING_MODALITY"),
            Map.entry("OPERATOR_CODE", "RULE_OPERATOR"),
            Map.entry("POSTING_STATUS_CODE", "LIFECYCLE_STATUS"),
            Map.entry("SETTLEMENT_STATUS_CODE", "LIFECYCLE_STATUS"),
            Map.entry("STATUS_CODE", "LIFECYCLE_STATUS"),
            Map.entry("APPLICATION_FREQUENCY_CODE", "APPLICATION_FREQUENCY"),
            Map.entry("CHARGING_FREQUENCY_CODE", "APPLICATION_FREQUENCY"),
            Map.entry("CALCULATION_FREQUENCY_CODE", "CALCULATION_FREQUENCY"),
            Map.entry("EVENT_TRIGGER_CODE", "ACTIVITY")
    );

    private final JdbcClient jdbc;
    private final String schemaName;
    private final Map<String, TableDescriptor> descriptorCache = new ConcurrentHashMap<>();

    public FeeAdminRepository(JdbcClient jdbc,
                              @Value("${core-banking.schemas.fee:FEE}") String schemaName) {
        this.jdbc = jdbc;
        this.schemaName = identifier(schemaName);
    }

    public String schemaName() { return schemaName; }

    public boolean tableExists(String requestedTable) {
        String table = supportedTable(requestedTable);
        Long count = jdbc.sql("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER=:owner AND TABLE_NAME=:table")
                .param("owner", schemaName).param("table", table).query(Long.class).single();
        return count != null && count > 0;
    }

    public long count(String requestedTable) {
        String table = supportedTable(requestedTable);
        if (!tableExists(table)) return 0;
        return jdbc.sql("SELECT COUNT(*) FROM " + qualified(table)).query(Long.class).single();
    }

    public TableDescriptor descriptor(String requestedTable) {
        String table = supportedTable(requestedTable);
        if (!tableExists(table)) throw new FeeAdminValidationException("جدول " + schemaName + "." + table + " در پایگاه داده ایجاد نشده است.");
        return descriptorCache.computeIfAbsent(table, this::loadDescriptor);
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
                    .filter(c -> isTextType(c.dataType()) && !"CLOB".equals(c.dataType()))
                    .map(ColumnDescriptor::name).toList();
            if (!textColumns.isEmpty()) {
                where.add("(" + String.join(" OR ", textColumns.stream()
                        .map(c -> "UPPER(TO_CHAR(T." + identifier(c) + ")) LIKE :searchText").toList()) + ")");
                params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
            }
        }
        if (filterColumn != null && !filterColumn.isBlank() && filterValue != null && !filterValue.isBlank()) {
            ColumnDescriptor column = column(descriptor, filterColumn);
            where.add("T." + identifier(column.name()) + " = :filterValue");
            params.put("filterValue", databaseValue(column, filterValue));
        }

        String fromWhere = " FROM " + qualified(descriptor.tableName()) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where));
        long total = jdbc.sql("SELECT COUNT(*)" + fromWhere).params(params).query(Long.class).single();
        String sql = "SELECT T.*" + fromWhere + " ORDER BY T." + identifier(descriptor.primaryKeyColumn()) + " DESC"
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        Map<String, Object> pageParams = new LinkedHashMap<>(params);
        pageParams.put("offset", safePage * safeSize);
        pageParams.put("pageSize", safeSize);
        List<Map<String, Object>> rows = jdbc.sql(sql).params(pageParams)
                .query((rs, rowNum) -> mapRow(rs, descriptor.columns())).list();
        return new TablePage(rows, total, safePage, safeSize);
    }

    public Optional<Map<String, Object>> findById(String requestedTable, long id) {
        TableDescriptor descriptor = descriptor(requestedTable);
        String sql = "SELECT T.* FROM " + qualified(descriptor.tableName()) + " T WHERE T."
                + identifier(descriptor.primaryKeyColumn()) + " = :id";
        return jdbc.sql(sql).param("id", id).query((rs, rowNum) -> mapRow(rs, descriptor.columns())).optional();
    }

    public long insert(String requestedTable, Map<String, Object> values, String actor) {
        TableDescriptor descriptor = descriptor(requestedTable);
        long id = nextId(descriptor.tableName());
        ColumnDescriptor pk = column(descriptor, descriptor.primaryKeyColumn());
        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        columns.add(identifier(pk.name())); placeholders.add(":" + pk.name()); params.put(pk.name(), id);

        for (ColumnDescriptor c : descriptor.columns()) {
            if (c.primaryKey() || SYSTEM_MANAGED.contains(c.name()) || !values.containsKey(c.name())) continue;
            Object value = values.get(c.name());
            if (isBlank(value) && hasDatabaseDefault(c.defaultValue())) continue;
            columns.add(identifier(c.name())); placeholders.add(":" + c.name()); params.put(c.name(), databaseValue(c, value));
        }
        addSystemInsertColumns(descriptor, actor, columns, placeholders, params);
        validateRequiredColumns(descriptor, values, columns);
        jdbc.sql("INSERT INTO " + qualified(descriptor.tableName()) + " (" + String.join(", ", columns)
                + ") VALUES (" + String.join(", ", placeholders) + ")").params(params).update();
        return id;
    }

    public boolean update(String requestedTable, long id, Map<String, Object> values, String actor) {
        TableDescriptor descriptor = descriptor(requestedTable);
        List<String> assignments = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        for (ColumnDescriptor c : descriptor.columns()) {
            if (c.primaryKey() || SYSTEM_MANAGED.contains(c.name()) || !values.containsKey(c.name())) continue;
            assignments.add(identifier(c.name()) + " = :" + c.name());
            params.put(c.name(), databaseValue(c, values.get(c.name())));
        }
        if (hasColumn(descriptor, "UPDATED_AT")) assignments.add("UPDATED_AT = SYSTIMESTAMP");
        if (hasColumn(descriptor, "UPDATED_BY")) { assignments.add("UPDATED_BY = :UPDATED_BY"); params.put("UPDATED_BY", actor); }
        if (hasColumn(descriptor, "RECORD_VERSION")) assignments.add("RECORD_VERSION = RECORD_VERSION + 1");
        if (assignments.isEmpty()) throw new FeeAdminValidationException("مقداری برای ویرایش ارسال نشده است.");
        params.put("id", id);
        String sql = "UPDATE " + qualified(descriptor.tableName()) + " SET " + String.join(", ", assignments)
                + " WHERE " + identifier(descriptor.primaryKeyColumn()) + " = :id";
        return jdbc.sql(sql).params(params).update() == 1;
    }

    public boolean delete(String requestedTable, long id) {
        TableDescriptor descriptor = descriptor(requestedTable);
        return jdbc.sql("DELETE FROM " + qualified(descriptor.tableName()) + " WHERE "
                + identifier(descriptor.primaryKeyColumn()) + " = :id").param("id", id).update() == 1;
    }

    public List<SelectOption> lookup(String requestedTable, String requestedColumn, String text, int limit) {
        TableDescriptor child = descriptor(requestedTable);
        ColumnDescriptor fk = column(child, requestedColumn);
        if (!fk.foreignKey() || fk.parentTable() == null) {
            throw new FeeAdminValidationException("ستون FK نیست: " + requestedColumn);
        }
        TableDescriptor parent = descriptor(fk.parentTable());
        ColumnDescriptor label = labelColumn(parent);
        ColumnDescriptor code = codeColumn(parent).orElse(label);
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            where.add("(UPPER(TO_CHAR(T." + identifier(code.name()) + ")) LIKE :q OR UPPER(TO_CHAR(T."
                    + identifier(label.name()) + ")) LIKE :q)");
            params.put("q", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (hasColumn(parent, "IS_ACTIVE")) where.add("NVL(T.IS_ACTIVE, 'Y') = 'Y'");
        params.put("limit", safeLimit);
        String sql = "SELECT T." + identifier(parent.primaryKeyColumn()) + " AS V, TO_CHAR(T."
                + identifier(code.name()) + ") AS C, TO_CHAR(T." + identifier(label.name()) + ") AS L FROM "
                + qualified(parent.tableName()) + " T" + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY T." + identifier(label.name()) + " FETCH FIRST :limit ROWS ONLY";
        return jdbc.sql(sql).params(params).query((rs, rowNum) ->
                new SelectOption(rs.getObject("V"), rs.getString("C"), rs.getString("L"))).list();
    }

    private TableDescriptor loadDescriptor(String table) {
        FeeAdminCatalog.Entry entry = FeeAdminCatalog.require(table);
        Map<String, ForeignKeyInfo> foreignKeys = loadForeignKeys(table);
        String pk = loadPrimaryKey(table);
        Map<String, List<SelectOption>> checkOptions = loadCheckOptions(table);
        String sql = "SELECT C.COLUMN_NAME, C.DATA_TYPE, C.CHAR_LENGTH, C.DATA_PRECISION, C.DATA_SCALE, C.NULLABLE, "
                + "C.DATA_DEFAULT, CC.COMMENTS FROM ALL_TAB_COLUMNS C LEFT JOIN ALL_COL_COMMENTS CC "
                + "ON CC.OWNER=C.OWNER AND CC.TABLE_NAME=C.TABLE_NAME AND CC.COLUMN_NAME=C.COLUMN_NAME "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table ORDER BY C.COLUMN_ID";
        List<ColumnDescriptor> columns = jdbc.sql(sql).param("owner", schemaName).param("table", table)
                .query((rs, rowNum) -> {
                    String name = rs.getString("COLUMN_NAME");
                    ForeignKeyInfo fk = foreignKeys.get(name);
                    String rawComment = rs.getString("COMMENTS");
                    String defaultValue = safeString(rs, "DATA_DEFAULT");
                    String domain = DOMAIN_BY_COLUMN.get(name);
                    List<SelectOption> options = checkOptions.getOrDefault(name, List.of());
                    if (options.isEmpty() && domain != null) options = loadDomainOptions(domain);
                    return new ColumnDescriptor(
                            name,
                            cleanLabel(rawComment, name),
                            normalizeType(rs.getString("DATA_TYPE")),
                            integerOrNull(rs.getObject("CHAR_LENGTH")),
                            integerOrNull(rs.getObject("DATA_PRECISION")),
                            integerOrNull(rs.getObject("DATA_SCALE")),
                            "Y".equals(rs.getString("NULLABLE")),
                            name.equals(pk),
                            fk != null,
                            fk == null ? null : fk.parentTable(),
                            fk == null ? null : fk.parentColumn(),
                            name.equals(pk) || SYSTEM_MANAGED.contains(name),
                            defaultValue == null ? null : defaultValue.trim(),
                            domain,
                            options
                    );
                }).list();
        if (columns.isEmpty()) throw new FeeAdminValidationException("Metadata جدول FEE قابل مشاهده نیست: " + table);
        return new TableDescriptor(schemaName, table, entry.title(), entry.groupCode(), entry.groupTitle(), entry.editable(),
                entry.baselineRows(), pk, columns);
    }

    private List<SelectOption> loadDomainOptions(String domainCode) {
        if (!tableExists("FEE_REF_DOMAIN") || !tableExists("FEE_REF_VALUE")) return List.of();
        String sql = "SELECT V.VALUE_CODE AS V, V.VALUE_CODE AS C, NVL(V.NAME_FA, NVL(V.NAME_EN, V.VALUE_CODE)) AS L "
                + "FROM " + qualified("FEE_REF_VALUE") + " V JOIN " + qualified("FEE_REF_DOMAIN") + " D ON D.DOMAIN_ID=V.DOMAIN_ID "
                + "WHERE D.DOMAIN_CODE=:domain AND NVL(D.IS_ACTIVE,'Y')='Y' AND NVL(V.IS_ACTIVE,'Y')='Y' "
                + "ORDER BY NVL(V.SORT_ORDER,999999), V.VALUE_CODE";
        try {
            return jdbc.sql(sql).param("domain", domainCode).query((rs, rowNum) ->
                    new SelectOption(rs.getString("V"), rs.getString("C"), rs.getString("L"))).list();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private String loadPrimaryKey(String table) {
        String sql = "SELECT CC.COLUMN_NAME FROM ALL_CONSTRAINTS C JOIN ALL_CONS_COLUMNS CC "
                + "ON CC.OWNER=C.OWNER AND CC.CONSTRAINT_NAME=C.CONSTRAINT_NAME "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table AND C.CONSTRAINT_TYPE='P' ORDER BY CC.POSITION";
        List<String> columns = jdbc.sql(sql).param("owner", schemaName).param("table", table).query(String.class).list();
        if (columns.size() != 1) throw new FeeAdminValidationException("جدول FEE باید PK تک‌ستونی داشته باشد: " + table);
        return columns.getFirst();
    }

    private Map<String, ForeignKeyInfo> loadForeignKeys(String table) {
        String sql = "SELECT CC.COLUMN_NAME, P.TABLE_NAME AS PARENT_TABLE, PCC.COLUMN_NAME AS PARENT_COLUMN "
                + "FROM ALL_CONSTRAINTS C JOIN ALL_CONS_COLUMNS CC ON CC.OWNER=C.OWNER AND CC.CONSTRAINT_NAME=C.CONSTRAINT_NAME "
                + "JOIN ALL_CONSTRAINTS P ON P.OWNER=C.R_OWNER AND P.CONSTRAINT_NAME=C.R_CONSTRAINT_NAME "
                + "JOIN ALL_CONS_COLUMNS PCC ON PCC.OWNER=P.OWNER AND PCC.CONSTRAINT_NAME=P.CONSTRAINT_NAME AND PCC.POSITION=CC.POSITION "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table AND C.CONSTRAINT_TYPE='R'";
        Map<String, ForeignKeyInfo> result = new HashMap<>();
        jdbc.sql(sql).param("owner", schemaName).param("table", table)
                .query((rs, rowNum) -> new ForeignKeyInfo(rs.getString("COLUMN_NAME"), rs.getString("PARENT_TABLE"), rs.getString("PARENT_COLUMN")))
                .list().forEach(fk -> result.put(fk.column(), fk));
        return result;
    }

    private Map<String, List<SelectOption>> loadCheckOptions(String table) {
        String sql = "SELECT SEARCH_CONDITION_VC FROM ALL_CONSTRAINTS WHERE OWNER=:owner AND TABLE_NAME=:table "
                + "AND CONSTRAINT_TYPE='C' AND SEARCH_CONDITION_VC IS NOT NULL";
        Map<String, LinkedHashSet<String>> raw = new LinkedHashMap<>();
        jdbc.sql(sql).param("owner", schemaName).param("table", table).query(String.class).list().forEach(condition -> {
            Matcher matcher = SIMPLE_IN.matcher(condition);
            if (!matcher.matches()) return;
            String column = matcher.group(1).toUpperCase(Locale.ROOT);
            for (String token : splitValues(matcher.group(2))) raw.computeIfAbsent(column, k -> new LinkedHashSet<>()).add(token);
        });
        Map<String, List<SelectOption>> result = new LinkedHashMap<>();
        raw.forEach((column, values) -> result.put(column, values.stream().map(this::checkOption).toList()));
        return result;
    }

    private SelectOption checkOption(String token) {
        String value = token.trim();
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1).replace("''", "'");
            return new SelectOption(value, value, yesNoLabel(value));
        }
        try {
            BigDecimal number = new BigDecimal(value);
            return new SelectOption(number, value, yesNoLabel(value));
        } catch (NumberFormatException ex) {
            return new SelectOption(value, value, value);
        }
    }

    private static String yesNoLabel(String value) {
        return switch (value) { case "Y", "1" -> "بله"; case "N", "0" -> "خیر"; default -> value; };
    }

    private long nextId(String table) {
        String sequence = identifier("SEQ_" + table);
        return jdbc.sql("SELECT " + schemaName + "." + sequence + ".NEXTVAL FROM DUAL").query(Long.class).single();
    }

    private void addSystemInsertColumns(TableDescriptor descriptor, String actor, List<String> columns,
                                        List<String> placeholders, Map<String, Object> params) {
        if (hasColumn(descriptor, "CREATED_AT")) { columns.add("CREATED_AT"); placeholders.add("SYSTIMESTAMP"); }
        if (hasColumn(descriptor, "CREATED_BY")) { columns.add("CREATED_BY"); placeholders.add(":CREATED_BY"); params.put("CREATED_BY", actor); }
        if (hasColumn(descriptor, "RECORD_VERSION")) { columns.add("RECORD_VERSION"); placeholders.add("1"); }
    }

    private void validateRequiredColumns(TableDescriptor descriptor, Map<String, Object> values, List<String> insertedColumns) {
        Set<String> inserted = new LinkedHashSet<>(insertedColumns);
        for (ColumnDescriptor c : descriptor.columns()) {
            if (c.primaryKey() || c.nullable() || inserted.contains(c.name()) || hasDatabaseDefault(c.defaultValue())) continue;
            if (SYSTEM_MANAGED.contains(c.name())) continue;
            if (!values.containsKey(c.name()) || isBlank(values.get(c.name()))) {
                throw new FeeAdminValidationException("فیلد اجباری درج نشده است: " + c.label() + " (" + c.name() + ")");
            }
        }
    }

    private Object databaseValue(ColumnDescriptor c, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) return null;
        try {
            if (c.dataType().startsWith("NUMBER")) return value instanceof Number ? value : new BigDecimal(value.toString());
            if ("DATE".equals(c.dataType())) return value instanceof LocalDate d ? Date.valueOf(d) : Date.valueOf(value.toString().substring(0, 10));
            if (c.dataType().startsWith("TIMESTAMP")) {
                if (value instanceof LocalDateTime dt) return Timestamp.valueOf(dt);
                String text = value.toString().replace('T', ' ');
                if (text.length() == 10) text += " 00:00:00";
                else if (text.length() == 16) text += ":00";
                return Timestamp.valueOf(text);
            }
            return value.toString();
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw new FeeAdminValidationException("مقدار نامعتبر برای " + c.label() + ": " + value);
        }
    }

    private static Map<String, Object> mapRow(ResultSet rs, List<ColumnDescriptor> columns) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (ColumnDescriptor c : columns) {
            Object value;
            if ("CLOB".equals(c.dataType())) {
                Clob clob = rs.getClob(c.name());
                value = clob == null ? null : clob.getSubString(1, (int) Math.min(clob.length(), Integer.MAX_VALUE));
            } else if ("DATE".equals(c.dataType())) {
                Date date = rs.getDate(c.name()); value = date == null ? null : date.toLocalDate().toString();
            } else if (c.dataType().startsWith("TIMESTAMP")) {
                Timestamp ts = rs.getTimestamp(c.name()); value = ts == null ? null : ts.toLocalDateTime().toString();
            } else value = rs.getObject(c.name());
            row.put(c.name(), value);
        }
        return row;
    }

    private static String cleanLabel(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String cleaned = COMMENT_SUFFIX.matcher(raw.trim()).replaceFirst("").trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static String normalizeType(String dataType) { return dataType == null ? "VARCHAR2" : dataType.toUpperCase(Locale.ROOT); }
    private static boolean isTextType(String dataType) { return dataType.contains("CHAR") || dataType.contains("CLOB"); }
    private static Integer integerOrNull(Object value) { return value == null ? null : ((Number) value).intValue(); }
    private static String safeString(ResultSet rs, String column) { try { return rs.getString(column); } catch (SQLException ex) { return null; } }
    private static boolean hasDatabaseDefault(String value) { return value != null && !value.isBlank() && !"NULL".equalsIgnoreCase(value.trim()); }
    private static boolean isBlank(Object value) { return value == null || (value instanceof String s && s.isBlank()); }
    private static boolean hasColumn(TableDescriptor descriptor, String name) { return descriptor.columns().stream().anyMatch(c -> c.name().equals(name)); }

    private static ColumnDescriptor column(TableDescriptor descriptor, String requested) {
        String name = identifier(requested);
        return descriptor.columns().stream().filter(c -> c.name().equals(name)).findFirst()
                .orElseThrow(() -> new FeeAdminValidationException("ستون ناشناخته: " + requested));
    }

    private static ColumnDescriptor labelColumn(TableDescriptor descriptor) {
        for (String name : List.of("NAME_FA", "TITLE_FA", "DESCRIPTION", "NAME_EN")) {
            Optional<ColumnDescriptor> found = descriptor.columns().stream().filter(c -> c.name().equals(name)).findFirst();
            if (found.isPresent()) return found.get();
        }
        return descriptor.columns().stream().filter(c -> isTextType(c.dataType()) && !c.readOnly()).findFirst()
                .orElse(column(descriptor, descriptor.primaryKeyColumn()));
    }

    private static Optional<ColumnDescriptor> codeColumn(TableDescriptor descriptor) {
        for (String name : List.of("FEE_CODE", "FEATURE_CODE", "POLICY_CODE", "RULE_CODE", "DOMAIN_CODE", "VALUE_CODE",
                "ARRANGEMENT_NO", "TRANSACTION_NO", "INSTRUCTION_NO", "PRODUCT_CODE", "ACCOUNT_NO", "PARTY_NO")) {
            Optional<ColumnDescriptor> found = descriptor.columns().stream().filter(c -> c.name().equals(name)).findFirst();
            if (found.isPresent()) return found;
        }
        return descriptor.columns().stream().filter(c -> c.name().endsWith("_CODE") || c.name().equals("CODE")).findFirst();
    }

    private static List<String> splitValues(String raw) {
        List<String> result = new ArrayList<>(); StringBuilder current = new StringBuilder(); boolean quoted = false;
        for (int i=0;i<raw.length();i++) {
            char ch=raw.charAt(i);
            if (ch=='\'' && (i+1>=raw.length() || raw.charAt(i+1)!='\'')) quoted=!quoted;
            if (ch==',' && !quoted) { result.add(current.toString().trim()); current.setLength(0); }
            else { current.append(ch); if (ch=='\'' && quoted && i+1<raw.length() && raw.charAt(i+1)=='\'') current.append(raw.charAt(++i)); }
        }
        if (!current.isEmpty()) result.add(current.toString().trim());
        return result;
    }

    private String supportedTable(String requested) { String table=identifier(requested); FeeAdminCatalog.require(table); return table; }
    private String qualified(String table) { return schemaName + "." + identifier(table); }
    private static String identifier(String value) {
        String normalized=value==null?"":value.trim().toUpperCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(normalized).matches()) throw new FeeAdminValidationException("شناسه SQL نامعتبر: " + value);
        return normalized;
    }

    private record ForeignKeyInfo(String column, String parentTable, String parentColumn) {}
}
