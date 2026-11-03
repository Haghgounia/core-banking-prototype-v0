package com.behsazan.corebanking.collateral.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Never reveal Oracle details, schema privileges, SQL or internal exception messages to API clients. */
@RestControllerAdvice(assignableTypes = CltR1aReadController.class)
public final class CltR1aExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CltR1aExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException e) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail("CLT_R1A_INVALID_INPUT");
        return detail;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ProblemDetail unavailable(Exception e) {
        log.warn("CLT_R1A_READ_UNAVAILABLE: {}", e.getClass().getSimpleName());
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        detail.setDetail("CLT_R1A_ORACLE_READ_UNAVAILABLE");
        return detail;
    }
}
