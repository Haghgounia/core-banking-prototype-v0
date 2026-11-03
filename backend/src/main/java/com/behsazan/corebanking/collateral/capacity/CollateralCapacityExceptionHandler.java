package com.behsazan.corebanking.collateral.capacity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Scoped only to Capacity endpoints; avoid exposing SQL, credentials or internal data. */
@RestControllerAdvice(assignableTypes = CollateralCapacityController.class)
public class CollateralCapacityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollateralCapacityExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException error) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setDetail(error.getMessage() != null && error.getMessage().startsWith("CLT_CAPACITY_")
                ? error.getMessage() : "CLT_CAPACITY_INPUT_INVALID");
        return p;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class, ArithmeticException.class})
    ProblemDetail unavailable(Exception error) {
        LOG.warn("CLT_CAPACITY_READ_ERROR: {}", error.getClass().getSimpleName());
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        p.setDetail("CLT_CAPACITY_EVIDENCE_UNAVAILABLE");
        return p;
    }
}
