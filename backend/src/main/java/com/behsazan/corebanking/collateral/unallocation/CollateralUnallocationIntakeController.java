package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationIntakeModels.*;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.PreviewRequest;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** R5B staging routes. Service-only submit/cancel/receipt, analyst-only read of journal. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}")
public class CollateralUnallocationIntakeController {
    private final CollateralUnallocationIntakeService service;
    public CollateralUnallocationIntakeController(CollateralUnallocationIntakeService service){this.service=service;}
    @GetMapping("/unallocation-requests/readiness")
    public Readiness readiness(Authentication auth){return service.readiness(auth);}
    @GetMapping("/unallocation-requests/journal")
    public List<JournalItem> journal(@PathVariable String collateralId,
            @RequestParam(defaultValue="20") int limit,Authentication auth){
        return service.journal(collateralId,limit,auth);
    }
    @PostMapping("/allocations/{allocationId}/unallocation/requests")
    public Receipt submit(@PathVariable String collateralId,@PathVariable String allocationId,
            @RequestHeader("Idempotency-Key") String key,@RequestHeader("If-Match") String etag,
            @RequestBody PreviewRequest request,Authentication auth){
        return service.submit(collateralId,allocationId,request,key,etag,auth);
    }
    /** Branch operator can stage a request without an external service JWT; NEVER changes USED. */
    @PostMapping("/allocations/{allocationId}/unallocation/analyst-requests")
    public Receipt submitAnalyst(@PathVariable String collateralId,@PathVariable String allocationId,
            @RequestHeader("Idempotency-Key") String key,@RequestHeader("If-Match") String etag,
            @RequestBody PreviewRequest request,Authentication auth) {
        return service.submitAnalyst(collateralId,allocationId,request,key,etag,auth);
    }
    @PostMapping("/allocations/{allocationId}/unallocation/analyst-requests/{requestId}/cancel")
    public Receipt cancelAnalyst(@PathVariable String collateralId,@PathVariable String allocationId,
            @PathVariable String requestId,@RequestHeader("Idempotency-Key") String key,
            @RequestHeader("If-Match") String etag,@RequestBody CancelRequest request,Authentication auth) {
        return service.cancelAnalyst(collateralId,allocationId,requestId,request,key,etag,auth);
    }
    @GetMapping("/allocations/{allocationId}/unallocation/requests/{requestId}")
    public Receipt receipt(@PathVariable String collateralId,@PathVariable String allocationId,
            @PathVariable String requestId,@RequestParam("consumerDomain") String consumerDomain,Authentication auth){
        return service.byId(collateralId,allocationId,requestId,consumerDomain,auth);
    }
    @PostMapping("/allocations/{allocationId}/unallocation/requests/{requestId}/cancel")
    public Receipt cancel(@PathVariable String collateralId,@PathVariable String allocationId,
            @PathVariable String requestId,@RequestHeader("Idempotency-Key") String key,
            @RequestHeader("If-Match") String etag,@RequestBody CancelRequest body,
            Authentication auth){return service.cancel(collateralId,allocationId,requestId,body,key,etag,auth);}
}
