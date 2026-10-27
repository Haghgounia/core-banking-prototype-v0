package com.behsazan.corebanking.fee3.r4b2;

import com.behsazan.corebanking.fee3.application.Fee3Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice(assignableTypes=Fee3R4B2Controller.class)
public class Fee3R4B2ProblemAdvice {
    @ExceptionHandler(Fee3Problem.class)
    public ProblemDetail business(Fee3Problem e){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(e.status(),"FEE3 R4-B2 read-only request denied");
        p.setType(URI.create("urn:core-banking:fee3:r4b2:problem"));p.setProperty("errorCode",e.errorCode());return p;
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail input(IllegalArgumentException e){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "FEE3 R4-B2 draft calculation contract rejected");
        p.setType(URI.create("urn:core-banking:fee3:r4b2:problem"));
        p.setProperty("errorCode","FEE3_R4B2_CONTRACT_REJECTED");return p;
    }
}
