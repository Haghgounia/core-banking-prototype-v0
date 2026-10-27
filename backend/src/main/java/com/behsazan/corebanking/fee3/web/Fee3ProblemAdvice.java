package com.behsazan.corebanking.fee3.web;

import com.behsazan.corebanking.fee3.application.Fee3Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes=Fee3Controller.class)
public class Fee3ProblemAdvice {
    @ExceptionHandler(Fee3Problem.class)
    public ProblemDetail handle(Fee3Problem ex) {
        ProblemDetail problem=ProblemDetail.forStatusAndDetail(ex.status(),ex.getMessage());
        problem.setTitle("FEE3: درخواست نامعتبر یا غیرمجاز");
        problem.setType(URI.create("urn:core-banking:fee3:problem"));
        problem.setProperty("errorCode",ex.errorCode());
        return problem;
    }
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException ex){
        ProblemDetail detail=ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "نسخه پیش‌نویس همزمان تغییر کرده است. اطلاعات را دوباره بارگذاری کنید.");
        detail.setProperty("errorCode","FEE3_CONCURRENT_UPDATE");
        return detail;
    }
}
