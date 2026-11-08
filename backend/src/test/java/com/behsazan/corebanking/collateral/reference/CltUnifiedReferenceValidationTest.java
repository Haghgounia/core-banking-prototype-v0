package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Column;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Offline bounded test for the reference CRUD validation contract; no Oracle writes. */
class CltUnifiedReferenceValidationTest {
    private static final Table TABLE = new Table("REF_CURRENCY", "Currency", "", "operation",
            "REF_CURRENCY_ID", "CURRENCY_CODE", "CURRENCY_NAME", List.of(
                new Column("REF_CURRENCY_ID", "ID", "NUMBER(19,0)", true, true),
                new Column("CURRENCY_CODE", "Code", "VARCHAR2(3 CHAR)", true, false),
                new Column("CURRENCY_NAME", "Name", "VARCHAR2(100 CHAR)", true, false),
                new Column("DISPLAY_ORDER", "Order", "NUMBER(5,0)", false, false),
                new Column("CREATED_AT", "Created at", "TIMESTAMP(6)", true, false),
                new Column("CREATED_BY", "Created by", "VARCHAR2(100 CHAR)", true, false),
                new Column("RECORD_VERSION", "Version", "NUMBER(10,0)", true, false)
            ), 1);

    @Test void createAllRequiredFieldsPresent() {
        Map<String, Object> valid = CltUnifiedReferenceValidation.normalized(TABLE,
                Map.of("CURRENCY_CODE", "IRR", "CURRENCY_NAME", "Rial", "DISPLAY_ORDER", "12"), true);
        assertEquals(new BigDecimal("12"), valid.get("DISPLAY_ORDER"));
    }
    @Test void refusesMissingRequiredField() {
        assertThrows(IllegalArgumentException.class,
                () -> CltUnifiedReferenceValidation.normalized(TABLE, Map.of("CURRENCY_CODE", "IRR"), true));
    }
    @Test void rejectsUnknownOrManagedColumns() {
        assertThrows(IllegalArgumentException.class, () -> CltUnifiedReferenceValidation.normalized(TABLE,
                Map.of("RECORD_VERSION", 999), false));
        assertThrows(IllegalArgumentException.class, () -> CltUnifiedReferenceValidation.normalized(TABLE,
                Map.of("HACK_COLUMN", "test"), false));
    }
    @Test void refusesOverflowAndBadVersion() {
        assertThrows(IllegalArgumentException.class, () -> CltUnifiedReferenceValidation.normalized(TABLE,
                Map.of("CURRENCY_CODE", "TOO-LONG"), false));
        assertThrows(IllegalArgumentException.class, () -> CltUnifiedReferenceValidation.normalized(TABLE,
                Map.of("DISPLAY_ORDER", "12345678"), false));
        assertThrows(IllegalArgumentException.class, () -> CltUnifiedReferenceValidation.version(0));
    }
    @Test void exactNumericIdRequired() {
        assertEquals(new BigDecimal("123"), CltUnifiedReferenceValidation.id("123"));
        assertThrows(IllegalArgumentException.class, () -> CltUnifiedReferenceValidation.id("1 OR 1=1"));
    }
}
