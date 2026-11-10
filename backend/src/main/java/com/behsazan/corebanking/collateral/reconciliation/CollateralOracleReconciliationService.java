package com.behsazan.corebanking.collateral.reconciliation;

import com.behsazan.corebanking.collateral.reconciliation.CollateralOracleReconciliationPolicy.CheckRow;
import com.behsazan.corebanking.collateral.reconciliation.CollateralOracleReconciliationPolicy.ReferenceRow;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Read-only inventory on the shared Spring DataSource; zero DML, DDL or financial permissions. */
@Service
public final class CollateralOracleReconciliationService {
    private final JdbcTemplate jdbc;

    public record Report(String schema, String checkedAt, String databaseName, String branchCode,
            String dictionaryState, String dataSource, boolean bankAccepted,
            Long ownBranchCollateralCount, Integer visibleSequences,
            List<ReferenceRow> references, List<CheckRow> checks, List<String> blockers) {}

    public CollateralOracleReconciliationService(DataSource dataSource) {
        CollateralOracleReconciliationPolicy.validateCatalog();
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(3);
    }

    public Report inspect(String branch) {
        // Partial evidence is preferable to an unbounded metadata/count probe.
        final long deadlineNanos = System.nanoTime() + 24_000_000_000L;
        String dbName = null;
        String dictionaryState = "UNKNOWN";
        Set<String> visible = Set.of();
        boolean metadataReady = false;
        try {
            dbName = jdbc.queryForObject("SELECT SYS_CONTEXT('USERENV','DB_NAME') FROM DUAL", String.class);
            List<String> tables = jdbc.queryForList(
                    "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER='CLT'", String.class);
            visible = new LinkedHashSet<>(tables);
            metadataReady = true;
            dictionaryState = visible.isEmpty() ? "NO_VISIBLE_TABLES" : "AVAILABLE";
        } catch (DataAccessException | IllegalStateException ex) {
            // An unreachable Oracle or a restricted dictionary is UNKNOWN, not an empty CLT schema.
            dbName = null;
        }

        List<ReferenceRow> references = new ArrayList<>(17);
        for (String table : CollateralOracleReconciliationPolicy.REFERENCE_TABLES) {
            String visibility = CollateralOracleReconciliationPolicy.visibility(metadataReady, visible, table);
            if (!"VISIBLE".equals(visibility) || System.nanoTime() >= deadlineNanos) {
                references.add(CollateralOracleReconciliationPolicy.unknownReference(table, visibility));
                continue;
            }
            try {
                // Table comes exclusively from a compile-time allowlist, never an API parameter.
                Map<String,Object> row = jdbc.queryForMap("SELECT COUNT(*) TOTAL_ROWS, "
                        + "COALESCE(SUM(CASE WHEN STATUS='ACTIVE' THEN 1 ELSE 0 END),0) ACTIVE_ROWS "
                        + "FROM CLT." + table);
                references.add(CollateralOracleReconciliationPolicy.observedReference(table,
                        count(row.get("TOTAL_ROWS")), count(row.get("ACTIVE_ROWS"))));
            } catch (DataAccessException | IllegalArgumentException ex) {
                references.add(CollateralOracleReconciliationPolicy.unknownReference(table, "VISIBLE"));
            }
        }

        Long branchCount = null;
        if (visible.contains("COLLATERAL") && System.nanoTime() < deadlineNanos) {
            try {
                // Branch is server-derived from a verified principal; never accept caller branch input.
                branchCount = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM CLT.COLLATERAL WHERE BRANCH_CODE = ?", Long.class, branch);
            } catch (DataAccessException ex) { /* UNKNOWN */ }
        }
        Integer seqCount = null;
        if (metadataReady && System.nanoTime() < deadlineNanos) {
            try {
                seqCount = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT'", Integer.class);
            } catch (DataAccessException ex) { /* UNKNOWN */ }
        }

        List<CheckRow> checks = new ArrayList<>();
        checks.add(CollateralOracleReconciliationPolicy.binary("CLT_CORE_REGISTRY_VISIBLE",
                !metadataReady ? null : visible.contains("COLLATERAL") ? true : null));
        checks.add(CollateralOracleReconciliationPolicy.binary("CLT_TYPE_AND_GROUP_VISIBLE",
                !metadataReady ? null : visible.contains("COLLATERAL_TYPE")
                        && visible.contains("COLLATERAL_GROUP") ? true : null));
        checks.add(CollateralOracleReconciliationPolicy.binary("G17_NUMBER19_PHYSICAL", System.nanoTime() < deadlineNanos ? number19() : null));
        checks.add(CollateralOracleReconciliationPolicy.binary("G17_NUMBER_UNIQUE", System.nanoTime() < deadlineNanos ? uniqueNumber() : null));
        checks.add(CollateralOracleReconciliationPolicy.binary("G03_G12_SERIAL_RANGE_VISIBLE",
                !metadataReady ? null : visible.contains("CLT_COLLATERAL_SERIAL_RANGE") ? true : null));
        checks.add(CollateralOracleReconciliationPolicy.binary("G14_ASSET_IDENTITY_VISIBLE",
                !metadataReady ? null : visible.contains("CLT_ASSET_IDENTITY")
                        && visible.contains("CLT_ASSET_COLLATERAL_LINK") ? true : null));
        checks.add(System.nanoTime() < deadlineNanos ? statusDefault(visible)
                : CollateralOracleReconciliationPolicy.transactionDefault(null, null, null));
        checks.add(CollateralOracleReconciliationPolicy.binary("BUC03_BRANCH_READBACK",
                branchCount == null ? null : branchCount > 0));
        checks.add(new CheckRow("BANK_POLICY_AND_ORACLE_WRITE_UAT", "BLOCKED", "APPROVAL_AND_E2E_NOT_ATTESTED"));
        List<String> blockers = List.of("G-03/G-12: serial integrity and approved persistence",
                "G-14: asset identity and history approval", "G-17: numbering format/uniqueness policy",
                "G-16: status default/reference reconciliation", "BUC-03/13: bank-authorized Oracle write/readback UAT");
        return new Report("CLT-ORACLE-RECONCILIATION-1", Instant.now().toString(), dbName,
                branch, dictionaryState, "SHARED_APPLICATION_DATASOURCE", false,
                branchCount, seqCount, List.copyOf(references), List.copyOf(checks), blockers);
    }

