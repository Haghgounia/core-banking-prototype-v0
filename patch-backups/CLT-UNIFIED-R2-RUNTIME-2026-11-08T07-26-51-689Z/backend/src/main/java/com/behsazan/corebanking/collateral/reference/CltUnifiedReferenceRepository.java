package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Physical Oracle CRUD with the one application DataSource. No schema changes or hidden fallback. */
@Repository
public final class CltUnifiedReferenceRepository {
    private final CollateralReferenceCatalog catalog;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private static final String ACTOR = "CLT_UNIFIED_LOCAL_DEV";
    public CltUnifiedReferenceRepository(CollateralReferenceCatalog catalog, DataSource dataSource) {
        this.catalog = catalog;
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(15);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.transaction.setTimeout(20);
    }

    public Map<String, Object> create(String tableName, Map<String, Object> values) {
        Table table = catalog.require(tableName);
        Map<String, Object> safe = CltUnifiedReferenceValidation.normalized(table, values, true);
        return transaction.execute(status -> {
            BigDecimal id = jdbc.queryForObject("SELECT CLT.SEQ_" + table.table() + ".NEXTVAL FROM DUAL", BigDecimal.class);
            if (id == null || id.precision() > 19) throw new IllegalStateException("CLT_UNIFIED_SEQUENCE_INVALID");
            List<Object> args = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            List<String> expressions = new ArrayList<>();
            columns.add(table.primaryKey()); expressions.add("?"); args.add(id);
            safe.forEach((column, value) -> { columns.add(column); expressions.add("?"); args.add(value); });
            columns.add("CREATED_AT"); expressions.add("SYSTIMESTAMP");
            columns.add("CREATED_BY"); expressions.add("?"); args.add(ACTOR);
            columns.add("RECORD_VERSION"); expressions.add("1");
            jdbc.update("INSERT INTO CLT." + table.table() + " (" + String.join(",", columns)
                    + ") VALUES (" + String.join(",", expressions) + ")", args.toArray());
            return find(table, id);
        });
    }

    public Map<String, Object> update(String tableName, String key, long expectedVersion, Map<String, Object> values) {
        Table table = catalog.require(tableName);
        BigDecimal id = CltUnifiedReferenceValidation.id(key);
        long version = CltUnifiedReferenceValidation.version(expectedVersion);
        Map<String, Object> safe = CltUnifiedReferenceValidation.normalized(table, values, false);
        return transaction.execute(status -> {
            List<String> setters = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            safe.forEach((name, value) -> { setters.add(name + " = ?"); args.add(value); });
            setters.add("UPDATED_AT = SYSTIMESTAMP");
            setters.add("UPDATED_BY = ?"); args.add(ACTOR);
            setters.add("RECORD_VERSION = RECORD_VERSION + 1");
            args.add(id); args.add(version);
            int changed = jdbc.update("UPDATE CLT." + table.table() + " SET " + String.join(",", setters)
                    + " WHERE " + table.primaryKey() + " = ? AND RECORD_VERSION = ?", args.toArray());
            if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_UNIFIED_VERSION_OR_ROW_CONFLICT");
            return find(table, id);
        });
    }

    public void delete(String tableName, String key, long expectedVersion) {
        Table table = catalog.require(tableName);
        BigDecimal id = CltUnifiedReferenceValidation.id(key);
        long version = CltUnifiedReferenceValidation.version(expectedVersion);
        transaction.executeWithoutResult(status -> {
            int changed = jdbc.update("DELETE FROM CLT." + table.table() + " WHERE " + table.primaryKey()
                    + " = ? AND RECORD_VERSION = ?", id, version);
            if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_UNIFIED_VERSION_OR_ROW_CONFLICT");
        });
    }

    private Map<String, Object> find(Table table, BigDecimal key) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + CollateralReferenceRepository.projection(table)
                + " FROM CLT." + table.table() + " WHERE " + table.primaryKey() + " = ?", key);
        if (rows.isEmpty()) throw new IllegalStateException("CLT_UNIFIED_WRITE_NOT_VISIBLE");
        return CollateralReferenceRepository.normalized(rows.getFirst());
    }
}
