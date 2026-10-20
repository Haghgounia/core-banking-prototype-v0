package com.behsazan.corebanking.organization.grade;

import org.springframework.http.HttpStatus;

public final class GradeException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public GradeException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public static GradeException invalid(String message) {
        return new GradeException(HttpStatus.UNPROCESSABLE_ENTITY, "GRADE_VALIDATION", message);
    }
    public static GradeException conflict(String message) {
        return new GradeException(HttpStatus.CONFLICT, "GRADE_CONFLICT", message);
    }
    public static GradeException notFound(String message) {
        return new GradeException(HttpStatus.NOT_FOUND, "GRADE_NOT_FOUND", message);
    }
    public static GradeException forbidden(String message) {
        return new GradeException(HttpStatus.FORBIDDEN, "GRADE_ACCESS_DENIED", message);
    }
}
