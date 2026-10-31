package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.ContactInput;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrganizationContactR5V2TargetTest {
    @Test
    void canonicalReadinessRejectsIncompleteLegacyCoverage() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("COUNT(DISTINCT COLUMN_NAME)")) return 15L;
            if (sql.contains("WHERE NOT EXISTS") && sql.contains("ORG.ORGANIZATION_UNIT_CONTACT_POINTS a")) return 86L;
            if (sql.contains("SELECT COUNT(*) FROM ORG.SERVICE_POINT_CONTACT_POINTS")) return 0L;
            return 1L;
        });
        CanonicalOrganizationContactService service = new CanonicalOrganizationContactService(jdbc, true);
        assertFalse(service.readiness().ready());
        assertFalse(service.readiness().writesEnabled());
        verify(jdbc, atLeastOnce()).queryForObject(argThat(sql ->
                sql.contains("TABLE_NAME='CONTACT_POINTS_V2'")), anyMap(), eq(Long.class));
    }

    @Test
    void canonicalReadinessRemainsClosedIfServicePointContactsArePresent() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("COUNT(DISTINCT COLUMN_NAME)")) return 15L;
            if (sql.contains("SELECT COUNT(*) FROM ORG.SERVICE_POINT_CONTACT_POINTS")) return 2L;
            if (sql.contains("WHERE NOT EXISTS") || sql.contains("LEGACY_ASSOCIATION_ID IS NOT NULL")) return 0L;
            return 1L;
        });
        assertFalse(new CanonicalOrganizationContactService(jdbc, true).readiness().ready());
    }

    @Test
    void writesStayDisabledWithoutExplicitApproval() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        CanonicalOrganizationContactService service = new CanonicalOrganizationContactService(jdbc, false);
        var contact = new ContactInput(1L, "PHONE", "02112345678", 2L, true,
                null, null, null, null);
        assertEquals("ORG_CONTACT_WRITES_DISABLED", assertThrows(
                OrganizationContactException.class, () -> service.create(contact)).code());
        assertEquals("ORG_CONTACT_WRITES_DISABLED", assertThrows(
                OrganizationContactException.class, () -> service.update(4L, contact)).code());
        verifyNoInteractions(jdbc);
    }

    @Test
    void migrationReadinessReturnsCountsOnlyAndDoesNotWrite() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.equals("SELECT COUNT(*) FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS")) return 6930L;
            if (sql.equals("SELECT COUNT(*) FROM ORG.CONTACT_POINTS_V2")) return 6844L;
            if (sql.contains("WHERE NOT EXISTS") && sql.contains("ORG.ORGANIZATION_UNIT_CONTACT_POINTS a")) return 86L;
            return 0L;
        });
        var status = new OrganizationContactMigrationReadinessController(jdbc).readiness();
        assertEquals(86, status.missingLegacyLinks());
        assertEquals(6844, status.canonicalV2Rows());
        assertFalse(status.sourceCoverageComplete());
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }
}
