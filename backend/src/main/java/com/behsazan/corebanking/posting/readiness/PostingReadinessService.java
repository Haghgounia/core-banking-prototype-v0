package com.behsazan.corebanking.posting.readiness;

import com.behsazan.corebanking.posting.readiness.PostingReadinessModels.Finding;
import com.behsazan.corebanking.posting.readiness.PostingReadinessModels.ReadinessReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * R1-D1: Oracle dictionary + small configuration counts only. No DML, DDL,
 * transaction commit, position update or accounting side effect exists here.
 * Explicitly NOT a posting adapter and never allows financial posting.
 */
@Service
@ConditionalOnProperty(prefix = "core-banking.deal-posting.readiness", name = "enabled", havingValue = "true")
public class PostingReadinessService {
    static final String TABLE_SQL = "SELECT OWNER || '.' || TABLE_NAME FROM ALL_TABLES WHERE OWNER IN ('PST','LGR','ACC','RCN')";
    static final String SEQUENCE_SQL = "SELECT SEQUENCE_OWNER || '.' || SEQUENCE_NAME FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER IN ('PST','ACC')";
    static final String INDEX_SQL = "SELECT OWNER, INDEX_NAME, UNIQUENESS, STATUS, INDEX_TYPE FROM ALL_INDEXES WHERE OWNER IN ('PST','ACC')";
    static final String CONSTRAINT_SQL = "SELECT OWNER, CONSTRAINT_NAME, STATUS, VALIDATED FROM ALL_CONSTRAINTS WHERE OWNER IN ('PST','ACC')";

    private final NamedParameterJdbcTemplate jdbc;

    public PostingReadinessService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ReadinessReport inspect() {
        List<Finding> findings = new ArrayList<>();
        Map<String, Long> configurationCounts = new LinkedHashMap<>();
        int visibleTables = 0, visibleSequences = 0, validIndexes = 0, validConstraints = 0;
        Set<String> knownTables;

        try {
            knownTables = new LinkedHashSet<>(jdbc.query(TABLE_SQL, Map.of(), (rs, n) -> rs.getString(1)));
        } catch (DataAccessException exception) {
            findings.add(new Finding("ORACLE", "ALL_TABLES", "UNAVAILABLE", "Dictionary query failed or was not authorized"));
            return report("METADATA_UNAVAILABLE", 0, 0, 0, 0, configurationCounts, findings);
        }

        for (String name : PostingReadinessCatalog.EXPECTED_TABLES) {
            if (knownTables.contains(name)) {
                visibleTables++;
            } else {
                findings.add(new Finding("TABLE", name, "MISSING_OR_NOT_VISIBLE", "Expected model object not visible"));
            }
        }

        try {
            Set<String> found = new LinkedHashSet<>(jdbc.query(SEQUENCE_SQL, Map.of(), (rs, n) -> rs.getString(1)));
            for (String name : PostingReadinessCatalog.NEW_SEQUENCES) {
                if (found.contains(name)) visibleSequences++;
                else findings.add(new Finding("SEQUENCE", name, "MISSING_OR_NOT_VISIBLE", "R1-B2 sequence is missing or hidden"));
            }
        } catch (DataAccessException exception) {
            findings.add(new Finding("SEQUENCE", "ALL_SEQUENCES", "UNAVAILABLE", "Dictionary query failed or was not authorized"));
        }

        try {
            Map<String, IndexState> found = new LinkedHashMap<>();
            jdbc.query(INDEX_SQL, Map.of(), (rs, n) -> new IndexState(
                    rs.getString(1) + "." + rs.getString(2),
                    rs.getString(3), rs.getString(4), rs.getString(5)
            )).forEach(index -> found.put(index.name(), index));
            for (String name : PostingReadinessCatalog.NEW_INDEXES) {
                IndexState index = found.get(name);
                boolean isUnique = name.contains(".UNIQUE_");
                if (index != null && "VALID".equals(index.status())
                        && (!isUnique || ("UNIQUE".equals(index.uniqueness())
                        && "FUNCTION-BASED NORMAL".equals(index.type())))) {
                    validIndexes++;
                } else {
                    findings.add(new Finding("INDEX", name, "MISSING_OR_INVALID", "Required lookup or function-based unique index is not valid"));
                }
            }
        } catch (DataAccessException exception) {
            findings.add(new Finding("INDEX", "ALL_INDEXES", "UNAVAILABLE", "Dictionary query failed or was not authorized"));
        }

        try {
            Map<String, ConstraintState> found = new LinkedHashMap<>();
            jdbc.query(CONSTRAINT_SQL, Map.of(), (rs, n) -> new ConstraintState(
                    rs.getString(1) + "." + rs.getString(2),
                    rs.getString(3), rs.getString(4)
            )).forEach(constraint -> found.put(constraint.name(), constraint));
            for (String name : PostingReadinessCatalog.NEW_CONSTRAINTS) {
                ConstraintState constraint = found.get(name);
                if (constraint != null && "ENABLED".equals(constraint.status())
                        && "VALIDATED".equals(constraint.validated())) {
                    validConstraints++;
                } else {
                    findings.add(new Finding("CONSTRAINT", name, "MISSING_OR_INVALID", "Required R1-B2 constraint is not enabled and validated"));
                }
            }
        } catch (DataAccessException exception) {
            findings.add(new Finding("CONSTRAINT", "ALL_CONSTRAINTS", "UNAVAILABLE", "Dictionary query failed or was not authorized"));
        }

        for (String table : PostingReadinessCatalog.CONFIGURATION_TABLES) {
            if (!knownTables.contains(table)) continue;
            // Only catalog-owned SQL identifier constants are interpolated here.
            if (!PostingReadinessCatalog.isWhitelistedTable(table)) {
                findings.add(new Finding("CONFIGURATION", table, "BLOCKED", "Table name is not whitelisted"));
                continue;
            }
            try {
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Map.of(), Long.class);
                if (count == null) {
                    findings.add(new Finding("CONFIGURATION", table, "UNKNOWN", "Count not available"));
                } else {
                    configurationCounts.put(table, count);
                    if (count == 0L) findings.add(new Finding("CONFIGURATION", table, "EMPTY", "No test configuration loaded"));
                }
            } catch (DataAccessException exception) {
                findings.add(new Finding("CONFIGURATION", table, "NO_SELECT_PRIVILEGE_OR_QUERY_FAILURE", "Cannot read configuration row count"));
            }
        }

        return report("READ_ONLY_INSPECTION_COMPLETED", visibleTables, visibleSequences,
                validIndexes, validConstraints, configurationCounts, findings);
    }

    private static ReadinessReport report(String status, int tables, int sequences,
                                          int indexes, int constraints,
                                          Map<String, Long> counts, List<Finding> findings) {
        return new ReadinessReport(
                "R1-D1", status, true, false,
                PostingReadinessCatalog.EXPECTED_TABLES.size(), tables,
                PostingReadinessCatalog.NEW_SEQUENCES.size(), sequences,
                PostingReadinessCatalog.NEW_INDEXES.size(), indexes,
                PostingReadinessCatalog.NEW_CONSTRAINTS.size(), constraints,
                Map.copyOf(counts), List.copyOf(findings),
                "BLOCKED: source-of-truth selection, money scale, GL period, approval, "
                        + "durable outbox and integration tests are not signed off. "
                        + "Existing DPS2 transaction posting remains unchanged.",
                Instant.now()
        );
    }

    private record IndexState(String name, String uniqueness, String status, String type) {}
    private record ConstraintState(String name, String status, String validated) {}
}
