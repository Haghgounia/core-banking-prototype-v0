package com.behsazan.corebanking.collateral.insurance.cancellation;

import com.behsazan.corebanking.collateral.insurance.cancellation.CollateralInsuranceCancellationPolicy.Command;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** R3/G-06 routes are branch-scoped and strictly gated by the service and Oracle evidence. */
@RestController
@ConditionalOnProperty(prefix="core-banking.collateral.prototype", name="enabled", havingValue="true")
@RequestMapping("/api/v1/collaterals")
public final class CollateralInsuranceCancellationController {
    private final CollateralInsuranceCancellationService service;
    public CollateralInsuranceCancellationController(CollateralInsuranceCancellationService service) {
        this.service=service;
    }
    @GetMapping("/{collateralId}/insurance-policies/{insuranceId}/cancellation-readiness")
    public CollateralInsuranceCancellationService.Readiness readiness(
            @PathVariable String collateralId,@PathVariable String insuranceId,Authentication auth) {
        return service.preview(collateralId,insuranceId,auth);
    }
    @GetMapping("/insurance-policies/cancellation-activation")
    public CollateralInsuranceCancellationService.Activation activation(Authentication auth) {
        return service.activation(auth);
    }
    @PostMapping("/{collateralId}/insurance-policies/{insuranceId}/cancellation-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public CollateralInsuranceCancellationService.StageRow request(
            @PathVariable String collateralId,@PathVariable String insuranceId,
            @RequestBody CollateralInsuranceCancellationService.NewRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,Authentication auth) {
        return service.create(collateralId,insuranceId,body,idempotencyKey,auth);
    }
    @GetMapping("/insurance-policies/cancellation-requests/mine")
    public CollateralInsuranceCancellationService.StagePage mine(
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            Authentication auth) { return service.mine(page,size,auth); }
    @GetMapping("/insurance-policies/cancellation-requests/pending")
    public CollateralInsuranceCancellationService.StagePage pending(
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            Authentication auth) { return service.pending(page,size,auth); }
    @PostMapping("/insurance-policies/cancellation-requests/{requestId}/transition")
    public CollateralInsuranceCancellationService.StageRow transition(
            @PathVariable String requestId,
            @RequestBody CollateralInsuranceCancellationService.Transition body,Authentication auth) {
        return service.transition(requestId,body,auth);
    }
    @GetMapping("/insurance-policies/cancellation-requests/{requestId}/receipt")
    public CollateralInsuranceCancellationService.Receipt receipt(
            @PathVariable String requestId,Authentication auth) {return service.receipt(requestId,auth);}
    @PostMapping("/insurance-policies/cancellation-requests/{requestId}/commit")
    public CollateralInsuranceCancellationService.Receipt commit(
            @PathVariable String requestId,@RequestBody Command body,Authentication auth) {
        return service.commit(requestId,body,auth);
    }
}
