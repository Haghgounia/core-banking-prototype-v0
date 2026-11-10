package com.behsazan.corebanking.collateral.posting;

import com.behsazan.corebanking.collateral.posting.CollateralPostingModels.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Service-only posting; browser may read readiness but NEVER issue a financial commit. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}/posting")
public final class CollateralPostingController {
    private final CollateralPostingService service;
    public CollateralPostingController(CollateralPostingService service){this.service=service;}
    @GetMapping("/readiness")
    public Readiness readiness(Authentication auth){return service.readiness(auth);}
    @PostMapping("/allocate")
    public Receipt allocate(@PathVariable String collateralId,@RequestHeader("Idempotency-Key") String key,
            @RequestBody CommitRequest request,Authentication auth){
        return service.commit(collateralId,request,key,"ALLOCATE",auth);
    }
    @PostMapping("/unallocate")
    public Receipt unallocate(@PathVariable String collateralId,@RequestHeader("Idempotency-Key") String key,
            @RequestBody CommitRequest request,Authentication auth){
        return service.commit(collateralId,request,key,"UNALLOCATE",auth);
    }
}
