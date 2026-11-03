package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** New endpoints are under the existing /api/v1/collaterals matcher and JWT security. */
@RestController
@RequestMapping("/api/v1/collaterals/introduction-drafts")
public final class CollateralIntroductionController {
    private final CollateralIntroductionService service;
    public CollateralIntroductionController(CollateralIntroductionService service) { this.service = service; }

    @GetMapping("/readiness")
    Readiness readiness(Authentication authentication) { return service.readiness(authentication); }

    @PostMapping("/validate")
    Validation validate(@RequestBody Request request, Authentication authentication) {
        return service.validate(request, authentication);
    }

    @PostMapping("/registration-preflight")
    RegistrationReview registrationPreflight(@RequestBody Request request, Authentication authentication) {
        return service.registrationPreflight(request, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Draft create(@RequestBody Request request, Authentication authentication,
                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                 @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return service.createDraft(request, authentication, idempotencyKey, correlationId);
    }

    @GetMapping("/{draftId}")
    Draft byId(@PathVariable String draftId, Authentication authentication) {
        return service.getDraft(authentication, draftId);
    }
}
