# CAL2 0.3.46 — FIX57 Business Event Recurrence UI QA

## هدف

ساده‌سازی فرم قواعد مناسبت `CAL2.EVENT_RECURRENCE_RULE` برای کاربر کسب‌وکاری، بدون تغییر مدل فیزیکی پایگاه داده یا منطق Materialization رخدادها.

## تغییرات اصلی

- عنوان فرم: **مناسبت‌های تقویم**.
- Grid فنی قبلی با Grid کسب‌وکاری جایگزین شد:
  - مناسبت (عنوان + کد)
  - تقویم مبنا (نام فارسی + Variant Code)
  - تاریخ وقوع با نام ماه
  - نحوه وقوع (سالانه/یک‌باره)
  - بازه سال
  - تعداد رخدادهای `GENERATED`
  - وضعیت
  - عملیات ویرایش و بازسازی
- `EVENT_RULE_ID` و شناسه‌های خام در Grid نمایش داده نمی‌شوند.
- endpoint جدید `GET /api/v1/calendar2/event-recurrence/rules` برای Summary صفحه‌بندی‌شده و قابل جستجو/مرتب‌سازی افزوده شد.
- endpoint جدید `GET /api/v1/calendar2/event-recurrence/months?variantId=...` نام ماه‌های همان سیستم تقویمی را برمی‌گرداند.
- Lookup مربوط به Calendar Variant نام فارسی تقویم را نشان می‌دهد و Variant Code را به‌صورت کد ثانویه حفظ می‌کند.
- ذخیره/ویرایش Rule همچنان از مسیر قبلی انجام می‌شود و بازسازی خودکار `EVENT_OCCURRENCE` بدون تغییر باقی مانده است.

## پایگاه داده

هیچ DDL یا Migration در FIX57 وجود ندارد. Schema `CAL2` همان 16 جدول FIX56 را نگه می‌دارد.

## کنترل‌های انجام‌شده

- TypeScript syntax transpile: PASS برای مدل، Service و Component تغییر‌یافته.
- `verify-cif-persisted-grids.mjs`: PASS.
- `verify-ea-oracle-comparison.mjs`: PASS.
- `verify-calendar-reference.mjs`: PASS.
- `verify-calendar-dataset-import.mjs`: PASS.
- `verify-calendar2-reference.mjs`: PASS با Guardهای جدید Business UI.
- `verify-calendar-display-labels.mjs`: PASS.
- Java source parsing با JDK 21 انجام شد؛ خطای Syntax در فایل‌های تغییر‌یافته مشاهده نشد. Full Maven build در محیط تولید artifact اجرا نشد، زیرا Maven Wrapper برای دریافت توزیع Maven به دسترسی شبکه نیاز داشت.

## انتظار Build روی Windows

```text
Building Core Banking Prototype 0.3.46-prototype-fee-p1...
...
Building core-banking-prototype 0.3.46-SNAPSHOT
```
