package com.behsazan.corebanking.collateral.reintroduction;

import com.behsazan.corebanking.collateral.reintroduction.CollateralReintroductionModels.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** BUC-12 does not reactivate or overwrite the released collateral. */
@RestController
@RequestMapping("/api/v1/collaterals/{previousCollateralId}/reintroduction")
public final class CollateralReintroductionController {
    private final CollateralReintroductionService service;
    public CollateralReintroductionController(CollateralReintroductionService service) {this.service=service;}
    @GetMapping("/readiness")
    public Readiness readiness(Authentication actor) {return service.readiness(actor);}
    @GetMapping("/evidence")
    public Evidence evidence(@PathVariable String previousCollateralId,Authentication actor) {
        return service.evidence(previousCollateralId,actor);
    }
    @GetMapping("/approved-permits")
    public Permits permits(@PathVariable String previousCollateralId,Authentication actor) {
        return service.permits(previousCollateralId,actor);
    }
    @GetMapping("/lineage")
    public Lineage lineage(@PathVariable String previousCollateralId,Authentication actor) {
        return service.lineage(previousCollateralId,actor);
    }
    @PostMapping("/commit")
    public Receipt commit(@PathVariable String previousCollateralId,
            @RequestBody FinalizeCommand command,
            @RequestHeader("If-Match") String expectedVersion,
            @RequestHeader("Idempotency-Key") String requestKey,
            Authentication actor) {
        return service.commit(previousCollateralId,command,expectedVersion,requestKey,actor);
    }
}
