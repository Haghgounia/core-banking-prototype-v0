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
            Long sourceId,
            String description,
            boolean ruleActive,
            boolean eventActive,
            boolean defaultHoliday
    ) {}

    public record GenerationResult(
            long eventRuleId,
            long eventId,
            String eventName,
            String ruleType,
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
