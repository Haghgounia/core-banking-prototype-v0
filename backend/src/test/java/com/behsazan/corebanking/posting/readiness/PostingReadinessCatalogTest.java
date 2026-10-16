package com.behsazan.corebanking.posting.readiness;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PostingReadinessCatalogTest {
    @Test void catalogContainsFiftyUniquePluralTables() {
        assertEquals(50, PostingReadinessCatalog.EXPECTED_TABLES.size());
        assertEquals(50, Set.copyOf(PostingReadinessCatalog.EXPECTED_TABLES).size());
        for (String name : PostingReadinessCatalog.EXPECTED_TABLES) {
            assertTrue(name.matches("(PST|LGR|ACC|RCN)\\.[A-Z][A-Z0-9_]*S"), name);
            assertTrue(PostingReadinessCatalog.isWhitelistedTable(name));
        }
    }

    @Test void newOracleObjectsAreExplicitlyTracked() {
        assertTrue(PostingReadinessCatalog.EXPECTED_TABLES.contains("PST.POSTING_RULE_TEMPLATE_ASSIGNMENTS"));
        assertTrue(PostingReadinessCatalog.EXPECTED_TABLES.contains("ACC.ACCOUNT_ROLE_ASSIGNMENTS"));
        assertEquals(2, PostingReadinessCatalog.NEW_SEQUENCES.size());
        assertEquals(4, PostingReadinessCatalog.NEW_INDEXES.size());
        assertEquals(9, PostingReadinessCatalog.NEW_CONSTRAINTS.size());
        assertEquals(13, PostingReadinessCatalog.CONFIGURATION_TABLES.size());
        assertTrue(PostingReadinessCatalog.EXPECTED_TABLES.containsAll(PostingReadinessCatalog.CONFIGURATION_TABLES));
        assertFalse(PostingReadinessCatalog.isWhitelistedTable("DPS2.DEPOSIT_ACCOUNT"));
    }
}
