package com.behsazan.corebanking.calendar2.monthview.application;

import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.CalendarContext;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.DayRow;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.DayView;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.EventRow;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.EventView;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.MonthOption;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.MonthViewResponse;
import com.behsazan.corebanking.calendar2.monthview.oracle.Calendar2MonthViewRepository;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class Calendar2MonthViewService {
    private static final Set<String> SUPPORTED_CALENDARS = Set.of("PERSIAN", "GREGORIAN", "ISLAMIC");

    private final Calendar2MonthViewRepository repository;

    public Calendar2MonthViewService(Calendar2MonthViewRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MonthViewResponse month(String calendarCode, Integer year, Integer monthNo) {
        String normalizedCalendar = normalizeCalendar(calendarCode);
        CalendarContext context = repository.calendarContext(normalizedCalendar)
                .orElseThrow(() -> new ReferenceNotFoundException("تقویم پیش‌فرض برای نمایش ماهانه در CAL2 یافت نشد."));

        if (context.currentYear() == null || context.currentMonth() == null
                || context.minimumYear() == null || context.maximumYear() == null) {
            throw new ReferenceNotFoundException("Dataset تقویم دو تاریخ جاری یا محدوده سال معتبر ندارد.");
        }

        int targetYear = year == null ? context.currentYear() : year;
        int targetMonth = monthNo == null ? context.currentMonth() : monthNo;
        if (targetYear < context.minimumYear() || targetYear > context.maximumYear()) {
            throw validation("سال انتخاب‌شده خارج از محدوده Dataset تقویم دو است.", "year");
        }

        List<MonthOption> months = repository.months(context.calendarSystemId());
        int monthIndex = indexOfMonth(months, targetMonth);
        if (monthIndex < 0) {
            throw validation("ماه انتخاب‌شده برای این تقویم معتبر نیست.", "month");
        }

        List<DayRow> dayRows = repository.monthDays(context.calendarVariantId(), targetYear, targetMonth);
        if (dayRows.isEmpty()) {
            throw new ReferenceNotFoundException("برای ماه انتخاب‌شده در Dataset تقویم دو روزی ثبت نشده است.");
        }
        List<EventRow> eventRows = repository.monthEvents(context.calendarVariantId(), targetYear, targetMonth);

        Map<Long, List<EventView>> eventsByDay = new HashMap<>();
        for (EventRow row : eventRows) {
            eventsByDay.computeIfAbsent(row.dayId(), ignored -> new ArrayList<>()).add(toEventView(row));
        }

        List<DayView> days = new ArrayList<>(dayRows.size());
        int holidayDayCount = 0;
        int weekendDayCount = 0;
        for (DayRow row : dayRows) {
            List<EventView> events = eventsByDay.getOrDefault(row.dayId(), List.of());
            boolean holiday = events.stream().anyMatch(EventView::holiday);
            boolean weekend = row.isoWeekdayNo() == 5; // Friday for the Iranian public-calendar convention.
            if (holiday) holidayDayCount++;
            if (weekend) weekendDayCount++;
            days.add(toDayView(row, weekend, holiday, events));
        }

        MonthOption selectedMonth = months.get(monthIndex);
        Integer previousYear = null;
        Integer previousMonth = null;
        Integer nextYear = null;
        Integer nextMonth = null;

        if (monthIndex > 0) {
            previousYear = targetYear;
            previousMonth = months.get(monthIndex - 1).monthNo();
        } else if (targetYear > context.minimumYear()) {
            previousYear = targetYear - 1;
            previousMonth = months.get(months.size() - 1).monthNo();
        }

        if (monthIndex < months.size() - 1) {
            nextYear = targetYear;
            nextMonth = months.get(monthIndex + 1).monthNo();
        } else if (targetYear < context.maximumYear()) {
            nextYear = targetYear + 1;
            nextMonth = months.get(0).monthNo();
        }

        return new MonthViewResponse(
                context.calendarCode(), context.calendarName(), context.calendarVariantId(), context.variantCode(),
                targetYear, targetMonth, selectedMonth.nameFa(),
                context.currentYear(), context.currentMonth(),
                previousYear, previousMonth, nextYear, nextMonth,
                context.minimumYear(), context.maximumYear(), dayRows.size(), eventRows.size(),
                holidayDayCount, weekendDayCount, months, days
        );
    }

    private static String normalizeCalendar(String calendarCode) {
        String normalized = calendarCode == null || calendarCode.isBlank()
                ? "PERSIAN" : calendarCode.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CALENDARS.contains(normalized)) {
            throw validation("نوع تقویم برای نمایش ماهانه معتبر نیست.", "calendarCode");
        }
        return normalized;
    }

    private static int indexOfMonth(List<MonthOption> months, int monthNo) {
        for (int i = 0; i < months.size(); i++) {
            if (months.get(i).monthNo() == monthNo) return i;
        }
        return -1;
    }

    private static EventView toEventView(EventRow row) {
        return new EventView(
                row.eventOccurrenceId(), row.eventId(), row.eventCode(), row.eventName(),
                row.eventTypeCode(), row.eventTypeName(), row.official(), row.holiday(),
                row.occurrenceSource(), row.dataStatus(), row.description()
        );
    }

    private static DayView toDayView(DayRow row, boolean weekend, boolean holiday, List<EventView> events) {
        return new DayView(
                row.dayId(), row.canonicalIsoDate(), row.isoWeekdayNo(), row.iranDisplayOrder(), row.weekdayName(),
                row.primaryYear(), row.primaryMonthNo(), row.primaryDayNo(), row.primaryMonthName(),
                row.persianYear(), row.persianMonthNo(), row.persianDayNo(), row.persianMonthName(),
                row.gregorianYear(), row.gregorianMonthNo(), row.gregorianDayNo(), row.gregorianMonthName(),
                row.islamicYear(), row.islamicMonthNo(), row.islamicDayNo(), row.islamicMonthName(),
                row.today(), weekend, holiday, events
        );
    }

    private static ReferenceValidationException validation(String message, String field) {
        return new ReferenceValidationException(message, Map.of(field, message));
    }
}
