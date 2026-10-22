package com.behsazan.corebanking.calendar2.personal.application;

import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Entry;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.SaveRequest;
import com.behsazan.corebanking.calendar2.personal.oracle.Calendar2PersonalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Calendar2PersonalServiceR1BTest {
    private final Calendar2PersonalRepository repository = mock(Calendar2PersonalRepository.class);
    private final Calendar2PersonalService service = new Calendar2PersonalService(repository, "prototype-admin", "Asia/Tehran");

    @Test void creationReturnsNumericOracleSequenceId() {
        SaveRequest request = new SaveRequest(123L, "REMINDER", "  کار روزانه  ", null, "09:00", 3, "DAILY");
        Entry saved = new Entry(47L, 123L, "2026-10-22", "REMINDER", "کار روزانه", null,
                "09:00", 3, "DAILY", "OPEN", null, false);
        when(repository.dayExists(123L)).thenReturn(true);
        when(repository.insert("prototype-admin", 123L, "REMINDER", "کار روزانه", null,
                "09:00", 3, "DAILY")).thenReturn(47L);
        when(repository.find("prototype-admin", 47L)).thenReturn(Optional.of(saved));
        assertEquals(47L, service.create(request).userCalendarEntryId());
        verify(repository).insert("prototype-admin", 123L, "REMINDER", "کار روزانه", null,
                "09:00", 3, "DAILY");
    }

    @Test void noteHasNoReminderEvenIfUntrustedClientSuppliesReminderFields() {
        SaveRequest request = new SaveRequest(123L, "NOTE", "یادداشت", null, "09:00", 30, "ONCE");
        when(repository.dayExists(123L)).thenReturn(true);
        when(repository.insert("prototype-admin", 123L, "NOTE", "یادداشت", null,
                null, 0, "DAILY")).thenReturn(55L);
        when(repository.find("prototype-admin", 55L)).thenReturn(Optional.of(
                new Entry(55L, 123L, "2026-10-22", "NOTE", "یادداشت", null,
                        null, 0, "DAILY", "OPEN", null, false)));
        assertEquals(55L, service.create(request).userCalendarEntryId());
    }

    @Test void numericIdMustFitOracleNumber12AndBePositive() {
        assertThrows(ResponseStatusException.class, () -> service.find(0));
        assertThrows(ResponseStatusException.class, () -> service.find(-1));
        assertThrows(ResponseStatusException.class, () -> service.find(1_000_000_000_000L));
        verifyNoInteractions(repository);
    }

    @Test void deletionIsAlwaysScopedToServerConfiguredActor() {
        when(repository.delete("prototype-admin", 42L)).thenReturn(1);
        service.delete(42L);
        verify(repository).delete("prototype-admin", 42L);
    }
}
