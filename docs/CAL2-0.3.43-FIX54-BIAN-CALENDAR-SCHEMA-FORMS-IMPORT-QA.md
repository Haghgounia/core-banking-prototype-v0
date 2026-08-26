# CAL2 0.3.43 / FIX54 — BIAN 400-Year Calendar Schema, Forms & JDBC ZIP Import

## هدف

مدل دوم تقویم از بسته `BIAN_Calendar_400Y_Oracle_Import` بدون ادغام با مدل موجود `CAL` در Schema مستقل `CAL2` پیاده‌سازی شده است. هدف این Fix، ایجاد ساختار Oracle، فرم‌های مستقل اطلاعات پایه و Import مستقیم بسته ۱۵-CSV از طریق JDBC است.

## مشخصات Dataset مبنا

مقادیر زیر از README/manifest خود بسته تحویلی استخراج شده‌اند:

- بازه Gregorian Canonical: `1826-01-01` تا `2225-12-31`
- تعداد روز Canonical: `146097`
- تعداد نگاشت `CALENDAR_DATE`: `438291`
- ICU: `76.1`
- Variantها:
  - `GREGORIAN_ICU`
  - `PERSIAN_ICU_CALCULATED`
  - `ISLAMIC_CIVIL_ICU_CALCULATED`
- فایل‌های Event/Business Calendar در بسته اولیه Header-only هستند و عمداً داده پیش‌بینی‌شده ۴۰۰ ساله ندارند.

تذکر منبع: Islamic Civil الگوریتمی است و معادل تاریخ رسمی آینده مبتنی بر رؤیت هلال ایران محسوب نمی‌شود.

## ۱۵ جدول CAL2

1. `CALENDAR_SYSTEM`
2. `SOURCE_AUTHORITY`
3. `DATASET_VERSION`
4. `CALENDAR_VARIANT`
5. `CALENDAR_MONTH`
6. `WEEKDAY`
7. `CANONICAL_DAY`
8. `CALENDAR_DATE`
9. `EVENT_TYPE`
10. `EVENT`
11. `EVENT_OCCURRENCE`
12. `BUSINESS_CALENDAR`
13. `BUSINESS_CALENDAR_DAY`
14. `VALIDATION_RUN`
15. `VALIDATION_RESULT`

DDL شامل PK، Unique، FK، Check Constraint، Index، Comment جدول و ۱۴۴ Comment فارسی ستون است. هیچ FK بین `CAL2` و `CAL` ایجاد نشده است.

## گروه‌بندی فرم‌ها

| گروه | فرم‌ها |
|---|---|
| تعاریف تقویم | Calendar System, Calendar Variant, Calendar Month, Weekday |
| منبع و نسخه Dataset | Source Authority, Dataset Version |
| Dataset چهارصدساله | Canonical Day, Calendar Date |
| مناسبت‌ها | Event Type, Event, Event Occurrence |
| تقویم کاری | Business Calendar, Business Calendar Day |
| کنترل و ممیزی | Validation Run, Validation Result |

### سیاست ویرایش

فقط‌خواندنی:
- `DATASET_VERSION`
- `CANONICAL_DAY`
- `CALENDAR_DATE`
- `VALIDATION_RUN`
- `VALIDATION_RESULT`

قابل نگهداری از UI:
- Calendar System / Variant / Month / Weekday / Source Authority / Event Type
- Event / Event Occurrence
- Business Calendar / Business Calendar Day

برای جداول foundational حذف از UI غیرفعال است تا وابستگی Dataset شکسته نشود. Event و Business Calendar قابلیت CRUD دارند و FKهای Oracle همچنان مرجع نهایی integrity هستند.

## مسیرهای UI و API

UI:
- `/calendar2/reference-data`
- `/calendar2/reference-data/import`
- `/calendar2/reference-data/:resource`

