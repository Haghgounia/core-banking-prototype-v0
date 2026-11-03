package com.behsazan.corebanking.collateral.valuation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Scoped problem mapping; does not affect other domains and does not disclose Oracle SQL. */
@RestControllerAdvice(assignableTypes = CollateralValuationController.class)
public class CollateralValuationExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollateralValuationExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail input(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail(ex.getMessage() != null && ex.getMessage().startsWith("CLT_VALUATION_")
                ? ex.getMessage() : "CLT_VALUATION_INVALID_INPUT");
        return problem;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ProblemDetail storage(Exception ex) {
        LOG.warn("CLT_VALUATION_STORAGE_UNAVAILABLE: {}", ex.getClass().getSimpleName());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setDetail("CLT_VALUATION_STORAGE_UNAVAILABLE");
        return problem;
    }
}
