package com.behsazan.corebanking.organization.contact;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice(assignableTypes = OrganizationContactController.class)
public class OrganizationContactProblemAdvice {
    @ExceptionHandler(OrganizationContactException.class)
    public ProblemDetail handle(OrganizationContactException ex) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        result.setType(URI.create("urn:core-banking:organization:contacts"));
        result.setTitle("خطا در اطلاعات تماس واحد سازمانی");
        result.setProperty("errorCode", ex.code());
        return result;
    }
}
