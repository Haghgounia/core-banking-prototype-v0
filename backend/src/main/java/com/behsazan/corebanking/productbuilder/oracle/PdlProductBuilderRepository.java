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
    private static final Pattern SIMPLE_IN = Pattern.compile("(?is)^\\s*\\(?\\s*([A-Z][A-Z0-9_$#]*)\\s+IN\\s*\\((.+)\\)\\s*\\)?\\s*$");
    private static final Set<String> SYSTEM_MANAGED = Set.of(
            "CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "RECORD_VERSION", "MIGRATED_AT"
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

        String sql = "SELECT T.*" + fromWhere
                + " ORDER BY T." + identifier(descriptor.primaryKeyColumn()) + " DESC"
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
        if (!fk.foreignKey() || fk.parentTable() == null) {
            throw new ProductBuilderValidationException("Column is not a foreign key: " + requestedColumn);
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
        if (hasColumn(parent, "IS_ACTIVE")) where.add("NVL(T.IS_ACTIVE, 1) = 1");
        if (hasColumn(parent, "IS_DELETED")) where.add("NVL(T.IS_DELETED, 0) = 0");
        params.put("limit", safeLimit);
        String sql = "SELECT T." + identifier(parent.primaryKeyColumn()) + " AS V, TO_CHAR(T."
                + identifier(code.name()) + ") AS C, TO_CHAR(T." + identifier(label.name()) + ") AS L FROM "
                + qualified(parent.tableName()) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY T." + identifier(label.name()) + " FETCH FIRST :limit ROWS ONLY";
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) ->
                new SelectOption(rs.getObject("V"), rs.getString("C"), rs.getString("L"))).list();
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
                            checkOptions.getOrDefault(name, List.of())
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
