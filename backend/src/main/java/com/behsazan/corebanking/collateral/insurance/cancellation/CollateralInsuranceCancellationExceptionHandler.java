package com.behsazan.corebanking.collateral.insurance.cancellation;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes=CollateralInsuranceCancellationController.class)
public final class CollateralInsuranceCancellationExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badInput(IllegalArgumentException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code","CLT_R3_CANCEL_INVALID_INPUT",
                        "message",error.getMessage()==null?"INVALID_INPUT":error.getMessage()));
    }
}
