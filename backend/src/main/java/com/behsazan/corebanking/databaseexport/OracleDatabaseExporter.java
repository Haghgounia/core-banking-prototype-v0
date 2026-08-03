package com.behsazan.corebanking.databaseexport;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OracleDatabaseExporter {
    private static final DateTimeFormatter DIRECTORY_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]{0,127}");
    private static final Pattern SEQUENCE_DEFAULT = Pattern.compile(
            "(?i)(?:\"?[A-Z0-9_$#]+\"?\\.)?\"?([A-Z][A-Z0-9_$#]{0,127})\"?\\.NEXTVAL"
    );
    private static final int CLOB_CHUNK_SIZE = 1000;

    private final DataSource dataSource;

    public OracleDatabaseExporter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ExportResult export(DatabaseExportRequest request) throws SQLException, IOException {
        Path exportDirectory = request.outputRoot()
                .resolve(DIRECTORY_TIME.format(LocalDateTime.now()))
                .resolve(request.schemaName().toLowerCase(Locale.ROOT));
        Path ddlDirectory = exportDirectory.resolve("ddl");
        Path dataDirectory = exportDirectory.resolve("data");
        Files.createDirectories(ddlDirectory);
        Files.createDirectories(dataDirectory);

        List<TableExportSummary> summaries = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            configureMetadata(connection);
            List<String> tables = findTables(connection, request.schemaName(), request.tablePrefix());
            if (tables.isEmpty()) {
                throw new IllegalStateException(
                        "No tables found for schema " + request.schemaName()
                                + " and prefix " + request.tablePrefix()
                );
            }

            for (String tableName : tables) {
                System.out.printf("  Exporting %s.%s...%n", request.schemaName(), tableName);
                exportDdl(connection, request.schemaName(), tableName, ddlDirectory);
                long rowCount = exportData(connection, request.schemaName(), tableName, dataDirectory);
                summaries.add(new TableExportSummary(tableName, rowCount));
                System.out.printf("    rows: %d%n", rowCount);
            }
        }

        writeManifest(exportDirectory, request, summaries);
        long totalRows = summaries.stream().mapToLong(TableExportSummary::rowCount).sum();
        return new ExportResult(exportDirectory, summaries.size(), totalRows);
    }

    private static void configureMetadata(Connection connection) throws SQLException {
        String block = """
                BEGIN
                  DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', TRUE);
                  DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'PRETTY', TRUE);
                  DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS', FALSE);
                  DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'REF_CONSTRAINTS', FALSE);
                END;
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(block);
        }
    }

    private static List<String> findTables(
            Connection connection,
            String schemaName,
            String tablePrefix
    ) throws SQLException {
        String sql = """
                SELECT TABLE_NAME
                  FROM ALL_TABLES
                 WHERE OWNER = ?
                   AND TABLE_NAME LIKE ? ESCAPE '\\'
                 ORDER BY TABLE_NAME
                """;
        String escapedPrefix = tablePrefix
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("%", "\\%");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            statement.setString(2, escapedPrefix + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> tables = new ArrayList<>();
                while (resultSet.next()) {
                    tables.add(requireIdentifier(resultSet.getString(1)));
                }
                return List.copyOf(tables);
            }
        }
    }

    private static void exportDdl(
            Connection connection,
            String schemaName,
            String tableName,
            Path ddlDirectory
    ) throws SQLException, IOException {
        StringBuilder sql = new StringBuilder(16_384);
        sql.append("-- Generated by Core Banking Prototype Oracle exporter.\n");
        sql.append("-- Schema: ").append(schemaName).append("\n");
        sql.append("-- Table : ").append(tableName).append("\n\n");
        sql.append("SET DEFINE OFF;\n");
        sql.append("SET SQLBLANKLINES ON;\n\n");

        appendSection(sql, "SEQUENCES", sequenceDdls(connection, schemaName, tableName));
        appendSection(sql, "TABLE", List.of(getDdl(connection, "TABLE", tableName, schemaName)));
        appendSection(sql, "INDEXES", dependentDdl(connection, "INDEX", tableName, schemaName));
        appendSection(sql, "PRIMARY, UNIQUE AND CHECK CONSTRAINTS",
                dependentDdl(connection, "CONSTRAINT", tableName, schemaName));
        appendSection(sql, "FOREIGN KEYS",
                dependentDdl(connection, "REF_CONSTRAINT", tableName, schemaName));
        appendSection(sql, "COMMENTS", commentDdls(connection, schemaName, tableName));
        appendSection(sql, "TRIGGERS", dependentDdl(connection, "TRIGGER", tableName, schemaName));
        appendSection(sql, "OBJECT GRANTS", dependentDdl(connection, "OBJECT_GRANT", tableName, schemaName));

        writeAtomically(ddlDirectory.resolve(tableName + "_ddl.sql"), sql.toString());
    }

    private static List<String> sequenceDdls(
            Connection connection,
            String schemaName,
            String tableName
    ) throws SQLException {
        Set<String> sequences = new LinkedHashSet<>();
        String defaultsSql = """
                SELECT DATA_DEFAULT
                  FROM ALL_TAB_COLUMNS
                 WHERE OWNER = ?
                   AND TABLE_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(defaultsSql)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String defaultExpression = resultSet.getString(1);
                    if (defaultExpression == null) {
                        continue;
                    }
                    Matcher matcher = SEQUENCE_DEFAULT.matcher(defaultExpression);
                    while (matcher.find()) {
                        sequences.add(requireIdentifier(matcher.group(1).toUpperCase(Locale.ROOT)));
                    }
                }
            }
        }

        String conventionalName = "SEQ_" + tableName;
        if (sequenceExists(connection, schemaName, conventionalName)) {
            sequences.add(conventionalName);
        }

        List<String> ddls = new ArrayList<>();
        for (String sequence : sequences) {
            ddls.add(getDdl(connection, "SEQUENCE", sequence, schemaName));
        }
        return List.copyOf(ddls);
    }

    private static boolean sequenceExists(
            Connection connection,
            String schemaName,
            String sequenceName
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM ALL_SEQUENCES
                 WHERE SEQUENCE_OWNER = ?
                   AND SEQUENCE_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            statement.setString(2, sequenceName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static String getDdl(
            Connection connection,
            String objectType,
            String objectName,
            String schemaName
    ) throws SQLException {
        String sql = "SELECT DBMS_METADATA.GET_DDL(?, ?, ?) FROM DUAL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, objectType);
            statement.setString(2, objectName);
            statement.setString(3, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return readText(resultSet, 1);
            }
        }
    }

    private static List<String> dependentDdl(
            Connection connection,
            String objectType,
            String tableName,
            String schemaName
    ) throws SQLException {
        String sql = "SELECT DBMS_METADATA.GET_DEPENDENT_DDL(?, ?, ?) FROM DUAL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, objectType);
            statement.setString(2, tableName);
            statement.setString(3, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                String ddl = readText(resultSet, 1);
                return ddl == null || ddl.isBlank() ? List.of() : List.of(ddl);
            }
        } catch (SQLException exception) {
            if (isNoDependentObject(exception)) {
                return List.of();
            }
            throw exception;
        }
    }

    private static boolean isNoDependentObject(SQLException exception) {
        return exception.getErrorCode() == 31608
                || exception.getMessage() != null
                && exception.getMessage().contains("ORA-31608");
    }

    private static List<String> commentDdls(
            Connection connection,
            String schemaName,
            String tableName
    ) throws SQLException {
        List<String> comments = new ArrayList<>();
        String tableCommentSql = """
                SELECT COMMENTS
                  FROM ALL_TAB_COMMENTS
                 WHERE OWNER = ?
                   AND TABLE_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(tableCommentSql)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String comment = resultSet.getString(1);
                    if (comment != null) {
                        comments.add("COMMENT ON TABLE " + qualified(schemaName, tableName)
                                + " IS " + sqlString(comment) + ";");
                    }
                }
            }
        }

        String columnCommentSql = """
                SELECT COLUMN_NAME, COMMENTS
                  FROM ALL_COL_COMMENTS
                 WHERE OWNER = ?
                   AND TABLE_NAME = ?
                   AND COMMENTS IS NOT NULL
                 ORDER BY COLUMN_NAME
                """;
        try (PreparedStatement statement = connection.prepareStatement(columnCommentSql)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String columnName = requireIdentifier(resultSet.getString(1));
                    String comment = resultSet.getString(2);
                    comments.add("COMMENT ON COLUMN " + qualified(schemaName, tableName)
                            + "." + columnName + " IS " + sqlString(comment) + ";");
                }
            }
        }
        return List.copyOf(comments);
    }

    private static void appendSection(StringBuilder target, String title, List<String> statements) {
        if (statements.isEmpty()) {
            return;
        }
        target.append("PROMPT ").append(title).append("\n");
        target.append("-- ============================================================================\n");
        for (String statement : statements) {
            if (statement == null || statement.isBlank()) {
                continue;
            }
            target.append(statement.strip()).append("\n");
            String trimmed = statement.stripTrailing();
            if (!trimmed.endsWith(";") && !trimmed.endsWith("/")) {
                target.append(";\n");
            }
            target.append("\n");
        }
    }

    private static long exportData(
            Connection connection,
            String schemaName,
            String tableName,
            Path dataDirectory
    ) throws SQLException, IOException {
        List<ColumnMetadata> columns = findExportableColumns(connection, schemaName, tableName);
        validateSupportedColumns(schemaName, tableName, columns);
        List<String> primaryKeyColumns = findPrimaryKeyColumns(connection, schemaName, tableName);

        String selectSql = buildSelectSql(schemaName, tableName, columns, primaryKeyColumns);
        Path target = dataDirectory.resolve(tableName + "_data.sql");
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        long rowCount = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                temporary,
                StandardCharsets.UTF_8
        ); PreparedStatement statement = connection.prepareStatement(
                selectSql,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY
        )) {
            statement.setFetchSize(500);
            writer.write("-- Generated by Core Banking Prototype Oracle exporter.\n");
            writer.write("-- Source: " + qualified(schemaName, tableName) + "\n\n");
            writer.write("SET DEFINE OFF;\n");
            writer.write("SET SQLBLANKLINES ON;\n\n");

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    writer.write(buildInsert(schemaName, tableName, columns, resultSet));
                    writer.newLine();
                    rowCount++;
                }
            }

            if (rowCount == 0) {
                writer.write("-- Table contains no rows.\n");
            } else {
                writer.write("\nCOMMIT;\n");
            }
        } catch (SQLException | IOException | RuntimeException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }

        moveReplacing(temporary, target);
        return rowCount;
    }

    private static List<ColumnMetadata> findExportableColumns(
            Connection connection,
            String schemaName,
            String tableName
    ) throws SQLException {
        String sql = """
                SELECT COLUMN_NAME, DATA_TYPE, DATA_TYPE_OWNER
                  FROM ALL_TAB_COLS
                 WHERE OWNER = ?
                   AND TABLE_NAME = ?
                   AND HIDDEN_COLUMN = 'NO'
                   AND VIRTUAL_COLUMN = 'NO'
                 ORDER BY COLUMN_ID
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ColumnMetadata> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(new ColumnMetadata(
                            requireIdentifier(resultSet.getString(1)),
                            resultSet.getString(2).toUpperCase(Locale.ROOT),
                            resultSet.getString(3)
                    ));
                }
                if (columns.isEmpty()) {
                    throw new IllegalStateException("No exportable columns found for "
                            + qualified(schemaName, tableName));
                }
                return List.copyOf(columns);
            }
        }
    }

    private static void validateSupportedColumns(
            String schemaName,
            String tableName,
            List<ColumnMetadata> columns
    ) {
        for (ColumnMetadata column : columns) {
            if (!column.isSupported()) {
                throw new IllegalStateException(
                        "Unsupported data type " + column.qualifiedType()
                                + " in " + qualified(schemaName, tableName)
                                + "." + column.name()
                );
            }
        }
    }

    private static List<String> findPrimaryKeyColumns(
            Connection connection,
            String schemaName,
            String tableName
    ) throws SQLException {
        String sql = """
                SELECT CC.COLUMN_NAME
                  FROM ALL_CONSTRAINTS C
                  JOIN ALL_CONS_COLUMNS CC
                    ON CC.OWNER = C.OWNER
                   AND CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME
                   AND CC.TABLE_NAME = C.TABLE_NAME
                 WHERE C.OWNER = ?
                   AND C.TABLE_NAME = ?
                   AND C.CONSTRAINT_TYPE = 'P'
                 ORDER BY CC.POSITION
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(requireIdentifier(resultSet.getString(1)));
                }
                return List.copyOf(columns);
            }
        }
    }

    private static String buildSelectSql(
            String schemaName,
            String tableName,
            List<ColumnMetadata> columns,
            List<String> primaryKeyColumns
    ) {
        StringJoiner select = new StringJoiner(", ");
        for (ColumnMetadata column : columns) {
            select.add(column.selectExpression());
        }

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(select)
                .append(" FROM ")
                .append(qualified(schemaName, tableName));
        if (!primaryKeyColumns.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", primaryKeyColumns));
        }
        return sql.toString();
    }

    private static String buildInsert(
            String schemaName,
            String tableName,
            List<ColumnMetadata> columns,
            ResultSet resultSet
    ) throws SQLException {
        StringJoiner names = new StringJoiner(", ");
        StringJoiner values = new StringJoiner(", ");
        for (int index = 0; index < columns.size(); index++) {
            ColumnMetadata column = columns.get(index);
            names.add(column.name());
            values.add(column.sqlLiteral(resultSet, index + 1));
        }
        return "INSERT INTO " + qualified(schemaName, tableName)
                + " (" + names + ") VALUES (" + values + ");";
    }

    private static void writeManifest(
            Path exportDirectory,
            DatabaseExportRequest request,
            List<TableExportSummary> summaries
    ) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("Core Banking Prototype - Oracle database export\n");
        content.append("Schema: ").append(request.schemaName()).append("\n");
        content.append("Table prefix: ").append(request.tablePrefix()).append("\n");
        content.append("Tables: ").append(summaries.size()).append("\n");
        content.append("Rows: ").append(summaries.stream().mapToLong(TableExportSummary::rowCount).sum())
                .append("\n\n");
        for (TableExportSummary summary : summaries) {
            content.append(summary.tableName()).append(": ").append(summary.rowCount()).append(" rows\n");
        }
        writeAtomically(exportDirectory.resolve("manifest.txt"), content.toString());
    }

    private static String readText(ResultSet resultSet, int index) throws SQLException {
        Object value = resultSet.getObject(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Clob clob) {
            try (Reader reader = clob.getCharacterStream(); BufferedReader buffered = new BufferedReader(reader)) {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[8192];
                int read;
                while ((read = buffered.read(buffer)) >= 0) {
                    content.append(buffer, 0, read);
                }
                return content.toString();
            } catch (IOException exception) {
                throw new SQLException("Could not read Oracle CLOB", exception);
            }
        }
        return value.toString();
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            moveReplacing(temporary, target);
        } catch (IOException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static String sqlString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String nationalSqlString(String value) {
        return "N" + sqlString(value);
    }

    private static String clobExpression(String value, boolean national) {
        if (value.isEmpty()) {
            return national ? "TO_NCLOB(N'')" : "EMPTY_CLOB()";
        }
        StringJoiner chunks = new StringJoiner(" || ");
        for (int offset = 0; offset < value.length(); offset += CLOB_CHUNK_SIZE) {
            String chunk = value.substring(offset, Math.min(value.length(), offset + CLOB_CHUNK_SIZE));
            chunks.add(national
                    ? "TO_NCLOB(" + nationalSqlString(chunk) + ")"
                    : "TO_CLOB(" + sqlString(chunk) + ")");
        }
        return chunks.toString();
    }

    private static String requireIdentifier(String value) {
        String normalized = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || !SAFE_IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        }
        return normalized;
    }

    private static String qualified(String schemaName, String objectName) {
        return requireIdentifier(schemaName) + "." + requireIdentifier(objectName);
    }

    public record ExportResult(Path outputDirectory, int tableCount, long rowCount) {
    }

    private record TableExportSummary(String tableName, long rowCount) {
    }

    private record ColumnMetadata(String name, String dataType, String dataTypeOwner) {
        private ColumnMetadata {
            dataType = normalizeDataType(dataType);
        }

        private static String normalizeDataType(String value) {
            String normalized = value.toUpperCase(Locale.ROOT);
            if (normalized.startsWith("TIMESTAMP") && normalized.contains("WITH LOCAL TIME ZONE")) {
                return "TIMESTAMP WITH LOCAL TIME ZONE";
            }
            if (normalized.startsWith("TIMESTAMP") && normalized.contains("WITH TIME ZONE")) {
                return "TIMESTAMP WITH TIME ZONE";
            }
            if (normalized.startsWith("TIMESTAMP")) {
                return "TIMESTAMP";
            }
            return normalized;
        }

        private boolean isSupported() {
            return switch (dataType) {
                case "CHAR", "NCHAR", "VARCHAR", "VARCHAR2", "NVARCHAR2", "LONG",
                     "NUMBER", "DECIMAL", "INTEGER", "FLOAT", "BINARY_FLOAT", "BINARY_DOUBLE",
                     "DATE", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE",
                     "RAW", "ROWID", "UROWID", "CLOB", "NCLOB" -> true;
                default -> false;
            };
        }

        private String qualifiedType() {
            return dataTypeOwner == null ? dataType : dataTypeOwner + "." + dataType;
        }

        private String selectExpression() {
            return switch (dataType) {
                case "DATE" -> "TO_CHAR(" + name + ", 'YYYY-MM-DD HH24:MI:SS') AS " + name;
                case "TIMESTAMP" -> "TO_CHAR(" + name + ", 'YYYY-MM-DD HH24:MI:SS.FF9') AS " + name;
                case "TIMESTAMP WITH TIME ZONE" -> "TO_CHAR(" + name
                        + ", 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM') AS " + name;
                case "TIMESTAMP WITH LOCAL TIME ZONE" -> "TO_CHAR(" + name
                        + ", 'YYYY-MM-DD HH24:MI:SS.FF9') AS " + name;
                case "RAW" -> "RAWTOHEX(" + name + ") AS " + name;
                default -> name;
            };
        }

        private String sqlLiteral(ResultSet resultSet, int index) throws SQLException {
            return switch (dataType) {
                case "CHAR", "VARCHAR", "VARCHAR2", "LONG", "ROWID", "UROWID" -> {
                    String value = resultSet.getString(index);
                    yield value == null ? "NULL" : sqlString(value);
                }
                case "NCHAR", "NVARCHAR2" -> {
                    String value = resultSet.getNString(index);
                    yield value == null ? "NULL" : nationalSqlString(value);
                }
                case "NUMBER", "DECIMAL", "INTEGER", "FLOAT" -> {
                    BigDecimal value = resultSet.getBigDecimal(index);
                    yield value == null ? "NULL" : value.toPlainString();
                }
                case "BINARY_FLOAT", "BINARY_DOUBLE" -> {
                    String value = resultSet.getString(index);
                    yield value == null ? "NULL" : value;
                }
                case "DATE" -> {
                    String value = resultSet.getString(index);
                    yield value == null ? "NULL"
                            : "TO_DATE(" + sqlString(value) + ", 'YYYY-MM-DD HH24:MI:SS')";
                }
                case "TIMESTAMP", "TIMESTAMP WITH LOCAL TIME ZONE" -> {
                    String value = resultSet.getString(index);
                    yield value == null ? "NULL"
                            : "TO_TIMESTAMP(" + sqlString(value) + ", 'YYYY-MM-DD HH24:MI:SS.FF9')";
                }
                case "TIMESTAMP WITH TIME ZONE" -> {
                    String value = resultSet.getString(index);
                    yield value == null ? "NULL"
                            : "TO_TIMESTAMP_TZ(" + sqlString(value)
                            + ", 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM')";
                }
                case "RAW" -> {
                    String value = resultSet.getString(index);
                    yield value == null ? "NULL" : "HEXTORAW(" + sqlString(value) + ")";
                }
                case "CLOB" -> {
                    Clob value = resultSet.getClob(index);
                    yield value == null ? "NULL" : clobExpression(readClob(value), false);
                }
                case "NCLOB" -> {
                    Clob value = resultSet.getNClob(index);
                    yield value == null ? "NULL" : clobExpression(readClob(value), true);
                }
                default -> throw new IllegalStateException("Unsupported Oracle data type: " + qualifiedType());
            };
        }

        private static String readClob(Clob clob) throws SQLException {
            try (Reader reader = clob.getCharacterStream()) {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[8192];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    content.append(buffer, 0, read);
                }
                return content.toString();
            } catch (IOException exception) {
                throw new SQLException("Could not read Oracle LOB", exception);
            }
        }
    }
}
