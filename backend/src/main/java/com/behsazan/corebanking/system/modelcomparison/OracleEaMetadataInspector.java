package com.behsazan.corebanking.system.modelcomparison;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Repository
class OracleEaMetadataInspector {
    static final String COLUMN_METADATA_SQL = """
            SELECT C.TABLE_NAME, C.COLUMN_NAME, C.DATA_TYPE, C.DATA_LENGTH, C.CHAR_LENGTH, C.CHAR_USED,
                   C.DATA_PRECISION, C.DATA_SCALE, C.NULLABLE, C.COLUMN_ID,
                   C.IDENTITY_COLUMN, COALESCE(TC.VIRTUAL_COLUMN, 'NO') AS VIRTUAL_COLUMN,
                   CC.COMMENTS AS COLUMN_COMMENT
              FROM ALL_TAB_COLUMNS C
              LEFT JOIN ALL_TAB_COLS TC
                ON TC.OWNER=C.OWNER AND TC.TABLE_NAME=C.TABLE_NAME AND TC.COLUMN_NAME=C.COLUMN_NAME
              LEFT JOIN ALL_COL_COMMENTS CC
                ON CC.OWNER=C.OWNER AND CC.TABLE_NAME=C.TABLE_NAME AND CC.COLUMN_NAME=C.COLUMN_NAME
             WHERE C.OWNER=:schema
             ORDER BY C.TABLE_NAME, C.COLUMN_ID
            """;

    static final String REFERENCED_COLUMN_METADATA_SQL = """
            SELECT C.COLUMN_NAME, C.DATA_TYPE, C.DATA_LENGTH, C.CHAR_LENGTH, C.CHAR_USED,
                   C.DATA_PRECISION, C.DATA_SCALE, C.NULLABLE, C.COLUMN_ID,
                   C.IDENTITY_COLUMN, COALESCE(TC.VIRTUAL_COLUMN, 'NO') AS VIRTUAL_COLUMN,
                   CC.COMMENTS AS COLUMN_COMMENT
              FROM ALL_TAB_COLUMNS C
              LEFT JOIN ALL_TAB_COLS TC
                ON TC.OWNER=C.OWNER AND TC.TABLE_NAME=C.TABLE_NAME AND TC.COLUMN_NAME=C.COLUMN_NAME
              LEFT JOIN ALL_COL_COMMENTS CC
                ON CC.OWNER=C.OWNER AND CC.TABLE_NAME=C.TABLE_NAME AND CC.COLUMN_NAME=C.COLUMN_NAME
             WHERE C.OWNER=:owner AND C.TABLE_NAME=:tableName
             ORDER BY C.COLUMN_ID
            """;

    private final JdbcClient jdbc;
    private final ConfiguredDatabaseSchemas schemas;

    OracleEaMetadataInspector(JdbcClient jdbc, ConfiguredDatabaseSchemas schemas) {
        this.jdbc = jdbc;
        this.schemas = schemas;
    }

