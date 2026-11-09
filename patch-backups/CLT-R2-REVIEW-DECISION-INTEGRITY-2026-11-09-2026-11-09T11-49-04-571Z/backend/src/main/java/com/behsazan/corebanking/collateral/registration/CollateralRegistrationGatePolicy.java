package com.behsazan.corebanking.collateral.registration;

import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadinessModels.Check;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadinessModels.Report;
import java.util.ArrayList;
import java.util.List;

/**
 * Evidence-driven R2 registration decision matrix. Business-policy approvals are NOT inferred
 * from a reviewed staging draft, a hash, a successful build, or a compatible Oracle column.
 */
public final class CollateralRegistrationGatePolicy {
    private CollateralRegistrationGatePolicy() {}

    public record Evidence(String reviewId, String draftId, String reviewStatus,
            String maker, String checker, String branch, String snapshotHash,
            boolean draftPresent, boolean persistedFingerprintMatches,
            boolean recomputedFingerprintMatches, boolean currentBusinessRulesValid,
            List<String> currentBusinessBlockers,
            boolean oracleNumber19Unique, boolean postedTransactionReferenceAvailable,
            boolean introductionTransactionReferenceAvailable,
            boolean countable, boolean serialRowsValid, String serialItemCount,
            String assetReferenceCandidate, String estimateAmount, String currencyCode) {}

