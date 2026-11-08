package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Column;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Every real 17-table descriptor must be accepted by the typed CRUD validator.
 * This is an offline contract test. It deliberately does not write to Oracle.
 */
class CltUnifiedReferenceAllTablesContractTest {
    private final CollateralReferenceCatalog catalog = new CollateralReferenceCatalog(new JsonMapper());

    @Test void createAndPartialUpdateAreTypeSafeOnAll17PhysicalTables() {
        assertEquals(17, catalog.all().tables().size());
        for (Table table : catalog.all().tables()) {
            Map<String, Object> input = new LinkedHashMap<>();
            for (Column column : table.columns()) {
                if (column.primaryKey() || managed(column.name())) continue;
                input.put(column.name(), example(column));
            }
            Map<String, Object> normalized = CltUnifiedReferenceValidation.normalized(table, input, true);
            assertEquals(input.size(), normalized.size(), table.table());
            assertFalse(normalized.containsKey(table.primaryKey()), table.table());
            assertFalse(normalized.containsKey("RECORD_VERSION"), table.table());
            assertEquals(Map.of("STATUS", "INACTIVE"),
                    CltUnifiedReferenceValidation.normalized(table, Map.of("STATUS", "INACTIVE"), false),
                    table.table());
            assertThrows(IllegalArgumentException.class, () ->
                    CltUnifiedReferenceValidation.normalized(table, Map.of("STATUS", "UNAPPROVED"), false),
                    table.table());
            assertThrows(IllegalArgumentException.class, () ->
                    CltUnifiedReferenceValidation.normalized(table, Map.of(table.primaryKey(), "1"), false),
                    table.table());
        }
    }

    @Test void optionalNullIsAllowedButMandatoryNullIsRejected() {
        Table currency = catalog.require("REF_CURRENCY");
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("CURRENCY_NUMERIC_CODE", null);
        assertTrue(CltUnifiedReferenceValidation.normalized(currency, change, false)
                .containsKey("CURRENCY_NUMERIC_CODE"));
        change.clear();
        change.put("CURRENCY_CODE", null);
        assertThrows(IllegalArgumentException.class,
                () -> CltUnifiedReferenceValidation.normalized(currency, change, false));
    }

    @Test void flagsRejectUngovernedValues() {
        Table table = catalog.require("REF_COLL_TXN_STATUS");
        assertEquals("N", CltUnifiedReferenceValidation.normalized(table,
                Map.of("IS_FINAL_FLAG", "N"), false).get("IS_FINAL_FLAG"));
        assertThrows(IllegalArgumentException.class, () ->
                CltUnifiedReferenceValidation.normalized(table,
                        Map.of("IS_FINAL_FLAG", "YES"), false));
    }

    private static boolean managed(String column) {
        return java.util.Set.of("CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "RECORD_VERSION")
                .contains(column);
    }
    private static String example(Column column) {
        if ("STATUS".equals(column.name())) return "ACTIVE";
        if (column.dataType().startsWith("CHAR(1")) return "Y";
        if (column.dataType().startsWith("NUMBER(")) return "1";
        return "T";
    }
}
