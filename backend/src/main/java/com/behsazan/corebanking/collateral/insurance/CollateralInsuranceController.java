package com.behsazan.corebanking.collateral.insurance;

import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** All insurance routes explicitly checked in CollateralRegistrySecurityConfiguration. */
@RestController
@RequestMapping("/api/v1/collaterals")
public class CollateralInsuranceController {
    private final CollateralInsuranceService service;
    public CollateralInsuranceController(CollateralInsuranceService service) { this.service = service; }

    @GetMapping("/insurance-policies/readiness")
    public Readiness readiness(Authentication actor) { return service.readiness(actor); }

    @GetMapping("/{collateralId}/insurance-policies")
    public PolicyPage list(@PathVariable String collateralId, Authentication actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(collateralId, page, size, actor);
    }

    @GetMapping("/{collateralId}/insurance-policies/options")
    public Options options(@PathVariable String collateralId, Authentication actor) {
        return service.options(collateralId, actor);
    }

    @GetMapping("/{collateralId}/insurance-policies/renewal-options")
    public java.util.List<RenewalOption> renewalOptions(@PathVariable String collateralId,
            @RequestParam String insuranceTypeId, Authentication actor) {
        return service.renewalOptions(collateralId, insuranceTypeId, actor);
    }
    @PostMapping("/{collateralId}/insurance-policies/validate")
    public Validation validate(@PathVariable String collateralId,
            @RequestBody CandidateRequest request, Authentication actor) {
        return service.validate(collateralId, request, actor);
    }

    @PostMapping("/{collateralId}/insurance-policies/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestRecord request(@PathVariable String collateralId,
            @RequestBody CandidateRequest body, Authentication actor,
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        return service.request(collateralId, body, actor, key, correlation);
    }

    @GetMapping("/insurance-policies/requests/mine")
    public RequestPage mine(Authentication actor, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.mine(actor, page, size); }
}
