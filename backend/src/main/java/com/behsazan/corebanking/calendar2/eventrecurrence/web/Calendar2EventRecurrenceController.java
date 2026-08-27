package com.behsazan.corebanking.calendar2.eventrecurrence.web;

import com.behsazan.corebanking.calendar2.eventrecurrence.application.Calendar2EventRecurrenceService;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.CalendarMonthOption;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.GenerateAllResult;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.GenerationResult;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.OccurrenceFilterMeta;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.OccurrenceSummary;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.RuleSummary;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar2/event-recurrence")
public class Calendar2EventRecurrenceController {
    private final Calendar2EventRecurrenceService service;

    public Calendar2EventRecurrenceController(Calendar2EventRecurrenceService service) {
        this.service = service;
    }

    @GetMapping("/rules")
    PageResponse<RuleSummary> rules(
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.searchRules(text, page, size, sortBy, direction);
    }

    @GetMapping("/occurrences")
    PageResponse<OccurrenceSummary> occurrences(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Integer solarYear,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String occurrenceSource,
            @RequestParam(required = false) Boolean holiday,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.searchOccurrences(text, solarYear, eventId, occurrenceSource, holiday, page, size, sortBy, direction);
    }

    @GetMapping("/occurrence-meta")
    OccurrenceFilterMeta occurrenceMeta() {
        return service.occurrenceFilterMeta();
    }

    @GetMapping("/months")
    java.util.List<CalendarMonthOption> months(@RequestParam long variantId) {
        return service.monthsForVariant(variantId);
    }

    @PostMapping("/rebuild")
    GenerationResult rebuild(@RequestParam long ruleId) {
        return service.rebuild(ruleId);
    }

    @PostMapping("/rebuild-all")
    GenerateAllResult rebuildAll() {
        return service.rebuildAllActive();
    }
}
