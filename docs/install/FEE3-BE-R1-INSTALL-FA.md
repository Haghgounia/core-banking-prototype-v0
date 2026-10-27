# FEE3-BE-R1 — Backend مرحله اول کارمزد ۳

## وضعیت و مرزهای اجرا
- مبنا: Core Banking Prototype `0.11.0`، رابط FEE3-R8 با Hotfix R8-HF1، و اسکیمای ازپیش‌موجود `FEE3` (۱۱ جدول / Seed اجراشده).
- هیچ DDL یا اجرای مجدد Seed لازم نیست؛ بسته این مرحله **هیچ SQL نوشتنی** ندارد.
- مسیرهای خواندنی به Oracle متصل هستند؛ ویزارد Angular همچنان **آفلاین** است. Adapter خواندن آماده است اما نباید وضعیت «ذخیره در Oracle» را در UI القا کند.
- مسیرهای نوشتن فقط برای اقلام تازه `F3-UAT-*`، نسخه ۱ `DRAFT`، بدون انتشار/فعال‌سازی/وصول/Posting، قابل فعال‌سازی هستند.
- به‌طور پیش‌فرض `FEE3_DRAFT_WRITES_ENABLED=false` و هیچ Header ارسالی از UI مجوز فعال‌سازی ندارد.
- سرویس از **همان DataSource مشترک Spring** استفاده می‌کند؛ برای `FEE3` Connection یا Oracle user جدا لازم نیست. حساب فعلی باید دسترسی‌های لازم را داشته باشد.
- این بسته یک نمونه UAT است، **احراز هویت عملیاتی، RBAC و تفکیک Maker/Checker هنوز ندارد**. توکن UAT جایگزین احراز هویت سازمانی نیست.

