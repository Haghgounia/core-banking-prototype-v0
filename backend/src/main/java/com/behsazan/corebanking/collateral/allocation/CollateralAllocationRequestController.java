package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Two explicit non-writing contracts. No POST /allocations/execute endpoint exists. */
@RestController
@RequestMapping("/api/v1/collaterals/{collateralId}/allocation-requests")
public class CollateralAllocationRequestController {
    private final CollateralAllocationRequestService service;
    public CollateralAllocationRequestController(CollateralAllocationRequestService service) {this.service = service;}

    @PostMapping("/preview")
    public Preflight analystPreview(@PathVariable String collateralId,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @RequestBody Request request, Authentication auth) {
        return service.preview(collateralId, request, key, auth);
    }
    @PostMapping("/service-preflight")
    public Preflight servicePreflight(@PathVariable String collateralId,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @RequestBody Request request, Authentication auth) {
        return service.servicePreflight(collateralId, request, key, auth);
    }
}
