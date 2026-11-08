package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** All SQL names are taken from the immutable, validated CLT allowlist, never from raw request text. */
@Repository
public class CollateralReferenceRepository {
    private final CollateralReferenceCatalog catalog;
    private final ObjectProvider<CollateralReferenceReaderPool> readerPool;
    public CollateralReferenceRepository(CollateralReferenceCatalog catalog, ObjectProvider<CollateralReferenceReaderPool> readerPool) {
        this.catalog = catalog;
        this.readerPool = readerPool;
    }
    private JdbcTemplate db() { return readerPool.getObject().jdbc(); }

    public Rows list(String tableName, String search, int page, int size) {
        return list(tableName, search, page, size, null, null, null, null, null);
    }

    /** CLT UI R3: pagination, status and sorting are enforced in Oracle for the entire result set. */
    public Rows list(String tableName, String search, int page, int size, String status,
                     String sort, String direction, String filterField, String filterValue) {
        Table table = catalog.require(tableName);
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_PAGE");
        String term = search == null ? "" : search.strip();
        if (term.length() > 80 || term.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_SEARCH");
        List<String> predicates = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (!term.isEmpty()) {
            LinkedHashSet<String> textFields = new LinkedHashSet<>();
            if (!table.codeColumn().isBlank()) textFields.add(table.codeColumn());
            if (!table.nameColumn().isBlank()) textFields.add(table.nameColumn());
            if (textFields.isEmpty()) throw new IllegalArgumentException("CLT_REFERENCE_SEARCH_NOT_SUPPORTED");
            List<String> or = new ArrayList<>();
            for (String col : textFields) {
                or.add("UPPER(" + col + ") LIKE ? ESCAPE '!'");
                args.add("%" + term.toUpperCase(Locale.ROOT).replace("!", "!!")
                        .replace("%", "!%").replace("_", "!_") + "%");
            }
            predicates.add("(" + String.join(" OR ", or) + ")");
        }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            if (!List.of("ACTIVE", "INACTIVE").contains(status) || table.columns().stream().noneMatch(c -> "STATUS".equals(c.name())))
                throw new IllegalArgumentException("CLT_REFERENCE_INVALID_STATUS_FILTER");
            predicates.add("STATUS = ?");
            args.add(status);
        }
        if (filterField != null && !filterField.isBlank()) {
            if (!"COLLATERAL_TYPE_ATTRIBUTE".equals(table.table()) ||
                !List.of("COLLATERAL_TYPE_ID", "COLLATERAL_ATTRIBUTE_DEFINITION_ID").contains(filterField) ||
                filterValue == null || !filterValue.matches("[1-9][0-9]{0,18}"))
                throw new IllegalArgumentException("CLT_REFERENCE_INVALID_RELATION_FILTER");
            predicates.add(filterField + " = ?");
            args.add(new BigDecimal(filterValue));
        } else if (filterValue != null && !filterValue.isBlank()) {
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_RELATION_FILTER");
        }
        String sortColumn = sort == null || sort.isBlank() ? table.primaryKey() : sort.toUpperCase(Locale.ROOT);
        if (table.columns().stream().noneMatch(c -> c.name().equals(sortColumn)))
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_SORT");
        String sortDirection = direction == null || direction.isBlank() ? "DESC" : direction.toUpperCase(Locale.ROOT);
        if (!List.of("ASC", "DESC").contains(sortDirection))
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_DIRECTION");
        String where = predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);
        String source = "CLT." + table.table();
        JdbcTemplate jdbc = db();
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + source + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((long) page * size);
        pageArgs.add(size);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT " + projection(table) + " FROM " + source + where +
                " ORDER BY " + sortColumn + " " + sortDirection + ", " + table.primaryKey() + " DESC" +
                " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", pageArgs.toArray())
                .stream().map(CollateralReferenceRepository::normalized).toList();
        return new Rows(rows, total == null ? 0 : total, page, size, "ORACLE_CLT_READ_ONLY");
    }

    public Map<String, Object> find(String tableName, String primaryKey) {
        Table table = catalog.require(tableName);
        if (primaryKey == null || !primaryKey.matches("[0-9]{1,19}"))
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_ID");
        List<Map<String, Object>> rows = db().queryForList("SELECT " + projection(table) + " FROM CLT."
                + table.table() + " WHERE " + table.primaryKey() + " = ?",
                new BigDecimal(primaryKey));
        if (rows.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "CLT_REFERENCE_ROW_NOT_FOUND");
        return normalized(rows.getFirst());
    }

    /**
     * CLT-RF1: check COUNT plus compiled, allowlisted column projection for every
     * reference table. A table with zero rows is READABLE, not an error. Unavailable
     * tables do not conceal other results; no Oracle details are exposed in the API.
     * This method never writes, changes schema, or supplies a data/permission fallback.
     */
    public ReferenceReadMatrix readinessMatrix() {
        JdbcTemplate jdbc = db();
        List<TableReadProbe> probes = new ArrayList<>();
        for (String name : CollateralReferenceCatalog.TABLES.stream().sorted().toList()) {
            Table table = catalog.require(name);
            try {
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM CLT." + name, Long.class);
                // Parse the full physical projection even for an empty reference table.
                jdbc.queryForList("SELECT " + projection(table) + " FROM CLT." + name
                        + " WHERE ROWNUM <= 1");
                probes.add(new TableReadProbe(name, count == null ? 0L : count,
                        "READABLE", "NONE"));
            } catch (org.springframework.dao.DataAccessException e) {
                probes.add(new TableReadProbe(name, null, "UNAVAILABLE",
                        "CLT_REFERENCE_TABLE_READ_UNAVAILABLE"));
            }
        }
        int readable = (int) probes.stream().filter(p -> "READABLE".equals(p.state())).count();
        return new ReferenceReadMatrix(CollateralReferenceCatalog.TABLES.size(), readable,
                probes.size() - readable, List.copyOf(probes), "ORACLE_CLT_SELECT_ONLY", true);
    }

    public Readiness readiness() {
        List<String> actual = db().queryForList(
                "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = 'CLT' AND TABLE_NAME IN ("
                + String.join(",", Collections.nCopies(CollateralReferenceCatalog.TABLES.size(), "?")) + ")",
                String.class, CollateralReferenceCatalog.TABLES.toArray());
        List<String> missing = CollateralReferenceCatalog.TABLES.stream().filter(x -> !actual.contains(x)).sorted().toList();
        return new Readiness(missing.isEmpty(), CollateralReferenceCatalog.TABLES.size(), actual.size(), missing,
                "ALL_TABLES_READ_ONLY");
    }

    static String projection(Table table) {
        return table.columns().stream().map(Column::name).reduce((a, b) -> a + "," + b).orElseThrow();
    }
    static Map<String, Object> normalized(Map<String, Object> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((name, value) -> {
            Object v = value;
            if (v instanceof BigDecimal bd) v = bd.toPlainString();
            else if (v instanceof Timestamp ts) v = ts.toLocalDateTime().toString();
            else if (v instanceof java.sql.Date date) v = date.toLocalDate().toString();
            else if (v instanceof TemporalAccessor) v = v.toString();
            result.put(name, v);
        });
        return result;
    }
}
