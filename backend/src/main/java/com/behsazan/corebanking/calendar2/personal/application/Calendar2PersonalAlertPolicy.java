package com.behsazan.corebanking.calendar2.personal.application;

import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Item;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/** Deterministic server-side notification rules, testable without Oracle. */
public final class Calendar2PersonalAlertPolicy {
    private Calendar2PersonalAlertPolicy() {}

    /** Returns ADVANCE, DUE, OVERDUE or null. No alert is emitted twice after acknowledgment on a day. */
    public static String alertKind(Item item, LocalDate today, LocalTime now) {
        if (!"REMINDER".equals(item.itemType()) || !"OPEN".equals(item.status())) return null;
        final LocalDate due = LocalDate.parse(item.canonicalIsoDate());
        final LocalDate acknowledged = item.lastAlertAckDate() == null ? null : LocalDate.parse(item.lastAlertAckDate());
        if (today.equals(acknowledged)) return null;
        if (today.isAfter(due)) return "OVERDUE";
        if (now.isBefore(LocalTime.parse(item.reminderTime()))) return null;
        if (today.equals(due)) return "DUE";
        if (today.isBefore(due.minusDays(item.advanceDays()))) return null;
        if (item.advanceDays() == 0) return null;
        if ("ONCE".equals(item.repeatPolicy()) && item.firstAdvanceAcknowledged()) return null;
        return "ADVANCE";
    }

    public static int daysToDue(Item item, LocalDate today) {
        return Math.toIntExact(ChronoUnit.DAYS.between(today, LocalDate.parse(item.canonicalIsoDate())));
    }
}
