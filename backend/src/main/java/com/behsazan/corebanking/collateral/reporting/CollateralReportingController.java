package com.behsazan.corebanking.collateral.reporting;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.localuat.CltLocalUatProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import com.behsazan.corebanking.collateral.reporting.CollateralReportingModels.*;
import com.behsazan.corebanking.collateral.monitoring.CollateralCoverageMonitoringService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** R6B read-only reports. The existing /api/v1/collaterals/** JWT scope gate is unchanged. */
@RestController
@RequestMapping("/api/v1/collaterals/reports")
public class CollateralReportingController {
    private final CollateralReportingRepository repository;
    private final CollateralRegistryProperties registry;
    private final CltLocalUatProperties localUat;
    private final CollateralCoverageMonitoringService coverage;
    private final CollateralTimelineService timeline;
    public CollateralReportingController(CollateralReportingRepository repository,
                                         CollateralRegistryProperties registry, CltLocalUatProperties localUat,
                                         CollateralCoverageMonitoringService coverage,
                                         CollateralTimelineService timeline) {
        this.repository = repository;
        this.registry = registry;
        this.localUat = localUat;
        this.coverage = coverage;
        this.timeline = timeline;
    }
    private String branch(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || jwt.getToken().getSubject() == null || jwt.getToken().getSubject().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CLT_REPORT_JWT_REQUIRED");
        Object value = jwt.getToken().getClaims().get("branch_code");
        if (!(value instanceof String branch))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_REPORT_BRANCH_REQUIRED");
        try { return CollateralRegistryPolicy.branch(branch); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_REPORT_BRANCH_INVALID");
        }
    }
    private String requireAvailable(Authentication authentication) {
        String branch = branch(authentication);
        if (!registry.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_REPORT_REGISTRY_READS_DISABLED");
        return branch;
    }

    @GetMapping("/overview")
    public Overview overview(Authentication authentication) {
        String branch = requireAvailable(authentication);
        List<StatusCount> statuses = repository.statuses(branch);
        List<ActiveExposure> exposure = repository.exposures(branch);
        if (statuses.size() > 100 || exposure.size() > 100)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_REPORT_CARDINALITY_REVIEW_REQUIRED");
        long total = 0;
        for (StatusCount item : statuses) {
            if (item.count() < 0 || total > Long.MAX_VALUE - item.count())
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_REPORT_COUNT_OVERFLOW");
            total += item.count();
        }
        return new Overview("ORACLE_CLT_READ_ONLY",
                localUat.enabled() ? "LOCAL_UAT_FIXED_BRANCH" : "JWT_COLLATERAL_BRANCH",
                branch, total,
                statuses, exposure, false,
                "Independent SELECT statements are not a serializable allocation snapshot; no new-allocation authorization.");
    }

    @GetMapping("/recent-events")
    public List<StatusEvent> recentEvents(Authentication authentication,
            @RequestParam(defaultValue = "20") int limit) {
        String branch = requireAvailable(authentication);
        return repository.statusEvents(branch, CollateralReportingPolicy.eventLimit(limit));
    }

    /** BUC-14: all active allocations in verified branch; page-level exact recorded shortfall. */
    @GetMapping("/coverage-monitor")
    public CollateralCoverageMonitoringService.CoveragePage coverageMonitor(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return coverage.report(requireAvailable(authentication), page, size);
    }

    /** BUC-16: direct registration-number history; unauthorized and missing indistinguishable. */
    @GetMapping("/by-registration/{number}/history")
    public CollateralTimelineService.Timeline history(Authentication authentication,
            @PathVariable String number,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return timeline.byRegistration(requireAvailable(authentication), number, page, size);
    }

    @GetMapping("/reference-summary")
    public ReferenceSummary referenceSummary(Authentication authentication) {
        requireAvailable(authentication);
        return repository.references();
    }
}
