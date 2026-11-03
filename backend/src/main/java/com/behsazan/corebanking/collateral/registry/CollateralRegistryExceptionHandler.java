package com.behsazan.corebanking.collateral.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Hide SQL details and avoid unauthorized object existence disclosure. */
@RestControllerAdvice(assignableTypes = CollateralRegistryController.class)
public final class CollateralRegistryExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollateralRegistryExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException ignored) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail("CLT_REGISTRY_INVALID_INPUT");
        return problem;
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ProblemDetail unavailable(Exception error) {
        LOG.warn("CLT_REGISTRY_STORAGE_UNAVAILABLE: {}", error.getClass().getSimpleName());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setDetail("CLT_REGISTRY_STORAGE_UNAVAILABLE");
        return problem;
    }
}
