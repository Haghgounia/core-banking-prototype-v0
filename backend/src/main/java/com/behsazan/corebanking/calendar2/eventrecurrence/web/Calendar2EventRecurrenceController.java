package com.behsazan.corebanking.calendar2.eventrecurrence.web;

import com.behsazan.corebanking.calendar2.eventrecurrence.application.Calendar2EventRecurrenceService;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.GenerateAllResult;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.GenerationResult;
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

    @PostMapping("/rebuild")
    GenerationResult rebuild(@RequestParam long ruleId) {
        return service.rebuild(ruleId);
    }

    @PostMapping("/rebuild-all")
    GenerateAllResult rebuildAll() {
        return service.rebuildAllActive();
    }
}
