package com.behsazan.corebanking.collateral.crossunit;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes=CollateralCrossUnitController.class)
public final class CollateralCrossUnitExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code","CLT_CROSS_UNIT_INVALID_REQUEST","message",ex.getMessage()));
    }
}
