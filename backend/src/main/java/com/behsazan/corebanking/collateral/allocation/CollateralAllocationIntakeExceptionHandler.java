package com.behsazan.corebanking.collateral.allocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Do not expose request data, debtor Party IDs, Oracle SQL or secrets. */
@RestControllerAdvice(assignableTypes = CollateralAllocationIntakeController.class)
public class CollateralAllocationIntakeExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(CollateralAllocationIntakeExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badInput(IllegalArgumentException error) {
        String reason=error.getMessage();
        if ("CLT_ALLOCATION_IDEMPOTENCY_PAYLOAD_CONFLICT".equals(reason)) {
            ProblemDetail p=ProblemDetail.forStatus(HttpStatus.CONFLICT);p.setDetail(reason);return p;
        }
        ProblemDetail p=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setDetail(reason!=null && (reason.startsWith("CLT_ALLOCATION_") || reason.startsWith("CLT_CAPACITY_"))
                ? reason : "CLT_ALLOCATION_REQUEST_INVALID");
        return p;
    }
    @ExceptionHandler({DataAccessException.class,IllegalStateException.class})
    ProblemDetail unavailable(Exception error) {
        LOG.warn("CLT_ALLOCATION_INTAKE_UNAVAILABLE: {}",error.getClass().getSimpleName());
        ProblemDetail p=ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        p.setDetail("CLT_ALLOCATION_INTAKE_UNAVAILABLE");return p;
    }
}
