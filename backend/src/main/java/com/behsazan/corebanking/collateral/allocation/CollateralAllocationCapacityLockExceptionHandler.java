package com.behsazan.corebanking.collateral.allocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Never expose SQL text, debtor identities, JDBC credentials or Oracle lock details. */
@RestControllerAdvice(assignableTypes = CollateralAllocationCapacityLockController.class)
public class CollateralAllocationCapacityLockExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(CollateralAllocationCapacityLockExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException error) {
        ProblemDetail detail=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail("CLT_R4D_LOCK_REQUEST_INVALID");return detail;
    }
    @ExceptionHandler({DataAccessException.class,IllegalStateException.class})
    ProblemDetail unavailable(Exception error) {
        LOG.warn("CLT_R4D_DIAGNOSTIC_ABORTED: {}",error.getClass().getSimpleName());
        ProblemDetail detail=ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        detail.setDetail("CLT_R4D_LOCK_CONTENTION_OR_DATABASE_UNAVAILABLE");return detail;
    }
}
