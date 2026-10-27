package com.behsazan.corebanking.fee3.r4c2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestControllerAdvice(assignableTypes=Fee3R4C2Controller.class)
public class Fee3R4C2ProblemAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ex){
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "FEE3 R4-C2 nonfinancial UAT request rejected");
        p.setType(URI.create("urn:core-banking:fee3:r4c2:problem"));
        p.setProperty("errorCode","FEE3_R4C2_REQUEST_REJECTED");return p;
    }
}
