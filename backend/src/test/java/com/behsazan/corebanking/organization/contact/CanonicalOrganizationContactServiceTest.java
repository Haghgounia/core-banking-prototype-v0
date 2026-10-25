package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.ContactInput;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CanonicalOrganizationContactServiceTest {
    private static ContactInput phone(String value) {
        return new ContactInput(26435L, "PHONE", value, 1L, false,
                null, null, null, null);
    }

    @Test
    void createIsBlockedWithoutExplicitPostCutoverWriteApproval() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        CanonicalOrganizationContactService service = new CanonicalOrganizationContactService(jdbc, false);
        OrganizationContactException ex = assertThrows(OrganizationContactException.class,
                () -> service.create(phone("08134526468")));
        assertEquals("ORG_CONTACT_WRITES_DISABLED", ex.code());
        verifyNoInteractions(jdbc);
    }

    @Test
    void updateIsBlockedWithoutExplicitPostCutoverWriteApproval() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        CanonicalOrganizationContactService service = new CanonicalOrganizationContactService(jdbc, false);
        OrganizationContactException ex = assertThrows(OrganizationContactException.class,
                () -> service.update(1811L, phone("08134526468")));
        assertEquals("ORG_CONTACT_WRITES_DISABLED", ex.code());
        verifyNoInteractions(jdbc);
    }

    @Test
    void canonicalReadinessFailsClosedOnOldMasterSchema() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class)))
                .thenAnswer(call -> {
                    String sql = call.getArgument(0);
                    return sql.contains("FROM ALL_TABLES") ? 1L : 0L;
                });
        CanonicalOrganizationContactService service = new CanonicalOrganizationContactService(jdbc, true);
        var readiness = service.readiness();
        assertEquals("CANONICAL", readiness.storageMode());
        assertFalse(readiness.ready());
        assertFalse(readiness.writesEnabled());
    }

    @Test
    void invalidPhoneIsRejectedBeforeOracleAccess() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        CanonicalOrganizationContactService service = new CanonicalOrganizationContactService(jdbc, true);
        OrganizationContactException ex = assertThrows(OrganizationContactException.class,
                () -> service.create(phone("ABC")));
        assertEquals("ORG_CONTACT_INVALID", ex.code());
        verifyNoInteractions(jdbc);
    }
}
