package com.behsazan.corebanking.organization.location.manage;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrganizationUnitLocationBrowseServiceTest {
    @Test
    void operationalBrowseSqlUsesCanonicalLocationRowsOnly() {
        for (boolean managed: new boolean[]{false,true}) {
            String sql=OrganizationUnitLocationBrowseService.canonicalCte("ORG",managed);
            assertTrue(sql.contains("FROM ORG.ORGANIZATION_UNIT_LOCATIONS UL"));
            assertTrue(sql.contains("JOIN ORG.ORGANIZATION_UNITS OU"));
            assertFalse(sql.contains("JOIN ORG.LOCATIONS "));
            assertFalse(sql.contains("POSTAL_ADDRESSES P"));
            assertFalse(sql.contains("FROM ORG.LOCATIONS"));
            assertFalse(sql.contains("FROM ORG.POSTAL_ADDRESSES"));
        }
    }

    @Test
    void rejectsUntrustedSchemaIdentifier() {
        NamedParameterJdbcTemplate jdbc=mock(NamedParameterJdbcTemplate.class);
        assertThrows(IllegalArgumentException.class,()->new OrganizationUnitLocationBrowseService(jdbc,"ORG;DROP"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void distinguishesR1OnlyAndManagedColumnProjections() {
        String initial=OrganizationUnitLocationBrowseService.canonicalCte("ORG",false);
        String after=OrganizationUnitLocationBrowseService.canonicalCte("ORG",true);
        assertTrue(initial.contains("CAST(NULL AS VARCHAR2(20)) AS UNIT_PRESENCE_CODE"));
        assertTrue(after.contains("UL.CANONICAL_MANAGED_FLAG"));
        assertTrue(after.contains("UL.UNIT_PRESENCE_CODE"));
    }
}
