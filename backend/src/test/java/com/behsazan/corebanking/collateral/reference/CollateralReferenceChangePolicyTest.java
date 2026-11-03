package com.behsazan.corebanking.collateral.reference;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralReferenceChangePolicyTest {
    private static final Map<String,String> GROUP_COLUMNS = Map.of(
            "COLLATERAL_GROUP_CODE", "VARCHAR2(5 CHAR)",
            "COLLATERAL_GROUP_NAME", "VARCHAR2(100 CHAR)",
            "STATUS", "VARCHAR2(10 CHAR)");
    private static final Set<String> GROUP_REQUIRED = Set.of(
            "COLLATERAL_GROUP_ID", "COLLATERAL_GROUP_CODE", "COLLATERAL_GROUP_NAME",
            "STATUS", "CREATED_AT", "CREATED_BY", "RECORD_VERSION");

    @Test void validBusinessReferenceCreateHasOnlyWhitelistedFields() {
        var result = CollateralReferenceChangePolicy.normalize("COLLATERAL_GROUP", "CREATE",
                "COLLATERAL_GROUP_ID", GROUP_COLUMNS, GROUP_REQUIRED,
                Map.of("COLLATERAL_GROUP_CODE", "003", "COLLATERAL_GROUP_NAME", "ملکی", "STATUS", "ACTIVE"));
        assertEquals("003", result.get("COLLATERAL_GROUP_CODE"));
        assertEquals(3, result.size());
    }
    @Test void technicalReferenceTablesCannotBeMutated() {
        assertThrows(IllegalArgumentException.class, () ->
                CollateralReferenceChangePolicy.normalize("REF_CURRENCY", "UPDATE",
                        "REF_CURRENCY_ID", GROUP_COLUMNS, GROUP_REQUIRED, Map.of("STATUS", "ACTIVE")));
    }
    @Test void primaryKeysSystemFieldsAndUnknownIdentifiersCannotBeChanged() {
        for (String col : Set.of("RECORD_VERSION", "CREATED_BY", "COLLATERAL_GROUP_ID", "STATUS;DROP_TABLE")) {
            assertThrows(IllegalArgumentException.class, () ->
                    CollateralReferenceChangePolicy.normalize("COLLATERAL_GROUP", "UPDATE",
                            "COLLATERAL_GROUP_ID", GROUP_COLUMNS, GROUP_REQUIRED, Map.of(col, "5")));
        }
    }
    @Test void requiredVersionAndMakerCheckerAreEnforced() {
        assertDoesNotThrow(() -> CollateralReferenceChangePolicy.requireTarget("UPDATE", "13", 2L));
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.requireTarget("UPDATE", "13", null));
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.requireTarget("CREATE", "1", null));
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.requireDistinctActors("user1", "user1"));
    }
    @Test void oversizedTextMissingRequiredAndControlCharactersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.normalize(
                "COLLATERAL_GROUP", "CREATE", "COLLATERAL_GROUP_ID", GROUP_COLUMNS, GROUP_REQUIRED,
                Map.of("COLLATERAL_GROUP_CODE", "123456", "COLLATERAL_GROUP_NAME", "نام", "STATUS", "ACTIVE")));
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.normalize(
                "COLLATERAL_GROUP", "CREATE", "COLLATERAL_GROUP_ID", GROUP_COLUMNS, GROUP_REQUIRED,
                Map.of("COLLATERAL_GROUP_CODE", "123", "STATUS", "ACTIVE")));
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.reason("\n"));
    }
    @Test void numericPrecisionAndDateValidationAreStrict() {
        var numberColumns = Map.of("RESERVE_RATIO", "NUMBER(3,0)", "VALID_FROM", "DATE");
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.normalize(
                "COLLATERAL_TYPE", "UPDATE", "COLLATERAL_TYPE_ID", numberColumns, Set.of(),
                Map.of("RESERVE_RATIO", "1234")));
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.normalize(
                "COLLATERAL_TYPE", "UPDATE", "COLLATERAL_TYPE_ID", numberColumns, Set.of(),
                Map.of("VALID_FROM", "1405-13-41")));
    }
    @Test void idempotencyKeyCannotBeWeak() {
        assertThrows(IllegalArgumentException.class, () -> CollateralReferenceChangePolicy.idempotencyKey("abc"));
        assertDoesNotThrow(() -> CollateralReferenceChangePolicy.idempotencyKey("clt-request-123456"));
    }
}
