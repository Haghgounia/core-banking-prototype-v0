package com.behsazan.corebanking.system.modelcomparison;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

class OracleEaMetadataInspectorDictionarySqlTest {

    @Test
    void virtualColumnMustComeFromAllTabColsNotAllTabColumns() {
        String main = OracleEaMetadataInspector.COLUMN_METADATA_SQL.toUpperCase();
        String stub = OracleEaMetadataInspector.REFERENCED_COLUMN_METADATA_SQL.toUpperCase();

        assertTrue(main.contains("LEFT JOIN ALL_TAB_COLS TC"));
        assertTrue(main.contains("TC.VIRTUAL_COLUMN"));
        assertFalse(hasBareAliasVirtualColumn(main));

        assertTrue(stub.contains("LEFT JOIN ALL_TAB_COLS TC"));
        assertTrue(stub.contains("TC.VIRTUAL_COLUMN"));
        assertFalse(hasBareAliasVirtualColumn(stub));

        assertTrue(hasBareAliasVirtualColumn("SELECT C.VIRTUAL_COLUMN FROM ALL_TAB_COLUMNS C"));
        assertFalse(hasBareAliasVirtualColumn("SELECT TC.VIRTUAL_COLUMN FROM ALL_TAB_COLS TC"));
    }

    private static boolean hasBareAliasVirtualColumn(String sql) {
        return Pattern.compile("(?<![A-Z0-9_])C\\.VIRTUAL_COLUMN\\b")
                .matcher(sql)
                .find();
    }
}
