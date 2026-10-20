package com.behsazan.corebanking.organization.grade;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice(assignableTypes = GradeController.class)
public class GradeProblemAdvice {
    @ExceptionHandler(GradeException.class)
    public ProblemDetail handle(GradeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        problem.setTitle("درخواست درجه‌بندی قابل انجام نیست");
        problem.setType(URI.create("urn:core-banking:organization:grade"));
        problem.setProperty("errorCode", exception.code());
        return problem;
    }
}
