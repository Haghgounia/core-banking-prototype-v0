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
public final class CollateralReferenceRepository {
    private final CollateralReferenceCatalog catalog;
    private final ObjectProvider<CollateralReferenceReaderPool> readerPool;
    public CollateralReferenceRepository(CollateralReferenceCatalog catalog, ObjectProvider<CollateralReferenceReaderPool> readerPool) {
        this.catalog = catalog;
        this.readerPool = readerPool;
    }
    private JdbcTemplate db() { return readerPool.getObject().jdbc(); }

    public Rows list(String tableName, String search, int page, int size) {
        Table table = catalog.require(tableName);
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_PAGE");
        String term = search == null ? "" : search.strip();
        if (term.length() > 80 || term.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("CLT_REFERENCE_INVALID_SEARCH");
        String columns = projection(table);
        String source = "CLT." + table.table();
        String filter = "";
        List<Object> args = new ArrayList<>();
        if (!term.isEmpty()) {
            LinkedHashSet<String> textFields = new LinkedHashSet<>();
            if (!table.codeColumn().isBlank()) textFields.add(table.codeColumn());
            if (!table.nameColumn().isBlank()) textFields.add(table.nameColumn());
            if (textFields.isEmpty()) throw new IllegalArgumentException("CLT_REFERENCE_SEARCH_NOT_SUPPORTED");
            StringJoiner clauses = new StringJoiner(" OR ", " WHERE (", ")");
            for (String col : textFields) {
                clauses.add("UPPER(" + col + ") LIKE ? ESCAPE '!'");
                args.add("%" + term.toUpperCase(Locale.ROOT).replace("!", "!!")
                        .replace("%", "!%").replace("_", "!_") + "%");
            }
            filter = clauses.toString();
        }
        JdbcTemplate jdbc = db();
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + source + filter, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((long) page * size);
        pageArgs.add(size);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + columns + " FROM " + source + filter
                + " ORDER BY " + table.primaryKey() + " DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                pageArgs.toArray()).stream().map(CollateralReferenceRepository::normalized).toList();
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
