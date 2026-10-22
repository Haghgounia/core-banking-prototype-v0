package com.behsazan.corebanking.calendar2.personal.application;

import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Entry;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/** Deterministic server-side notification rules, testable without Oracle. */
public final class Calendar2PersonalAlertPolicy {
    private Calendar2PersonalAlertPolicy() {}

    /** Returns ADVANCE, DUE, OVERDUE or null. No alert is emitted twice after acknowledgment on a day. */
    public static String alertKind(Entry entry, LocalDate today, LocalTime now) {
        if (!"REMINDER".equals(entry.entryType()) || !"OPEN".equals(entry.status())) return null;
        final LocalDate due = LocalDate.parse(entry.canonicalIsoDate());
        final LocalDate acknowledged = entry.lastAlertAckDate() == null ? null : LocalDate.parse(entry.lastAlertAckDate());
        if (today.equals(acknowledged)) return null;
        if (today.isAfter(due)) return "OVERDUE";
        if (now.isBefore(LocalTime.parse(entry.reminderTime()))) return null;
        if (today.equals(due)) return "DUE";
        if (today.isBefore(due.minusDays(entry.advanceDays()))) return null;
        if (entry.advanceDays() == 0) return null;
        if ("ONCE".equals(entry.repeatPolicy()) && entry.firstAdvanceAcknowledged()) return null;
        return "ADVANCE";
    }

    public static int daysToDue(Entry entry, LocalDate today) {
        return Math.toIntExact(ChronoUnit.DAYS.between(today, LocalDate.parse(entry.canonicalIsoDate())));
    }
}
