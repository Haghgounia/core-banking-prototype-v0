package com.behsazan.corebanking.fee3.application;

import org.springframework.http.HttpStatus;

public final class Fee3Problem extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    public Fee3Problem(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.errorCode = code;
    }
    public HttpStatus status() { return status; }
    public String errorCode() { return errorCode; }
}
