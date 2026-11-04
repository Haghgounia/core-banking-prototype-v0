package com.behsazan.corebanking.collateral.release;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** No SQL identifiers, sensitive actor data or cross-branch metadata in HTTP errors. */
@RestControllerAdvice(assignableTypes=CollateralFinalReleaseController.class)
public class CollateralFinalReleaseExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(CollateralFinalReleaseExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ignored){
        ProblemDetail d=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        d.setDetail("CLT_R5C_INVALID_INPUT");return d;
    }
    @ExceptionHandler({DataAccessException.class,IllegalStateException.class})
    public ProblemDetail unavailable(Exception e){
        LOG.warn("CLT_R5C_EVIDENCE_UNAVAILABLE: {}",e.getClass().getSimpleName());
        ProblemDetail d=ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        d.setDetail("CLT_R5C_EVIDENCE_UNAVAILABLE");return d;
    }
}
