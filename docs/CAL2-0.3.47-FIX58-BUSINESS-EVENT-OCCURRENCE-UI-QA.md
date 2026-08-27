# CAL2 0.3.47 — FIX58 Business Event Occurrence UI QA

## هدف

بازطراحی فرم `CAL2.EVENT_OCCURRENCE` از نمایش مستقیم ستون‌های فنی به یک Explorer کسب‌وکاری برای مشاهده رخدادهای واقعی مناسبت‌ها در تقویم دو.

## تغییرات UI

- عنوان فرم: «رخدادهای مناسبت‌ها».
- دکمه ایجاد: «ثبت رخداد دستی».
- فیلتر سال شمسی با مقدار پیش‌فرض سال شمسی جاری Dataset.
- فیلتر مناسبت، منشأ رخداد و تعطیل.
- گزینه «نمایش همه سال‌ها» برای حذف محدودیت سال پیش‌فرض.
- Grid کسب‌وکاری شامل:
  - عنوان و نوع مناسبت
  - تاریخ شمسی
  - تاریخ میلادی
  - تاریخ قمری
  - نوع وقوع سالانه/یک‌باره
  - منشأ تولید خودکار/دستی/رسمی
  - تعطیل
  - وضعیت داده
  - نام منبع
- Drawer جزئیات شامل قاعده مولد، روز هفته، سه تاریخ، منبع، نسخه Dataset و شناسه‌های فنی.

## قواعد دسترسی رخداد

- `GENERATED`: مشاهده فقط؛ ویرایش/حذف در UI نمایش داده نمی‌شود و Backend نیز درخواست مستقیم را رد می‌کند.
- `MANUAL`: قابل ایجاد، ویرایش و حذف.
- `OFFICIAL`: قابل ویرایش کنترل‌شده، غیرقابل حذف و غیرقابل تغییر منشأ به `MANUAL`.
- کاربر از فرم دستی نمی‌تواند `GENERATED` ایجاد کند؛ این مقدار فقط توسط موتور Materialize قواعد ساخته می‌شود.

## API جدید

```text
GET /api/v1/calendar2/event-recurrence/occurrences
GET /api/v1/calendar2/event-recurrence/occurrence-meta
```

فیلترهای API خلاصه رخداد:

```text
text
solarYear
eventId
occurrenceSource
holiday
page
size
sortBy
direction
```

## Query سه‌تقویمی

Backend Variant پیش‌فرض سیستم‌های `PERSIAN`, `GREGORIAN`, `ISLAMIC` را از Metadata خود CAL2 پیدا می‌کند و برای هر `DAY_ID` سه نمایش تاریخ را Join می‌کند؛ هیچ شناسه Variant در کد Hard-code نشده است.

## پایگاه داده

هیچ DDL یا Migration جدیدی ندارد. ساختار زیر بدون تغییر است:

```text
CAL2.EVENT
CAL2.EVENT_RECURRENCE_RULE
CAL2.EVENT_OCCURRENCE
CAL2.CANONICAL_DAY
CAL2.CALENDAR_DATE
```

## کنترل‌های انجام‌شده

Static verifiers:

```text
CIF persisted-grid verification OK
Fix33 EA/Oracle comparison verification OK
Calendar reference verification OK
Calendar raw import verification OK
CAL2 verification OK: 16 independent tables/forms, business-oriented rules and occurrence explorer, protected generated rows, materialization, ZIP JDBC import, separate CAL2 schema and routes.
Calendar display-label verification OK
```

Java بخش `calendar2.eventrecurrence` با Java 21 و Stubهای Spring/JDBC از نظر semantic compilation کنترل شد و PASS شد.

TypeScript فایل‌های تغییرکرده با TypeScript 5.8 parser از نظر Syntax کنترل شدند و PASS شدند.

Full Maven build در محیط بسته QA اجرا نشد، چون Maven Wrapper برای دریافت Maven 3.9.16 نیاز به دسترسی شبکه داشت و دانلود امکان‌پذیر نبود. Build نهایی باید روی Windows پروژه انجام شود.

## نسخه مورد انتظار Build

```text
Building Core Banking Prototype 0.3.47-prototype-fee-p1...
Building core-banking-prototype 0.3.47-SNAPSHOT
```
