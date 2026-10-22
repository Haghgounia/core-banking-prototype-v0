package com.behsazan.corebanking.calendar2.personal.application;

import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Item;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class Calendar2PersonalAlertPolicyTest {
    private final LocalDate due = LocalDate.of(2026, 11, 11);

    private Item reminder(int advance, String policy, String status, String ack, boolean firstAcknowledged) {
        return new Item("test", 123L, due.toString(), "REMINDER", "گزارش شعب", null,
                "09:00", advance, policy, status, ack, firstAcknowledged);
    }

    @Test void dailyAdvanceNoticeStartsOnConfiguredDayAndRespectsTime() {
        Item reminder = reminder(3, "DAILY", "OPEN", null, false);
        assertNull(Calendar2PersonalAlertPolicy.alertKind(reminder, due.minusDays(4), LocalTime.NOON));
        assertNull(Calendar2PersonalAlertPolicy.alertKind(reminder, due.minusDays(3), LocalTime.of(8, 59)));
        assertEquals("ADVANCE", Calendar2PersonalAlertPolicy.alertKind(reminder, due.minusDays(3), LocalTime.of(9, 0)));
        assertEquals("ADVANCE", Calendar2PersonalAlertPolicy.alertKind(reminder, due.minusDays(1), LocalTime.NOON));
        assertEquals("DUE", Calendar2PersonalAlertPolicy.alertKind(reminder, due, LocalTime.NOON));
        assertEquals("OVERDUE", Calendar2PersonalAlertPolicy.alertKind(reminder, due.plusDays(2), LocalTime.MIDNIGHT));
    }

    @Test void advanceOnceIsSilencedAfterAckButDueNoticeStillAppears() {
        Item item = reminder(7, "ONCE", "OPEN", "2026-11-04", true);
        assertNull(Calendar2PersonalAlertPolicy.alertKind(item, due.minusDays(4), LocalTime.NOON));
        assertEquals("DUE", Calendar2PersonalAlertPolicy.alertKind(item, due, LocalTime.NOON));
    }

    @Test void acknowledgementSuppressesOnlyCurrentDateAndCompletionSuppressesAll() {
        Item acknowledged = reminder(3, "DAILY", "OPEN", "2026-11-09", false);
        assertNull(Calendar2PersonalAlertPolicy.alertKind(acknowledged, due.minusDays(2), LocalTime.NOON));
        assertEquals("ADVANCE", Calendar2PersonalAlertPolicy.alertKind(acknowledged, due.minusDays(1), LocalTime.NOON));
        assertNull(Calendar2PersonalAlertPolicy.alertKind(reminder(3, "DAILY", "DONE", null, false), due, LocalTime.NOON));
        assertNull(Calendar2PersonalAlertPolicy.alertKind(reminder(0, "DAILY", "OPEN", null, false), due.minusDays(1), LocalTime.NOON));
    }

    @Test void notesDoNotProduceNotifications() {
        Item note = new Item("note", 12L, due.toString(), "NOTE", "متن", null, null, 0, "DAILY", "OPEN", null, false);
        assertNull(Calendar2PersonalAlertPolicy.alertKind(note, due, LocalTime.NOON));
    }
}
