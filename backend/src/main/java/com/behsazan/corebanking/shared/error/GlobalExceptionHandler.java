package com.behsazan.corebanking.shared.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReferenceNotFoundException.class)
    ProblemDetail handleNotFound(ReferenceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("urn:reference-data:problem:not-found"));
        problem.setTitle("رکورد یافت نشد");
        problem.setProperty("errorCode", "RECORD_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(ReferenceValidationException.class)
    ProblemDetail handleValidation(ReferenceValidationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setType(URI.create("urn:reference-data:problem:validation"));
        problem.setTitle("اطلاعات ورودی معتبر نیست");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("fieldErrors", exception.fieldErrors());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException exception) {
        String message = rootMessage(exception).toUpperCase(Locale.ROOT);
        boolean dependent = message.contains("ORA-02292");
        boolean duplicate = message.contains("ORA-00001");

        HttpStatus status = HttpStatus.CONFLICT;
        String title = dependent ? "امکان حذف وجود ندارد" : "تعارض اطلاعات";
        String detail = dependent
                ? "برای این رکورد، اطلاعات وابسته ثبت شده است."
                : duplicate
                ? "کد یا مقدار یکتای واردشده قبلاً ثبت شده است."
                : "عملیات با محدودیت‌های پایگاه داده سازگار نیست.";
        String code = dependent ? "DEPENDENT_RECORDS_EXIST" : duplicate ? "DUPLICATE_VALUE" : "DATA_CONFLICT";

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:reference-data:problem:data-conflict"));
        problem.setTitle(title);
        problem.setProperty("errorCode", code);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "خطای پیش‌بینی‌نشده در سرویس رخ داد."
        );
        problem.setType(URI.create("urn:reference-data:problem:unexpected"));
        problem.setTitle("خطای سرویس");
        problem.setProperty("errorCode", "UNEXPECTED_ERROR");
        return problem;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }
}
