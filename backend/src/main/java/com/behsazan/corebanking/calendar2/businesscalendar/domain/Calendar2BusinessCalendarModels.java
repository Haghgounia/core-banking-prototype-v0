package com.behsazan.corebanking.calendar2.businesscalendar.domain;

import java.time.LocalDate;

public final class Calendar2BusinessCalendarModels {
    private Calendar2BusinessCalendarModels() {}

    public record RebuildResult(
            long businessCalendarId,
            LocalDate fromDate,
            LocalDate toDate,
            int affectedRows,
            long totalResolvedDays,
            long scheduleDays,
            long holidayDays,
            long exceptionDays,
            long defaultDays
    ) {}
}
