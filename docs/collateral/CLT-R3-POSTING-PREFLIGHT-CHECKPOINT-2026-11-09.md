# CLT-R3 — پیش‌بررسی ثبت قطعی درخواست‌ها (Stage-to-Domain Posting Dry-Run)

**تاریخ:** 2026-11-09  
**نسخه تحویل:** `CLT-R3-POSTING-PREFLIGHT-2026-11-09`  
**Baseline:** `core-banking-prototype-v0-2026-11-09-R3-LIFECYCLE-EVIDENCE-SOURCE.zip` (نصب بسته قبلی توسط کاربر تأیید شد)  
**فاز مصوب:** R3 — ضامن، بیمه و ارزیابی (بدون تغییر مرز R0..R7)  
**وضعیت شواهد:** Source implemented / Java 21 smoke and synthetic typecheck / Runtime Windows and Oracle Live not verified.

## هدف و پیاده‌سازی

در کارتابل مشترک درخواست‌های R3، برای هر شناسه درخواست یک دکمه **«پیش‌بررسی ثبت قطعی (فقط شواهد)»** افزوده شده است؛ هیچ فرم سفارشی سه‌گانه بازنویسی نمی‌شود. سرویس پیش‌بررسی فقط رکورد Stage و مجوزهای خواندن Oracle را بررسی می‌کند؛ این خدمت هیچ نوع posting، allocation، creation، cancellation یا renewal در جداول عملیاتی انجام نمی‌دهد.

`GET /api/v1/collaterals/r3-workflow/{kind}/{requestId}/posting-readiness`، `kind` فقط `sponsor|insurance|valuation`، نیازمند Scope خواندن وثایق و شعبه مستخرج از principal است. جدول و ستون‌ها از `Kind` کد ثابت می‌آیند، نه ورودی کاربر. برای هر درخواست، **یک SELECT** شامل Stage و Event و رکورد وثیقه/نوع و تعارض جدول عملیاتی انجام می‌شود. Payload hash از ستون‌های Stage بازتولید می‌شود؛ الگوریتم Hash موجود Stage در یک کلاس مشترک بازآرایی شده تا رفتار بررسی در transition و preflight یکسان بماند.

### فهرست کنترل‌های شواهد

| کد | معنای کنترل | اصل محافظه‌کاری |
|---|---|---|
| `STAGE_REVIEWED` | آخرین وضعیت داخلی Stage | `REVIEWED` معادل تأیید بانکی نیست |
| `STAGE_VERSION` | نسخه ۳ در جریان کنونی Draft/Submit/Review | مدل فعلی؛ تغییر فرآیند مستلزم به‌روزرسانی قرارداد |
| `PAYLOAD_FINGERPRINT` | SHA-256 محاسبه‌شده از داده واقعی Stage | هیچ Hash ارسال‌شده از UI قبول نمی‌شود |
| `CREATED_AUDIT` | یک Create توسط Maker و Snapshot معتبر | نبود/تکرار رویداد = Blocked |
| `SUBMITTED_AUDIT` | یک Submit توسط Maker با دلیل و Hash معتبر | از Stage Event Oracle خوانده می‌شود |
| `INDEPENDENT_REVIEW_AUDIT` | یک Review توسط غیر Maker با دلیل و Hash معتبر | عدم تفکیک Maker/Reviewer = Blocked |
| `AUDIT_EVENT_COUNT` | دقیقاً ۳ رخداد معتبر برای Reviewed | رخداد اضافی نیز نیاز به رفع مغایرت دارد |
| `COLLATERAL_NOT_RELEASED` | شناسنامه فاقد وضعیت پایان شناخته‌شده | سایر وضعیت‌ها همچنان نیازمند سیاست بانک‌اند |
| `COLLATERAL_TYPE_ACTIVE` | نوع وثیقه ACTIVE | پیش‌شرط لازم، ناکافی برای posting |
| `NO_PHYSICAL_DUPLICATE` | عدم تعارض ضامن/بیمه‌نامه/مرجع ارزیابی در جدول اصلی | فقط یک بررسی خواندنی در زمان پاسخ |
| `VALUATION_SUPERSESSION_STABLE` | اشاره‌گر جاری ارزیابی با درخواست سازگار است | کنترل مثبت در یک لحظه، نه قفل نهایی |
| `VALUATION_REFERENCE_DUPLICATION` | اگر مرجع ارزیابی خالی است نتیجه UNKNOWN | به‌اشتباه PASS اعلام نمی‌شود |