    OracleEaPhysicalModel inspect(OracleEaExportOptions options) {
        String schema = schemas.require(options.schema());
        ensureSchemaVisible(schema);
        Predicate<String> tableFilter = tableFilter(options.tablePattern());
        List<String> warnings = new ArrayList<>();

        Map<String, TableHeader> allHeaders = loadTableHeaders(schema);
        Set<String> selected = new LinkedHashSet<>();
        allHeaders.keySet().stream().filter(tableFilter).sorted().forEach(selected::add);
        if (selected.isEmpty()) {
            throw new ModelComparisonValidationException("هیچ جدول Oracle با الگوی انتخاب‌شده پیدا نشد.");
        }

        Map<String, Map<String, String>> defaults = loadDefaults(schema, warnings);
        Map<String, List<OracleEaColumn>> columnsByTable = loadColumns(schema, options.includeComments(), defaults);
        Map<String, List<OracleEaKeyConstraint>> keysByTable = loadKeys(schema);
        Map<String, List<OracleEaIndex>> indexesByTable = options.includeIndexes() ? loadIndexes(schema, keysByTable) : Map.of();
        Map<String, List<OracleEaCheckConstraint>> checksByTable = options.includeChecks() ? loadChecks(schema, warnings) : Map.of();
        List<OracleEaForeignKey> allForeignKeys = options.includeForeignKeys() ? loadForeignKeys(schema) : List.of();
        List<OracleEaForeignKey> foreignKeys = allForeignKeys.stream()
                .filter(fk -> selected.contains(fk.childTable()))
                .toList();

        Map<String, OracleEaTable> tables = new LinkedHashMap<>();
        for (String tableName : selected) {
            TableHeader header = allHeaders.get(tableName);
            tables.put(qualified(schema, tableName), new OracleEaTable(
                    schema,
                    tableName,
                    options.includeComments() ? header.comment() : null,
                    header.tablespace(),
                    false,
                    List.copyOf(columnsByTable.getOrDefault(tableName, List.of())),
                    List.copyOf(keysByTable.getOrDefault(tableName, List.of())),
                    List.copyOf(indexesByTable.getOrDefault(tableName, List.of())),
                    List.copyOf(checksByTable.getOrDefault(tableName, List.of()))
            ));
        }

        if (options.includeForeignKeys() && options.includeExternalReferences()) {
            addReferencedTableStubs(schema, selected, foreignKeys, tables, warnings, options.includeComments());
        } else if (options.includeForeignKeys()) {
            long unresolvedTargets = foreignKeys.stream()
                    .filter(fk -> !schema.equals(fk.parentOwner()) || !selected.contains(fk.parentTable()))
                    .count();
            if (unresolvedTargets > 0) {
                warnings.add(unresolvedTargets + " رابطه FK به جدولی خارج از محدوده انتخاب‌شده مقصد دارد؛ چون Reference Stub غیرفعال است Association آن روابط در XMI ساخته نمی‌شود.");
            }
        }

        if (options.includeIndexes()) {
            long functionBased = tables.values().stream().flatMap(t -> t.indexes().stream())
                    .filter(index -> index.indexType() != null && index.indexType().toUpperCase(Locale.ROOT).contains("FUNCTION-BASED"))
                    .count();
            if (functionBased > 0) {
                warnings.add(functionBased + " ایندکس Function-based شناسایی شد؛ نام ستون‌های Oracle در XMI ثبت می‌شود و عبارت تابع به‌صورت مستقل بازسازی نمی‌شود.");
            }
        }

        return new OracleEaPhysicalModel(schema, List.copyOf(tables.values()), List.copyOf(foreignKeys), List.copyOf(warnings));
    }

    private void ensureSchemaVisible(String schema) {
        long exists = jdbc.sql("SELECT COUNT(*) FROM ALL_USERS WHERE USERNAME=:schema")
                .param("schema", schema)
                .query(Long.class)
                .single();
        if (exists == 0) {
            throw new ModelComparisonValidationException("Schema در Oracle قابل مشاهده نیست: " + schema);
        }
    }

