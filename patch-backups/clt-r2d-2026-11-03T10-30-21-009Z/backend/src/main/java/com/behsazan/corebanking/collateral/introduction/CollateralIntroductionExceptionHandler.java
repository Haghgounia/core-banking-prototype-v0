package com.behsazan.corebanking.collateral.introduction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Only public domain error codes leave this endpoint; no SQL or token details. */
@RestControllerAdvice(assignableTypes = CollateralIntroductionController.class)
public final class CollateralIntroductionExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollateralIntroductionExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setDetail(ex.getMessage() != null && ex.getMessage().startsWith("CLT_")
                ? ex.getMessage() : "CLT_INTRODUCTION_INVALID_INPUT");
        return p;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ProblemDetail unavailable(Exception ex) {
        LOG.warn("CLT_INTRODUCTION_STORAGE_UNAVAILABLE: {}", ex.getClass().getSimpleName());
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        p.setDetail("CLT_INTRODUCTION_STORAGE_UNAVAILABLE");
        return p;
    }
}
