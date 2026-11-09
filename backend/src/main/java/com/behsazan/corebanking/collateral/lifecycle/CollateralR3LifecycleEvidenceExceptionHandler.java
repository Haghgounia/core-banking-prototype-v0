package com.behsazan.corebanking.collateral.lifecycle;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CollateralR3LifecycleEvidenceController.class)
public final class CollateralR3LifecycleEvidenceExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badId(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", "CLT_R3_EVIDENCE_INVALID_REQUEST", "message",
                exception.getMessage() == null ? "INVALID_REQUEST" : exception.getMessage()));
    }
}