    private Map<String, TableHeader> loadTableHeaders(String schema) {
        Map<String, TableHeader> result = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT T.TABLE_NAME, T.TABLESPACE_NAME, C.COMMENTS
                  FROM ALL_TABLES T
                  LEFT JOIN ALL_TAB_COMMENTS C
                    ON C.OWNER=T.OWNER AND C.TABLE_NAME=T.TABLE_NAME AND C.TABLE_TYPE='TABLE'
                 WHERE T.OWNER=:schema
                 ORDER BY T.TABLE_NAME
                """)
                .param("schema", schema)
                .query((rs, rowNum) -> new TableHeader(
                        upper(rs.getString("TABLE_NAME")),
                        rs.getString("TABLESPACE_NAME"),
                        rs.getString("COMMENTS")
                ))
                .list()
                .forEach(header -> result.put(header.tableName(), header));
        return result;
    }

    private Map<String, Map<String, String>> loadDefaults(String schema, List<String> warnings) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        try {
            jdbc.sql("""
                    SELECT TABLE_NAME, COLUMN_NAME, DATA_DEFAULT
                      FROM ALL_TAB_COLUMNS
                     WHERE OWNER=:schema
                     ORDER BY TABLE_NAME, COLUMN_ID
                    """)
                    .param("schema", schema)
                    .query((rs, rowNum) -> new DefaultValue(
                            upper(rs.getString("TABLE_NAME")),
                            upper(rs.getString("COLUMN_NAME")),
                            trim(rs.getString("DATA_DEFAULT"))
                    ))
                    .list()
                    .forEach(value -> result.computeIfAbsent(value.tableName(), ignored -> new LinkedHashMap<>())
                            .put(value.columnName(), value.value()));
        } catch (DataAccessException exception) {
            warnings.add("خواندن DATA_DEFAULT از Data Dictionary ممکن نبود؛ XMI بدون Default ستون‌ها تولید می‌شود.");
        }
        return result;
    }

    // VIRTUAL_COLUMN belongs to ALL_TAB_COLS in Oracle. Keep ALL_TAB_COLUMNS as the visible-column source.
    private Map<String, List<OracleEaColumn>> loadColumns(
            String schema,
            boolean includeComments,
            Map<String, Map<String, String>> defaults
    ) {
        Map<String, List<OracleEaColumn>> result = new LinkedHashMap<>();
        jdbc.sql(COLUMN_METADATA_SQL)
                .param("schema", schema)
                .query((rs, rowNum) -> {
                    String table = upper(rs.getString("TABLE_NAME"));
                    String column = upper(rs.getString("COLUMN_NAME"));
                    return new ColumnRow(table, new OracleEaColumn(
                            column,
                            rs.getString("DATA_TYPE"),
                            integer(rs.getObject("DATA_LENGTH")),
                            integer(rs.getObject("CHAR_LENGTH")),
                            rs.getString("CHAR_USED"),
                            integer(rs.getObject("DATA_PRECISION")),
                            integer(rs.getObject("DATA_SCALE")),
                            "Y".equalsIgnoreCase(rs.getString("NULLABLE")),
                            rs.getInt("COLUMN_ID"),
                            defaults.getOrDefault(table, Map.of()).get(column),
                            includeComments ? rs.getString("COLUMN_COMMENT") : null,
                            "YES".equalsIgnoreCase(rs.getString("IDENTITY_COLUMN")),
                            "YES".equalsIgnoreCase(rs.getString("VIRTUAL_COLUMN"))
                    ));
                })
                .list()
                .forEach(row -> result.computeIfAbsent(row.tableName(), ignored -> new ArrayList<>()).add(row.column()));
        result.values().forEach(list -> list.sort(Comparator.comparingInt(OracleEaColumn::position)));
        return result;
    }

    private Map<String, List<OracleEaKeyConstraint>> loadKeys(String schema) {
        Map<String, KeyBuilder> builders = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT C.TABLE_NAME, C.CONSTRAINT_NAME, C.CONSTRAINT_TYPE, C.STATUS, C.INDEX_NAME,
                       CC.COLUMN_NAME, CC.POSITION
                  FROM ALL_CONSTRAINTS C
                  JOIN ALL_CONS_COLUMNS CC
                    ON CC.OWNER=C.OWNER AND CC.CONSTRAINT_NAME=C.CONSTRAINT_NAME AND CC.TABLE_NAME=C.TABLE_NAME
                 WHERE C.OWNER=:schema
                   AND C.CONSTRAINT_TYPE IN ('P','U')
                 ORDER BY C.TABLE_NAME, C.CONSTRAINT_NAME, CC.POSITION
                """)
                .param("schema", schema)
                .query((rs, rowNum) -> new KeyRow(
                        upper(rs.getString("TABLE_NAME")),
                        upper(rs.getString("CONSTRAINT_NAME")),
                        rs.getString("CONSTRAINT_TYPE"),
                        rs.getString("STATUS"),
                        upper(rs.getString("INDEX_NAME")),
                        upper(rs.getString("COLUMN_NAME")),
                        rs.getInt("POSITION")
                ))
                .list()
                .forEach(row -> builders.computeIfAbsent(row.tableName() + "|" + row.constraintName(), ignored ->
                                new KeyBuilder(row.tableName(), row.constraintName(), row.constraintType(), row.status(), row.indexName()))
                        .columns.add(row.columnName()));

