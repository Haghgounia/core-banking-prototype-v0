package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.ContactInput;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Historical configuration alias is safe even when selected explicitly. */
class CanonicalOrganizationContactServiceTest {
    private static ContactInput phone(String value) {
        return new ContactInput(26435L, "PHONE", value, 1L, false,
                null, null, null, null);
    }

    private static void stubUnitSchema(NamedParameterJdbcTemplate jdbc) {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("COLUMN_NAME='ROW_VERSION'")) return 1L;
            if (sql.contains("CONTACT_TYPE_CODE','CONTACT_VALUE'")) return 5L;
            if (sql.contains("COLUMN_NAME='CONTACT_POINT_ID' AND NULLABLE='Y'")) return 1L;
            return 0L;
        });
    }

    @Test
    void createIsBlockedWithoutUnitWriteApproval() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        stubUnitSchema(jdbc);
        var service = new CanonicalOrganizationContactService(jdbc);
        var ex = assertThrows(OrganizationContactException.class,
                () -> service.create(phone("08134526468")));
        assertEquals("ORG_CONTACT_WRITES_DISABLED", ex.code());
        // Metadata reads are allowed; there must be no DML.
        verify(jdbc, never()).update(anyString(), any(Map.class));
    }

    @Test
    void updateIsBlockedWithoutUnitWriteApproval() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        stubUnitSchema(jdbc);
        var service = new CanonicalOrganizationContactService(jdbc);
        var ex = assertThrows(OrganizationContactException.class,
                () -> service.update(1811L, phone("08134526468")));
        assertEquals("ORG_CONTACT_WRITES_DISABLED", ex.code());
        verify(jdbc, never()).update(anyString(), any(Map.class));
    }

    @Test
    void canonicalAliasUsesUnitReadinessAndFailsClosedOnMissingSchema() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(0L);
        var state = new CanonicalOrganizationContactService(jdbc).readiness();
        assertEquals("UNIT", state.storageMode());
        assertFalse(state.ready());
        assertFalse(state.writesEnabled());
    }

    @Test
    void invalidPhoneIsRejectedBeforeOracleAccess() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        var service = new CanonicalOrganizationContactService(jdbc);
        var ex = assertThrows(OrganizationContactException.class,
                () -> service.create(phone("ABC")));
        assertEquals("ORG_CONTACT_INVALID", ex.code());
        verifyNoInteractions(jdbc);
    }
}
