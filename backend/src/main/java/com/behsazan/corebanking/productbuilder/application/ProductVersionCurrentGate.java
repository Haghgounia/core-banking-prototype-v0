package com.behsazan.corebanking.productbuilder.application;

import java.time.LocalDate;

/**
 * R30-HF2: pure, fail-closed rules for the explicit current-version governance command.
 * Setting IS_CURRENT never approves a version, opens origination or activates a product.
 */
public final class ProductVersionCurrentGate {
    private ProductVersionCurrentGate() { }

    public static void requireRequest(long productId, long versionId, String reason) {
        if (productId <= 0 || versionId <= 0)
            throw new ProductBuilderValidationException("شناسه محصول و نسخه برای تعیین نسخه جاری معتبر نیست.");
        if (reason == null || reason.trim().length() < 5 || reason.trim().length() > 500)
            throw new ProductBuilderValidationException("علت تعیین نسخه جاری باید بین ۵ تا ۵۰۰ نویسه باشد.");
    }

    public static void requireProduct(String status, boolean deleted) {
        if (deleted || "RETIRED".equals(status))
            throw new ProductBuilderValidationException("نسخه محصول حذف‌شده یا بازنشسته را نمی‌توان جاری کرد.");
        if (!"DRAFT".equals(status) && !"INACTIVE".equals(status) && !"ACTIVE".equals(status))
            throw new ProductBuilderValidationException("وضعیت محصول برای تعیین نسخه جاری معتبر نیست.");
    }

    public static void requireVersion(String versionStatus, String recordStatus, boolean approvalRecorded) {
        if (!"APPROVED".equals(versionStatus) || !"ACTIVE".equals(recordStatus))
            throw new ProductBuilderValidationException("فقط نسخه تصویب‌شده و دارای رکورد فعال می‌تواند نسخه جاری شود.");
        if (!approvalRecorded)
            throw new ProductBuilderValidationException("نسخه تصویب‌شده فاقد اطلاعات ثبت‌شده تصویب‌کننده یا زمان تصویب است.");
    }

    public static void requireEffective(LocalDate validFrom, LocalDate validTo, LocalDate bankToday) {
        if (bankToday == null || validFrom == null || validTo != null && validTo.isBefore(validFrom))
            throw new ProductBuilderValidationException("بازه اعتبار نسخه برای جاری‌سازی معتبر نیست.");
        if (validFrom.isAfter(bankToday) || validTo != null && validTo.isBefore(bankToday))
            throw new ProductBuilderValidationException("تنها نسخه دارای اعتبار در تاریخ روز بانک می‌تواند جاری شود.");
    }

    public static void requireGovernance(boolean canApprove, java.util.List<String> blockers) {
        if (!canApprove)
            throw new ProductBuilderValidationException("تعیین نسخه جاری به دلیل نواقص حاکمیت یا اعتبارسنجی مجاز نیست: "
                    + String.join("؛ ", blockers));
    }

    public static void requireSingleCurrent(long currentCount) {
        if (currentCount < 0 || currentCount > 1)
            throw new ProductBuilderValidationException("بیش از یک نسخه جاری برای محصول وجود دارد؛ رفع مغایرت داده با تأیید راهبر ضروری است.");
    }
}
