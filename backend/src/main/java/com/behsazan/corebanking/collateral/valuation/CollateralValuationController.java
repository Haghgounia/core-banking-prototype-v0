package com.behsazan.corebanking.collateral.valuation;

import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** SECURED in CollateralRegistrySecurityConfiguration; no operational valuation mutation routes. */
@RestController
@RequestMapping("/api/v1/collaterals")
public class CollateralValuationController {
    private final CollateralValuationService service;
    public CollateralValuationController(CollateralValuationService service) { this.service = service; }

    @GetMapping("/valuations/readiness")
    public Readiness readiness(Authentication actor) { return service.readiness(actor); }

    @GetMapping("/{collateralId}/valuations")
    public ValuationPage history(@PathVariable String collateralId, Authentication actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.history(collateralId, actor, page, size);
    }

    @GetMapping("/{collateralId}/valuations/current-evidence")
    public CurrentEvidence current(@PathVariable String collateralId, Authentication actor) {
        return service.current(collateralId, actor);
    }

    @GetMapping("/{collateralId}/valuations/options")
    public Options options(@PathVariable String collateralId, Authentication actor) {
        return service.options(collateralId, actor);
    }

    @PostMapping("/{collateralId}/valuations/validate")
    public Validation validate(@PathVariable String collateralId,
            @RequestBody CandidateRequest request, Authentication actor) {
        return service.validate(collateralId, request, actor);
    }

    @PostMapping("/{collateralId}/valuations/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestRecord request(@PathVariable String collateralId,
            @RequestBody CandidateRequest request, Authentication actor,
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        return service.request(collateralId, request, actor, key, correlation);
    }

    @GetMapping("/valuations/requests/mine")
    public RequestPage mine(Authentication actor, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.mine(actor, page, size);
    }
}
