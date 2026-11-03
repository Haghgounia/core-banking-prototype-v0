# CLT-R4C — ثبت پایدار درخواست تخصیص و کنترل هم‌زمانی درخواست

**نسخه محصول:** 0.11.0  
**مرجع نقشه راه:** CLT-ROADMAP-R1.0 / فاز R4 «ظرفیت و تخصیص بین‌دامنه‌ای»  
**وابستگی اجرایی:** R4B، خواندن امن R4A، گیت JWT/Client همان R4B.  
**وضعیت:** سورس توسعه‌یافته؛ اسکریپت بانک تایید نشده؛ Write پیش‌فرض OFF؛ تخصیص مالی OFF.

## ۱. مرز فنی

R4B یک Preflight فقط‌خواندنی است. R4C برای **ثبت یک درخواست غیرعملیاتی** سه موجودیت مرحله‌ای پیشنهاد می‌کند: `CLT_ALLOCATION_REQUEST`، `CLT_ALLOCATION_REQUEST_DEBTOR` و `CLT_ALLOCATION_REQUEST_EVENT`. با ثبت معتبر، وضعیت فقط `RECORDED_NOT_ALLOCATED` خواهد بود. حتی ثبت موفق، تخصیص، تأیید تعهد، بلوکه کردن ظرفیت، Hold در DPS2 یا گواهی کفایت بیمه/ضامن/ارزیابی نیست.

هیچ DML روی `CLT.COLLATERAL`, `CLT.COLLATERAL_ALLOCATION`, `CLT.COLLATERAL_TRANSACTION` یا جداول مالی DPS2 وجود ندارد. پروژه و Installer هیچ DDL/DML اجرا نمی‌کنند. طرح SQL **نامصوب و عمداً غیرقابل اجرا** است.

## ۲. APIها

| Method | Path | Scope | نتیجه |
|---|---|---|---|
| POST | `/api/v1/collaterals/{collateralId}/allocation-requests/intake` | `clt.allocation.request` + JWT client-credentials + Client Binding مصوب | Receipt مرحله‌ای، بدون تخصیص |
| GET | `/api/v1/collaterals/{collateralId}/allocation-requests/intake/readiness` | `clt.collateral.read` | وضعیت گیت / اسکیمای مرحله‌ای، بدون ادعای آماده‌بودن عملیات مالی |
| GET | `/api/v1/collaterals/{collateralId}/allocation-requests/intake/journal?limit=20` | `clt.collateral.read` + feature flag | فهرست محدود و بدون شناسه بدهکاران، فقط شعبه JWT |
| GET | `/api/v1/collaterals/{collateralId}/allocation-requests/intake/receipts/{requestId}?consumerDomain=LOAN` | `clt.allocation.request` + Client Binding همان دامین | دریافت Receipt صرفاً Client و شعبه اصلی |

Body برای POST و `Idempotency-Key: <UUID>` همان قرارداد R4B است؛ مبلغ و شناسه‌ها **string** با دقت Oracle هستند. Headerهای قابل جعل برای شعبه، Client و Actor پذیرفته نمی‌شوند. Client_ID با `azp` و دامنه/Grant JWT تطبیق می‌یابد.

## ۳. قرارداد تراکنشی

