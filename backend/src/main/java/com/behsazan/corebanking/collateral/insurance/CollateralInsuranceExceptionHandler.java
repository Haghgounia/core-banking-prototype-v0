package com.behsazan.corebanking.collateral.insurance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CollateralInsuranceController.class)
public class CollateralInsuranceExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollateralInsuranceExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail input(IllegalArgumentException ex) {
        var p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setDetail(ex.getMessage() != null && ex.getMessage().startsWith("CLT_INSURANCE_")
                ? ex.getMessage() : "CLT_INSURANCE_INVALID_INPUT");
        return p;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ProblemDetail unavailable(Exception ex) {
        LOG.warn("CLT_INSURANCE_STORAGE_UNAVAILABLE: {}", ex.getClass().getSimpleName());
        var p = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        p.setDetail("CLT_INSURANCE_STORAGE_UNAVAILABLE");
        return p;
    }
}
