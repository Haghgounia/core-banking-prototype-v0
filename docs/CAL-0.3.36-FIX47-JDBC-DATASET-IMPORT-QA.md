# CAL 0.3.36 / FIX47 — JDBC Calendar Dataset Import

## هدف

افزودن مسیر Import مستقیم Dataset چهارصدساله تقویم از مرورگر به Oracle با همان DataSource برنامه، بدون نیاز به `sqlldr`, SQL*Loader یا Oracle Client در سیستم کاربر.

## فایل‌های هدف

- `calendar_day.csv` → `CAL.CALENDAR_DAY`
- `calendar_date.csv` → `CAL.CALENDAR_DATE`

فایل‌های ارائه‌شده برای QA:

| فایل | رکورد داده | حجم تقریبی | SHA-256 |
|---|---:|---:|---|
| calendar_day.csv | 146,462 | 5.3 MB | `cedd41829fdaea732594bffe9ceb5414a95cc0c55597112563239681a279e3e7` |
| calendar_date.csv | 439,386 | 31 MB | `9b7b20c7a2a0f9704b296941c43cfd13c595b9e66f8189ff32f568facf3d26ff` |

این Hashها با `calendar_manifest.json` نسخه Enterprise Calendar v1.0.0 یکسان‌اند.

## طراحی Runtime

### UI

مسیر جدید:

`/calendar/reference-data/import`

از منوی مستقل «اطلاعات پایه تقویم سازمانی» قابل دسترسی است. UI قبل از فعال کردن دکمه Import وضعیت Schema را از Backend دریافت می‌کند و این موارد را نشان می‌دهد:

- تعداد `CALENDAR_DAY`
- تعداد `CALENDAR_DATE`
- تعداد سیستم‌های تقویم
- تعداد الگوریتم‌ها
- تعداد روزهای هفته
- تعداد ماه‌ها
- آماده بودن Seed
- خالی بودن Dataset

### API

- `GET /api/v1/calendar/dataset-import/status`
- `POST /api/v1/calendar/dataset-import/import` با `multipart/form-data`
  - part: `calendarDayFile`
  - part: `calendarDateFile`

### ایمنی Import

1. هر دو فایل الزامی و CSV هستند.
2. حداکثر اندازه هر فایل 64MB است؛ کل Request تا 96MB مجاز است.
3. جدول‌های Dataset در ابتدای تراکنش Lock می‌شوند تا Import موازی رخ ندهد.
4. Seedهای سه سیستم تقویم، سه الگوریتم، 7 روز هفته و 36 ماه کنترل می‌شوند.
5. اگر `CALENDAR_DAY` یا `CALENDAR_DATE` داده داشته باشد Import متوقف می‌شود؛ Append ناخواسته مجاز نیست.
6. CSV به‌صورت Streaming خوانده می‌شود و کل Dataset در Heap نگهداری نمی‌شود.
7. INSERT با JDBC Batchهای 1,000 رکوردی انجام می‌شود.
8. ترتیب ثبت: `CALENDAR_DAY` سپس `CALENDAR_DATE`.
9. خطای Parse، Constraint، FK یا Validation باعث Rollback کل تراکنش می‌شود.
10. SHA-256 هر دو فایل برای Audit محاسبه و در نتیجه نمایش داده می‌شود.

## کنترل‌های پس از Load قبل از Commit

- `calendar_date_rows = calendar_day_rows × 3`
- برای هر `DAY_ID` دقیقاً سه نمایش تاریخ
- عدم Gap در `CANONICAL_DATE`
- پیوستگی `DAY_ID`
- کنترل `JULIAN_DAY_NUMBER = EPOCH_DAY + 2440588`
- تطبیق `ISO_WEEKDAY_NO` با محاسبه ISO خود Oracle
- عدم وجود `CALENDAR_SYSTEM_CODE` خارج از:
  - `GREGORIAN`
  - `SOLAR_HIJRI_IR`
  - `HIJRI_CIVIL`
- تعداد هر سیستم باید دقیقاً برابر تعداد روزها باشد.

## کنترل فایل‌های واقعی پیوست

کلاس CSV Parser نسخه FIX47 به‌صورت مستقل با Java 21 روی هر دو فایل واقعی اجرا شد:

`days=146462 dates=439386 ratio=true`

Header و تمام 585,848 رکورد داده توسط Parser جدید بدون خطا خوانده شدند.

## Build/QA

- `tools/verify-calendar-reference.mjs`: PASS
- `tools/verify-calendar-dataset-import.mjs`: PASS — 16 کنترل Static
- کامپایل مستقل `CalendarDatasetModels` و `CalendarDatasetCsvParser` با Java: PASS
- Parse کامل فایل‌های واقعی: PASS
- Maven/Angular build در محیط بسته‌بندی اجرا نشد زیرا Maven Wrapper نیازمند Download خارجی بود؛ Build نهایی باید با `build-production.cmd` در محیط پروژه اجرا شود.

## Database Migration

هیچ DDL/Migration جدید لازم نیست. مدل فیزیکی CAL باید مطابق Enterprise Calendar v1.0.0 از قبل نصب شده باشد و Seed اولیه قبل از Import Dataset وجود داشته باشد.
