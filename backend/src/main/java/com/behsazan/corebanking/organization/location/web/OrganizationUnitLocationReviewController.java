package com.behsazan.corebanking.organization.location.web;

import com.behsazan.corebanking.organization.location.application.OrganizationUnitLocationReviewService;
import com.behsazan.corebanking.organization.location.application.OrganizationUnitLocationReviewService.ReviewSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only legacy projection used for branch-address reconciliation (R1). */
// Default OFF: after R4 archival these endpoints would query renamed legacy tables.
// DBA-only historic comparison can be explicitly re-enabled before archival.
@ConditionalOnProperty(prefix="core-banking.organization.locations", name="legacy-review-enabled", havingValue="true")
@RestController
@RequestMapping("/api/v1/organization/unit-location-review")
public class OrganizationUnitLocationReviewController {
    private final OrganizationUnitLocationReviewService service;

    public OrganizationUnitLocationReviewController(OrganizationUnitLocationReviewService service) {
        this.service = service;
    }

    @GetMapping
    public ReviewSnapshot review(@RequestParam(required = false) Long organizationUnitId,
                                 @RequestParam(required = false) String text,
                                 @RequestParam(defaultValue = "150") Integer limit) {
        return service.review(organizationUnitId, text, limit);
    }
}
