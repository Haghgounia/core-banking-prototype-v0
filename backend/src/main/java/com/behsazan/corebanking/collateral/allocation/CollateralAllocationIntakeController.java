package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationIntakeModels.*;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.Request;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Bank-to-bank intake is separate from browser preview and never allocates collateral. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}/allocation-requests/intake")
public class CollateralAllocationIntakeController {
    private final CollateralAllocationIntakeService service;
    public CollateralAllocationIntakeController(CollateralAllocationIntakeService service) {this.service=service;}
    @PostMapping
    public Receipt submit(@PathVariable String collateralId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody Request request, Authentication auth) {
        return service.submit(collateralId,request,key,auth);
    }
    /** Local analyst Stage: scoped to the app's loopback bridge; never a consumer instruction. */
    @PostMapping("/analyst")
    public Receipt submitAnalyst(@PathVariable String collateralId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody Request request,Authentication auth) {
        return service.submitAnalyst(collateralId,request,key,auth);
    }
    @GetMapping("/readiness")
    public Readiness readiness(Authentication auth) {return service.readiness(auth);}
    @GetMapping("/journal")
    public List<JournalItem> journal(@PathVariable String collateralId,
            @RequestParam(defaultValue="20") int limit, Authentication auth) {
        return service.journal(collateralId,limit,auth);
    }
    @GetMapping("/receipts/{requestId}")
    public Receipt receipt(@PathVariable String collateralId,@PathVariable String requestId,
            @RequestParam String consumerDomain, Authentication auth) {
        return service.receiptById(collateralId,requestId,consumerDomain,auth);
    }
}