## نصب
فایل Patch را در ریشه پروژه `D:\Projects\core-banking-prototype-v0` استخراج کرده و فایل‌های هم‌نام `build-production.*` را جایگزین کنید. از هر فایل اصلی قبل از جایگزینی نسخه پشتیبان بگیرید.

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\fee3-backend\verify-r1.mjs
bin\stop.cmd
build-production.cmd
bin\start.cmd
```

### کنترل‌های فقط‌خواندنی REST
در صورت اجرای Backend روی پورت 8091:

```bat
curl.exe -s http://127.0.0.1:8091/api/v1/fee3/readiness
curl.exe -s "http://127.0.0.1:8091/api/v1/fee3/items?page=0&size=10"
curl.exe -s http://127.0.0.1:8091/api/v1/fee3/references/operations
curl.exe -s http://127.0.0.1:8091/api/v1/fee3/references/formulas
curl.exe -s http://127.0.0.1:8091/api/v1/fee3/items/FCE-000001
```

نتیجه مورد انتظار `readiness`: `readsEnabled=true`, `draftWritesEnabled=false`, `missingTables=[]` و شمارش واقعی رکوردها. تعداد Seed در درخواست‌های پیشین ۲۹۷ قلم و ۳۰۰ نسخه گزارش شده بود؛ **شمارش زنده ممکن است تغییر کرده باشد، بنابراین API ملاک است**.

اگر `missingTables` خالی نیست، قبل از عملیات نوشتنی سطح دسترسی یا Schema/Connection را بررسی کنید.

## فعال‌سازی آزمایشی درج Draft (فقط پس از پذیرش Readiness)
برای اجرای UAT روی دیتابیس توسعه، متغیرهای محیطی فقط در نشست اجرای Backend تنظیم می‌شوند؛ هیچ رمزی در فایل ZIP قرار نگرفته است. متغیر زیر یک رشته تصادفی دست‌کم ۲۰ کاراکتری است که نباید در UI یا Repository ثبت شود.

```bat
set FEE3_READS_ENABLED=true
set FEE3_DRAFT_WRITES_ENABLED=true
set FEE3_UAT_ACTOR=FEE3_UAT
set FEE3_UAT_WRITE_TOKEN=YOUR_PRIVATE_RANDOM_TOKEN_AT_LEAST_20_CHARS
bin\stop.cmd
bin\start.cmd
```

برای POST/PUT باید در درخواست HTTP Header زیر فرستاده شود؛ برای آزمون POST از JSON نمونه همراه بسته استفاده کنید، **اما تاریخ `validFrom` را ابتدا برابر روز جاری معتبر Oracle قرار دهید**:

```bat
curl.exe -X POST "http://127.0.0.1:8091/api/v1/fee3/drafts" -H "Content-Type: application/json" -H "X-FEE3-UAT-Token: %FEE3_UAT_WRITE_TOKEN%" --data-binary "@docs\install\FEE3-BE-R1-SAMPLE-DRAFT.json"
```

در صورت موفقیت، پاسخ شامل `calcItemId`, `tariffVersionId`, `calcCode=F3-UAT-*`, `status=DRAFT`, `published=false` است. سپس رکورد را با `GET /items/{calcCode}` بخوانید. برای PUT دو شماره `expectedItemRecordVersion` و `expectedVersionRecordVersion` باید از خروجی ذخیره/بارگذاری واقعی تنظیم شوند؛ استفاده از مقادیر قدیمی خطای `409` می‌دهد.

**هشدار:** عملیات POST/PUT پس از روشن‌کردن Feature Flag، واقعاً در Oracle توسعه درج/ویرایش می‌کند و Audit تولید می‌کند. روی دیتابیس عملیاتی اجرا نکنید. برای غیرفعال‌سازی دوباره `FEE3_DRAFT_WRITES_ENABLED=false` کنید و Backend را راه‌اندازی مجدد نمایید.

## ماتریس قرارداد داده
| بخش UI | موجودیت Oracle | سیاست R1 |
|---|---|---|
| شناسنامه | `FCE_CALC_ITEM` | درج UAT با `IS_ENABLED=0` و کد سیستمی |
| منشأ، اعتبار، مالیات/گردکردن | `FCE_TARIFF_VERSION` | فقط `DRAFT`, `IS_ACTIVE=0` |
| کانال، مشتری، گروه، محصول، عقد، ارز | `FCE_VERSION_SCOPE` | یک دامنه اصلی، `PRIORITY_NO=100` صرفاً مقدار فنی ذخیره‌سازی |
| روش، نرخ، مبلغ و JSON تخصصی | `FCE_CALC_RULE` | فقط تعریف، بدون اجرای مالی |
| سناریوهای آزمون | `FCE_TEST_CASE` | فقط تعریف ورودی/انتظار؛ هیچ نتیجه ساختگی ثبت نمی‌شود |
| تاریخچه تغییر | `FCE_AUDIT_EVENT` | رخدادهای UAT در همان تراکنش |
| واژگان و فرمول‌ها | `FCE_REF_*` | فقط خواندنی، فعال بودن عملیات/فرمول کنترل می‌شود |
| ارجاع/تأیید/انتشار | `FCE_CHANGE_REQUEST` | غیرفعال تا اتصال RBAC/Workflow |

### شکاف‌های شناخته‌شده قبل از اتصال کامل UI
1. `serviceCurrency`, `feeCurrency` و discriminatorهای `context` در مدل Angular، ستون اختصاصی مستقل ندارند. ذخیره سازگار نسخه‌های بعدی باید در یک قرارداد JSON نسخه‌گذاری‌شده تعیین تکلیف شود؛ در R1 نگاشت خودکار ساختگی انجام نمی‌شود.
2. دامنه `PRODUCT_CODES_JSON` در R1 صرفاً از نظر ساختار و مجازبودن عملیات کنترل می‌شود؛ **وجود و اهلیت محصول در PDL هنوز استعلام نمی‌شود**. فعال‌سازی تعرفه بدون این کنترل ممنوع است.
3. بررسی زنده تعارض دامنه، تعارض نسخه‌های زمانی، سقف/کف کل سبد وثایق، Formula Sandbox و ارزیابی محاسبه کامل در R1 انجام نمی‌شود.
4. Endpointهای POST/PUT برای UAT از توکن مستقل استفاده می‌کنند؛ اتصال نوشتن از مرورگر قبل از Authentication/RBAC صحیح ممنوع است.
5. استقرار Java/Oracle و Build کامل Angular/مرورگر در محیط تولید این Patch انجام نشده؛ کنترل Java در اینجا با امضاهای شبیه‌سازی‌شده کتابخانه‌ها، و آزمون سیاست مجزا انجام شده است.

## معیار پذیرش و گام بعد
- Readiness و APIهای مرجع و کاتالوگ با داده واقعی Oracle برابر باشند.
- خطاهای 403 برای نوشتن غیرفعال، 404 برای قلم ناموجود، 409 برای نسخه قدیمی/غیرپیش‌نویس، 422 برای شناسنامه و تاریخ نامعتبر کنترل شوند.
- هیچ رکورد Seed و هیچ تعرفه ACTIVE تغییر نکند.
- قبل از R2: قرارداد قطعی JSON با فیلدهای R8، اعتبارسنجی PDL، تمایز تاریخ شروع اعتبار نسخه جدید/تاریخی، RBAC و اتصال واقعی «ذخیره پیش‌نویس» در ویزارد تکمیل شوند.
