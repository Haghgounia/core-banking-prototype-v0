package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.*;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** R5A routes live within the existing CLT JWT chain; no final release or POST DML. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}/allocations")
public class CollateralUnallocationController {
    private final CollateralUnallocationService service;
    public CollateralUnallocationController(CollateralUnallocationService service){this.service=service;}
    @GetMapping("/readiness")
    public Readiness readiness(Authentication auth){return service.readiness(auth);}
    @GetMapping
    public List<AllocationLine> list(@PathVariable String collateralId,
            @RequestParam(defaultValue="20") int limit,Authentication auth){
        return service.allocations(collateralId,limit,auth);
    }
    @GetMapping("/{allocationId}/transactions")
    public List<TransactionLine> transactions(@PathVariable String collateralId,
            @PathVariable String allocationId,@RequestParam(defaultValue="20") int limit,Authentication auth){
        return service.transactions(collateralId,allocationId,limit,auth);
    }
    @PostMapping("/{allocationId}/unallocation/preview")
    public Preflight preview(@PathVariable String collateralId,@PathVariable String allocationId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader("If-Match") String etag,@RequestBody PreviewRequest request,
            Authentication auth){return service.preview(collateralId,allocationId,request,key,etag,auth);}
    @PostMapping("/{allocationId}/unallocation/service-preflight")
    public Preflight servicePreflight(@PathVariable String collateralId,
            @PathVariable String allocationId,@RequestHeader("Idempotency-Key") String key,
            @RequestHeader("If-Match") String etag,@RequestBody PreviewRequest request,
            Authentication auth){return service.servicePreflight(collateralId,allocationId,request,key,etag,auth);}
}