API Reference Data:
- `/api/v1/calendar2/reference/catalog`
- `/api/v1/calendar2/reference/{resource}`
- `/api/v1/calendar2/reference/{resource}/lookup`

API Import:
- `POST /api/v1/calendar2/dataset/import`
- Multipart part: `packageFile`

## Import ZIP

Importer فایل ZIP اصلی را می‌پذیرد و ۱۵ فایل زیر را با basename پیدا می‌کند؛ بنابراین وجود پوشه `data/` داخل ZIP مشکلی ایجاد نمی‌کند. Import به ترتیب `01` تا `15` انجام می‌شود تا FKها رعایت شوند. Batch size برابر 1000 است و کل عملیات در یک Transaction Spring اجرا می‌شود. پس از اتمام موفق همه Insertها Transaction Commit می‌شود؛ خطای ساختار CSV، تبدیل نوع داده یا Constraint/JDBC موجب Rollback می‌شود.

Importer هیچ `TRUNCATE`/`DELETE` خودکار ندارد؛ اجرای مجدد روی Dataset موجود می‌تواند طبق انتظار با PK/Unique Constraint متوقف شود.

## نصب Oracle

ترتیب پیشنهادی:

```sql
-- SYSTEM/SYS or CREATE USER privilege
@database/oracle/cal2/00-create-cal2-schema.sql

-- CAL2 or privileged account
@database/oracle/cal2/01-create-cal2-tables.sql

-- فقط اگر DataSource برنامه با User دیگری متصل است
@database/oracle/cal2/02-grant-cal2-to-application-user.sql

-- read-only verification
@database/oracle/cal2/99-verify-cal2-schema.sql
```

در محیطی که Tablespace `USERS` وجود ندارد، `00-create-cal2-schema.sql` باید مطابق Tablespace استاندارد همان محیط تنظیم شود.

## System Specification / Tooling

- `core-banking.schemas.calendar2: CAL2` به Config اضافه شده است.
- `CAL2` در ابزار مقایسه EA ↔ Oracle و Oracle → EA XMI Export قابل انتخاب است.
- مجموع فرم‌های اطلاعات پایه/تقویم اکنون `200` است: `20 GEO + 99 CIF + 50 DPS + 16 CAL + 15 CAL2`.

## کنترل‌های انجام‌شده در محیط بسته‌بندی

- تطبیق وجود هر ۱۵ CSV و Headerها با Repository Import.
- کنترل تعداد فایل‌ها و Row countهای manifest/source package.
- کنترل عدم وجود multiline CSV در Dataset تحویلی؛ Parser خط‌محور برای این بسته کافی است.
- کنترل ساختار DDL، تعداد ۱۵ Table و ۱۴۴ Column Comment.
- اجرای Verifierهای استاتیک پروژه، شامل Verifier اختصاصی CAL2.
- Compile مستقل کلاس‌های خالص Java و compile harness برای Sourceهای جدید Backend با Stubهای Spring/JDBC جهت کشف خطاهای Syntax/Type در کد جدید. این کنترل یک IOException کنترل‌نشده در loop خواندن CSV را قبل از تحویل شناسایی کرد و Repository اصلاح و مجدداً با موفقیت Compile شد.
- Parser واقعی Java روی هر ۱۵ CSV اجرا شد: جمعاً ۵۸۴٬۴۶۱ رکورد داده بدون خطای Header/Column parse شد.
- PK/UQ/FKهای Dataset پرشده و ظرفیت VARCHAR2/CHARهای DDL در برابر داده تحویلی کنترل شدند.
- Sourceهای TypeScript جدید/تغییریافته با TypeScript transpile از نظر syntax کنترل شدند.

Build کامل Maven/Angular در محیط بسته‌بندی اجرا نمی‌شود زیرا Maven/Node dependencies پروژه در این محیط Cache نشده‌اند؛ Build نهایی Integration باید با `build-production.cmd` در محیط ویندوز پروژه انجام شود.
