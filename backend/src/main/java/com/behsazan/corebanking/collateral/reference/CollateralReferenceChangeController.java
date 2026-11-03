package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceChanges.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** HQ maker/checker changes. All write paths are denied by SecurityFilterChain unless expressly enabled. */
@RestController
@RequestMapping("/api/v1/collateral/reference")
public final class CollateralReferenceChangeController {
    private final CollateralReferenceChangeService service;
    private final CollateralReferenceProperties config;
    public CollateralReferenceChangeController(CollateralReferenceChangeService service,
                                               CollateralReferenceProperties config) {
        this.service = service;
        this.config = config;
    }

    @GetMapping("/governance")
    Governance governance() {
        return new Governance(config.writesEnabled(), false, false, "REFERENCE_GOVERNANCE");
    }

    @GetMapping("/changes")
    Changes changes(@RequestParam(defaultValue="PENDING_APPROVAL") String status) {
        return service.list(status);
    }

    @PostMapping("/changes")
    Change propose(@RequestBody Proposal request, Authentication authentication,
                   @RequestHeader("Idempotency-Key") String idempotencyKey,
                   @RequestHeader(value="X-Correlation-Id", required=false) String correlation) {
        return service.propose(request, authentication, idempotencyKey, correlation);
    }

    @PostMapping("/changes/{changeId}/decisions")
    Change decide(@PathVariable String changeId, @RequestBody Decision request,
                  Authentication authentication,
                  @RequestHeader(value="X-Correlation-Id", required=false) String correlation) {
        return service.decide(changeId, request, authentication, correlation);
    }
}
