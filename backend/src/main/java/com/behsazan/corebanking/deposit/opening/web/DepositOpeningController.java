package com.behsazan.corebanking.deposit.opening.web;

import com.behsazan.corebanking.deposit.account.application.DepositAccountLifecycleService;
import com.behsazan.corebanking.deposit.account.domain.DepositAccountModels.AccountLifecycleResponse;
import com.behsazan.corebanking.deposit.opening.application.DepositOpeningAggregateService;
import com.behsazan.corebanking.deposit.opening.batch.application.DepositOpeningBatchService;
import com.behsazan.corebanking.deposit.opening.batch.domain.DepositOpeningBatchModels.*;
import com.behsazan.corebanking.deposit.opening.audit.application.DepositOpeningAuditService;
import com.behsazan.corebanking.deposit.opening.audit.domain.DepositOpeningAuditModels.*;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.AggregateRequest;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.PersistedAggregateResponse;
import com.behsazan.corebanking.deposit.opening.operational.application.DepositOpeningOperationalService;
import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.ActivationReadinessResponse;
import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.ReadinessEvaluationRequest;
import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.SettlementRequest;
import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.SettlementResponse;
import com.behsazan.corebanking.deposit.opening.readiness.application.DepositOpeningReadinessService;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.ReadinessReport;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.RollbackProbeResult;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.RuntimeValidationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/deposit-opening")
public class DepositOpeningController {
    private final DepositOpeningAggregateService service;
    private final DepositAccountLifecycleService accountLifecycleService;
    private final DepositOpeningAuditService auditService;
    private final DepositOpeningBatchService batchService;
    private final DepositOpeningReadinessService readinessService;
    private final DepositOpeningOperationalService operationalService;

    public DepositOpeningController(
            DepositOpeningAggregateService service,
            DepositAccountLifecycleService accountLifecycleService,
            DepositOpeningAuditService auditService,
            DepositOpeningBatchService batchService,
            DepositOpeningReadinessService readinessService,
            DepositOpeningOperationalService operationalService
    ) {
        this.service = service;
        this.accountLifecycleService = accountLifecycleService;
        this.auditService = auditService;
        this.batchService = batchService;
        this.readinessService = readinessService;
        this.operationalService = operationalService;
    }

    @PostMapping("/batches")
    ResponseEntity<BatchView> createBatch(
            @RequestBody BatchCreateRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor
    ) {
        BatchView result = batchService.create(request, normalizedActor(actor));
        if (result.idempotentReplay()) return ResponseEntity.ok(result);
        return ResponseEntity.created(URI.create("/api/v1/deposit-opening/batches/" + result.batch().openingBatchId())).body(result);
    }

    @GetMapping("/batches/{batchId}")
    ResponseEntity<BatchView> getBatch(@PathVariable("batchId") long batchId) {
        return ResponseEntity.ok(batchService.get(batchId));
    }

    @PostMapping("/batches/{batchId}/validate")
    ResponseEntity<BatchView> validateBatch(
            @PathVariable("batchId") long batchId,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor
    ) {
        return ResponseEntity.ok(batchService.validate(batchId, normalizedActor(actor)));
    }

    @PostMapping("/batches/{batchId}/refresh")
    ResponseEntity<BatchView> refreshBatch(
            @PathVariable("batchId") long batchId,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor
    ) {
        return ResponseEntity.ok(batchService.refresh(batchId, normalizedActor(actor)));
    }

