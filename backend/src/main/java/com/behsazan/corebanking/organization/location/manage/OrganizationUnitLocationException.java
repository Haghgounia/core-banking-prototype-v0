package com.behsazan.corebanking.organization.location.manage;

import org.springframework.http.HttpStatus;

public class OrganizationUnitLocationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private OrganizationUnitLocationException(HttpStatus status, String code, String message) {
        super(message); this.status=status; this.code=code;
    }
    public HttpStatus status() {return status;}
    public String code() {return code;}
    public static OrganizationUnitLocationException invalid(String message) {
        return new OrganizationUnitLocationException(HttpStatus.BAD_REQUEST, "ORG_LOCATION_INVALID",message);
    }
    public static OrganizationUnitLocationException missing(String message) {
        return new OrganizationUnitLocationException(HttpStatus.NOT_FOUND, "ORG_LOCATION_NOT_FOUND",message);
    }
    public static OrganizationUnitLocationException conflict(String message) {
        return new OrganizationUnitLocationException(HttpStatus.CONFLICT, "ORG_LOCATION_CONFLICT",message);
    }
    public static OrganizationUnitLocationException schema() {
        return new OrganizationUnitLocationException(HttpStatus.SERVICE_UNAVAILABLE, "ORG_LOCATION_R2B_NOT_READY",
                "ساختار ویرایش R2-B هنوز نصب و تأیید نشده است. داده‌های قدیمی قابل مشاهده‌اند ولی ثبت/ویرایش مسدود است.");
    }
}
