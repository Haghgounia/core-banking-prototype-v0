package com.behsazan.corebanking.fee3.f1;

import com.behsazan.corebanking.fee3.application.Fee3Problem;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = Fee3F1UsageController.class)
public class Fee3F1UsageProblemAdvice {
    @ExceptionHandler(Fee3Problem.class)
    public ProblemDetail business(Fee3Problem e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(e.status(), "FEE3 USAGE preview denied");
        p.setType(URI.create("urn:core-banking:fee3:f1:problem"));
        p.setProperty("errorCode", e.errorCode());
        return p;
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "FEE3 USAGE calculation request rejected");
        p.setType(URI.create("urn:core-banking:fee3:f1:problem"));
        p.setProperty("errorCode", "FEE3_F1_INVALID_USAGE_PREVIEW");
        return p;
    }
}
