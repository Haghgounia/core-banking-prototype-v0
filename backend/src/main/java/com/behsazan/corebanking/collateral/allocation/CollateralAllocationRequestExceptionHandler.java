package com.behsazan.corebanking.collateral.allocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Avoid exposing protected Party IDs or Oracle internals in errors. */
@RestControllerAdvice(assignableTypes = CollateralAllocationRequestController.class)
public class CollateralAllocationRequestExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollateralAllocationRequestExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException error) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        String message = error.getMessage();
        p.setDetail(message != null && (message.startsWith("CLT_ALLOCATION_") || message.startsWith("CLT_CAPACITY_")
                || message.startsWith("CLT_REGISTRY_")) ? message : "CLT_ALLOCATION_INPUT_INVALID");
        return p;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class, ArithmeticException.class})
    ProblemDetail unavailable(Exception error) {
        LOG.warn("CLT_ALLOCATION_READ_FAILED: {}", error.getClass().getSimpleName());
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        p.setDetail("CLT_ALLOCATION_EVIDENCE_UNAVAILABLE");
        return p;
    }
}
