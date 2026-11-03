# قرارداد درخواست تخصیص R4B — نسخه 1.0

**محصول:** Core Banking Prototype 0.11.0  
**نقشه راه ثابت:** CLT-ROADMAP-R1.0 / فاز R4 «ظرفیت و تخصیص بین‌دامنه‌ای»  
**مراجع:** Collateral UI V1.42، BUC-09، SUC-08، SUC-11، SUC-12، SUC-19، SUC-20، API Contract FA، ساختار CLT صادرشده 2026-11-01.

## ۱. مرز عملیاتی تحویل

R4B یک **قرارداد قابل فراخوانی، فقط‌خواندنی و Fail-Closed** برای درخواست تخصیص فراهم می‌کند. ثبت درخواست در دیتابیس، تخصیص، Hold سپرده، تأیید تعهد و تغییر موجودی انجام نمی‌شود. هر پاسخ حتماً `allocationAuthorized=false`, `capacityLocked=false`, `idempotencyPersisted=false`, `consumingObligationVerified=false`, `debtorRosterVerified=false` دارد. نام `preflight` را نباید با مجوز عملیاتی یا رزرو ظرفیت اشتباه گرفت.

## ۲. APIهای اجرایی جدید

| Method | Path | Scope | سطح اعتماد |
|---|---|---|---|
| POST | `/api/v1/collaterals/{collateralId}/allocation-requests/preview` | `clt.collateral.read` | پیش‌نمایش تحلیلگر؛ هویت دامین مصرف‌کننده احراز نشده |
| POST | `/api/v1/collaterals/{collateralId}/allocation-requests/service-preflight` | `clt.allocation.preflight` | JWT سرویس، `client_id`/`azp` سازگار، `gty` یا `grant_type=client_credentials` و شناسه مجاز سمت سرور |

مسیر پیشنهادی `POST /api/v1/collateral-allocations` از سند مبنا **پیاده‌سازی نشده** و باید تا فراهم‌شدن زیرساخت اتمیک بسته بماند. هیچ مجوز `clt.allocation.write` برای این بسته تعریف نشده است.

هر دو API درخواست زیر را در بدنه می‌پذیرند؛ `Idempotency-Key` یک UUID استاندارد در Header است. این UUID و Digest صرفاً برچسب تشخیصی هستند؛ بدون ثبت پایدار، رفتار Idempotent تضمین نمی‌شود.

```http
POST /api/v1/collaterals/712/allocation-requests/preview
Authorization: Bearer <JWT with clt.collateral.read>
Idempotency-Key: 68b8fcb4-89eb-40d4-a326-d77fe2a500a1
Content-Type: application/json
```

```json
{
  "consumerDomain": "LOAN",
  "caseReference": "FAC-2026-001",
  "fileNumber": "1234567890123456789",
  "amount": "60000000.0000",
  "currencyCode": "IRR",
  "debtors": [
    {"partyId":"9000000000000000001", "role":"PRIMARY"},
    {"partyId":"9000000000000000002", "role":"JOINT"}
  ]
}
```

مبالغ و شناسه‌های Oracle رشته‌اند؛ تبدیل به Float یا JavaScript Number ممنوع است. دقیقا یک بدهکار `PRIMARY` لازم است، هر شناسه Party یکتا است و حداکثر ۲۰ بدهکار ارسال می‌شود. کدهای دامین مطابق R4A: `LOAN`, `GUARANTEE`, `LETTER_OF_CREDIT`, `GREEN_LICENSE`. این‌ها **معادل مصوب مقدار عددی `MODULE_ID` نیستند.**

## ۳. شروط احراز هویت

1. Token باید توسط Decoder اختصاصی Collateral راستی‌آزمایی شده و Scope مسیر را داشته باشد.
2. `branch_code` و Subject فقط از JWT استخراج می‌شوند؛ از فرم، پارامتر Body یا Header دلخواه خوانده نمی‌شوند. SQL فقط `CLT.COLLATERAL` همان شعبه را استعلام می‌کند.
3. در مسیر Service، `client_id` باید با `azp` موجود (در صورت حضور) برابر باشد و Token نشان‌دهنده `client_credentials` باشد.
4. نگاشت سروری `core-banking.collateral.allocation.service-clients.{loan,guarantee,letter-of-credit,green-license}` باید شناسه Client مجاز واقعی را داشته باشد؛ **همه پیش‌فرض خالی و غیرمجاز هستند**. این تنظیمات فقط با توافق IAM/مالک هر دامین و تأیید JWT Issuer/Audience فعال شوند؛ تنظیم آن‌ها تخصیص را فعال نمی‌کند.
5. پیام HTTP خطا نباید Party خارجی، SQL یا اطلاعات شعبه دیگر را افشا کند.

