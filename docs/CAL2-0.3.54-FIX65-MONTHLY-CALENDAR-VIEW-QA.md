# CAL2 0.3.54 — FIX65: Monthly Calendar View

## هدف

افزودن یک نمای عملیاتی ماه‌به‌ماه برای تقویم دو که در آن کاربر بدون ورود به Gridهای فنی بتواند روزهای ماه، مناسبت‌ها، تعطیلی‌ها و تاریخ متناظر سه تقویم را مشاهده کند.

## تصمیم طراحی

این صفحه یک CRUD جدید نیست و تعداد ۱۶ جدول/فرم مرجع CAL2 را تغییر نمی‌دهد. صفحه به‌عنوان **Query / Read Model** روی داده‌های موجود ساخته شده است:

`CALENDAR_SYSTEM → CALENDAR_VARIANT → CALENDAR_DATE → CANONICAL_DAY`

و برای مناسبت‌ها:

`EVENT_TYPE → EVENT → EVENT_OCCURRENCE → CANONICAL_DAY`

نمای ماهانه عمداً از `EVENT_OCCURRENCE` استفاده می‌کند، نه از `EVENT_RECURRENCE_RULE`؛ بنابراین آنچه کاربر می‌بیند وقوع materialized و واقعی رویداد در یک روز مشخص است.

## Backend

API جدید:

`GET /api/v1/calendar2/month-view?calendarCode=PERSIAN&year=1405&month=1`

پارامترهای `year` و `month` اختیاری‌اند. اگر ارسال نشوند، تاریخ جاری Dataset برای Calendar Variant پیش‌فرض همان تقویم انتخاب می‌شود. `calendarCode` یکی از مقادیر `PERSIAN / GREGORIAN / ISLAMIC` است و پیش‌فرض `PERSIAN` است.

Read Model برای هر روز این اطلاعات را برمی‌گرداند:

- تاریخ مرجع ISO و روز هفته
- تاریخ مبنا و نام ماه
- تاریخ متناظر هجری شمسی، میلادی و هجری قمری
- وضعیت امروز / جمعه / تعطیل
- فهرست occurrenceهای روز شامل عنوان، نوع رویداد، رسمی بودن، تعطیل بودن، منشأ و وضعیت داده

هیچ DDL یا Migration جدیدی لازم نیست.

## Angular UI

Route جدید:

`/calendar2/month-view`

قابلیت‌ها:

- شروع هفته از شنبه و پایان با جمعه
- پیش‌فرض تقویم هجری شمسی و ماه جاری
- ماه قبل / ماه بعد / امروز
- انتخاب سال و ماه
- تغییر تقویم مبنا بین شمسی، میلادی و قمری
- نمایش حداکثر سه عنوان مناسبت در Cell روز و شمارنده برای موارد بیشتر
- فیلتر سریع «فقط تعطیلی‌ها»
- تمایز بصری امروز، جمعه، روز تعطیل و نوع مناسبت
- پنل جزئیات روز شامل سه تاریخ و تمام رخدادهای همان روز
- Responsive layout؛ در عرض کم، پنل جزئیات زیر Grid قرار می‌گیرد
- سازگار با Theme روشن/تیره از طریق CSS Variableهای موجود پروژه

## فایل‌های اصلی

- `backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/domain/Calendar2MonthViewModels.java`
- `backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/oracle/Calendar2MonthViewRepository.java`
- `backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/application/Calendar2MonthViewService.java`
- `backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/web/Calendar2MonthViewController.java`
- `frontend/src/app/features/calendar2-reference/calendar2-month-view.component.ts`
- `frontend/src/app/features/calendar2-reference/calendar2-month-view.component.html`
- `frontend/src/app/features/calendar2-reference/calendar2-month-view.component.scss`
- `tools/verify-calendar2-month-view.mjs`

## کنترل‌های انجام‌شده در محیط تولید بسته

- `verify-calendar2-reference.mjs`: PASS
- `verify-calendar-display-labels.mjs`: PASS
- `verify-runtime-artifact-contract.mjs`: PASS
- `verify-calendar2-month-view.mjs`: PASS
- Source-level Java compile harness برای چهار کلاس جدید با `javac --release 21`: PASS
- TypeScript parser check روی Component/Models/Service: بدون Syntax Error؛ فقط importهای Angular/RxJS به دلیل نبود `node_modules` در محیط بسته‌سازی resolve نشدند.
- Maven compile کامل در این محیط اجرا نشد، چون Maven Wrapper برای دریافت Maven از Maven Central دسترسی شبکه ندارد. Build اصلی روی محیط Windows پروژه با `build-production.cmd` باید کنترل نهایی باشد.

## Acceptance Criteria

1. صفحه از منوی تقویم دو قابل دسترسی باشد.
2. باز شدن اولیه روی ماه جاری هجری شمسی باشد.
3. تغییر ماه و بازگشت به امروز بدون دستکاری URL انجام شود.
4. تغییر تقویم مبنا به Gregorian/Islamic همان روزها را با ماه تقویم انتخاب‌شده Query کند.
5. روزهای دارای `EVENT_OCCURRENCE.HOLIDAY_FLAG='Y'` به‌عنوان تعطیل مشخص شوند.
6. جمعه‌ها حتی بدون مناسبت با وضعیت weekend مشخص باشند.
7. کلیک روی هر روز، سه تاریخ متناظر و تمام مناسبت‌های همان روز را نشان دهد.
8. هیچ FK یا Query به Schema `CAL` ایجاد نشده باشد.
