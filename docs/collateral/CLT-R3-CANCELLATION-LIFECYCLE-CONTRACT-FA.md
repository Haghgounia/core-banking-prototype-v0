# قرارداد نسخه‌دار چرخه لغو بیمه‌نامه وثیقه — CLT R3 / G-06

**تاریخ:** ۱۴۰۵/۰۸/۱۹ (۲۰۲۶-۱۱-۱۰)  
**وضعیت:** Candidate — کد پیاده‌سازی شده، فعال‌سازی بانکی و Oracle Live تأیید نشده  
**مراجع:** BUC-07 / SUC-09 / BR-08 / BR-12 / BR-14، مشخصات DB و UI مرجع، `CLT-ROADMAP-R1.0-FA.md`، `CLT-GAP-REGISTER-R1.0-FA.md`.

## ۱. تفکیک مفهوم لغو

- **درخواست لغو (Stage):** داده مستقل از `CLT.COLLATERAL_INSURANCE`، با `DRAFT → SUBMITTED → REVIEWED/REJECTED/WITHDRAWN`؛ `REVIEWED` صرفاً تایید مرحله‌ای بازبین است.
- **ثبت رویداد لغو (Posting):** با `COMMITTED` درخواست و ایجاد یک رکورد **Append-only** در `CLT.R3_INS_CANCEL_EVENT` انجام می‌شود؛ خود بیمه‌نامه تاریخی به هیچ‌وجه حذف یا بازنویسی نمی‌شود.
- **اثر لغو بر پوشش / تخصیص:** مستقل و **حل‌نشده تا تصویب سیاست G-04/G-06/G-09 و اتصال مصرف‌کنندگان R4** است. وجود Event به‌تنهایی مجوز از بین‌رفتن تعهد یا انعقاد تخصیص جدید نیست.
- **تمدید یا انقضای بیمه:** لغو تلقی نمی‌شود؛ ارتباط با `PREVIOUS_INSURANCE_ID` حفظ می‌شود.

## ۲. قرارداد اجرایی مسیر جدید

| محور | پیاده‌سازی | محدودیت / شرط عملیاتی |
|---|---|---|
| Read-only Evidence | `GET /api/v1/collaterals/{collateralId}/insurance-policies/{insuranceId}/cancellation-readiness` | Oracle فعلی؛ Scope خواندن + Branch، بدون DML؛ نبود جدول Event به‌صورت `UNKNOWN/BLOCKED` |
| فعال‌سازی | `GET /api/v1/collaterals/insurance-policies/cancellation-activation` | Feature Flag Stage و Final مستقل، Schema + Bank Release + اختیار کاربر |
| آغاز درخواست | `POST /api/v1/collaterals/{collateralId}/insurance-policies/{insuranceId}/cancellation-requests` | درخواست Stage فقط با Flag روشن، Hash/Version/Idempotency و Schema مصوب |
| بررسی مرحله‌ای | `POST /api/v1/collaterals/insurance-policies/cancellation-requests/{requestId}/transition` | Maker/Reviewer مستقل، CAS نسخه، Audit و Revalidation مثبت؛ COMMITTED تنها از مسیر Posting |
| رسید و فهرست | `GET .../cancellation-requests/{requestId}/receipt`, `GET .../mine`, `GET .../pending` | شعبه‌محور و مجوزمحور؛ Receipt فقط در صورت وجود Event واقعی |
| ثبت رویداد | `POST .../cancellation-requests/{requestId}/commit` | مسیر کدنویسی شده؛ **Flag خاموش**؛ زیرساخت prototype اجازه POST قطعی نمی‌دهد، تا گیت‌های رسمی بانک فراهم شوند |
| تاریخچه | Event و Audit با Insert-only و Trigger پیشنهادی منع UPDATE/DELETE | Migration هنوز تأیید و اجرا نشده |
| DML دامنه | **فقط Insert رویداد** در صورت فعال‌سازی رسمی | هیچ `DELETE/UPDATE` روی بیمه اصلی یا تخصیص‌های موجود وجود ندارد |

## ۳. محافظت‌های داده و کسب‌وکار

