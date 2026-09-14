package com.behsazan.corebanking.deposit.opening.web;

import com.behsazan.corebanking.deposit.opening.application.DepositOpeningAggregateService;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.AggregateRequest;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.PersistedAggregateResponse;
import org.springframework.http.ResponseEntity;
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

    public DepositOpeningController(DepositOpeningAggregateService service) {
        this.service = service;
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
}
