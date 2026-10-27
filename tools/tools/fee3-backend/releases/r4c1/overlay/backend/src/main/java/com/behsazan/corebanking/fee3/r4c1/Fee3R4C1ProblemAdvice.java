package com.behsazan.corebanking.fee3.r4c1;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestControllerAdvice(assignableTypes=Fee3R4C1Controller.class)
public class Fee3R4C1ProblemAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ex){
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "FEE3 R4-C1 simulation request rejected");
        p.setType(URI.create("urn:core-banking:fee3:r4c1:problem"));
        p.setProperty("errorCode","FEE3_R4C1_REQUEST_REJECTED");return p;
    }
}
