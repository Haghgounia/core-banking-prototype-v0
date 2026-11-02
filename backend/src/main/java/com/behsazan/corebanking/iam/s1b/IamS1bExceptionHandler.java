package com.behsazan.corebanking.iam.s1b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Do not return raw Oracle SQL or JDBC credentials in public error messages. */
@RestControllerAdvice(assignableTypes = IamS1bReadController.class)
public class IamS1bExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(IamS1bExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setDetail("IAM_S1B_INVALID_INPUT");
        return p;
    }

    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    public ProblemDetail unavailable(Exception ex) {
        log.error("IAM_S1B_DATABASE_OR_CONTRACT_UNAVAILABLE cause={}", ex.getClass().getSimpleName());
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        p.setDetail("IAM_S1B_DATA_SOURCE_UNAVAILABLE");
        return p;
    }
}
