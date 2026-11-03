# گزارش تحویل CLT-R1A — جداول اطلاعات پایه وثایق

**تاریخ:** 2026-11-03  
**مرجع ثابت:** CLT-ROADMAP-R1.0 / فاز R1 «اطلاعات پایه و حاکمیت قواعد»  
**برش اجرایی:** R1A — کاتالوگ، Read-only Oracle، UI ستاد، پیش‌نمایش Seed فنی و Preflight  
**Baseline:** `core-banking-prototype-v0-2026-11-03-0910.zip` / VERSION=0.11.0

## ۱. تحویل‌های انجام‌شده

- فراداده **تمام ۱۷ جدول Reference Data و ۲۰۴ ستون** از `Collateral_DB_Specification_FA.html` تولید شد؛ PK، نوع و عنوان فارسی فیلدها در مدل JSON ثبت شد. مدل فیزیکی همچنان متعلق به Schema `CLT` است.
- **۱۱ جدول دارای مجموعاً ۵۳ ردیف Seed فنی** بودند؛ فقط برای پیش‌نمایش آفلاین در Angular استخراج شدند. ۶ جدول کسب‌وکار بدون Seed مصوب باقی ماندند: `COLLATERAL_GROUP`, `COLLATERAL_TYPE`, `COLLATERAL_ATTRIBUTE_DEFINITION`, `COLLATERAL_TYPE_ATTRIBUTE`, `COLLATERAL_INSURANCE_TYPE`, `INSURANCE_COMPANY`.
- Backend اختصاصی در `com.behsazan.corebanking.collateral.reference` ایجاد شد: APIهای GET برای catalog، descriptor، صفحه‌بندی ۱۷ جدول، جزئیات و readiness. Queryهای SELECT فقط از Allowlist ثابت تولید می‌شوند و مقادیر ورودی Parameterized هستند.
- پیکربندی **اختصاصی** `clt/r1a.properties` برای JWT HTTPS/RS256 و Oracle reader محدود؛ هیچ اتصال CLT از Connection Pool اصلی SYSTEM انجام نمی‌شود. به‌صورت پیش‌فرض `reads-enabled=false` و تمام مسیر `/api/v1/collateral/**` مسدود است.
- Frontend Angular مستقل در `features/collateral`: فهرست گروه‌بندی‌شده، جست‌وجو، نمایش ۱۷ جدول، پیش‌نمایش صریح Seed فنی، جدول داده و جزئیات همه ستون‌ها. هیچ دکمه تغییر اطلاعات وجود ندارد.
- فقط **سه نقطه اتصال Frontend** باید در Integration تغییر کنند: `app.routes.ts`، `app-shell.component.html` و `sidebar-r22-group.component.ts`. نصب‌کننده Node، فقط با Marker و Anchor همان خطوط را اضافه می‌کند؛ فایل‌های مشترک را به‌صورت کامل جایگزین نمی‌کند.
- SQL `01_CLT_REFERENCE_READONLY_PRECHECK.sql` فقط SELECT از Dictionary دارد و روی Oracle اجرا نشده است.
- تست JUnit کاتالوگ، Allowlist و Fail Closed در کنار VERIFY ایستای Node اضافه شد.

## ۲. فهرست فنی جداول

| حوزه | جدول‌ها | Seed فنی |
|---|---|---|
| ساختار، انواع و ویژگی‌ها | COLLATERAL_GROUP, COLLATERAL_TYPE, COLLATERAL_ATTRIBUTE_DEFINITION, COLLATERAL_TYPE_ATTRIBUTE, REF_COLLATERAL_ATTRIBUTE_DATA_TYPE | صرفاً ۵ ردیف Data Type |
| بیمه | COLLATERAL_INSURANCE_TYPE, INSURANCE_COMPANY | هیچ داده سیاست/شرکت بیمه‌ای بارگذاری نشد |
| ارزیابی و ارز | REF_VALUATION_TYPE, REF_VALUATION_METHOD, REF_VALUATION_STATUS, REF_CURRENCY | ۱۹ ردیف نمونه فنی |
| وضعیت و عملیات | REF_COLLATERAL_STATUS, REF_COLL_TXN_STATUS, REF_COLL_TXN_TYPE, REF_COLL_REVERSAL_REASON, REF_SOURCE_SYSTEM, REF_CHANNEL | ۲۹ ردیف نمونه فنی |

> مجموع ۵+۱۹+۲۹=۵۳. ردیف‌های Seed به هیچ دیتابیس واقعی اعمال نشده‌اند.

## ۳. قراردادهای Read API (تنها در صورت فعال‌سازی امن)

