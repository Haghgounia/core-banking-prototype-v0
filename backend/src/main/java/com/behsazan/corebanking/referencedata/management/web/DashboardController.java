package com.behsazan.corebanking.referencedata.management.web;

import com.behsazan.corebanking.referencedata.management.application.ReferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final ReferenceService service;

    public DashboardController(ReferenceService service) {
        this.service = service;
    }

    @GetMapping("/counts")
    Map<String, Long> counts() {
        return service.dashboardCounts();
    }
}
