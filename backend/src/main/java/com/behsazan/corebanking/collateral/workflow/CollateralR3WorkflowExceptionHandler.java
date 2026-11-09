package com.behsazan.corebanking.collateral.workflow;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CollateralR3WorkflowController.class)
public class CollateralR3WorkflowExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "CLT_R3_WORKFLOW_INVALID_REQUEST",
                        "message", exception.getMessage() == null ? "INVALID_REQUEST" : exception.getMessage()));
    }
}
