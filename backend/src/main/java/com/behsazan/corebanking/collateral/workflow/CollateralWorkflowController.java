package com.behsazan.corebanking.collateral.workflow;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** R3 Stage-only maker/reviewer workflow; no final-domain DML. */
@RestController
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
@RequestMapping({"/api/v1/collaterals/workflow", "/api/v1/collaterals/r3-workflow"})
public class CollateralWorkflowController {
    private final CollateralWorkflowService workflow;
    private final CollateralPostingReadinessService posting;
    private final CollateralSponsorCommitService sponsorCommit;
    private final CollateralInsuranceCommitService insuranceCommit;
    private final CollateralValuationCommitService valuationCommit;
    public CollateralWorkflowController(CollateralWorkflowService workflow,
            CollateralPostingReadinessService posting,
            CollateralSponsorCommitService sponsorCommit,
            CollateralInsuranceCommitService insuranceCommit,
            CollateralValuationCommitService valuationCommit) {
        this.workflow = workflow;
        this.posting = posting;
        this.sponsorCommit = sponsorCommit;
        this.insuranceCommit = insuranceCommit;
        this.valuationCommit = valuationCommit;
    }
    public record Transition(String action, String expectedVersion, String reason) {}

    @GetMapping("/{kind}/readiness")
    public CollateralWorkflowService.Readiness readiness(@PathVariable String kind, Authentication auth) {
        return workflow.readiness(kind, auth);
    }
    @GetMapping("/{kind}/pending")
    public CollateralWorkflowService.PendingPage pending(@PathVariable String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return workflow.pending(kind, page, size, auth);
    }
    /** Non-mutating posting preflight for an Oracle Stage request; never grants final DML. */
    @GetMapping("/{kind}/{requestId}/posting-readiness")
    public CollateralPostingReadinessPolicy.Readiness postingReadiness(@PathVariable String kind,
            @PathVariable String requestId, Authentication auth) {
        return posting.inspect(kind, requestId, auth);
    }
    /** Sponsor operational insert is disabled by default and additionally requires DBA/bank release. */
    @GetMapping("/sponsor/posting-activation")
    public CollateralSponsorCommitService.Activation sponsorActivation(Authentication auth) {
        return sponsorCommit.activation(auth);
    }
    @GetMapping("/sponsor/{requestId}/receipt")
    public CollateralSponsorCommitService.PostedEvidence sponsorReceipt(
            @PathVariable String requestId, Authentication auth) {
        return sponsorCommit.receipt(requestId, auth);
    }
    @PostMapping("/sponsor/{requestId}/commit")
    public CollateralSponsorCommitPolicy.Receipt sponsorCommit(
            @PathVariable String requestId,
            @RequestBody CollateralSponsorCommitPolicy.Command body,
            Authentication auth) {
        return sponsorCommit.commit(requestId, body, auth);
    }
    /** Bank-gated creation / renewal only. Cancellation is not silently inferred or applied. */
    @GetMapping("/insurance/posting-activation")
    public CollateralInsuranceCommitService.Activation insuranceActivation(Authentication auth) {
        return insuranceCommit.activation(auth);
    }
    @GetMapping("/insurance/{requestId}/receipt")
    public CollateralInsuranceCommitService.PostedEvidence insuranceReceipt(
            @PathVariable String requestId, Authentication auth) {
        return insuranceCommit.receipt(requestId, auth);
    }
    @PostMapping("/insurance/{requestId}/commit")
    public CollateralInsuranceCommitPolicy.Receipt insuranceCommit(
            @PathVariable String requestId,
            @RequestBody CollateralInsuranceCommitPolicy.Command body, Authentication auth) {
        return insuranceCommit.commit(requestId, body, auth);
    }
    /** Bank-gated initial/follow-up valuation, preserving Oracle history and pointer. */
    @GetMapping("/valuation/posting-activation")
    public CollateralValuationCommitService.Activation valuationActivation(Authentication auth) {
        return valuationCommit.activation(auth);
    }
    @GetMapping("/valuation/{requestId}/receipt")
    public CollateralValuationCommitService.PostedEvidence valuationReceipt(
            @PathVariable String requestId, Authentication auth) {
        return valuationCommit.receipt(requestId, auth);
    }
    @PostMapping("/valuation/{requestId}/commit")
    public CollateralValuationCommitPolicy.Receipt valuationCommit(
            @PathVariable String requestId,
            @RequestBody CollateralValuationCommitPolicy.Command body, Authentication auth) {
        return valuationCommit.commit(requestId, body, auth);
    }
    @PostMapping("/{kind}/{requestId}/transition")
    public CollateralWorkflowService.StageRow transition(@PathVariable String kind,
            @PathVariable String requestId, @RequestBody Transition body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication auth) {
        return workflow.transition(kind, requestId, body, correlation, auth);
    }
}
