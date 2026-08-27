package com.behsazan.corebanking.calendar2.monthview.domain;

import java.util.List;

public final class Calendar2MonthViewModels {
    private Calendar2MonthViewModels() {}

    public record MonthOption(int monthNo, String nameFa) {}

    public record CalendarContext(
            long calendarSystemId,
            long calendarVariantId,
            String calendarCode,
            String calendarName,
            String variantCode,
            Integer currentYear,
            Integer currentMonth,
            Integer minimumYear,
            Integer maximumYear
    ) {}

    public record DayRow(
            long dayId,
            String canonicalIsoDate,
            int isoWeekdayNo,
            int iranDisplayOrder,
            String weekdayName,
            int primaryYear,
            int primaryMonthNo,
            int primaryDayNo,
            String primaryMonthName,
            Integer persianYear,
            Integer persianMonthNo,
            Integer persianDayNo,
            String persianMonthName,
            Integer gregorianYear,
            Integer gregorianMonthNo,
            Integer gregorianDayNo,
            String gregorianMonthName,
            Integer islamicYear,
            Integer islamicMonthNo,
            Integer islamicDayNo,
            String islamicMonthName,
            boolean today
    ) {}

    public record EventRow(
            long eventOccurrenceId,
            long eventId,
            long dayId,
            String eventCode,
            String eventName,
            String eventTypeCode,
            String eventTypeName,
            boolean official,
            boolean holiday,
            String occurrenceSource,
            String dataStatus,
            String description
    ) {}

    public record EventView(
            long eventOccurrenceId,
            long eventId,
            String eventCode,
            String eventName,
            String eventTypeCode,
            String eventTypeName,
            boolean official,
            boolean holiday,
            String occurrenceSource,
            String dataStatus,
            String description
    ) {}

    public record DayView(
            long dayId,
            String canonicalIsoDate,
            int isoWeekdayNo,
            int iranDisplayOrder,
            String weekdayName,
            int primaryYear,
            int primaryMonthNo,
            int primaryDayNo,
            String primaryMonthName,
            Integer persianYear,
            Integer persianMonthNo,
            Integer persianDayNo,
            String persianMonthName,
            Integer gregorianYear,
            Integer gregorianMonthNo,
            Integer gregorianDayNo,
            String gregorianMonthName,
            Integer islamicYear,
            Integer islamicMonthNo,
            Integer islamicDayNo,
            String islamicMonthName,
            boolean today,
            boolean weekend,
            boolean holiday,
            List<EventView> events
    ) {
        public DayView { events = List.copyOf(events); }
    }

    public record MonthViewResponse(
            String calendarCode,
            String calendarName,
            long calendarVariantId,
            String variantCode,
            int year,
            int monthNo,
            String monthName,
            int currentYear,
            int currentMonthNo,
            Integer previousYear,
            Integer previousMonthNo,
            Integer nextYear,
            Integer nextMonthNo,
            int minimumYear,
            int maximumYear,
            int daysInMonth,
            int eventCount,
            int holidayDayCount,
            int weekendDayCount,
            List<MonthOption> months,
            List<DayView> days
    ) {
        public MonthViewResponse {
            months = List.copyOf(months);
            days = List.copyOf(days);
        }
    }
}
