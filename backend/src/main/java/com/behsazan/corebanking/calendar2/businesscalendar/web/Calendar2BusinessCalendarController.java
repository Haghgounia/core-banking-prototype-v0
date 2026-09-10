package com.behsazan.corebanking.calendar2.businesscalendar.web;

import com.behsazan.corebanking.calendar2.businesscalendar.application.Calendar2BusinessCalendarService;
import com.behsazan.corebanking.calendar2.businesscalendar.domain.Calendar2BusinessCalendarModels.RebuildResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/calendar2/business-calendar")
public class Calendar2BusinessCalendarController {
    private final Calendar2BusinessCalendarService service;

    public Calendar2BusinessCalendarController(Calendar2BusinessCalendarService service) {
        this.service = service;
    }

    @PostMapping("/rebuild")
    RebuildResult rebuild(
            @RequestParam long businessCalendarId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.rebuild(businessCalendarId, fromDate, toDate);
    }
}
