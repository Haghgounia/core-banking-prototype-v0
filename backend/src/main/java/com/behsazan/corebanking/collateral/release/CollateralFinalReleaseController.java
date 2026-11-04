package com.behsazan.corebanking.collateral.release;

import com.behsazan.corebanking.collateral.release.CollateralFinalReleaseModels.*;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Final release diagnostics; no POST mutation route exists. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}/final-release")
public class CollateralFinalReleaseController {
    private final CollateralFinalReleaseService service;
    public CollateralFinalReleaseController(CollateralFinalReleaseService service){this.service=service;}
    @GetMapping("/readiness")
    public Readiness readiness(Authentication auth){return service.readiness(auth);}
    @GetMapping("/evidence")
    public Evidence evidence(@PathVariable String collateralId,Authentication auth){return service.evidence(collateralId,auth);}
    @GetMapping("/status-history")
    public List<StatusEvent> history(@PathVariable String collateralId,
            @RequestParam(defaultValue="20") int limit,Authentication auth){
        return service.history(collateralId,limit,auth);
    }
    @PostMapping("/preview")
    public PreviewResult preview(@PathVariable String collateralId,
            @RequestHeader("Idempotency-Key") String key,@RequestHeader("If-Match") String etag,
            @RequestBody PreviewRequest request,Authentication auth){
        return service.preview(collateralId,request,etag,key,auth);
    }
}