    private static long count(Object value) {
        if (!(value instanceof Number n)) throw new IllegalArgumentException("CLT_COUNT_INVALID");
        return n.longValue();
    }

    private Boolean number19() {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList("""
                    SELECT DATA_TYPE, DATA_PRECISION, DATA_SCALE FROM ALL_TAB_COLUMNS
                     WHERE OWNER='CLT' AND TABLE_NAME='COLLATERAL'
                       AND COLUMN_NAME='COLLATERAL_NUMBER'
                    """);
            if (rows.size() != 1) return null;
            Map<String,Object> c = rows.getFirst();
            return "NUMBER".equals(c.get("DATA_TYPE"))
                    && new BigDecimal("19").compareTo(new BigDecimal(c.get("DATA_PRECISION").toString())) == 0
                    && new BigDecimal("0").compareTo(new BigDecimal(c.get("DATA_SCALE").toString())) == 0;
        } catch (DataAccessException | NullPointerException | NumberFormatException ex) {
            return null;
        }
    }

    private Boolean uniqueNumber() {
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS c JOIN ALL_CONS_COLUMNS cc
                      ON c.OWNER=cc.OWNER AND c.CONSTRAINT_NAME=cc.CONSTRAINT_NAME
                     WHERE c.OWNER='CLT' AND c.TABLE_NAME='COLLATERAL'
                       AND c.CONSTRAINT_TYPE IN ('U','P') AND c.STATUS='ENABLED'
                       AND cc.COLUMN_NAME='COLLATERAL_NUMBER'
                       AND (SELECT COUNT(*) FROM ALL_CONS_COLUMNS x
                             WHERE x.OWNER=c.OWNER AND x.CONSTRAINT_NAME=c.CONSTRAINT_NAME)=1
                    """, Integer.class);
            return count == null ? null : count > 0;
        } catch (DataAccessException ex) {return null;}
    }

    private CheckRow statusDefault(Set<String> visible) {
        if (!visible.contains("COLLATERAL_TRANSACTION") || !visible.contains("REF_COLL_TXN_STATUS"))
            return CollateralOracleReconciliationPolicy.transactionDefault(null, null, null);
        try {
            List<String> defaults = jdbc.query("""
                    SELECT DATA_DEFAULT FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                     AND TABLE_NAME='COLLATERAL_TRANSACTION' AND COLUMN_NAME='TRANSACTION_STATUS_CODE'
                    """, (ResultSet rs, int i) -> rs.getString(1));
            String code = defaults.size() == 1
                    ? CollateralOracleReconciliationPolicy.canonicalDefault(defaults.getFirst()) : null;
            Map<String,Object> rows = jdbc.queryForMap("""
                    SELECT COALESCE(SUM(CASE WHEN TRANSACTION_STATUS_CODE='PENDING' AND STATUS='ACTIVE'
                        THEN 1 ELSE 0 END),0) PENDING_COUNT,
                        COALESCE(SUM(CASE WHEN TRANSACTION_STATUS_CODE='PENDING_APPROVAL' AND STATUS='ACTIVE'
                        THEN 1 ELSE 0 END),0) PENDING_APPROVAL_COUNT
                      FROM CLT.REF_COLL_TXN_STATUS
                    """);
            return CollateralOracleReconciliationPolicy.transactionDefault(
                    count(rows.get("PENDING_COUNT")), count(rows.get("PENDING_APPROVAL_COUNT")), code);
        } catch (DataAccessException | IllegalArgumentException ex) {
            return CollateralOracleReconciliationPolicy.transactionDefault(null, null, null);
        }
    }
}
