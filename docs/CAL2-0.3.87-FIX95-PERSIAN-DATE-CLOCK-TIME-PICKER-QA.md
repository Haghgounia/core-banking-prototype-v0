# CAL2 0.3.87 / FIX95 — Persian Policy Dates + Clock-style Time Picker QA

## هدف

دو اصلاح UX برای سیاست ساعات کاری CAL2:

1. تاریخ استثناء تک‌روز و سایر فیلدهای DATE قابل نگهداری CAL2 در UI به هجری شمسی نمایش داده شوند.
2. Time Picker مشترک از فهرست زمان به Clock/Dial گرافیکی مشابه Material clock-style picker نمونه Grudus تبدیل شود.

مرجع UX درخواست‌شده:
`https://www.cssscript.com/clock-style-time-picker-in-pure-javascript-grudus-timepicker/`

## تاریخ شمسی در CAL2

Editor از قبل برای FieldType.DATE از `app-persian-date-input` استفاده می‌کرد. در FIX95 لایه Grid نیز DATE را از مقدار ISO Canonical به نمایش هجری شمسی تبدیل می‌کند.

نمونه:

- مقدار API/Oracle: `2027-03-01`
- مقدار UI: `1405/12/10`

این قاعده برای `BUSINESS_CALENDAR_EXCEPTION.EXCEPTION_DATE` و تاریخ‌های اعتبار `BUSINESS_CALENDAR_SCHEDULE` نیز برقرار است. Sort/Query/Storage همچنان روی مقدار Canonical انجام می‌شود و Schema تغییر نکرده است.

## Clock-style Time Picker

Component:
`frontend/src/app/shared/ui/time-input.component.ts`

رفتار:

- Overlay ساعت‌محور با Header زمان انتخاب‌شده.
- انتخاب دو مرحله‌ای Hour سپس Minute.
- 24-hour dial: حلقه بیرونی `1..12` و حلقه داخلی `13..23,00`.
- Minute dial با interval پیش‌فرض 5 دقیقه.
- نمایش عقربه و نقطه مرکز.
- دکمه‌های «اکنون»، «پاک کردن»، «انصراف» و «تأیید».
- تایپ مستقیم `HH:mm` همچنان مجاز است.
- Min/Max و Validation قبل از تأیید کنترل می‌شوند.
- `ControlValueAccessor` و `NG_VALIDATORS` حفظ شده‌اند.
- API value همیشه String استاندارد `HH:mm` باقی می‌ماند.
- Persian/Arabic digit normalization حفظ شده است.
- Light/Dark Theme از `--app-*` tokenها استفاده می‌کند.
- Dial buttonها keyboard-focusable و دارای ARIA label هستند.

پیاده‌سازی Angular-native است؛ هیچ dependency خارجی `grudus-timepicker` به package.json افزوده نشده است. بنابراین UX مشابه نمونه است ولی Component با Angular 21/Reactive Forms/Theme Contract خود پروژه یکپارچه باقی می‌ماند.

## Database / API

هیچ DDL یا Migration جدیدی لازم نیست.

- DATE در Backend همان ISO/Gregorian Canonical قبلی است.
- TIME همان `VARCHAR2(5 CHAR)` / `HH:mm` باقی می‌ماند.

## Regression Guards

`tools/verify-time-picker.mjs` موارد زیر را کنترل می‌کند:

- Clock face و Overlay داخلی وجود دارند.
- Material list timepicker قبلی حذف شده است.
- Hour/Minute phase وجود دارد.
- 24-hour outer/inner ring وجود دارد.
- interval و Min/Max حفظ شده‌اند.
- Reactive Forms/Validation و `HH:mm` contract حفظ شده‌اند.
- Theme tokenها و ARIA وجود دارند.
- هیچ `input type=time` در Frontend وجود ندارد.
- CAL2/CAL/CIF از Component مشترک استفاده می‌کنند.

`tools/verify-calendar2-business-calendar-schedule.mjs` نیز کنترل می‌کند که DATE editor شمسی است و Gridهای عمومی DATE را با `persianDateCell` نمایش می‌دهند.

## نتیجه QA بسته‌بندی

- `verify-time-picker.mjs`: `19/19 PASS`.
- `verify-calendar2-business-calendar-schedule.mjs`: `15/15 PASS`.
- کل verifierهای `tools/verify-*.mjs`: `28/28 PASS`.
- تبدیل نمونه `2027-03-01` با formatter شمسی UI: `1405/12/10`.
- TypeScript syntax pass برای فایل‌های تغییرکرده انجام شد؛ تنها خطاهای اجرای `tsc --noResolve` مربوط به unavailable بودن Angular modules در محیط بسته‌بندی بود و parse error جدیدی گزارش نشد.
- Full npm/Maven build در محیط بسته‌بندی تکمیل نشد: npm dependency install در محیط sandbox timeout شد و Maven Wrapper به `repo.maven.apache.org` دسترسی نداشت. `build-production.cmd` در محیط توسعه باید Compile واقعی Angular/Java را قبل از تولید JAR انجام دهد.
