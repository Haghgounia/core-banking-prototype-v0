package com.behsazan.corebanking.collateral.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Never reveal Oracle details, schema privileges, SQL or internal exception messages to API clients. */
@RestControllerAdvice(assignableTypes = {CollateralReferenceController.class, CollateralReferenceChangeController.class, CollateralReferenceLocalMatrixController.class})
public final class CollateralReferenceExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CollateralReferenceExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException e) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail("CLT_REFERENCE_INVALID_INPUT");
        return detail;
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException e) {
        log.warn("CLT_REFERENCE_CONSTRAINT_REJECTED: {}", e.getClass().getSimpleName());
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setDetail("CLT_REFERENCE_CONSTRAINT_CONFLICT");
        return detail;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ProblemDetail unavailable(Exception e) {
        log.warn("CLT_REFERENCE_UNAVAILABLE: {}", e.getClass().getSimpleName());
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        detail.setDetail("CLT_REFERENCE_OPERATION_UNAVAILABLE");
        return detail;
    }
}
