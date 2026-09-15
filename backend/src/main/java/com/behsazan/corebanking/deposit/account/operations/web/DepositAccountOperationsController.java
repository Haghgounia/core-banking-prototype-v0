package com.behsazan.corebanking.deposit.account.operations.web;

import com.behsazan.corebanking.deposit.account.operations.application.DepositAccountOperationsService;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountDetails;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountSearchResponse;
import com.behsazan.corebanking.deposit.account.servicing.application.DepositAccountServicingService;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountServicingModels.CloseAccountRequest;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountServicingModels.CloseAccountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deposit-accounts")
public class DepositAccountOperationsController {
    private final DepositAccountOperationsService service;
    private final DepositAccountServicingService servicingService;

    public DepositAccountOperationsController(
            DepositAccountOperationsService service,
            DepositAccountServicingService servicingService
    ) {
        this.service = service;
        this.servicingService = servicingService;
    }

    @GetMapping
    public ResponseEntity<AccountSearchResponse> search(
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long openingRequestId,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) String productFamilyCode,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(service.search(
                accountNo, status, openingRequestId, partyId, productFamilyCode, offset, limit
        ));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDetails> get(@PathVariable long accountId) {
        return ResponseEntity.ok(service.get(accountId));
    }

    @PostMapping("/{accountId}/close")
    public ResponseEntity<CloseAccountResponse> close(
            @PathVariable long accountId,
            @RequestBody CloseAccountRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "deposit.operator") String actor,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(servicingService.close(
                accountId,
                request,
                normalizeActor(actor),
                normalizeCorrelationId(correlationId)
        ));
    }

    private static String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "deposit.operator" : actor.trim();
    }

    private static String normalizeCorrelationId(String correlationId) {
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId.trim();
    }
}
