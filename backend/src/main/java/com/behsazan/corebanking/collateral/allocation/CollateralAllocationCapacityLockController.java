package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationCapacityLockModels.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Bank service diagnostic; never called from the analyst browser for locking. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}/allocation-requests/lock")
public class CollateralAllocationCapacityLockController {
    private final CollateralAllocationCapacityLockService service;
    public CollateralAllocationCapacityLockController(CollateralAllocationCapacityLockService service) {
        this.service=service;
    }
    @GetMapping("/readiness")
    public Readiness readiness(Authentication auth) {return service.readiness(auth);}
    @PostMapping("/probes/{requestId}")
    public Probe probe(@PathVariable String collateralId,@PathVariable String requestId,
            @RequestParam String consumerDomain,Authentication auth) {
        return service.inspect(collateralId,requestId,consumerDomain,auth);
    }
}
