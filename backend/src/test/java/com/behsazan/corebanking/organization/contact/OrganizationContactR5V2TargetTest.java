package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.ContactInput;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** R5 historical tests rebased on the final OUCP-only operational contract. */
class OrganizationContactR5V2TargetTest {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

    private void detachedSchema(boolean detached) {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("TABLE_NAME='CONTACT_POINTS'")) return 0L;
            if (sql.contains("FK_OUCP_CONTACT_MASTER")) return detached ? 0L : 1L;
            if (sql.contains("FK_SPCP_CONTACT_MASTER")) return detached ? 0L : 1L;
            if (sql.contains("COLUMN_NAME='ROW_VERSION'")) return 1L;
            if (sql.contains("CONTACT_TYPE_CODE','CONTACT_VALUE'")) return 5L;
            if (sql.contains("COLUMN_NAME='CONTACT_POINT_ID' AND NULLABLE='Y'")) return detached ? 1L : 0L;
            return 0L;
        });
    }

    @Test
    void oldCanonicalSettingUsesUnitStoreAfterDetach() {
        detachedSchema(true);
        var state = new CanonicalOrganizationContactService(jdbc).readiness();
        assertEquals("UNIT", state.storageMode());
        assertTrue(state.ready());
        assertFalse(state.writesEnabled());
        verify(jdbc, never()).queryForObject(argThat(sql -> sql.contains("CONTACT_POINTS_V2")),
                anyMap(), eq(Long.class));
    }

    @Test
    void oldCanonicalSettingCannotBypassOracleDetachGate() {
        detachedSchema(false);
        var state = new CanonicalOrganizationContactService(jdbc).readiness();
        assertFalse(state.writesEnabled());
        assertTrue(state.unitForeignKeyPresent());
    }

    @Test
    void oldCanonicalSettingIsReadOnlyRegardlessOfUnitWriteApproval() {
        detachedSchema(true);
        var service = new CanonicalOrganizationContactService(jdbc);
        var input = new ContactInput(1L, "PHONE", "02112345678", 2L, true,
                null, null, null, null);
        assertEquals("ORG_CONTACT_WRITES_DISABLED", assertThrows(
                OrganizationContactException.class, () -> service.create(input)).code());
        assertEquals("ORG_CONTACT_WRITES_DISABLED", assertThrows(
                OrganizationContactException.class, () -> service.update(4L, input)).code());
        verify(jdbc, never()).update(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class));
    }

    @Test
    void retiredMigrationEndpointReturnsGoneWithoutDatabaseAccess() {
        var response = new OrganizationContactMigrationReadinessController().readiness();
        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ORG_V2_MIGRATION_ENDPOINT_RETIRED", response.getBody().get("code"));
        assertEquals("/api/v1/organization/unit-contacts/readiness",
                response.getBody().get("operationalReadinessEndpoint"));
    }
}
