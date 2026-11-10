package com.behsazan.corebanking.collateral.crossunit;

import com.behsazan.corebanking.collateral.crossunit.CollateralCrossUnitModels.*;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Exact lookup only; no broad cross-branch enumerations or unapproved allocations. */
@RestController
@RequestMapping("/api/v1/collaterals/cross-unit")
public final class CollateralCrossUnitController {
    private final CollateralCrossUnitService service;
    public CollateralCrossUnitController(CollateralCrossUnitService service){this.service=service;}
    @GetMapping("/readiness")
    public Readiness readiness(Authentication actor){return service.readiness(actor);}
    @GetMapping("/by-registration/{registrationNumber}")
    public Eligibility inquire(@PathVariable String registrationNumber,Authentication actor){
        return service.inquire(registrationNumber,actor);
    }
    @GetMapping("/owner/{collateralId}/grants")
    public Grants grants(@PathVariable String collateralId,Authentication actor){
        return service.grants(collateralId,actor);
    }
    @PostMapping("/owner/{collateralId}/grants")
    public Grant request(@PathVariable String collateralId,@RequestBody GrantCommand input,
            @RequestHeader("Idempotency-Key") String key,Authentication actor){
        return service.requestGrant(collateralId,input,key,actor);
    }
    @PostMapping("/owner/{collateralId}/grants/{grantId}/decision")
    public Grant decision(@PathVariable String collateralId,@PathVariable String grantId,
            @RequestBody GrantDecision input,@RequestHeader("Idempotency-Key") String key,
            Authentication actor){return service.decide(collateralId,grantId,input,key,actor);}
    @PostMapping("/owner/{collateralId}/grants/{grantId}/revoke")
    public Grant revoke(@PathVariable String collateralId,@PathVariable String grantId,
            @RequestBody RevokeCommand input,Authentication actor){
        return service.revoke(collateralId,grantId,input,actor);
    }
    @PostMapping("/intents/analyst")
    public IntentReceipt stageAnalyst(@RequestBody IntentCommand input,
            @RequestHeader("Idempotency-Key") String key,Authentication actor){
        return service.stageAnalyst(input,key,actor);
    }
    @PostMapping("/intents/consumer")
    public IntentReceipt stageConsumer(@RequestBody IntentCommand input,
            @RequestHeader("Idempotency-Key") String key,Authentication actor){
        return service.stageConsumer(input,key,actor);
    }
    @GetMapping("/intents")
    public List<IntentReceipt> intents(Authentication actor){return service.intents(actor);}
}
