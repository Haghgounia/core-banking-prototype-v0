package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** The catalog is an explicit SQL allowlist, not caller-supplied schema/table/column names. */
@Component
public final class CollateralReferenceCatalog {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]*");
    public static final Set<String> TABLES = Set.of(
            "COLLATERAL_TYPE_ATTRIBUTE", "REF_COLLATERAL_ATTRIBUTE_DATA_TYPE", "REF_COLL_TXN_STATUS",
            "REF_CURRENCY", "REF_VALUATION_TYPE", "REF_COLL_REVERSAL_REASON",
            "COLLATERAL_ATTRIBUTE_DEFINITION", "COLLATERAL_TYPE", "COLLATERAL_GROUP",
            "REF_SOURCE_SYSTEM", "REF_COLLATERAL_STATUS", "COLLATERAL_INSURANCE_TYPE",
            "REF_CHANNEL", "INSURANCE_COMPANY", "REF_VALUATION_METHOD",
            "REF_VALUATION_STATUS", "REF_COLL_TXN_TYPE");
    private final Catalog catalog;
    private final Map<String, Table> byTable;

    public CollateralReferenceCatalog(JsonMapper mapper) {
        try (InputStream input = new ClassPathResource("clt/reference-catalog.json").getInputStream()) {
            catalog = mapper.readValue(input, Catalog.class);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("CLT_REFERENCE_REFERENCE_CATALOG_UNAVAILABLE", e);
        }
        if (!"CLT".equals(catalog.schema())
                || catalog.totalTables() != 17 || catalog.tables() == null || catalog.tables().size() != 17
                || catalog.technicalSeedPreviewCount() != 53) {
            throw new IllegalStateException("CLT_REFERENCE_REFERENCE_CATALOG_INVALID");
        }
        Map<String, Table> found = new LinkedHashMap<>();
        for (Table table : catalog.tables()) {
            if (!TABLES.contains(table.table()) || found.putIfAbsent(table.table(), table) != null
                    || table.columns() == null || table.columns().isEmpty()) {
                throw new IllegalStateException("CLT_REFERENCE_TABLE_ALLOWLIST_INVALID");
            }
            Set<String> columns = new HashSet<>();
            for (Column c : table.columns()) {
                if (!identifier(c.name()) || !columns.add(c.name())) {
                    throw new IllegalStateException("CLT_REFERENCE_COLUMN_ALLOWLIST_INVALID");
                }
            }
            if (!columns.contains(table.primaryKey()) ||
                    (!table.codeColumn().isEmpty() && !columns.contains(table.codeColumn())) ||
                    (!table.nameColumn().isEmpty() && !columns.contains(table.nameColumn()))) {
                throw new IllegalStateException("CLT_REFERENCE_COLUMN_CONTRACT_INVALID");
            }
        }
        if (!found.keySet().equals(TABLES)) throw new IllegalStateException("CLT_REFERENCE_TABLE_SET_MISMATCH");
        byTable = Map.copyOf(found);
    }

    public Catalog all() { return catalog; }
    public Table require(String name) {
        Table table = byTable.get(name);
        if (table == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CLT_REFERENCE_UNKNOWN_TABLE");
        return table;
    }
    private static boolean identifier(String candidate) {
        return candidate != null && IDENTIFIER.matcher(candidate).matches();
    }
}
