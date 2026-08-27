package com.behsazan.corebanking.calendar2.eventrecurrence.application;

import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.CalendarMonthOption;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.GenerateAllResult;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.GenerationResult;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.OccurrenceFilterMeta;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.OccurrenceSummary;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.RuleDefinition;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.RuleSummary;
import com.behsazan.corebanking.shared.model.PageResponse;
import com.behsazan.corebanking.calendar2.eventrecurrence.oracle.Calendar2EventRecurrenceRepository;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class Calendar2EventRecurrenceService {
    private static final Set<String> OCCURRENCE_SOURCES = Set.of("GENERATED", "MANUAL", "OFFICIAL");
    private final Calendar2EventRecurrenceRepository repository;

    public Calendar2EventRecurrenceService(Calendar2EventRecurrenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RuleSummary> searchRules(String text, int page, int size, String sortBy, String direction) {
        return repository.searchRuleSummaries(text, page, size, sortBy, direction);
    }

    @Transactional(readOnly = true)
    public PageResponse<OccurrenceSummary> searchOccurrences(String text, Integer solarYear, Long eventId,
                                                              String occurrenceSource, Boolean holiday,
                                                              int page, int size, String sortBy, String direction) {
        String normalizedSource = occurrenceSource == null || occurrenceSource.isBlank()
                ? null : occurrenceSource.trim().toUpperCase();
        if (normalizedSource != null && !OCCURRENCE_SOURCES.contains(normalizedSource)) {
            throw validation("منشأ رخداد معتبر نیست.", "occurrenceSource");
        }
        return repository.searchOccurrenceSummaries(text, solarYear, eventId, normalizedSource, holiday,
                page, size, sortBy, direction);
    }

    @Transactional(readOnly = true)
    public OccurrenceFilterMeta occurrenceFilterMeta() {
        return repository.occurrenceFilterMeta();
    }

    @Transactional(readOnly = true)
    public List<CalendarMonthOption> monthsForVariant(long variantId) {
        return repository.monthsForVariant(variantId);
    }

    @Transactional
    public GenerationResult rebuild(long ruleId) {
        RuleDefinition rule = repository.findRule(ruleId)
                .orElseThrow(() -> new ReferenceNotFoundException("قاعده تکرار CAL2 یافت نشد."));
        validateRule(rule);

        int deleted = repository.deleteGenerated(ruleId);
        boolean active = rule.ruleActive() && rule.eventActive();
        if (!active) {
            return new GenerationResult(rule.eventRuleId(), rule.eventId(), rule.eventName(), rule.ruleType(),
                    false, 0, deleted, 0, 0);
        }

        int matched = repository.countCalendarMatches(rule);
        int inserted = repository.insertGenerated(rule);
        int skipped = Math.max(matched - inserted, 0);
        return new GenerationResult(rule.eventRuleId(), rule.eventId(), rule.eventName(), rule.ruleType(),
                true, matched, deleted, inserted, skipped);
    }

    @Transactional
    public GenerateAllResult rebuildAllActive() {
        List<GenerationResult> results = new ArrayList<>();
        int inserted = 0;
        int deleted = 0;
        for (Long ruleId : repository.activeRuleIds()) {
            GenerationResult result = rebuild(ruleId);
            results.add(result);
            inserted += result.insertedGeneratedOccurrences();
            deleted += result.deletedGeneratedOccurrences();
        }
        return new GenerateAllResult(results.size(), inserted, deleted, results);
    }

    private static void validateRule(RuleDefinition rule) {
        if (!"ANNUAL_FIXED_DATE".equals(rule.ruleType()) && !"ONE_TIME_DATE".equals(rule.ruleType())) {
            throw validation("نوع قاعده تکرار پشتیبانی نمی‌شود.", "ruleType");
        }
        if (rule.monthNo() < 1 || rule.monthNo() > 13) throw validation("ماه قاعده معتبر نیست.", "monthNo");
        if (rule.dayNo() < 1 || rule.dayNo() > 31) throw validation("روز قاعده معتبر نیست.", "dayNo");
        if ("ONE_TIME_DATE".equals(rule.ruleType()) && rule.yearNo() == null) {
            throw validation("سال وقوع برای قاعده یک‌باره الزامی است.", "yearNo");
        }
        if ("ANNUAL_FIXED_DATE".equals(rule.ruleType()) && rule.yearNo() != null) {
            throw validation("برای قاعده سالانه، سال وقوع باید خالی باشد.", "yearNo");
        }
        if (rule.startYearNo() != null && rule.endYearNo() != null && rule.startYearNo() > rule.endYearNo()) {
            throw validation("بازه سال قاعده معتبر نیست.", "endYearNo");
        }
    }

    private static ReferenceValidationException validation(String message, String field) {
        return new ReferenceValidationException(message, Map.of(field, message));
    }
}
