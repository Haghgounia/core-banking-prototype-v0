package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.AssetCandidateInspection;
import java.util.Locale;

/**
 * R2/G-14: evidence about exact candidate references in unregistered Oracle drafts.
 * This is NOT a verified asset identity and does not authorize final registration.
 */
public final class CollateralAssetCandidatePolicy {
    private CollateralAssetCandidatePolicy() {}

    /** Comparable only within a single collateral type, with trim + case folding. */
    public static String lookup(String candidate) {
        return candidate == null || candidate.isBlank() ? null
                : candidate.strip().toUpperCase(Locale.ROOT);
    }

    public static AssetCandidateInspection assess(String draftId, String reference,
            boolean extensionAvailable, long sameOwner, long differentOwner) {
        if (sameOwner < 0 || differentOwner < 0)
            throw new IllegalArgumentException("CLT_ASSET_STAGE_COUNTER_INVALID");
        if (!extensionAvailable)
            return report(draftId, reference, 0, false, true, "STAGE_EXTENSION_REQUIRED",
                    "ستون نامزد دارایی در ساختار پیش نویس Oracle موجود نیست؛ بررسی تکرار انجام نشده است.");
        if (lookup(reference) == null)
            return report(draftId, null, 0, false, true, "NO_ASSET_REFERENCE",
                    "شناسه نامزد دارایی ارائه نشده است؛ این بررسی جایگزین احراز هویت G-14 نیست.");
        if (differentOwner > 0)
            return report(draftId, reference, sameOwner, true, false, "OTHER_OWNER_STAGE_CONFLICT",
                    "برای همین نوع و شناسه متنی، پیش نویس متعلق به مالک دیگر وجود دارد؛ تا تعیین تکلیف تعارض، ارسال بازبینی مسدود است.");
        if (sameOwner > 0)
            return report(draftId, reference, sameOwner, false, true, "SAME_OWNER_DRAFTS_FOUND",
                    "پیش نویس دیگری با همین شناسه برای همین مالک ثبت شده است؛ ممکن است نسخه اصلاحی باشد.");
        return report(draftId, reference, 0, false, true, "NO_STAGE_MATCH",
                "در پیش نویس های Oracle تطبیق متنی مشاهده نشد؛ نبود دارایی فعال یا تکراری در کل بانک احراز نشده است.");
    }
    private static AssetCandidateInspection report(String id, String reference, long sameOwner,
            boolean otherOwner, boolean reviewAllowed, String status, String explanation) {
        return new AssetCandidateInspection(id, reference, sameOwner, otherOwner,
                reviewAllowed, false, false, status, explanation,
                "STAGE_ONLY_TYPE_AND_TRIMMED_CASE_INSENSITIVE_REFERENCE_G14_OPEN");
    }
}
