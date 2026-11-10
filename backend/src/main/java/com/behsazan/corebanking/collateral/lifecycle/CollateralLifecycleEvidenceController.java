package com.behsazan.corebanking.collateral.lifecycle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only R3 evidence API; no transition, posting, allocation or final-domain DML. */
@RestController
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/collaterals")
public final class CollateralLifecycleEvidenceController {
    private final CollateralLifecycleEvidenceService service;
    public CollateralLifecycleEvidenceController(CollateralLifecycleEvidenceService service) {
        this.service = service;
    }

    @GetMapping({"/{collateralId}/lifecycle-evidence", "/{collateralId}/r3/lifecycle-evidence"})
    public CollateralLifecycleEvidencePolicy.Evidence evidence(@PathVariable String collateralId,
            Authentication authentication) {
        return service.evidence(collateralId, authentication);
    }
}
