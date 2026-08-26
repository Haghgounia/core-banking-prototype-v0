# CAL 0.3.35 — FIX46: فرم‌های اطلاعات پایه تقویم سازمانی

## مبنا
این تغییر مستقیماً بر اساس مدل فیزیکی Oracle داخل بسته `core-banking-cal-enterprise-calendar-v1.0.0` انجام شده است. Schema فیزیکی مبنا `CAL` است و تغییر DDL جدیدی در این Fix اعمال نمی‌شود.

## پوشش مدل
تمام 16 جدول مدل CAL در UI پوشش داده شده‌اند:

### ساختار و داده تقویم
- `CALENDAR_SYSTEM`
- `CALENDAR_ALGORITHM`
- `WEEKDAY`
- `CALENDAR_MONTH`
- `CALENDAR_DAY` — فقط‌خواندنی
- `CALENDAR_DATE` — فقط‌خواندنی

### تقویم کاری و بانکی
- `BUSINESS_CALENDAR`
- `BUSINESS_CALENDAR_DAY`
- `CALENDAR_EXCEPTION`
- `BUSINESS_DAY_CONVENTION`

### مناسبت‌ها و رویدادها
- `OCCASION_CATEGORY`
- `OCCASION`
- `OCCASION_RULE`
- `OCCASION_OCCURRENCE`
- `CALENDAR_DAY_OCCASION`

### اصلاحات رسمی قمری
- `HIJRI_DATE_OVERRIDE`

## تصمیم UX
دامنه «اطلاعات پایه تقویم سازمانی» به‌صورت منوی مستقل داخل Hub اطلاعات پایه اضافه شده است و فرم‌ها به چهار گروه فوق تقسیم می‌شوند تا 16 جدول در یک فهرست تخت نمایش داده نشوند.

`CALENDAR_DAY` و `CALENDAR_DATE` Dataset محاسباتی/Canonical چهارصدساله هستند و از UI فقط جستجو و مشاهده می‌شوند. این تصمیم مانع تغییر دستی Timeline و از بین رفتن صحت تبدیل سه‌تقویمی می‌شود.

برای تمام فیلدهایی که به `DAY_ID` اشاره می‌کنند، Lookup جستجویی سه‌تقویمی اضافه شده است. کاربر می‌تواند با `DAY_ID`، تاریخ میلادی یا `FORMATTED_DATE` شمسی/قمری جستجو کند و نتیجه شامل نام روز هفته، شمسی، میلادی و قمری است.

## Backend
API جدید:

`/api/v1/calendar/reference`

قابلیت‌ها:
- Catalog و Descriptor فرم‌ها
- Search/Pagination/Sort سمت Oracle
- Lookup سمت سرور
- CRUD برای جداول قابل نگهداری
- کلید مرکب برای `CALENDAR_MONTH`
- تولید شناسه عددی برای جداول بدون Sequence با Lock تراکنشی + `MAX+1` در Prototype
- نگهداری Flagهای Oracle به صورت `Y/N`
- Validation فیلدهای اجباری، Select، زمان و بازه‌های تاریخ/سال/روز

## Frontend
مسیرها:
- `/calendar/reference-data`
- `/calendar/reference-data/:resource`

منوی اطلاعات پایه دارای کارت مستقل «اطلاعات پایه تقویم سازمانی» است.

## System Specification
Schema `CAL` و 16 فرم جدید در `system-specification` ثبت شده‌اند. شمار فرم‌های اطلاعات پایه/تقویم به 185 رسیده است:
- GEO: 20
- CIF Party Reference: 99
- DPS: 50
- CAL: 16

## Oracle / EA tools
`core-banking.schemas.calendar: CAL` نیز به Schemaهای تنظیم‌شده اضافه شده است؛ بنابراین ابزارهای مقایسه EA/Oracle و Oracle→EA XMI می‌توانند Schema CAL را نیز انتخاب کنند.

## Database migration
**ندارد.** مدل CAL طبق درخواست از قبل در Oracle ایجاد شده است. فقط اسکریپت read-only زیر برای کنترل Contract افزوده شده است:

`database/oracle/cal/verify-calendar-reference-ui-contract.sql`

## Static QA
- `verify-cif-persisted-grids.mjs` → PASS
- `verify-ea-oracle-comparison.mjs` → PASS
- `verify-calendar-reference.mjs` → PASS (16 physical CAL tables + routes + menu + config)
- Registry Java sources were syntax/type checked with Java 21-compatible stubs in the packaging environment.

Full Maven/Angular dependency build is not available in the packaging environment because Maven/npm dependency download is restricted; the Windows production build remains the final integration check.
