package com.behsazan.corebanking.calendar2.personalv2;

import com.behsazan.corebanking.calendar2.personalv2.PersonalV2Models.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pure service-level regression tests; does not write to Oracle. */
class PersonalV2ServiceTest {
    private final PersonalV2Repository repo=mock(PersonalV2Repository.class);
    private final PersonalV2Service service=new PersonalV2Service(repo,false,"prototype-admin",
            "urn:test-issuer","Asia/Tehran");

    @Test void unauthenticatedPrincipalFailsClosed() {
        assertThrows(ResponseStatusException.class,()->service.owner(null));
        verifyNoInteractions(repo);
    }

    @Test void authenticatedPrincipalDeterminesOwnerInsteadOfRequestBody() {
        assertEquals(new Owner("urn:test-issuer","bank-staff-27"),service.owner(()->"bank-staff-27"));
    }

    @Test void number12IdentifierIsValidatedBeforeDatabase() {
        Owner owner=new Owner("urn:test-issuer","bank-staff-27");
        assertThrows(ResponseStatusException.class,()->service.find(owner,"1000000000000"));
        assertThrows(ResponseStatusException.class,()->service.find(owner,"0"));
        verifyNoInteractions(repo);
    }

    @Test void missingIfMatchRejectsWithoutWrite() {
        Owner owner=new Owner("urn:test-issuer","bank-staff-27");
        assertThrows(ResponseStatusException.class,()->service.delete(owner,"12",null));
        verifyNoInteractions(repo);
    }

    @Test void idempotencyReplayReturnsExistingEntryWithoutInsert() {
        Owner owner=new Owner("urn:test-issuer","bank-staff-27");
        SaveRequest req=new SaveRequest("12","NOTE","test",null,null,null,0,"DAILY",
                "Asia/Tehran","NONE",null,1,null,null,null,"SKIP");
        when(repo.dayExists(BigDecimal.valueOf(12))).thenReturn(true);
        when(repo.idempotencyExisting(eq(owner),eq("dedup-123456"),any(byte[].class))).thenReturn(81L);
        Entry existing=new Entry("81","91","12","2026-10-24","NOTE","test",null,null,
                null,0,"DAILY","OPEN",1,1,null);
        when(repo.entry(owner,81)).thenReturn(Optional.of(existing));
        assertEquals(existing,service.create(owner,"dedup-123456",req));
        verify(repo,never()).claimIdempotency(any(),anyString(),any());
        verify(repo,never()).insertEntry(any(),anyLong(),any(),any(),any(),any(),any(),any());
    }
}
