package com.behsazan.corebanking.collateral.sponsor;

import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorModels.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Branch-scoped R3A read, candidate validation and optional DRAFT intake. */
@RestController
@RequestMapping("/api/v1/collaterals")
public class CollateralSponsorController {
    private final CollateralSponsorService service;
    public CollateralSponsorController(CollateralSponsorService service) { this.service = service; }

    @GetMapping("/sponsors/readiness")
    public Readiness readiness(Authentication actor) { return service.readiness(actor); }

    @GetMapping("/{collateralId}/sponsors")
    public SponsorPage list(@PathVariable String collateralId, Authentication actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(collateralId, page, size, actor);
    }

    @PostMapping("/{collateralId}/sponsors/validate")
    public Validation validate(@PathVariable String collateralId, @RequestBody CandidateRequest request,
            Authentication actor) {
        return service.validate(collateralId, request, actor);
    }

    @PostMapping("/{collateralId}/sponsors/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestRecord request(@PathVariable String collateralId, @RequestBody CandidateRequest request,
            Authentication actor, @RequestHeader("Idempotency-Key") String idem,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        return service.request(collateralId, request, actor, idem, correlation);
    }

    @GetMapping("/sponsors/requests/mine")
    public RequestPage mine(Authentication actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.mine(actor, page, size);
    }
}
