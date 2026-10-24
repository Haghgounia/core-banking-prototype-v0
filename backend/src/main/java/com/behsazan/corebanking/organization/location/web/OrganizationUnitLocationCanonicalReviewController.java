package com.behsazan.corebanking.organization.location.web;

import com.behsazan.corebanking.organization.location.application.OrganizationUnitLocationCanonicalReviewService;
import com.behsazan.corebanking.organization.location.application.OrganizationUnitLocationCanonicalReviewService.Snapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R2-A read-only endpoint; never changes official branch, address or GEO data. */
@RestController
@RequestMapping("/api/v1/organization/unit-location-canonical-review")
public class OrganizationUnitLocationCanonicalReviewController {
    private final OrganizationUnitLocationCanonicalReviewService service;

    public OrganizationUnitLocationCanonicalReviewController(OrganizationUnitLocationCanonicalReviewService service) {
        this.service = service;
    }

    @GetMapping
    public Snapshot review(@RequestParam(required = false) Long organizationUnitId,
                           @RequestParam(required = false) String text,
                           @RequestParam(defaultValue = "150") Integer limit,
                           @RequestParam(defaultValue = "0") Integer page) {
        return service.review(organizationUnitId, text, limit, page);
    }
}
