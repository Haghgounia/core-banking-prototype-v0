# SYS 0.3.60 / FIX71 — Persian Date Picker Current Default QA

## Scope

اصلاح کامپوننت مشترک `app-persian-date-input` به‌گونه‌ای که در صورت خالی بودن مقدار، Picker هنگام باز شدن روی سال/ماه/روز جاری شمسی قرار گیرد؛ بدون آنکه باز شدن Picker به‌تنهایی مقداری در FormControl ثبت کند.

## Root cause

در Selectهای Native، مقدار `[value]` روی عنصر `select` ممکن است پیش از ایجاد Optionهای `@for` اعمال شود. پس از ایجاد Optionها، Browser می‌تواند به اولین Option برگردد. در فهرست سال این مقدار `1300` و در فهرست ماه `فروردین` بود.

## Fix

- Seed انتخاب داخلی از تاریخ جاری با `seedTodaySelection()` برای مقدار خالی.
- اضافه شدن `[selected]` صریح روی Optionهای سال، ماه و روز؛ همین الگو برای ساعت/دقیقه نیز اعمال شد.
- تاریخ ذخیره‌شده همچنان بر Today precedence دارد.
- `minDate` و `maxDate` همچنان با `clampToBounds()` اعمال می‌شوند.
- تغییر فقط در state داخلی Picker است و تا فشردن «انتخاب» یا «امروز» مقدار جدیدی به FormControl ارسال نمی‌شود.

## Expected behavior

در تاریخ 2026-08-27، برای یک Date field خالی، Picker باید به‌جای `1300 / فروردین / 1` روی تاریخ جاری شمسی یعنی حدود `1405 / شهریور / 5` باز شود (وابسته به تاریخ محلی Browser).

اگر فیلد قبلاً مثلاً `2025-03-21` داشته باشد، Picker باید تاریخ شمسی متناظر همان مقدار را نشان دهد، نه Today.

## Verification

```text
node tools/verify-persian-date-picker-current-default.mjs
Persian date-picker default verification OK: 6 checks.
```

Frontend production build در محیط بسته اجرا نشد، چون executable محلی Angular CLI (`frontend/node_modules/.bin/ng`) در package workspace موجود نبود. تغییر TypeScript محدود به Template binding و منطق موجود همان Component است.

## Database impact

No DDL / no migration.