## ۴. قواعد اعتبارسنجی و شواهد

- اعتبارسنجی طول/ساختار Case Reference، File Number، مبلغ `NUMBER(20,4)` و ارز ISO3.
- محاسبه SHA-256 با Canonicalization مبلغ و مرتب‌سازی بدهکاران، همراه شناسه Client، شعبه، شناسه وثیقه و مشخصات پرونده؛ **Digest جایگزین Unique Index یا جدول Idempotency نیست**.
- بررسی اختلاف شناسه Party هر بدهکار اصلی/مشترک با `CLT.COLLATERAL_SPONSOR`؛ حتی یک تعارض مانع تخصیص است. چون وضعیت تأیید/لغو ضامن هنوز مدل نشده، نبود تعارض به‌معنای تأیید کفایت ضامن نیست.
- استفاده مجدد از شواهد R4A، گروه‌بندی `USED='YES'` و حساب مبلغ خام؛ عدم تبدیل ارز و عدم اجرای ضرایب مصوبنشده.
- چون استعلام‌ها snapshot سریال‌شده و همراه قفل در یک تراکنش تغییر نیستند، نتیجه ظرفیت هرگز مجوز محسوب نمی‌شود.
- Cross-branch در این نسخه صراحتاً Fail-Closed است؛ وجود پرچم Type کافی نیست (G-13).
- فهرست بدهکاران **داده ارسالی مصرف‌کننده** است؛ صحت، کامل‌بودن، مالکیت پرونده، بدهکاران واقعی و وضعیت تعهد باید توسط Adapter معتبر Consumer/Party اثبات شوند. صرف امضای JWT به‌تنهایی برای این مدرک کافی نیست.

## ۵. موانع باقیمانده پیش از Write

| شناسه | تصمیم / پیش‌نیاز رسمی |
|---|---|
| G-02 | جدول درخواست/تصمیم، Unique `(CONSUMER,REQUEST_ID)`، Snapshot و Audit، بازیابی تراکنشی/Replay/Conflict |
| G-09 | قرارداد رسمی تأیید تعهد و همه بدهکاران از Loan/Guarantee/LC؛ استعلام وضعیت/مالک تعهد |
| G-13 | دو مجوز مستقل Type و معرفی همان وثیقه برای بین‌واحدی |
| G-04/05/06/07/10/11/18 | سیاست بیمه، ضامن، دوره ارزیابی، نرخ‌ها و رفتار تغییر فوری قواعد |
| R4 | دستور اتمیک با `SELECT ... FOR UPDATE` / optimistic version و **بررسی مجدد** ظرفیت تحت قفل، ذخیره درخواست، Allocation و تراکنش در یک Unit of Work |
| G-16 | تطبیق `TRANSACTION_STATUS_CODE` و Seed پیش از اولین DML واقعی |

تخصیص‌های قبلی در اثر تغییر ارزش یا سیاست به‌صورت خودکار حذف نمی‌شوند. اگر کسری وجود داشته باشد، باید مانع تخصیص جدید شد و کسری گزارش شود.

## ۶. ساختار پایگاه داده

R4B **هیچ DDL یا DML** ندارد. فایل `database/oracle/clt/r4b/00_ALLOCATION_CONTRACT_READONLY_PRECHECK.sql` فقط ساختار واقعی جداول و محدودیت‌های موجود را برای DBA استعلام می‌کند. مدل G-02 باید در ADR مستقل و Migration مصوب آینده تثبیت شود؛ ایجاد جدول به‌صورت غیررسمی ممنوع است.

## ۷. معیار خروج و آزمون

- نصب SHA-Guard/Idempotent روی R4A، عدم تغییر IAM/CIF/DPS2/Shared.
- آزمون‌های pure Java قرارداد و تضاد بدهکار/ضامن؛ آزمون JUnit در Maven.
- Build Angular/Backend و JAR Fingerprint، کنترل `NG8102=0`.
- Runtime واقعی JWT و Oracle، اعتبار داده Service Client و UAT تحلیلگر هنوز در این محیط اثبات نشده‌اند.
- **معیار خاتمه R4B محدود به قرارداد و پیش‌نمایش است**؛ خاتمه R4 عملیاتی نیست.