    public static Report assess(Evidence e) {
        if (e == null || e.reviewId() == null || e.draftId() == null)
            throw new IllegalArgumentException("CLT_REGISTRATION_EVIDENCE_REQUIRED");
        List<Check> checks = new ArrayList<>();
        boolean approvedReview = "REVIEWED".equals(e.reviewStatus()) && e.maker() != null
                && e.checker() != null && !e.maker().equals(e.checker());
        add(checks, "REVIEW_DECISION", "بازبینی مستقل پیش نویس", approvedReview ? "VERIFIED" : "BLOCKED",
                approvedReview ? "تصمیم REVIEWED و تفاوت ثبت کننده و بازبین ثبت شده است."
                               : "پیش نویس باید با تصمیم بازبین مستقل REVIEWED باشد.");
        boolean immutable = e.draftPresent() && e.persistedFingerprintMatches()
                && e.recomputedFingerprintMatches();
        add(checks, "SNAPSHOT_INTEGRITY", "تمامیت نسخه ذخیره شده", immutable ? "VERIFIED" : "BLOCKED",
                immutable ? "اثر انگشت ذخیره شده با درخواست اصلی و نسخه بازبینی مطابقت دارد."
                          : "داده پیش نویس یا اثر انگشت آن ناقص، متفاوت یا غیرقابل بازسازی است.");
        boolean liveValid = immutable && e.currentBusinessRulesValid();
        add(checks, "LIVE_CIF_CLT_RECHECK", "بازاعتبارسنجی مالک و قواعد CLT", liveValid ? "VERIFIED" : "BLOCKED",
                liveValid ? "اعتبارسنجی جاری CIF و اطلاعات پایه وثیقه موفق است."
                          : "استعلام جاری قابل تأیید نیست: " + String.join(", ",
                            e.currentBusinessBlockers() == null ? List.of("CLT_REVALIDATION_NOT_PROVEN")
                                                                     : e.currentBusinessBlockers()));
        add(checks, "NUMBER19_STORAGE", "ظرفیت و یکتایی ستون شماره انتظامی",
                e.oracleNumber19Unique() ? "VERIFIED" : "BLOCKED",
                e.oracleNumber19Unique()
                 ? "نوع NUMBER(19,0) و قید یکتایی در دیکشنری Oracle مشاهده شد؛ این به معنی تصویب قالب شماره نیست."
                 : "نوع/دقت/قید یکتایی واقعی Oracle قابل تأیید نیست.");
        boolean typesPresent = e.postedTransactionReferenceAvailable()
                && e.introductionTransactionReferenceAvailable();
        add(checks, "TRANSACTION_REFERENCES", "کدهای مرجع تراکنش معرفی",
                typesPresent ? "VERIFIED" : "BLOCKED",
                typesPresent ? "INTRODUCTION و POSTED در اطلاعات پایه موجودند."
                             : "کدهای INTRODUCTION یا POSTED در مرجع عملیاتی تأیید نشدند.");
        if (e.countable()) {
            add(checks, "COUNTABLE_ITEM_VALIDATION", "کنترل دامنه اقلام شمارشی",
                    e.serialRowsValid() ? "VERIFIED" : "BLOCKED",
                    e.serialRowsValid() ? "قواعد بازه، هم پوشانی و جمع مبلغ در نسخه ذخیره شده رعایت شده است."
                                        : "بازه های ذخیره شده نیازمند اصلاح اند.");
            add(checks, "G-03", "مدل قطعی نگهداری اقلام شمارشی", "DECISION_REQUIRED",
                    "افزونه قطعی اقلام و کنترل همزمانی باید در مدل EA/Oracle تصویب شود.");
            add(checks, "G-12", "یکسان سازی HAS_MULTIPLICITY و اقلام", "DECISION_REQUIRED",
                    "معنای شمارشی بودن و رابطه با اقلام باید به تصویب بانک برسد.");
        } else {
            add(checks, "COUNTABLE_ITEM_VALIDATION", "کنترل اقلام شمارشی", "NOT_APPLICABLE",
                    "نوع مورد بررسی شمارشی نیست.");
        }
        boolean hasCandidate = e.assetReferenceCandidate() != null
                && !e.assetReferenceCandidate().isBlank();
        add(checks, "ASSET_CANDIDATE", "شناسه پیشنهادی دارایی",
                hasCandidate ? "INFORMATIONAL" : "BLOCKED",
                hasCandidate ? "در پیش نویس ثبت شده است؛ صحت و یکتایی آن هنوز احراز نشده است."
                             : "شناسه پیشنهادی دارایی برای اتصال به مرجع هویت ثبت نشده است.");
        add(checks, "G-14", "هویت پایدار دارایی و معرفی مجدد", "DECISION_REQUIRED",
                "مرجع یکتا، قواعد تطبیق و پیوند معرفی مجدد باید مصوب شود؛ شناسه نامزد کافی نیست.");
        add(checks, "G-17", "صدور شماره انتظامی و حدود ۱۹ رقم", "DECISION_REQUIRED",
                "الگوریتم معتبر بانک، منبع شماره مشتری، کنترل رقابت و قرارداد رشته ای API تصویب نشده است.");
        add(checks, "G-01", "چرخه اصلاحیه شناسنامه", "DECISION_REQUIRED",
                "نسخه و فرایند اصلاحیه پیش از هر تعهد یا تخصیص باید تصویب شود.");
        add(checks, "G-16", "همخوانی پیش فرض وضعیت تراکنش", "DECISION_REQUIRED",
                "پیش فرض PENDING در DDL با Seed کدهای وضعیت مغایر است؛ تصمیم اصلاح باید ثبت شود.");
        add(checks, "FINAL_WRITER", "مجوز ثبت قطعی و تغییر داده بانکی", "BLOCKED",
                "در R2E هیچ API یا مجوز نوشتن روی CLT.COLLATERAL وجود ندارد.");
        List<String> blockers = checks.stream().filter(c -> "BLOCKED".equals(c.status())
                || "DECISION_REQUIRED".equals(c.status())).map(Check::code).toList();
        // Fail closed regardless of component-level positive results. No client flag can override.
        return new Report(e.reviewId(), e.draftId(), e.reviewStatus(), e.branch(),
                e.snapshotHash(), e.assetReferenceCandidate(),
                e.serialItemCount() == null ? "0" : e.serialItemCount(),
                e.estimateAmount(), e.currencyCode(), null, "NOT_REGISTERED", false, false,
                List.copyOf(checks), blockers, "REVIEWED_DRAFT_DECISION_GATE_ONLY");
    }
    private static void add(List<Check> out, String code, String label, String status, String evidence) {
        out.add(new Check(code, label, status, evidence));
    }
}
