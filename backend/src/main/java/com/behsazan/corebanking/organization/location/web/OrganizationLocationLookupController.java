package com.behsazan.corebanking.organization.location.web;

import com.behsazan.corebanking.organization.location.application.OrganizationLocationLookupService;
import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organization/location-lookup")
public class OrganizationLocationLookupController {
    private final OrganizationLocationLookupService service;

    public OrganizationLocationLookupController(OrganizationLocationLookupService service) {
        this.service = service;
    }

    @GetMapping("/cities")
    public List<LookupOption> cities(
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "100") Integer limit
    ) {
        return service.searchCities(provinceId, text, limit);
    }
}
