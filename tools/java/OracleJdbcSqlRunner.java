import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OracleJdbcSqlRunner {
    private record Connect(String user, String password, String jdbcUrl) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: OracleJdbcSqlRunner <sql-file>");
            System.exit(2);
        }
        String spec = System.getenv("CORE_BANKING_ORACLE_CONNECT");
        if (spec == null || spec.isBlank()) {
            System.err.println("ERROR: CORE_BANKING_ORACLE_CONNECT is not set.");
            System.exit(2);
        }
        Connect c = parse(spec.trim());
        Path sqlFile = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(sqlFile)) {
            System.err.println("ERROR: SQL file not found: " + sqlFile);
            System.exit(2);
        }
        System.out.println("INFO: Oracle JDBC target: " + sanitizedTarget(c.jdbcUrl));
        try (Connection con = DriverManager.getConnection(c.jdbcUrl, c.user, c.password)) {
            con.setAutoCommit(false);
            enableDbmsOutput(con);
            try {
                runScript(con, sqlFile);
                con.commit();
                drainDbmsOutput(con);
            } catch (Exception e) {
                // Preserve DBMS_OUTPUT diagnostics emitted before a failing PL/SQL block raises.
                // Without this, verifier PASS/FAIL lines are lost and only ORA-20xxx is visible.
                try { drainDbmsOutput(con); } catch (SQLException outputError) { e.addSuppressed(outputError); }
                try { con.rollback(); } catch (SQLException ignored) {}
                throw e;
            }
        }
    }

    private static Connect parse(String spec) {
        int slash = spec.indexOf('/');
        int at = spec.lastIndexOf("@//");
        if (slash <= 0 || at <= slash + 1 || at + 3 >= spec.length()) {
            throw new IllegalArgumentException("CORE_BANKING_ORACLE_CONNECT must use user/password@//host:port/service format.");
        }
        String user = spec.substring(0, slash);
        String password = spec.substring(slash + 1, at);
        String target = spec.substring(at + 3);
        return new Connect(user, password, "jdbc:oracle:thin:@//" + target);
    }

    private static String sanitizedTarget(String jdbcUrl) {
        return jdbcUrl.replace("jdbc:oracle:thin:@//", "//");
    }

    private static void enableDbmsOutput(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("BEGIN DBMS_OUTPUT.ENABLE(NULL); END;");
        }
    }

    private static void drainDbmsOutput(Connection con) throws SQLException {
        while (true) {
            try (CallableStatement cs = con.prepareCall("BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;")) {
                cs.registerOutParameter(1, Types.VARCHAR);
                cs.registerOutParameter(2, Types.INTEGER);
                cs.execute();
                int status = cs.getInt(2);
                if (status != 0) return;
                String line = cs.getString(1);
                if (line != null) System.out.println(line);
            }
        }
    }

    private static void runScript(Connection con, Path file) throws Exception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        StringBuilder simple = new StringBuilder();
        StringBuilder plsql = null;
        for (String raw : lines) {
            String trimmed = raw.trim();
            String upper = trimmed.toUpperCase();
            if (plsql != null) {
                if (trimmed.equals("/")) {
                    execute(con, plsql.toString());
                    drainDbmsOutput(con);
                    plsql = null;
                } else {
                    plsql.append(raw).append('\n');
                }
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            if (upper.startsWith("SET ") || upper.startsWith("WHENEVER ") || upper.startsWith("SPOOL ") || upper.equals("EXIT")) continue;
            if (upper.startsWith("PROMPT")) {
                System.out.println(trimmed.length() > 6 ? trimmed.substring(6).stripLeading() : "");
                continue;
            }
            if (upper.equals("DECLARE") || upper.equals("BEGIN") || upper.startsWith("CREATE OR REPLACE ")) {
                if (!simple.toString().isBlank()) {
                    executeSimpleBuffer(con, simple);
                    simple.setLength(0);
                }
                plsql = new StringBuilder();
                plsql.append(raw).append('\n');
                continue;
            }
            simple.append(raw).append('\n');
            if (trimmed.endsWith(";")) {
                executeSimpleBuffer(con, simple);
                simple.setLength(0);
                drainDbmsOutput(con);
            }
        }
        if (plsql != null) throw new IllegalStateException("Unterminated PL/SQL block in " + file);
        if (!simple.toString().isBlank()) executeSimpleBuffer(con, simple);
    }

    private static void executeSimpleBuffer(Connection con, StringBuilder buf) throws SQLException {
        String sql = buf.toString().trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();
        if (sql.equalsIgnoreCase("COMMIT")) { con.commit(); return; }
        if (sql.equalsIgnoreCase("ROLLBACK")) { con.rollback(); return; }
        execute(con, sql);
    }

    private static void execute(Connection con, String sql) throws SQLException {
        if (sql == null || sql.isBlank()) return;
        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }
}
