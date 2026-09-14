package com.behsazan.corebanking.deposit.opening.web;

import com.behsazan.corebanking.deposit.account.application.DepositAccountLifecycleService;
import com.behsazan.corebanking.deposit.account.domain.DepositAccountModels.AccountLifecycleResponse;
import com.behsazan.corebanking.deposit.opening.application.DepositOpeningAggregateService;
import com.behsazan.corebanking.deposit.opening.audit.application.DepositOpeningAuditService;
import com.behsazan.corebanking.deposit.opening.audit.domain.DepositOpeningAuditModels.*;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.AggregateRequest;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.PersistedAggregateResponse;
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

    public DepositOpeningController(
            DepositOpeningAggregateService service,
            DepositAccountLifecycleService accountLifecycleService,
            DepositOpeningAuditService auditService
    ) {
        this.service = service;
        this.accountLifecycleService = accountLifecycleService;
        this.auditService = auditService;
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
