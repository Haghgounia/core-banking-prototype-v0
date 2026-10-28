package com.behsazan.corebanking.fee3.f2;

import com.behsazan.corebanking.fee3.application.Fee3Problem;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes=Fee3F2FormulaController.class)
public class Fee3F2FormulaProblemAdvice {
    @ExceptionHandler(Fee3Problem.class)
    public ProblemDetail business(Fee3Problem e){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(e.status(),"FEE3 F2 preview is not authorized");
        p.setType(URI.create("urn:core-banking:fee3:f2:problem"));
        p.setProperty("errorCode",e.errorCode());return p;
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException e){
        ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
            "FEE3 F2 formula preview rejected");
        p.setType(URI.create("urn:core-banking:fee3:f2:problem"));
        p.setProperty("errorCode","FEE3_F2_INVALID_FORMULA_PREVIEW");return p;
    }
}
