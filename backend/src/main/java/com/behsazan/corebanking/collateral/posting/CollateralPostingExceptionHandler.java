package com.behsazan.corebanking.collateral.posting;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Bounded public messages; SQL/bank references and underlying JDBC details never escape. */
@RestControllerAdvice(assignableTypes=CollateralPostingController.class)
public final class CollateralPostingExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code",exception.getMessage()));
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String,String>> databaseUnavailable(org.springframework.dao.DataAccessException ignored){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code","POSTING_ORACLE_TRANSACTION_UNAVAILABLE_OR_ROLLED_BACK"));
    }
}
