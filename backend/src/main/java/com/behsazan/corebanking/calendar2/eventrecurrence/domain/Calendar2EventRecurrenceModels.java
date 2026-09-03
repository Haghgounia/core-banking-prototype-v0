package com.behsazan.corebanking.calendar2.eventrecurrence.domain;

import java.util.List;

public final class Calendar2EventRecurrenceModels {
    private Calendar2EventRecurrenceModels() {}

    public record RuleDefinition(
            long eventRuleId,
            long eventId,
            String eventName,
            String ruleType,
            long calendarVariantId,
            Integer yearNo,
            int monthNo,
            int dayNo,
            Integer startYearNo,
            Integer endYearNo,
            String dayResolutionPolicy,
            Long sourceId,
            String description,
            boolean ruleActive,
            boolean eventActive,
            boolean defaultHoliday
    ) {}

    public record RuleSummary(
            long eventRuleId,
            long eventId,
            String eventCode,
            String eventName,
            String ruleType,
            long calendarVariantId,
            String variantCode,
            String calendarCode,
            String calendarName,
            String monthName,
            Integer yearNo,
            int monthNo,
            int dayNo,
            Integer startYearNo,
            Integer endYearNo,
            String dayResolutionPolicy,
            boolean active,
            int generatedOccurrences
    ) {}

    public record OccurrenceSummary(
            long eventOccurrenceId,
            long eventId,
            Long eventRuleId,
            String eventCode,
            String eventName,
            String eventTypeName,
            long dayId,
            String canonicalIsoDate,
            String weekdayName,
            Integer solarYear,
            Integer solarMonthNo,
            Integer solarDayNo,
            String solarMonthName,
            Integer gregorianYear,
            Integer gregorianMonthNo,
            Integer gregorianDayNo,
            String gregorianMonthName,
            Integer hijriYear,
            Integer hijriMonthNo,
            Integer hijriDayNo,
            String hijriMonthName,
            String ruleType,
            String ruleCalendarName,
            String ruleVariantCode,
            String ruleMonthName,
            Integer ruleYearNo,
            Integer ruleMonthNo,
            Integer ruleDayNo,
            String occurrenceSource,
            String dataStatus,
            boolean holiday,
            Long sourceId,
            String sourceCode,
            String sourceName,
            String datasetVersionCode,
            String description
    ) {}

    public record OccurrenceFilterMeta(Integer currentSolarYear, Integer minimumSolarYear, Integer maximumSolarYear) {}

    public record CalendarMonthOption(int monthNo, String nameFa) {}

    public record GenerationResult(
            long eventRuleId,
            long eventId,
            String eventName,
            String ruleType,
            String dayResolutionPolicy,
            boolean active,
            int matchedCalendarDates,
            int deletedGeneratedOccurrences,
            int insertedGeneratedOccurrences,
            int skippedExistingOccurrences
    ) {}

    public record GenerateAllResult(
            int processedRules,
            int insertedGeneratedOccurrences,
            int deletedGeneratedOccurrences,
            List<GenerationResult> rules
    ) {
        public GenerateAllResult { rules = List.copyOf(rules); }
    }
}
