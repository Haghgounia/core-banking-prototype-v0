package com.behsazan.corebanking.collateral.capacity;

import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Read-only branch-scoped SVC-09 evidence and non-authorizing scenario comparison. */
@RestController
@RequestMapping("/api/v1/collaterals")
public class CollateralCapacityController {
    private final CollateralCapacityService service;
    public CollateralCapacityController(CollateralCapacityService service) {this.service = service;}

    @GetMapping("/{collateralId}/capacity")
    public CapacityEvidence evidence(@PathVariable String collateralId, Authentication auth) {
        return service.evidence(collateralId, auth);
    }
    @PostMapping("/{collateralId}/capacity/preview")
    public PreviewResult preview(@PathVariable String collateralId, @RequestBody PreviewRequest request,
            Authentication auth) {
        return service.preview(collateralId, request, auth);
    }
}