        Map<String, List<OracleEaKeyConstraint>> result = new LinkedHashMap<>();
        builders.values().forEach(builder -> result.computeIfAbsent(builder.tableName, ignored -> new ArrayList<>()).add(
                new OracleEaKeyConstraint(builder.constraintName, builder.constraintType, List.copyOf(builder.columns), builder.status, builder.indexName)
        ));
        return result;
    }

    private Map<String, List<OracleEaIndex>> loadIndexes(String schema, Map<String, List<OracleEaKeyConstraint>> keysByTable) {
        Set<String> backingIndexes = new LinkedHashSet<>();
        keysByTable.values().stream().flatMap(List::stream).map(OracleEaKeyConstraint::indexName)
                .filter(name -> name != null && !name.isBlank()).forEach(backingIndexes::add);

        Map<String, IndexBuilder> builders = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT I.TABLE_NAME, I.INDEX_NAME, I.UNIQUENESS, I.INDEX_TYPE, I.STATUS,
                       IC.COLUMN_NAME, IC.COLUMN_POSITION, IC.DESCEND
                  FROM ALL_INDEXES I
                  JOIN ALL_IND_COLUMNS IC
                    ON IC.INDEX_OWNER=I.OWNER AND IC.INDEX_NAME=I.INDEX_NAME AND IC.TABLE_OWNER=I.TABLE_OWNER
                 WHERE I.TABLE_OWNER=:schema
                   AND I.OWNER=:schema
                 ORDER BY I.TABLE_NAME, I.INDEX_NAME, IC.COLUMN_POSITION
                """)
                .param("schema", schema)
                .query((rs, rowNum) -> new IndexRow(
                        upper(rs.getString("TABLE_NAME")),
                        upper(rs.getString("INDEX_NAME")),
                        rs.getString("UNIQUENESS"),
                        rs.getString("INDEX_TYPE"),
                        rs.getString("STATUS"),
                        indexColumnName(rs.getString("COLUMN_NAME"), rs.getInt("COLUMN_POSITION")),
                        rs.getInt("COLUMN_POSITION"),
                        rs.getString("DESCEND")
                ))
                .list()
                .stream()
                .filter(row -> !backingIndexes.contains(row.indexName()))
                .forEach(row -> builders.computeIfAbsent(row.tableName() + "|" + row.indexName(), ignored ->
                                new IndexBuilder(row.tableName(), row.indexName(), "UNIQUE".equalsIgnoreCase(row.uniqueness()), row.indexType(), row.status()))
                        .columns.add(new OracleEaIndexColumn(row.columnName(), row.position(), row.descend())));

        Map<String, List<OracleEaIndex>> result = new LinkedHashMap<>();
        builders.values().forEach(builder -> result.computeIfAbsent(builder.tableName, ignored -> new ArrayList<>()).add(
                new OracleEaIndex(builder.indexName, builder.unique, builder.indexType, builder.status, List.copyOf(builder.columns))
        ));
        return result;
    }

    private Map<String, List<OracleEaCheckConstraint>> loadChecks(String schema, List<String> warnings) {
        Map<String, List<OracleEaCheckConstraint>> result = new LinkedHashMap<>();
        try {
            jdbc.sql("""
                    SELECT TABLE_NAME, CONSTRAINT_NAME, SEARCH_CONDITION_VC, STATUS, GENERATED
                      FROM ALL_CONSTRAINTS
                     WHERE OWNER=:schema
                       AND CONSTRAINT_TYPE='C'
                     ORDER BY TABLE_NAME, CONSTRAINT_NAME
                    """)
                    .param("schema", schema)
                    .query((rs, rowNum) -> new CheckRow(
                            upper(rs.getString("TABLE_NAME")),
                            upper(rs.getString("CONSTRAINT_NAME")),
                            trim(rs.getString("SEARCH_CONDITION_VC")),
                            rs.getString("STATUS"),
                            rs.getString("GENERATED")
                    ))
                    .list()
                    .stream()
                    .filter(row -> row.condition() != null && !looksLikeNotNull(row.condition()))
                    .forEach(row -> result.computeIfAbsent(row.tableName(), ignored -> new ArrayList<>()).add(
                            new OracleEaCheckConstraint(row.constraintName(), row.condition(), row.status())
                    ));
        } catch (DataAccessException exception) {
            warnings.add("خواندن SEARCH_CONDITION_VC ممکن نبود؛ Check Constraintها در این خروجی درج نشدند.");
        }
        return result;
    }

    private List<OracleEaForeignKey> loadForeignKeys(String schema) {
        Map<String, ForeignKeyBuilder> builders = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT FK.CONSTRAINT_NAME, FK.TABLE_NAME AS CHILD_TABLE, FK.R_OWNER AS PARENT_OWNER,
                       PK.TABLE_NAME AS PARENT_TABLE, PK.CONSTRAINT_NAME AS PARENT_CONSTRAINT,
                       PK.CONSTRAINT_TYPE AS PARENT_CONSTRAINT_TYPE, FK.DELETE_RULE, FK.STATUS,
                       FK.DEFERRABLE, FK.DEFERRED,
                       FKC.COLUMN_NAME AS CHILD_COLUMN, PKC.COLUMN_NAME AS PARENT_COLUMN, FKC.POSITION
                  FROM ALL_CONSTRAINTS FK
                  JOIN ALL_CONS_COLUMNS FKC
                    ON FKC.OWNER=FK.OWNER AND FKC.CONSTRAINT_NAME=FK.CONSTRAINT_NAME AND FKC.TABLE_NAME=FK.TABLE_NAME
                  JOIN ALL_CONSTRAINTS PK
                    ON PK.OWNER=FK.R_OWNER AND PK.CONSTRAINT_NAME=FK.R_CONSTRAINT_NAME
                  JOIN ALL_CONS_COLUMNS PKC
                    ON PKC.OWNER=PK.OWNER AND PKC.CONSTRAINT_NAME=PK.CONSTRAINT_NAME
                   AND PKC.TABLE_NAME=PK.TABLE_NAME AND PKC.POSITION=FKC.POSITION
                 WHERE FK.OWNER=:schema
                   AND FK.CONSTRAINT_TYPE='R'
                 ORDER BY FK.TABLE_NAME, FK.CONSTRAINT_NAME, FKC.POSITION
                """)
                .param("schema", schema)
                .query((rs, rowNum) -> new ForeignKeyRow(
                        upper(rs.getString("CONSTRAINT_NAME")),
                        upper(rs.getString("CHILD_TABLE")),
                        upper(rs.getString("PARENT_OWNER")),
                        upper(rs.getString("PARENT_TABLE")),
                        upper(rs.getString("PARENT_CONSTRAINT")),
                        rs.getString("PARENT_CONSTRAINT_TYPE"),
                        rs.getString("DELETE_RULE"),
                        rs.getString("STATUS"),
                        rs.getString("DEFERRABLE"),
                        rs.getString("DEFERRED"),
                        upper(rs.getString("CHILD_COLUMN")),
                        upper(rs.getString("PARENT_COLUMN")),
                        rs.getInt("POSITION")
                ))
                .list()
                .forEach(row -> builders.computeIfAbsent(row.childTable() + "|" + row.constraintName(), ignored ->
                                new ForeignKeyBuilder(schema, row.constraintName(), row.childTable(), row.parentOwner(), row.parentTable(),
                                        row.parentConstraint(), row.parentConstraintType(), row.deleteRule(), row.status(), row.deferrable(), row.deferred()))
                        .columns.add(new OracleEaForeignKeyColumn(row.childColumn(), row.parentColumn(), row.position())));

        return builders.values().stream().map(builder -> new OracleEaForeignKey(
                builder.constraintName,
                builder.childOwner,
                builder.childTable,
                builder.parentOwner,
                builder.parentTable,
                builder.parentConstraintName,
                builder.parentConstraintType,
                builder.deleteRule,
                builder.status,
                builder.deferrable,
                builder.deferred,
                List.copyOf(builder.columns)
        )).toList();
    }

    private void addReferencedTableStubs(
            String sourceSchema,
            Set<String> selected,
            List<OracleEaForeignKey> foreignKeys,
            Map<String, OracleEaTable> tables,
            List<String> warnings,
            boolean includeComments
    ) {
        Map<String, List<OracleEaForeignKey>> byTarget = new LinkedHashMap<>();
        for (OracleEaForeignKey fk : foreignKeys) {
            boolean targetAlreadyIncluded = sourceSchema.equals(fk.parentOwner()) && selected.contains(fk.parentTable());
            if (!targetAlreadyIncluded) {
                byTarget.computeIfAbsent(qualified(fk.parentOwner(), fk.parentTable()), ignored -> new ArrayList<>()).add(fk);
            }
        }
        for (Map.Entry<String, List<OracleEaForeignKey>> entry : byTarget.entrySet()) {
            OracleEaForeignKey first = entry.getValue().getFirst();
            OracleEaTable stub = loadReferencedTableStub(first.parentOwner(), first.parentTable(), entry.getValue(), includeComments, warnings);
            tables.put(entry.getKey(), stub);
        }
    }

    private OracleEaTable loadReferencedTableStub(
            String owner,
            String tableName,
            List<OracleEaForeignKey> foreignKeys,
            boolean includeComments,
            List<String> warnings
    ) {
        Set<String> requiredColumns = new LinkedHashSet<>();
        foreignKeys.forEach(fk -> fk.columns().forEach(pair -> requiredColumns.add(pair.parentColumn())));

        List<OracleEaColumn> columns = new ArrayList<>();
        try {
            jdbc.sql(REFERENCED_COLUMN_METADATA_SQL)
                    .param("owner", owner)
                    .param("tableName", tableName)
                    .query((rs, rowNum) -> new OracleEaColumn(
                            upper(rs.getString("COLUMN_NAME")),
                            rs.getString("DATA_TYPE"),
                            integer(rs.getObject("DATA_LENGTH")),
                            integer(rs.getObject("CHAR_LENGTH")),
                            rs.getString("CHAR_USED"),
                            integer(rs.getObject("DATA_PRECISION")),
                            integer(rs.getObject("DATA_SCALE")),
                            "Y".equalsIgnoreCase(rs.getString("NULLABLE")),
                            rs.getInt("COLUMN_ID"),
                            null,
                            includeComments ? rs.getString("COLUMN_COMMENT") : null,
                            "YES".equalsIgnoreCase(rs.getString("IDENTITY_COLUMN")),
                            "YES".equalsIgnoreCase(rs.getString("VIRTUAL_COLUMN"))
                    ))
                    .list()
                    .stream()
                    .filter(column -> requiredColumns.contains(column.columnName()))
                    .forEach(columns::add);
        } catch (DataAccessException exception) {
            warnings.add("جزئیات ستون‌های جدول مرجع " + owner + "." + tableName + " قابل خواندن نبود؛ Stub حداقلی ساخته شد.");
        }

        if (columns.size() < requiredColumns.size()) {
            Set<String> found = columns.stream().map(OracleEaColumn::columnName).collect(java.util.stream.Collectors.toSet());
            int position = columns.stream().mapToInt(OracleEaColumn::position).max().orElse(0) + 1;
            for (String missing : requiredColumns) {
                if (!found.contains(missing)) {
                    columns.add(new OracleEaColumn(missing, "VARCHAR2", 200, 200, "C", null, null, true, position++, null, null, false, false));
                }
            }
        }
        columns.sort(Comparator.comparingInt(OracleEaColumn::position));

        Map<String, OracleEaKeyConstraint> keys = new LinkedHashMap<>();
        for (OracleEaForeignKey fk : foreignKeys) {
            keys.computeIfAbsent(fk.parentConstraintName(), ignored -> new OracleEaKeyConstraint(
                    fk.parentConstraintName(),
                    fk.parentConstraintType(),
                    fk.columns().stream().sorted(Comparator.comparingInt(OracleEaForeignKeyColumn::position)).map(OracleEaForeignKeyColumn::parentColumn).toList(),
                    "ENABLED",
                    null
            ));
        }

        String comment = null;
        String tablespace = null;
        try {
            List<TableHeader> headers = jdbc.sql("""
                    SELECT T.TABLE_NAME, T.TABLESPACE_NAME, C.COMMENTS
                      FROM ALL_TABLES T
                      LEFT JOIN ALL_TAB_COMMENTS C
                        ON C.OWNER=T.OWNER AND C.TABLE_NAME=T.TABLE_NAME AND C.TABLE_TYPE='TABLE'
                     WHERE T.OWNER=:owner AND T.TABLE_NAME=:tableName
                    """)
                    .param("owner", owner)
                    .param("tableName", tableName)
                    .query((rs, rowNum) -> new TableHeader(upper(rs.getString("TABLE_NAME")), rs.getString("TABLESPACE_NAME"), rs.getString("COMMENTS")))
                    .list();
            if (!headers.isEmpty()) {
                comment = includeComments ? headers.getFirst().comment() : null;
                tablespace = headers.getFirst().tablespace();
            }
        } catch (DataAccessException ignored) {
            // Stub metadata is optional; relationship remains exportable.
        }

        return new OracleEaTable(owner, tableName, comment, tablespace, true, List.copyOf(columns), List.copyOf(keys.values()), List.of(), List.of());
    }

    private static Predicate<String> tableFilter(String rawPattern) {
        String pattern = rawPattern == null || rawPattern.isBlank() ? "%" : rawPattern.trim().toUpperCase(Locale.ROOT);
        if ("*".equals(pattern)) pattern = "%";
        pattern = pattern.replace('*', '%').replace('?', '_');
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '%') regex.append(".*");
            else if (ch == '_') regex.append('.');
            else regex.append(Pattern.quote(String.valueOf(ch)));
        }
        regex.append('$');
        Pattern compiled = Pattern.compile(regex.toString());
        return table -> compiled.matcher(table.toUpperCase(Locale.ROOT)).matches();
    }

    private static boolean looksLikeNotNull(String condition) {
        String normalized = condition.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return normalized.matches("\\\"?[A-Z0-9_$#]+\\\"? IS NOT NULL");
    }

    private static String qualified(String owner, String table) {
        return upper(owner) + "." + upper(table);
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private static String indexColumnName(String value, int position) {
        String normalized = upper(value);
        return normalized == null ? "EXPRESSION_" + position : normalized;
    }

    private static Integer integer(Object value) {
        if (value == null) return null;
        return value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString());
    }

    private record TableHeader(String tableName, String tablespace, String comment) {
    }

    private record DefaultValue(String tableName, String columnName, String value) {
    }

    private record ColumnRow(String tableName, OracleEaColumn column) {
    }

    private record KeyRow(String tableName, String constraintName, String constraintType, String status,
                          String indexName, String columnName, int position) {
    }

    private static final class KeyBuilder {
        final String tableName;
        final String constraintName;
        final String constraintType;
        final String status;
        final String indexName;
        final List<String> columns = new ArrayList<>();

        KeyBuilder(String tableName, String constraintName, String constraintType, String status, String indexName) {
            this.tableName = tableName;
            this.constraintName = constraintName;
            this.constraintType = constraintType;
            this.status = status;
            this.indexName = indexName;
        }
    }

    private record IndexRow(String tableName, String indexName, String uniqueness, String indexType, String status,
                            String columnName, int position, String descend) {
    }

    private static final class IndexBuilder {
        final String tableName;
        final String indexName;
        final boolean unique;
        final String indexType;
        final String status;
        final List<OracleEaIndexColumn> columns = new ArrayList<>();

        IndexBuilder(String tableName, String indexName, boolean unique, String indexType, String status) {
            this.tableName = tableName;
            this.indexName = indexName;
            this.unique = unique;
            this.indexType = indexType;
            this.status = status;
        }
    }

    private record CheckRow(String tableName, String constraintName, String condition, String status, String generated) {
    }

    private record ForeignKeyRow(String constraintName, String childTable, String parentOwner, String parentTable,
                                 String parentConstraint, String parentConstraintType, String deleteRule, String status,
                                 String deferrable, String deferred, String childColumn, String parentColumn, int position) {
    }

    private static final class ForeignKeyBuilder {
        final String childOwner;
        final String constraintName;
        final String childTable;
        final String parentOwner;
        final String parentTable;
        final String parentConstraintName;
        final String parentConstraintType;
        final String deleteRule;
        final String status;
        final String deferrable;
        final String deferred;
        final List<OracleEaForeignKeyColumn> columns = new ArrayList<>();

        ForeignKeyBuilder(String childOwner, String constraintName, String childTable, String parentOwner, String parentTable,
                          String parentConstraintName, String parentConstraintType, String deleteRule, String status,
                          String deferrable, String deferred) {
            this.childOwner = childOwner;
            this.constraintName = constraintName;
            this.childTable = childTable;
            this.parentOwner = parentOwner;
            this.parentTable = parentTable;
            this.parentConstraintName = parentConstraintName;
            this.parentConstraintType = parentConstraintType;
            this.deleteRule = deleteRule;
            this.status = status;
            this.deferrable = deferrable;
            this.deferred = deferred;
        }
    }
}
