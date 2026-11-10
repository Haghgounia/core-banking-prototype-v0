package com.behsazan.corebanking.collateral.release;

import com.behsazan.corebanking.collateral.release.CollateralReleasePostingModels.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** BUC-11 final release; no client-provided legal/consumer attestation is accepted. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}/final-release")
public final class CollateralReleasePostingController {
    private final CollateralReleasePostingService service;
    public CollateralReleasePostingController(CollateralReleasePostingService service){this.service=service;}
    @GetMapping("/posting-readiness")
    public Readiness readiness(Authentication auth){return service.readiness(auth);}
    @GetMapping("/approved-permits")
    public Permits approved(@PathVariable String collateralId,Authentication auth){return service.permits(collateralId,auth);}
    @PostMapping("/commit")
    @ResponseStatus(HttpStatus.OK)
    public Receipt commit(@PathVariable String collateralId,@RequestBody FinalizeCommand command,
            @RequestHeader("Idempotency-Key") String key,@RequestHeader("If-Match") String etag,
            Authentication auth){return service.commit(collateralId,command,etag,key,auth);}
}
