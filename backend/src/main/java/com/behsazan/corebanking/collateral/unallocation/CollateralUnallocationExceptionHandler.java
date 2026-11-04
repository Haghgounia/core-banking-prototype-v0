package com.behsazan.corebanking.collateral.unallocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Suppress SQL diagnostics; do not disclose whether a foreign-branch allocation exists. */
@RestControllerAdvice(assignableTypes=CollateralUnallocationController.class)
public class CollateralUnallocationExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(CollateralUnallocationExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badInput(IllegalArgumentException ignored) {
        ProblemDetail p=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setDetail("CLT_R5A_INVALID_INPUT");return p;
    }
    @ExceptionHandler({DataAccessException.class,IllegalStateException.class})
    public ProblemDetail unavailable(Exception error) {
        LOG.warn("CLT_R5A_READ_ONLY_UNAVAILABLE: {}",error.getClass().getSimpleName());
        ProblemDetail p=ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        p.setDetail("CLT_R5A_READ_ONLY_UNAVAILABLE");return p;
    }
}