**نکته هم‌زمانی:** این Endpoint snapshot خواندنی Oracle است و قرار نیست نتیجه‌اش بین دو تراکنش معتبر باقی بماند. در زمان صدور مجوز نهایی، تمامی کنترل‌ها باید مجدداً در تراکنش `SERIALIZABLE`/row-lock یا سازوکار مصوب معادل، همراه با `RECORD_VERSION` و Idempotency اجرا شوند. سرویس فعلی `finalPostingEnabled=false`، `finalPostingEligible=false` و `policyApproved=false` بازمی‌گرداند، حتی اگر تمام کنترل‌های فنی موفق باشند.

### وضعیت تصمیم‌ها و گیت‌های خروج R3

- ضامن: G-05/G-11/G-09؛ موجودیت `COLLATERAL_SPONSOR` وضعیت/سابقه تأیید و لغو کافی ندارد.
- بیمه‌نامه: G-04/G-06/G-09؛ قرارداد الزامات پوشش، ابطال و تمدید قطعی تعیین تکلیف نشده است.
- ارزیابی: G-07/G-18/G-09؛ کد فیزیکی `FINAL`، قرارداد تأیید، دوره تجدید ارزیابی و منبع روز کاری هنوز مصوب نیست.
- همه حوزه‌ها: Schema/DML عملیاتی، سازوکار Audit، بازگشت/جبران و مجوز ثبت نهایی نیازمند تأیید DBA، معماری داده و مالک کسب‌وکار است.
- سه Migration Stage موجود نیز Candidate/Fail-Closed باقی مانده‌اند؛ اجرای خودکار Oracle DDL/DML انجام نمی‌شود. تغییر IAM، پیکربندی DataSource و سایر ماژول‌ها خارج از Scope است.

### مستندات مرجع و ردیابی

- `BUC-06/SUC-07/SVC-05` ضامن؛ `BUC-07/SUC-09/SVC-07` بیمه؛ `BUC-08/SUC-10/SVC-08` ارزیابی.
- `CLT-R3-POSTING-DECISION-MATRIX-FA.md` حاوی پیش‌نیازهای تصمیم‌گیری و قرارداد نوشتن اتمیکِ پیشنهادی، نه سیاست تصویب‌شده است.
- Roadmap R1.0 و Gap Register R1.0 با افزودن Checkpoint این نسخه، **بدون بستن هیچ Gap**، به‌روزرسانی شدند.

### شواهد QA و محدودیت‌ها

- Smoke Java مستقل، parsing Java، typecheck با Stubهای synthetic، Contract SQL خواندنی، parse TypeScript و آزمون نصب/نصب مجدد/rollback محافظت‌شده. گزارش دقیق در `QA-REPORT.json`.
- **انجام‌نشده:** Build کامل Maven/Angular روی ماشین مقصد، Query زنده روی Oracle بانک، آزمون وابستگی واقعی JDBC، UAT جامع تحلیلگر و اجرای Migration مصوب. موفقیت نصب به‌تنهایی به معنی PASS این موارد نیست.
- **گام بعد:** تصویب داده/سیاست G-04/G-05/G-06/G-07/G-09/G-11/G-18 و پیاده‌سازی Final Posting اتمیک تحت کنترل کامل، بدون فعال‌سازی قبل از شواهد Oracle و اجازه رسمی.
