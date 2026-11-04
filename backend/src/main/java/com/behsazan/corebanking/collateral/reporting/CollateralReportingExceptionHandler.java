package com.behsazan.corebanking.collateral.reporting;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CollateralReportingController.class)
public class CollateralReportingExceptionHandler {
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String,String>> databaseError(DataAccessException ignored) {
        // Suppress SQL/connection details from user-visible error responses.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "CLT_REPORT_READ_UNAVAILABLE"));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ignored) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "CLT_REPORT_INVALID_REQUEST"));
    }
}
