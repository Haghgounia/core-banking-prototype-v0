package com.behsazan.corebanking.collateral.unallocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Hide database details and cross-client identifiers from HTTP responses. */
@RestControllerAdvice(assignableTypes=CollateralUnallocationIntakeController.class)
public class CollateralUnallocationIntakeExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(CollateralUnallocationIntakeExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ignored){
        ProblemDetail detail=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail("CLT_R5B_INVALID_REQUEST");return detail;
    }
    @ExceptionHandler(CollateralUnallocationIntakePolicy.Conflict.class)
    public ProblemDetail conflict(RuntimeException ignored){
        ProblemDetail detail=ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setDetail("CLT_R5B_IDEMPOTENCY_OR_STATUS_CONFLICT");return detail;
    }
    @ExceptionHandler(CollateralUnallocationIntakePolicy.Stale.class)
    public ProblemDetail stale(RuntimeException ignored){
        ProblemDetail detail=ProblemDetail.forStatus(HttpStatus.PRECONDITION_FAILED);
        detail.setDetail("CLT_R5B_EXPECTED_VERSION_MISMATCH");return detail;
    }
    @ExceptionHandler({DataAccessException.class,IllegalStateException.class})
    public ProblemDetail unavailable(Exception ex){
        LOG.warn("CLT_R5B_STAGING_UNAVAILABLE: {}",ex.getClass().getSimpleName());
        ProblemDetail detail=ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        detail.setDetail("CLT_R5B_STAGING_UNAVAILABLE");return detail;
    }
}
