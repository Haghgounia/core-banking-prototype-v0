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
    public CollateralR3WorkflowController(CollateralR3WorkflowService workflow) {
        this.workflow = workflow;
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
    @PostMapping("/{kind}/{requestId}/transition")
    public CollateralR3WorkflowService.StageRow transition(@PathVariable String kind,
            @PathVariable String requestId, @RequestBody Transition body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation,
            Authentication auth) {
        return workflow.transition(kind, requestId, body, correlation, auth);
    }
}