    @PostMapping("/batches/{batchId}/process")
    ResponseEntity<BatchView> processBatch(
            @PathVariable("batchId") long batchId,
            @RequestBody BatchProcessRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(batchService.process(
                batchId, request, normalizedActor(actor), DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @PostMapping("/batches/{batchId}/activate")
    ResponseEntity<BatchView> activateBatch(
            @PathVariable("batchId") long batchId,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(batchService.activate(
                batchId, normalizedActor(actor), DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @PostMapping("/requests/validate")
    ResponseEntity<RuntimeValidationResponse> validateRequest(@RequestBody AggregateRequest request) {
        return ResponseEntity.ok(service.validateRuntime(request));
    }

    @GetMapping("/readiness")
    ResponseEntity<ReadinessReport> readiness() {
        return ResponseEntity.ok(readinessService.readiness());
    }

    @PostMapping("/readiness/rollback-probe")
    ResponseEntity<RollbackProbeResult> rollbackProbe() {
        return ResponseEntity.ok(readinessService.rollbackProbe());
    }

    @PostMapping("/requests")
    ResponseEntity<PersistedAggregateResponse> create(
            @RequestBody AggregateRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        PersistedAggregateResponse result = service.create(
                request,
                actor == null || actor.isBlank() ? "opening.operator" : actor.trim(),
                DepositOpeningAggregateService.correlationId(correlationId)
        );
        if (result.idempotentReplay()) return ResponseEntity.ok(result);
        return ResponseEntity.created(URI.create("/api/v1/deposit-opening/requests/" + result.openingRequestId())).body(result);
    }
    @PostMapping("/requests/{id}/account")
    ResponseEntity<AccountLifecycleResponse> createAccount(
            @PathVariable("id") long openingRequestId,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        AccountLifecycleResponse result = accountLifecycleService.createAccount(
                openingRequestId, normalizedActor(actor), DepositOpeningAggregateService.correlationId(correlationId)
        );
        if (result.idempotentReplay()) return ResponseEntity.ok(result);
        return ResponseEntity.created(URI.create(
                "/api/v1/deposit-opening/requests/" + openingRequestId + "/account"
        )).body(result);
    }

    @PostMapping({"/requests/{id}/account/settlement", "/requests/{id}/account/settle"})
    ResponseEntity<SettlementResponse> settleOpening(
            @PathVariable("id") long openingRequestId,
            @RequestBody SettlementRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(operationalService.settle(
                openingRequestId, request, normalizedActor(actor),
                DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @PostMapping("/requests/{id}/account/readiness")
    ResponseEntity<ActivationReadinessResponse> evaluateActivationReadiness(
            @PathVariable("id") long openingRequestId,
            @RequestBody(required = false) ReadinessEvaluationRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(operationalService.evaluateReadiness(
                openingRequestId, request, normalizedActor(actor),
                DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @GetMapping("/requests/{id}/account/readiness")
    ResponseEntity<ActivationReadinessResponse> getActivationReadiness(
            @PathVariable("id") long openingRequestId
    ) {
        return ResponseEntity.ok(operationalService.getReadiness(openingRequestId));
    }

    @PostMapping("/requests/{id}/account/activate")
    ResponseEntity<AccountLifecycleResponse> activateAccount(
            @PathVariable("id") long openingRequestId,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(accountLifecycleService.activateAccount(
                openingRequestId, normalizedActor(actor), DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @GetMapping("/requests/{id}/account")
    ResponseEntity<AccountLifecycleResponse> getAccount(@PathVariable("id") long openingRequestId) {
        return ResponseEntity.ok(accountLifecycleService.getAccount(openingRequestId));
    }


    @PostMapping("/requests/{id}/change-sets")
    ResponseEntity<ChangeSetView> createChangeSet(
            @PathVariable("id") long openingRequestId,
            @RequestBody ChangeSetCreateRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(auditService.createChangeSet(
                openingRequestId, request, normalizedActor(actor),
                DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @PostMapping("/requests/{id}/change-sets/{changeSetId}/approve")
    ResponseEntity<ChangeSetView> approveChangeSet(
            @PathVariable("id") long openingRequestId,
            @PathVariable("changeSetId") long changeSetId,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.approver") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(auditService.approveChangeSet(
                openingRequestId, changeSetId, normalizedActor(actor),
                DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @PostMapping("/requests/{id}/change-sets/{changeSetId}/reject")
    ResponseEntity<ChangeSetView> rejectChangeSet(
            @PathVariable("id") long openingRequestId,
            @PathVariable("changeSetId") long changeSetId,
            @RequestBody(required = false) ChangeSetActionRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.approver") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(auditService.rejectChangeSet(
                openingRequestId, changeSetId, request, normalizedActor(actor),
                DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @PostMapping("/requests/{id}/change-sets/{changeSetId}/apply")
    ResponseEntity<ChangeSetApplyResponse> applyChangeSet(
            @PathVariable("id") long openingRequestId,
            @PathVariable("changeSetId") long changeSetId,
            @RequestBody ApplyChangeSetRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "opening.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(auditService.applyChangeSet(
                openingRequestId, changeSetId, request, normalizedActor(actor),
                DepositOpeningAggregateService.correlationId(correlationId)
        ));
    }

    @GetMapping("/requests/{id}/audit-trail")
    ResponseEntity<AuditTrailResponse> getAuditTrail(@PathVariable("id") long openingRequestId) {
        return ResponseEntity.ok(auditService.auditTrail(openingRequestId));
    }

    @GetMapping("/requests/{id}/snapshots/{snapshotId}")
    ResponseEntity<SnapshotDocument> getSnapshot(
            @PathVariable("id") long openingRequestId,
            @PathVariable("snapshotId") long snapshotId
    ) {
        return ResponseEntity.ok(auditService.snapshot(openingRequestId, snapshotId));
    }

    private static String normalizedActor(String actor) {
        return actor == null || actor.isBlank() ? "opening.operator" : actor.trim();
    }

}