1. **بدون تخصیص:** هر ردیف در `CLT.COLLATERAL_ALLOCATION` (حتی اگر `USED='NO'`) موجب توقف Posting می‌شود؛ چون نبود تعهد فعال از این ستون به‌تنهایی استنتاج نمی‌شود. این محافظه‌کاری مانع تشخیص غلط «رفع تخصیص» می‌شود.
2. **بدون جانشین قبلی:** بیمه‌نامه‌ای که فرزند تمدید (`PREVIOUS_INSURANCE_ID`) دارد، تا تعیین قرارداد زنجیره لغو در G-06 وارد Posting نمی‌شود.
3. **پوشش جایگزین:** برای ورود به Posting، شواهد مصوب جانشین از جدول مستقل `R3_INS_CANCEL_IMPACT`، هم‌وثیقه، هم‌نوع پوشش، هم‌ارز، معتبر در روز بانکی و فاقد رویداد لغو قبلی لازم است. این تنها یک **شرط لازم** است؛ کفایت همه پوشش‌های وثیقه با این شرط ثابت نمی‌شود.
4. **موافقت بانک:** جدول `R3_INS_CANCEL_RELEASE` باید تصمیم‌های واقعی Business/Data/Coverage/Allocation/Integration/Downstream را با Revision و تأییدکننده ثبت کرده باشد. هیچ Seed Approval انجام نشده است.
5. **Maker/Reviewer/Poster/Impact Approver:** هر نقش مستقل است؛ درخواست ثبت‌شده به‌وسیله همان Maker با نقش Reviewer قابل تأیید نیست؛ Poster نیز نباید همان شخص باشد.
6. **Idempotency و هم‌زمانی:** قفل‌گذاری رکورد، کنترل `RECORD_VERSION`، یکتایی Event به ازای هر بیمه و هر Request، یکتایی `(BRANCH_CODE,IDEMPOTENCY_KEY)` و بررسی Hash در تراکنش واحد؛ خطا موجب Rollback کامل می‌شود.
7. **Append-only در لایه داده:** علاوه بر نبود DML ویرایشی در کد، دو Trigger پیشنهادی `TRG_R3_INS_CANCEL_EVENT_APPEND` و `TRG_R3_INS_CANCEL_AUDIT_APPEND` باید توسط DBA تصویب و فعال باشند؛ سرویس در نبود آن‌ها Fail Closed است.
8. **عدم اتکا به تایید بیرونی:** `INSURANCE_COMPANY.STATUS` فقط وجود وضعیت مرجع را بیان می‌کند؛ اعتبار پروانه، استعلام بیمه‌نامه و تایید ذی‌نفع مستقل و باز است.

## ۴. SQL و فعال‌سازی

- `database/oracle/clt/r3cancellation/00_CANCELLATION_READONLY_DICTIONARY.sql`: فقط SELECT برای Recon.
- `database/oracle/clt/r3cancellation/01_CANCELLATION_EVENT_SCHEMA_CANDIDATE_NOT_APPROVED.sql`: در ابتدای فایل با `RAISE_APPLICATION_ERROR` و `WHENEVER SQLERROR EXIT` توقف قطعی دارد؛ بدنه پس از آن **پیشنهاد طراحی است، نه Migration اجرایی**.
- Configuration Flags: `core-banking.collateral.r3-cancellation.stage-enabled=false` و `core-banking.collateral.r3-cancellation.final-enabled=false` در صورت نبود مقدار صریح. Installer هیچ Config یا Flag را روشن نمی‌کند.
- استفاده از همان `applicationDataSource` برنامه و Schema `CLT`؛ **هیچ User یا DataSource جدید** ساخته نمی‌شود.
- برای فعال‌سازی Final، علاوه بر DDL مصوب و Rule، نیاز به قرارداد امنیتی/تفکیک نقش و اصلاح کنترل‌شده ALLOWLIST سرویس است؛ در prototype محلی، مسیر قطعی عمداً از جانب لایه HTTP مسدود است.

## ۵. وضعیت شکاف‌ها و معیار اتمام R3

- **G-04:** حداقل پوشش، انواع اجباری، جایگزینی و کفایت مجموعه بیمه‌ها — `OPEN`.
- **G-06:** سیاست لغو، زمان اثر، تضامین، تمدید/حالت تاریخی، شواهد مصوب بانک و پذیرش مصرف‌کننده — `OPEN`.
- **G-09:** تبادل با شرکت بیمه و سرویس مصرف‌کننده R4 — `OPEN`.
- **D-07:** امضای DBA و نصب/تأیید جدول‌های Event/Stage/Impact/Release و Triggerهای Append-only — `OPEN`.
- `UAT/OracleLive`: اجرا نشده. R3 هنوز Closure کسب‌وکاری ندارد و **بدون تصویب تصمیم‌ها به R4 مهاجرت نمی‌کند**.

## ۶. شواهد QA

`tools/clt/r3-cancellation/R3CancellationSmoke.java`: آزمون‌های خالص سیاست و Guard؛ همراه بسته `QA-REPORT.json`، `SOURCE-DIFF.patch` و نصب‌کننده Hash/Backup/Rollback. آزمون‌های ساختگی Spring جایگزین Maven/Oracle/Angular واقعی نیستند.
