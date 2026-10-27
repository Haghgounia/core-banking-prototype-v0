package com.behsazan.corebanking.fee3.r4;

import com.behsazan.corebanking.fee3.application.Fee3Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice(assignableTypes=Fee3R4Controller.class)
public class Fee3R4ProblemAdvice {
    @ExceptionHandler(Fee3Problem.class)
    public ProblemDetail business(Fee3Problem e){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(e.status(),"FEE3 R4-A request rejected");
        p.setType(URI.create("urn:core-banking:fee3:r4a:problem"));
        p.setProperty("errorCode",e.errorCode());return p;
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail contract(IllegalArgumentException e){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "FEE3 R4-A calculation contract rejected");
        p.setType(URI.create("urn:core-banking:fee3:r4a:problem"));
        p.setProperty("errorCode","FEE3_R4A_CONTRACT_REJECTED");return p;
    }
}
