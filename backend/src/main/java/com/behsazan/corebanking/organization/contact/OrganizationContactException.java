package com.behsazan.corebanking.organization.contact;

import org.springframework.http.HttpStatus;

/** Business rule or installation problem; no stack trace is sent to the UI. */
public final class OrganizationContactException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public OrganizationContactException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }

    public static OrganizationContactException invalid(String message) {
        return new OrganizationContactException(HttpStatus.UNPROCESSABLE_ENTITY, "ORG_CONTACT_INVALID", message);
    }
    public static OrganizationContactException notFound(String message) {
        return new OrganizationContactException(HttpStatus.NOT_FOUND, "ORG_CONTACT_NOT_FOUND", message);
    }
    public static OrganizationContactException conflict(String message) {
        return new OrganizationContactException(HttpStatus.CONFLICT, "ORG_CONTACT_CONFLICT", message);
    }
    public static OrganizationContactException schemaMissing() {
        return new OrganizationContactException(HttpStatus.SERVICE_UNAVAILABLE, "ORG_CONTACT_SCHEMA_MISSING",
                "ساختار راه‌های تماس ORG هنوز نصب نشده است؛ ابتدا اسکریپت اختیاری R1 را بررسی و اعمال کنید.");
    }
}
