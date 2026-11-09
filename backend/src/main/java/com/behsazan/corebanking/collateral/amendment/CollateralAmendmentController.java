package com.behsazan.corebanking.collateral.amendment;

import com.behsazan.corebanking.collateral.amendment.CollateralAmendmentModels.*;
import com.behsazan.corebanking.collateral.amendment.CollateralAmendmentModels.Record;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Real Oracle-backed G-01 proposal; this route never creates a new collateral. */
@RestController
@RequestMapping("/api/v1/collaterals")
public class CollateralAmendmentController {
    private final CollateralAmendmentService service;
    public CollateralAmendmentController(CollateralAmendmentService service) {this.service=service;}

    @GetMapping("/amendments/readiness")
    Readiness readiness(Authentication authentication){return service.readiness(authentication);}

    @GetMapping("/{collateralId}/amendments")
    ListResult list(@PathVariable String collateralId,Authentication authentication){
        return service.list(collateralId,authentication);
    }

    @PostMapping("/{collateralId}/amendments")
    @ResponseStatus(HttpStatus.CREATED)
    Record propose(@PathVariable String collateralId,@RequestBody Proposal request,Authentication authentication,
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlation){
        return service.propose(collateralId,request,authentication,key,correlation);
    }

    @PostMapping("/{collateralId}/amendments/{amendmentId}/decision")
    Record decide(@PathVariable String collateralId,@PathVariable String amendmentId,
            @RequestBody Decision request,Authentication authentication,
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlation){
        return service.decide(collateralId,amendmentId,request,authentication,key,correlation);
    }
}
