# CLT-R6B — یکپارچه‌سازی رابط و گزارش‌های واقعی مستقل از IAM

**مرجع:** `CLT-ROADMAP-R1.0`، فاز **R6: تکمیل UI و UAT جامع**  
**نسخه بستر:** Core Banking Prototype `0.11.0` + R6A  
**وضعیت:** سورس و Installer آماده؛ Build کامل Windows و Oracle Runtime هنوز باید اثبات شود.

## ۱. هدف و مرز محدوده

مطابق تصمیم پروژه، تکمیل IAM پیش‌شرط توسعه صفحات، گزارش‌های فقط‌خواندنی و آماده‌سازی UAT نیست. بااین‌حال **هر نوع دسترسی به داده واقعی همچنان با JWT معتبر، Scope و Branch Claim سرور کنترل می‌شود**. این بسته نه IAM را تغییر می‌دهد، نه Auth Guard را حذف می‌کند و نه حساب آزمایشی بدون مجوز می‌سازد.

## ۲. اتصال دقیق به Angular

سه فایل مشترک صرفاً با افزودن بلوک‌های علامت‌دار `CLT:R6B` تغییر می‌کنند، بدون جایگزینی تمام متن فایل:

| فایل مشترک | نوع تغییر |
|---|---|
| `frontend/src/app/app.routes.ts` | هفت Route Lazy به کامپوننت‌های موجود CLT |
| `frontend/src/app/layout/app-shell.component.html` | گروه منوی وثایق با شش لینک |
| `frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts` | فعال‌سازی گروه منو هنگام حضور در `/collateral` |

Routeهای دقیق: `/collateral`، `/collateral/lookup`، `/collateral/introduction`، `/collateral/uat`، `/collateral/reports`، `/collateral/reference-data` و `/collateral/reference-data/:table`.

Installer قبل از هر تغییر وجود Anchor دقیق و عدم وجود Route متعارض را بررسی می‌کند. اگر تیم دیگری متن پیرامونی را تغییر داده باشد ولی Anchor محفوظ باشد، فقط بلوک CLT افزوده می‌شود. اگر Anchor یا بلوک CLT تغییر کرده باشد، نصب **Fail-Closed** متوقف می‌شود؛ Backup و Receipt برای بازبینی تهیه می‌شود.

## ۳. گزارش‌ها و قراردادهای API

| روش و مسیر | منبع داده | محدوده/کنترل |
|---|---|---|
| `GET /api/v1/collaterals/reports/overview` | `CLT.COLLATERAL` و `CLT.COLLATERAL_ALLOCATION` | `c.BRANCH_CODE = JWT.branch_code`؛ تخصیص فعال فقط `USED='YES'`؛ Group By ارز |
| `GET /api/v1/collaterals/reports/recent-events?limit=20` | `CLT.COLLATERAL_STATUS_HISTORY` + `CLT.COLLATERAL` | همان محدوده شعبه؛ حداکثر ۵۰ سطر |
| `GET /api/v1/collaterals/reports/reference-summary` | `CLT.COLLATERAL_TYPE`, `CLT.COLLATERAL_GROUP` | شمارش فنی مرجع سراسری؛ نه گواهی سیاست ستاد |

این Endpointها فقط `GET` هستند و تحت Security Chain موجود CLT با `SCOPE_clt.collateral.read` قرار می‌گیرند. کنترل فعلی `CollateralRegistryProperties.readsEnabled()`، JWT احرازشده، `subject` و `branch_code` سرور به آن‌ها اعمال می‌شود. در نبود هر پیش‌شرط، API خطای 401/403/503 می‌دهد. در نبود اتصال واقعی، UI عدد صفر یا Mock را به‌عنوان واقعیت بانکی نمایش نمی‌دهد.

**محدودیت معنادار:** چند `SELECT` مستقل نمای یک Transaction Serializable نیستند. مبلغ‌های ارزهای متفاوت تجمیع نمی‌شوند؛ هیچ مقدار نمایش‌داده‌شده به معنی مجوز تخصیص جدید نیست. گزارش شامل شناسه بدهکاران یا پرونده مشتریان نیست؛ ردیف‌های رویداد تنها برای وثایق شعبه مجاز قابل دریافت‌اند.

## ۴. قابلیت UAT قابل حمل

میزکار UAT اکنون می‌تواند فایل JSON نسخه‌دار `CLT-R6A-UAT-EVIDENCE-1` را وارد کند. اعتبارسنجی شامل سازگاری نقشه راه، مجموعه دقیق ۱۰ سناریو، شناسه‌های یکتا، وضعیت‌های چهارگانه و حد ۲۸۰ نویسه برای یادداشت است. نتایج فایل‌های نامعتبر جایگزین وضعیت موجود نمی‌شوند؛ فیلدهای جعلی گواهی عملیات بانکی نادیده گرفته می‌شوند. فایل فقط در نشست مرورگر خوانده می‌شود و هیچ Write Backend ایجاد نمی‌کند.

## ۵. معیارهای باقی‌مانده

- `ng build` واقعی **با Route فعال CLT** و نبود خطای Template و `NG8102`؛
- باز شدن Routeها در مرورگر و درستی RTL/Responsive؛
- رفتار واقعی 401/403/503 و کنترل Data Leakage بین شعبه‌ها؛
- وجود دسترسی Read-only مجاز CLT و داده واقعی برای گزارش‌ها؛
- ثبت نتایج **واقعی** ده سناریوی UAT توسط تحلیلگر؛
- تکمیل IAM، G-01..G-22 و تصویب داده/قواعد مالی در مسیر R7 مستقل.

**هیچ DDL/DML یا Migration در این بسته اجرا نمی‌شود. تمام Writeهای بانکی کماکان غیرفعال هستند.**
