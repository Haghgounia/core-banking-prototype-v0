package com.behsazan.corebanking.calendar.monthview.web;

import com.behsazan.corebanking.calendar.monthview.application.CalendarMonthViewService;
import com.behsazan.corebanking.calendar.monthview.domain.CalendarMonthViewModels.MonthViewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar/month-view")
public class CalendarMonthViewController {
    private final CalendarMonthViewService service;

    public CalendarMonthViewController(CalendarMonthViewService service) {
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
