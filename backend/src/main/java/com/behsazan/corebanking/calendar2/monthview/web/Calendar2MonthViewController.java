package com.behsazan.corebanking.calendar2.monthview.web;

import com.behsazan.corebanking.calendar2.monthview.application.Calendar2MonthViewService;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.MonthViewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar2/month-view")
public class Calendar2MonthViewController {
    private final Calendar2MonthViewService service;

    public Calendar2MonthViewController(Calendar2MonthViewService service) {
        this.service = service;
    }

    @GetMapping
    MonthViewResponse month(
            @RequestParam(defaultValue = "PERSIAN") String calendarCode,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return service.month(calendarCode, year, month);
    }
}
