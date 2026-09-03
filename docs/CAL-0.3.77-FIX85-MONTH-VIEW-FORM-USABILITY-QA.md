# CAL 0.3.77 — FIX85 — بازبینی فرم‌ها و نمای ماهانه

## دامنه تغییر

این Fix رابط CAL (تقویم یک) را پس از تکمیل داده‌های OCCASION و BUSINESS_CALENDAR با CAL2 هم‌سطح می‌کند. هیچ DDL جدیدی ندارد.

## نمای ماهانه CAL

Route جدید:

- `/calendar/month-view`
- API: `/api/v1/calendar/month-view`

نمای ماهانه به‌صورت پیش‌فرض روی هجری شمسی باز می‌شود و سه تقویم `PERSIAN / GREGORIAN / ISLAMIC` را از داده‌های خود CAL پشتیبانی می‌کند.

منابع Read Model:

- `CAL.CALENDAR_DAY`
- `CAL.CALENDAR_DATE`
- `CAL.CALENDAR_MONTH`
- `CAL.WEEKDAY`
- `CAL.CALENDAR_DAY_OCCASION`
- `CAL.OCCASION_OCCURRENCE`
- `CAL.OCCASION`
- `CAL.OCCASION_CATEGORY`
- `CAL.BUSINESS_CALENDAR`
- `CAL.BUSINESS_CALENDAR_DAY`

هیچ Query زمان اجرای نمای ماهانه CAL به Schema `CAL2` وابسته نیست.

## بازبینی Gridهای CAL

پنج Grid پرحجم/شناسه‌محور دارای Read Model اختصاصی شدند:

1. `CALENDAR_DAY`: تاریخ میلادی، تاریخ شمسی، تاریخ قمری و روز هفته.
2. `BUSINESS_CALENDAR_DAY`: نام تقویم کاری، تاریخ شمسی، روز هفته و Flagهای بانکی به فارسی.
3. `OCCASION_RULE`: عنوان مناسبت، نوع قاعده، تقویم مبنا، ماه/روز و بازه اعتبار.
4. `OCCASION_OCCURRENCE`: عنوان/دسته مناسبت، تاریخ شمسی/میلادی، وضعیت، مرجع، رسمی/تأیید.
5. `CALENDAR_DAY_OCCASION`: تاریخ شمسی، روز هفته، عنوان مناسبت و اولویت نمایش.

PK/FKهای واقعی Oracle و CRUD موجود تغییر نکرده‌اند؛ فقط Grid با زمینه کسب‌وکاری غنی شده است.

عنوان‌های ترکیبی انگلیسی/فارسی قدیمی مانند `Deterministic`، `Canonical` و `Override` نیز در Labelهای کاربری CAL با عبارت‌های فارسی جایگزین شدند؛ نام فنی ستون و Code دیتابیس تغییر نکرده است.

## وضعیت مناسبت‌های ثابت CAL1/CAL2

`CAL.OCCASION_OCCURRENCE` در مرحله تکمیل داده از `CAL2.EVENT_OCCURRENCE` ساخته شده است؛ در نتیجه CAL2 منبع داده 74,807 رخداد بوده و درج مجدد همان مناسبت‌ها در CAL2 نباید انجام شود.

برای کنترل پوشش، اسکریپت Read-only `CAL_CAL2_FIXED_OCCASION_COVERAGE_AUDIT.sql` ارائه شده است. اگر در آینده Rule جدیدی در CAL2 اضافه شود، از سرویس Rebuild موجود CAL2 برای تولید رخدادهای `GENERATED` استفاده شود؛ Bulk Insert تکراری توصیه نمی‌شود.

## نکته تعطیلات

وجود مناسبت لزوماً به معنی تعطیلی نیست. در CAL، نمای ماهانه Flag تعطیلی را از `BUSINESS_CALENDAR_DAY.IS_BANK_HOLIDAY` می‌خواند. در وضعیت فعلی Business Calendar سال 1405 دارای 365 روز است؛ بنابراین مناسبت‌های همه سال‌ها دیده می‌شوند ولی Highlight تعطیلی بانکی برای سال‌هایی که Business Calendar Day آن‌ها تولید نشده، قابل استنتاج نیست.

## Verification

- `verify-calendar-month-view.mjs`
- `verify-calendar-form-usability.mjs`
- تمام Guardهای استاتیک پروژه

Build کامل Maven/Angular باید در محیط توسعه متصل به Dependency Repository اجرا شود.
