package com.behsazan.corebanking.collateral.workflow;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** R3 Stage-only maker/reviewer workflow; no final-domain DML. */
@RestController
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/collaterals/r3-workflow")
public class CollateralR3WorkflowController {
    private final CollateralR3WorkflowService workflow;
    private final CollateralR3PostingReadinessService posting;
    private final CollateralR3SponsorCommitService sponsorCommit;
    public CollateralR3WorkflowController(CollateralR3WorkflowService workflow,
            CollateralR3PostingReadinessService posting,
            CollateralR3SponsorCommitService sponsorCommit) {
        this.workflow = workflow;
        this.posting = posting;
        this.sponsorCommit = sponsorCommit;
    }
    public record Transition(String action, String expectedVersion, String reason) {}

    @GetMapping("/{kind}/readiness")
    public CollateralR3WorkflowService.Readiness readiness(@PathVariable String kind, Authentication auth) {
        return workflow.readiness(kind, auth);
    }
    @GetMapping("/{kind}/pending")
    public CollateralR3WorkflowService.PendingPage pending(@PathVariable String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return workflow.pending(kind, page, size, auth);
    }
    /** Non-mutating posting preflight for an Oracle Stage request; never grants final DML. */
    @GetMapping("/{kind}/{requestId}/posting-readiness")
    public CollateralR3PostingReadinessPolicy.Readiness postingReadiness(@PathVariable String kind,
            @PathVariable String requestId, Authentication auth) {
        return posting.inspect(kind, requestId, auth);
    }
    /** Sponsor operational insert is disabled by default and additionally requires DBA/bank release. */
    @GetMapping("/sponsor/posting-activation")
    public CollateralR3SponsorCommitService.Activation sponsorActivation(Authentication auth) {
        return sponsorCommit.activation(auth);
    }
    @GetMapping("/sponsor/{requestId}/receipt")
    public CollateralR3SponsorCommitService.PostedEvidence sponsorReceipt(
            @PathVariable String requestId, Authentication auth) {
        return sponsorCommit.receipt(requestId, auth);
    }
    @PostMapping("/sponsor/{requestId}/commit")
    public CollateralR3SponsorCommitPolicy.Receipt sponsorCommit(
            @PathVariable String requestId,
            @RequestBody CollateralR3SponsorCommitPolicy.Command body,
            Authentication auth) {
        return sponsorCommit.commit(requestId, body, auth);
    }
    @PostMapping("/{kind}/{requestId}/transition")
    public CollateralR3WorkflowService.StageRow transition(@PathVariable String kind,
            @PathVariable String requestId, @RequestBody Transition body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication auth) {
        return workflow.transition(kind, requestId, body, correlation, auth);
    }
}
