package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.application.Fee3Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice(assignableTypes=Fee3R3Controller.class)
public class Fee3R3ProblemAdvice {
    @ExceptionHandler(Fee3Problem.class)
    public ProblemDetail rejected(Fee3Problem error){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(error.status(),error.getMessage());
        p.setType(URI.create("urn:core-banking:fee3:r3:problem"));
        p.setProperty("errorCode",error.errorCode());return p;
    }
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail concurrent(IllegalStateException error){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"FEE3 concurrent transition; reload and retry");
        p.setProperty("errorCode","FEE3_CONCURRENT_TRANSITION");return p;
    }
}
