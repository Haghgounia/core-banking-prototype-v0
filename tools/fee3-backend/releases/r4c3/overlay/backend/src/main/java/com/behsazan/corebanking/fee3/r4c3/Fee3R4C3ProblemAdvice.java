package com.behsazan.corebanking.fee3.r4c3;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice(assignableTypes=Fee3R4C3Controller.class)
public class Fee3R4C3ProblemAdvice {
    @ExceptionHandler({IllegalArgumentException.class,ArithmeticException.class})
    public ProblemDetail reject(RuntimeException problem){
        ProblemDetail detail=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "Qard draft preview rejected; no financial action performed");
        detail.setType(URI.create("urn:fee3:r4c3:qard:blocked"));
        detail.setProperty("bankEffect",false);
        return detail;
    }
}
