package com.behsazan.corebanking.system.modelcomparison;

import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Repository
class OracleSchemaInspector {
    private final JdbcClient jdbcClient;
    private final DataSource dataSource;
    private final ConfiguredDatabaseSchemas schemas;

    OracleSchemaInspector(JdbcClient jdbcClient, DataSource dataSource, ConfiguredDatabaseSchemas schemas) {
        this.jdbcClient = jdbcClient;
        this.dataSource = dataSource;
        this.schemas = schemas;
    }

    ComparisonConfiguration configuration() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return new ComparisonConfiguration(
                    schemas.options(),
                    schemas.defaultSchema(),
                    metadata.getDatabaseProductName(),
                    metadata.getDatabaseProductVersion(),
                    metadata.getURL(),
                    metadata.getUserName()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot read Oracle connection metadata", exception);
        }
    }

    OracleSchemaSnapshot inspect(String schemaName, Set<String> rowCountTables, boolean includeRowCounts) {
        String schema = schemas.require(schemaName);
        long schemaExists = jdbcClient.sql("SELECT COUNT(*) FROM ALL_USERS WHERE USERNAME = :schema")
                .param("schema", schema)
                .query(Long.class)
                .single();
        if (schemaExists == 0) {
            throw new ModelComparisonValidationException("Schema تنظیم‌شده در Oracle وجود ندارد یا برای کاربر اتصال قابل مشاهده نیست: " + schema);
        }

        List<String> tableNames = jdbcClient.sql("SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = :schema ORDER BY TABLE_NAME")
                .param("schema", schema)
                .query(String.class)
                .list();

        Map<String, String> tableComments = new LinkedHashMap<>();
        jdbcClient.sql("""
                SELECT TABLE_NAME, COMMENTS
                  FROM ALL_TAB_COMMENTS
                 WHERE OWNER = :schema
                   AND TABLE_TYPE = 'TABLE'
                """)
                .param("schema", schema)
                .query((rs, rowNum) -> new NamedComment(rs.getString("TABLE_NAME"), rs.getString("COMMENTS")))
                .list()
                .forEach(value -> tableComments.put(value.name(), value.comment()));

        Map<String, List<OracleColumnDefinition>> columnsByTable = new LinkedHashMap<>();
        jdbcClient.sql("""
                SELECT C.TABLE_NAME, C.COLUMN_NAME, C.DATA_TYPE, C.DATA_LENGTH, C.CHAR_LENGTH, C.CHAR_USED,
                       C.DATA_PRECISION, C.DATA_SCALE, C.NULLABLE, C.COLUMN_ID, CC.COMMENTS AS COLUMN_COMMENT
                  FROM ALL_TAB_COLUMNS C
                  LEFT JOIN ALL_COL_COMMENTS CC
                    ON CC.OWNER = C.OWNER
                   AND CC.TABLE_NAME = C.TABLE_NAME
                   AND CC.COLUMN_NAME = C.COLUMN_NAME
                 WHERE C.OWNER = :schema
                 ORDER BY C.TABLE_NAME, C.COLUMN_ID
                """)
                .param("schema", schema)
                .query((rs, rowNum) -> new ColumnWithTable(
                        rs.getString("TABLE_NAME"),
                        new OracleColumnDefinition(
                                rs.getString("COLUMN_NAME"),
                                rs.getString("DATA_TYPE"),
                                nullableInteger(rs.getObject("DATA_LENGTH")),
                                nullableInteger(rs.getObject("CHAR_LENGTH")),
                                rs.getString("CHAR_USED"),
                                nullableInteger(rs.getObject("DATA_PRECISION")),
                                nullableInteger(rs.getObject("DATA_SCALE")),
                                "Y".equalsIgnoreCase(rs.getString("NULLABLE")),
                                rs.getInt("COLUMN_ID"),
                                rs.getString("COLUMN_COMMENT")
                        )
                ))
                .list()
                .forEach(value -> columnsByTable.computeIfAbsent(value.tableName(), ignored -> new ArrayList<>()).add(value.column()));

        Map<String, List<String>> primaryKeys = new LinkedHashMap<>();
        jdbcClient.sql("""
                SELECT CC.TABLE_NAME, CC.COLUMN_NAME, CC.POSITION
                  FROM ALL_CONSTRAINTS C
                  JOIN ALL_CONS_COLUMNS CC
                    ON CC.OWNER = C.OWNER
                   AND CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME
                 WHERE C.OWNER = :schema
                   AND C.CONSTRAINT_TYPE = 'P'
                 ORDER BY CC.TABLE_NAME, CC.POSITION
                """)
                .param("schema", schema)
                .query((rs, rowNum) -> new PrimaryKeyColumn(
                        rs.getString("TABLE_NAME"),
                        rs.getString("COLUMN_NAME"),
                        rs.getInt("POSITION")
                ))
                .list()
                .forEach(value -> primaryKeys.computeIfAbsent(value.tableName(), ignored -> new ArrayList<>()).add(value.columnName()));

        Map<String, List<OracleEaForeignKey>> foreignKeys = loadForeignKeys(schema);
        Map<String, List<OracleEaCheckConstraint>> checks = loadChecks(schema);

        Map<String, OracleTableDefinition> tables = new LinkedHashMap<>();
        for (String rawName : tableNames) {
            String tableName = rawName.toUpperCase(Locale.ROOT);
            List<OracleColumnDefinition> columns = columnsByTable.getOrDefault(rawName, List.of()).stream()
                    .sorted(Comparator.comparingInt(OracleColumnDefinition::position)).toList();
            Long rowCount = null;
            String rowCountNote = null;
            if (includeRowCounts && rowCountTables.contains(tableName)) {
                try {
                    rowCount = countRows(schema, rawName);
                } catch (DataAccessException exception) {
                    rowCountNote = "تعداد رکورد به‌دلیل محدودیت دسترسی یا خطای Oracle قابل محاسبه نبود.";
                }
            }
            tables.put(tableName, new OracleTableDefinition(
                    tableName,
                    tableComments.get(rawName),
                    columns,
                    List.copyOf(primaryKeys.getOrDefault(rawName, List.of())),
                    List.copyOf(foreignKeys.getOrDefault(tableName, List.of())),
                    List.copyOf(checks.getOrDefault(tableName, List.of())),
                    rowCount,
                    rowCountNote
            ));
        }
        return new OracleSchemaSnapshot(schema, Map.copyOf(tables));
    }


    private Map<String, List<OracleEaForeignKey>> loadForeignKeys(String schema) {
        Map<String, ForeignKeyBuilder> builders = new LinkedHashMap<>();
        jdbcClient.sql("""
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

        Map<String, List<OracleEaForeignKey>> result = new LinkedHashMap<>();
        builders.values().forEach(builder -> result.computeIfAbsent(builder.childTable, ignored -> new ArrayList<>()).add(
                new OracleEaForeignKey(
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
                )
        ));
        result.values().forEach(list -> list.sort(Comparator.comparing(OracleEaForeignKey::constraintName)));
        return result;
    }

    private Map<String, List<OracleEaCheckConstraint>> loadChecks(String schema) {
        Map<String, List<OracleEaCheckConstraint>> result = new LinkedHashMap<>();
        try {
            jdbcClient.sql("""
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
                            trimToNull(rs.getString("SEARCH_CONDITION_VC")),
                            rs.getString("STATUS"),
                            rs.getString("GENERATED")
                    ))
                    .list()
                    .stream()
                    .filter(row -> row.condition() != null)
                    .filter(row -> !looksLikeNotNull(row.condition()))
                    .forEach(row -> result.computeIfAbsent(row.tableName(), ignored -> new ArrayList<>()).add(
                            new OracleEaCheckConstraint(row.constraintName(), row.condition(), row.status())
                    ));
        } catch (DataAccessException exception) {
            throw new ModelComparisonValidationException(
                    "خواندن Check Constraintها از Oracle Data Dictionary ممکن نبود (SEARCH_CONDITION_VC).",
                    exception
            );
        }
        result.values().forEach(list -> list.sort(Comparator.comparing(OracleEaCheckConstraint::constraintName)));
        return result;
    }

    private static boolean looksLikeNotNull(String condition) {
        String normalized = condition.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return normalized.matches("\\\"?[A-Z0-9_$#]+\\\"? IS NOT NULL");
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long countRows(String schema, String tableName) {
        String sql = "SELECT COUNT(*) FROM " + quoteIdentifier(schema) + "." + quoteIdentifier(tableName);
        return jdbcClient.sql(sql).query(Long.class).single();
    }

    private static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank() || identifier.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Unsafe Oracle identifier");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static Integer nullableInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.valueOf(value.toString());
    }

    private record ColumnWithTable(String tableName, OracleColumnDefinition column) {
    }

    private record PrimaryKeyColumn(String tableName, String columnName, int position) {
    }


    private record ForeignKeyRow(
            String constraintName,
            String childTable,
            String parentOwner,
            String parentTable,
            String parentConstraint,
            String parentConstraintType,
            String deleteRule,
            String status,
            String deferrable,
            String deferred,
            String childColumn,
            String parentColumn,
            int position
    ) {
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

        ForeignKeyBuilder(
                String childOwner,
                String constraintName,
                String childTable,
                String parentOwner,
                String parentTable,
                String parentConstraintName,
                String parentConstraintType,
                String deleteRule,
                String status,
                String deferrable,
                String deferred
        ) {
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

    private record CheckRow(String tableName, String constraintName, String condition, String status, String generated) {
    }

    private record NamedComment(String name, String comment) {
    }
}
