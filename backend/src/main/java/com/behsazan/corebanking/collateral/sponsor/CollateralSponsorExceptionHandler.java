package com.behsazan.corebanking.collateral.sponsor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CollateralSponsorController.class)
public class CollateralSponsorExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollateralSponsorExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException ex) {
        var detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail(ex.getMessage() != null && ex.getMessage().startsWith("CLT_SPONSOR_")
                ? ex.getMessage() : "CLT_SPONSOR_INVALID_INPUT");
        return detail;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ProblemDetail unavailable(Exception ex) {
        LOG.warn("CLT_SPONSOR_STORAGE_UNAVAILABLE: {}", ex.getClass().getSimpleName());
        var detail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        detail.setDetail("CLT_SPONSOR_STORAGE_UNAVAILABLE");
        return detail;
    }
}