| Method | Path | توضیح |
|---|---|---|
| GET | `/api/v1/collateral/reference/catalog` | ۱۷ جدول و فراداده |
| GET | `/api/v1/collateral/reference/readiness` | کنترل دیده‌شدن ۱۷ جدول در `ALL_TABLES` |
| GET | `/api/v1/collateral/reference/{table}/descriptor` | شرح کامل فیلدها |
| GET | `/api/v1/collateral/reference/{table}?page=0&size=25&text=...` | جست‌وجوی کد/عنوان و صفحه‌بندی؛ سقف ۵۰ ردیف |
| GET | `/api/v1/collateral/reference/{table}/{primaryKey}` | جزئیات رکورد با کلید فنی عددی |

تمام عملیات POST/PUT/PATCH/DELETE در Security Chain رد می‌شوند. Scope خواندن `clt.reference.read` و Audience مورد توافق الزامی است. هر کاربر باید JWT معتبر از منبع تأییدشده داشته باشد؛ به Header دلخواه `X-User-Id` اعتماد نمی‌شود.

## ۴. نسبت R1A با کل R1 و شکاف‌ها

| Gap | اثر در R1A | وضعیت |
|---|---|---|
| G-10 | Seedهای Demo/Policy قابل پذیرش نیستند | OPEN؛ Seed فنی فقط Preview |
| G-15 | قبل/بعد تغییرات مرجع و تأیید ستاد در Schema وجود ندارد | OPEN؛ همه Writeها مسدودند |
| G-07 | `APPROVED` در UI در برابر `FINAL` مدل ارزیابی | OPEN؛ هشدار در صفحه وضعیت ارزیابی |
| G-16 | `PENDING` پیش‌فرض در برابر `PENDING_APPROVAL` Seed | OPEN؛ هشدار در صفحه وضعیت تراکنش |
| D-01/D-02/D-03 | تفاوت Sequence/Identity و semantics ستون‌ها | OPEN؛ هیچ Migration اجرا نشد |
| S-01 | سازگاری امنیت CLT و IAM در Integration | در R1A با Security Chain مستقل و Deny-by-default مدیریت موقت شد؛ نهایی در R7 |

**R1 هنوز کامل نیست.** R1B باید تغییرات امن و Audit، قواعد HQ، کنترل یکتایی، Workflow تصویب/رد و ثبت Migration مربوط به G-15 را پیاده کند. اجرای آن نیازمند تصمیم DBA/Business و سیاست امنیت مجاز است. این تحویل هیچ ادعایی درباره اجرای Oracle، موفقیت Build کامل یا UAT ندارد مگر شواهد ثبت‌شده جداگانه.

## ۵. مرز تغییرات و عدم تداخل

- **Backend جدید:** `backend/src/main/java/com/behsazan/corebanking/collateral/reference/**`.
- **Resource جدید:** `backend/src/main/resources/clt/**`.
- **Frontend جدید:** `frontend/src/app/features/collateral/**`.
- **SQL جدید:** `database/oracle/clt/r1a/**`.
- **ابزار و مستند جدید:** `tools/clt/r1a/**`, `docs/collateral/**`.
- **ادغام سه فایل مشترک:** افزودن Markerهای `CLT-R1A:ROUTES`، `CLT-R1A:SIDEBAR` و `CLT-R1A:ACTIVE-GROUP`؛ Installer فایل را بازنویسی کامل نمی‌کند.
- **بدون تغییر:** همه Packageهای IAM/CIF/DPS2/ORG/FEE/PDL/Posting، `backend/pom.xml`, `application.yml`, `frontend/package.json`, `VERSION`, هیچ DDL یا DML Oracle.

## ۶. آزمون‌ها و معیار تحویل

- بررسی ۱۷ جدول، ۲۰۴ ستون و ۵۳ Seed Preview.
- تطبیق مستقیم نام ستون‌ها و جدول‌ها با DDL/XMI و بررسی Markerهای سه فایل Frontend.
- صحت محرومیت پیش‌فرض CLT API و عدم وجود endpoint نوشتنی.
- اجرای Maven JUnit، Angular Compile و Oracle Dictionary فقط در صورت وجود زیرساخت مناسب؛ در غیاب ابزار و اتصال واقعی باید در QA با وضعیت **NOT RUN** ثبت شود، نه PASS.
- UAT طراح/تحلیلگر در UI: مسیر اطلاعات پایه وثایق، جست‌وجو، بررسی مدل و مشاهده مقادیر Preview بدون نسبت‌دادن آن‌ها به داده واقعی بانک.

**گام بعد:** R1B — API و فرم‌های ثبت/ویرایش HQ با Audit اجباری G-15 و Review/Approve و کنترل نسخه؛ بدون ورود به R2 پیش از بسته‌شدن Gate مرتبط.
