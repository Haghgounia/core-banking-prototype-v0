package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.ContactInput;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrganizationUnitContactR5C2IndependenceTest {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

    private void stubMetadata(boolean detached) {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("TABLE_NAME='CONTACT_POINTS'")) return detached ? 0L : 1L;
            if (sql.contains("FK_OUCP_CONTACT_MASTER")) return detached ? 0L : 1L;
            if (sql.contains("FK_SPCP_CONTACT_MASTER")) return detached ? 0L : 1L;
            if (sql.contains("COLUMN_NAME='ROW_VERSION'")) return 1L;
            if (sql.contains("CONTACT_TYPE_CODE','CONTACT_VALUE'")) return 5L;
            if (sql.contains("COLUMN_NAME='CONTACT_POINT_ID' AND NULLABLE='Y'")) return detached ? 1L : 0L;
            return 0L;
        });
    }

    @Test void newWritesAreBlockedBeforeOracleDetachEvenIfEnvIsTrue() {
        stubMetadata(false);
        var svc = new OrganizationContactService(jdbc, true);
        assertTrue(svc.readiness().ready());
        assertFalse(svc.readiness().writesEnabled());
        assertEquals("UNIT", svc.readiness().storageMode());
        var raw = new ContactInput(1L, "PHONE", "02112345678", 1L,
                false, null, null, null, null);
        var ex = assertThrows(OrganizationContactException.class, () -> svc.create(raw));
        assertEquals("ORG_CONTACT_WRITES_DISABLED", ex.code());
        verify(jdbc, never()).update(anyString(), anyMap());
    }

    @Test void postDetachReadinessIsIndependentOfArchivedMaster() {
        stubMetadata(true);
        var svc = new OrganizationContactService(jdbc, true);
        var state = svc.readiness();
        assertTrue(state.ready());
        assertTrue(state.writesEnabled());
        assertFalse(state.masterPresent());
        assertFalse(state.unitForeignKeyPresent());
        assertFalse(state.servicePointForeignKeyPresent());
    }

    @Test void writesRemainDisabledUnlessExplicitlyApproved() {
        stubMetadata(true);
        var svc = new OrganizationContactService(jdbc, false);
        assertTrue(svc.readiness().ready());
        assertFalse(svc.readiness().writesEnabled());
    }

    @Test void invalidPhoneIsRejectedBeforeOracleAccess() {
        var svc = new OrganizationContactService(jdbc, false);
        var raw = new ContactInput(1L, "PHONE", "INVALID", 1L,
                false, null, null, null, null);
        assertEquals("ORG_CONTACT_INVALID", assertThrows(OrganizationContactException.class,
                () -> svc.create(raw)).code());
        verifyNoInteractions(jdbc);
    }
}