1. احراز JWT و Scope، Client Binding و شعبه؛ Normalization پایدار مبلغ و مرتب‌سازی بدهکاران؛ Digest شامل Client/شعبه/وثیقه و محتوا.
2. گیت `request-writes-enabled=false` پیش‌فرض؛ نیاز به حساب غیرصاحب‌اسکیما و مجوز محدود تنها روی سه جدول مرحله‌ای؛ schema/unique constraint readiness کنترل می‌شود.
3. داخل یک تراکنش، ابتدا درخواست با کلید همان Client/دامین به‌صورت `SELECT ... FOR UPDATE` خوانده می‌شود. اگر موجود بود، فقط Digest دقیقاً برابر بازپخش می‌شود؛ عدم تساوی `409 CONFLICT` است.
4. در درخواست جدید، وجود وثیقه برای همان شعبه در CLT بررسی می‌شود و تعارض شناسه‌های بدهکار با ضامن فعلی نیز صرفاً به‌عنوان شواهد خوانده می‌شود. این خواندن **قفل ظرفیت یا Snapshot اتمیک مالی نیست**.
5. در همان Unit of Work، سطر درخواست، حداکثر ۲۰ سطر بدهکار و رویداد `REQUEST_RECORDED` ثبت می‌شود. اگر هر INSERT شکست خورد، همه Rollback می‌شوند.
6. Oracle UNIQUE `(CONSUMER_DOMAIN, CLIENT_ID, IDEMPOTENCY_KEY)` تعارض هم‌زمانی همان درخواست را تضمین می‌کند؛ استثنای Unique با خواندن مجدد Receipt پس از commit/rollback به پاسخ Idempotent یا 409 تبدیل می‌شود. Replay **رویداد جدید یا سطر تخصیص ایجاد نمی‌کند**.
7. `capacityLocked=false`, `consumingObligationVerified=false`, `debtorRosterVerified=false`, `allocationAuthorized=false` در همه Receiptها ثابت هستند.

**محدوده کنترل هم‌زمانی:** قفل‌های R4C صرفاً به *کلید درخواست* مربوط‌اند؛ هنوز `SELECT ... FOR UPDATE` روی `CLT.COLLATERAL` در همان تراکنش Allocation و تجدید محاسبه ظرفیت زیر قفل نداریم. نباید نام R4C را «تخصیص اتمیک» تفسیر کرد.

## ۴. کنترل اختیاری فعال‌سازی

در پیکربندی اختصاصی `backend/src/main/resources/clt/allocation-intake.properties`:

```properties
core-banking.collateral.allocation.intake.request-writes-enabled=${CLT_ALLOCATION_INTAKE_WRITES_ENABLED:false}
core-banking.collateral.allocation.intake.writer-jdbc-url=${CLT_ALLOCATION_INTAKE_JDBC_URL:}
core-banking.collateral.allocation.intake.writer-jdbc-username=${CLT_ALLOCATION_INTAKE_JDBC_USERNAME:}
core-banking.collateral.allocation.intake.writer-jdbc-password=${CLT_ALLOCATION_INTAKE_JDBC_PASSWORD:}
```

این‌ها **فقط پس از تصویب مکتوب مدل، اجرای Migration توسط DBA، ایجاد حساب کم‌اختیار، و اعتبارسنجی JWT/Issuer/Audience قابل فعال‌سازی‌اند**. فعال‌سازی ثبت درخواست هرگز مجوز نوشتن مالی نیست. در حالت پیش‌فرض حتی اتصال Writer ساخته نمی‌شود.

## ۵. شکاف‌ها و معیار خروج

- G-02: بخش «ثبت پایدار درخواست و Audit/Idempotency» در سورس طراحی و پیاده‌سازی شده است، اما اجرای واقعی Oracle، کنترل Migration و مشتری سرویس هنوز تأیید نشده‌اند؛ لذا **باز** است.
- G-09: نبود Adapter معتبر تأیید تعهد، پرونده و کل فهرست بدهکاران؛ باز.
- G-13: دو مجوز مستقل نوع وثیقه و سطح معرفی برای بین‌شعبه‌ای؛ باز.
- G-04/05/06/07/10/11/18: قواعد بانکی مربوط به ارزش، ضامن، بیمه و پذیرش؛ باز.
- G-16: مغایرت Seed و مقدار پیش‌فرض وضعیت تراکنش؛ باز.
- قفل تراکنشی ظرفیت/Allocation و نگاشت نهایی `MODULE_ID`؛ باز.

**QA موردنیاز پیش از نتیجه عملیاتی:** Build واقعی Maven/Angular، صحت کلاس‌ها در JAR، آزمون Oracle Transaction/Rollback/Concurrent INSERT/Replay/Conflict/Privileges، اجرای JWT واقعی و UAT تحلیلگر.
