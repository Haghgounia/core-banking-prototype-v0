package com.behsazan.corebanking.calendar.datasetimport.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CalendarDatasetModels {
    private CalendarDatasetModels() {}

    public record DatasetStatus(
            String schemaName,
            long calendarDayRows,
            long calendarDateRows,
            long calendarSystemRows,
            long calendarAlgorithmRows,
            long weekdayRows,
            long calendarMonthRows,
            boolean seedReady,
            boolean datasetEmpty,
            List<String> missingSeedItems
    ) {
        public DatasetStatus {
            missingSeedItems = List.copyOf(missingSeedItems);
        }
    }

    public record DatasetVerification(
            long badRepresentationDayCount,
            long canonicalDateGapCount,
            long dayIdGapCount,
            long badJulianDayCount,
            long badIsoWeekdayCount,
            long unknownCalendarSystemCount,
            Map<String, Long> calendarSystemCounts
    ) {
        public DatasetVerification {
            calendarSystemCounts = Map.copyOf(new LinkedHashMap<>(calendarSystemCounts));
        }

        public boolean valid() {
            return badRepresentationDayCount == 0
                    && canonicalDateGapCount == 0
                    && dayIdGapCount == 0
                    && badJulianDayCount == 0
                    && badIsoWeekdayCount == 0
                    && unknownCalendarSystemCount == 0;
        }
    }

    public record DatasetImportResult(
            String schemaName,
            String calendarDayFileName,
            String calendarDateFileName,
            long calendarDayRows,
            long calendarDateRows,
            long elapsedMillis
    ) {}

    public record CalendarDayCsvRow(
            long dayId,
            java.time.LocalDate canonicalDate,
            long epochDay,
            long julianDayNumber,
            int weekdayId,
            int isoWeekdayNo,
            int irWeekdayNo
    ) {}

    public record CalendarDateCsvRow(
            long calendarDateId,
            long dayId,
            String calendarSystemCode,
            int yearNo,
            int monthNo,
            int dayNo,
            int dayOfYear,
            String formattedDate,
            String isLeapYear,
            String algorithmCode
    ) {}
}
