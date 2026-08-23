package com.behsazan.corebanking.shared.error;

import com.behsazan.corebanking.cif.error.CifNotFoundException;
import com.behsazan.corebanking.cif.error.CifValidationException;
import com.behsazan.corebanking.system.modelcomparison.ModelComparisonValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


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

    @ExceptionHandler(CifNotFoundException.class)
    ProblemDetail handleCifNotFound(CifNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("urn:cif:problem:not-found"));
        problem.setTitle("اطلاعات مشتری یافت نشد");
        problem.setProperty("errorCode", "CIF_RECORD_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(CifValidationException.class)
    ProblemDetail handleCifValidation(CifValidationException exception) {
        log.warn("CIF validation failed: {} fields={}", exception.getMessage(), exception.fieldErrors());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setType(URI.create("urn:cif:problem:validation"));
        problem.setTitle("اطلاعات مشتری معتبر نیست");
        problem.setProperty("errorCode", "CIF_VALIDATION_FAILED");
        problem.setProperty("fieldErrors", exception.fieldErrors());
        return problem;
    }

    @ExceptionHandler(ModelComparisonValidationException.class)
    ProblemDetail handleModelComparisonValidation(ModelComparisonValidationException exception) {
        log.warn("EA/Oracle comparison validation failed: {}", exception.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setType(URI.create("urn:core-banking:problem:model-comparison-validation"));
        problem.setTitle("فایل یا تنظیمات مقایسه معتبر نیست");
        problem.setProperty("errorCode", "MODEL_COMPARISON_VALIDATION_FAILED");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException exception) {
        java.util.Map<String, String> fieldErrors = new java.util.LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage() == null ? "مقدار معتبر نیست." : error.getDefaultMessage())
        );
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "اطلاعات فرم را اصلاح کنید.");
        problem.setType(URI.create("urn:core-banking:problem:validation"));
        problem.setTitle("اطلاعات ورودی معتبر نیست");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException exception) {
        log.warn("Database integrity violation: {}", rootMessage(exception));
        String message = rootMessage(exception).toUpperCase(Locale.ROOT);
        boolean dependent = message.contains("ORA-02292");
        boolean parentMissing = message.contains("ORA-02291");
        boolean duplicate = message.contains("ORA-00001");
        boolean requiredMissing = message.contains("ORA-01400");
        String requiredColumn = requiredMissing ? oracleColumnFromNotNullViolation(rootMessage(exception)) : null;

        HttpStatus status = HttpStatus.CONFLICT;
        String title = dependent ? "امکان حذف وجود ندارد"
                : parentMissing ? "مقدار مرجع نامعتبر است"
                : requiredMissing ? "مقدار اجباری ثبت نشده است"
                : "تعارض اطلاعات";
        String detail = dependent
                ? "برای این رکورد، اطلاعات وابسته ثبت شده است."
                : parentMissing
                ? "یکی از کدهای مرجع ارسالی در جدول مرجع متناظر وجود ندارد."
                : duplicate
                ? "کد یا مقدار یکتای واردشده قبلاً ثبت شده است."
                : requiredMissing
                ? (requiredColumn == null
                    ? "یکی از فیلدهای اجباری پایگاه داده بدون مقدار ارسال شده است."
                    : "فیلد اجباری پایگاه داده «" + requiredColumn + "» بدون مقدار ارسال شده است.")
                : "عملیات با محدودیت‌های پایگاه داده سازگار نیست.";
        String code = dependent ? "DEPENDENT_RECORDS_EXIST"
                : parentMissing ? "REFERENCE_VALUE_NOT_FOUND"
                : duplicate ? "DUPLICATE_VALUE"
                : requiredMissing ? "REQUIRED_DATABASE_VALUE_MISSING" : "DATA_CONFLICT";

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:reference-data:problem:data-conflict"));
        problem.setTitle(title);
        problem.setProperty("errorCode", code);
        return problem;
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleDatabaseAccess(DataAccessException exception) {
        String message = rootMessage(exception).toUpperCase(Locale.ROOT);
        if (message.contains("ORA-01950")) {
            log.error("Oracle tablespace quota error", exception);
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Schema پایگاه داده روی Tablespace مورد نیاز Quota کافی ندارد."
            );
            problem.setType(URI.create("urn:core-banking:problem:oracle-quota"));
            problem.setTitle("خطای فضای Oracle");
            problem.setProperty("errorCode", "ORACLE_TABLESPACE_QUOTA");
            return problem;
        }

        log.error("Database access error", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "عملیات پایگاه داده انجام نشد. جزئیات فنی در Log سرویس ثبت شده است."
        );
        problem.setType(URI.create("urn:core-banking:problem:database"));
        problem.setTitle("خطای پایگاه داده");
        problem.setProperty("errorCode", "DATABASE_ERROR");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled service error", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "خطای پیش‌بینی‌نشده در سرویس رخ داد."
        );
        problem.setType(URI.create("urn:reference-data:problem:unexpected"));
        problem.setTitle("خطای سرویس");
        problem.setProperty("errorCode", "UNEXPECTED_ERROR");
        return problem;
    }

    private static String oracleColumnFromNotNullViolation(String message) {
        if (message == null) return null;
        int lastQuote = message.lastIndexOf('"');
        if (lastQuote <= 0) return null;
        int previousQuote = message.lastIndexOf('"', lastQuote - 1);
        if (previousQuote < 0 || previousQuote + 1 >= lastQuote) return null;
        return message.substring(previousQuote + 1, lastQuote);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }
}
