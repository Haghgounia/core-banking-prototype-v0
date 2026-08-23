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
                    rowCount,
                    rowCountNote
            ));
        }
        return new OracleSchemaSnapshot(schema, Map.copyOf(tables));
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

    private record NamedComment(String name, String comment) {
    }
}
