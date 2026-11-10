package com.behsazan.corebanking.collateral.reintroduction;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Invalid business identifiers do not become a server error or expose cross-branch records. */
@RestControllerAdvice(assignableTypes=CollateralReintroductionController.class)
public final class CollateralReintroductionExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code",ex.getMessage()==null?"REINTRO_INVALID_ARGUMENT":ex.getMessage()));
    }
}
