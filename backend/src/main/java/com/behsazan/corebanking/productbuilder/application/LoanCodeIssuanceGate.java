package com.behsazan.corebanking.productbuilder.application;

import java.util.List;

/** Pure domain gate, shared by stage-six runtime issuance and independent regression tests. */
public final class LoanCodeIssuanceGate {
    private LoanCodeIssuanceGate() { }

    public static void requireDraft(String productStatus) {
        if (!"DRAFT".equals(productStatus))
            throw new ProductBuilderValidationException("فقط محصول پیش‌نویس و بدون کد قابلیت نهایی‌سازی دارد.");
    }

    public static void requireApprovedCurrent(String versionStatus, boolean current, String recordStatus) {
        if (!"APPROVED".equals(versionStatus) || !current || !"ACTIVE".equals(recordStatus))
            throw new ProductBuilderValidationException("صدور کد فقط برای نسخه جاری، فعال و تصویب‌شده مجاز است.");
    }

    public static void requireSingleProfile(long activeProfileCount) {
        if (activeProfileCount != 1)
            throw new ProductBuilderValidationException("نسخه جاری برای صدور کد باید دقیقاً یک پروفایل تسهیلات داشته باشد.");
    }

    /** Code issuance needs validated product design, not operational go-live.
     *  canOpen is a separate gate for loan origination; the issued product stays INACTIVE.
     */
    public static void requireGovernance(boolean canApprove, List<String> blockers) {
        if (!canApprove)
            throw new ProductBuilderValidationException("صدور کد تا رفع تمام موانع حاکمیت، اعتبارسنجی و آمادگی نسخه مجاز نیست: "
                    + String.join("؛ ", blockers));
    }
}
