# DPS2 0.11.0 — Account Operations Reference Parity V2

## مبنا
این Patch بر مبنای `Deposit_Account_Operations_Operational_v11_RC_2026-09-22` بازتنظیم شده است. هدف، کمینه‌کردن اختلاف ظاهری و رفتاری Angular Account Operations با فرم عملیاتی مرجع، بدون جایگزین‌کردن Runtime/API واقعی با state نمایشی HTML است.

## اصلاحات اصلی
- Step 01 دوباره به ساختار مرجع «نگهداری حساب سپرده» سازمان‌دهی شد:
  - انتخاب حساب و وضعیت
  - اطلاعات پایه، Snapshot واحد افتتاح و واحد متولی جاری
  - ویژگی حساب با ComboBox فارسی و مقدار فنی پایدار
  - تماس حساب
  - تغییر نسخه محصول و استثناء شرط
  - اختیار صاحب امضا
- Step 02 به ساختار مرجع «چرخه عمر، راکدی و انسداد» نزدیک شد:
  - Activation کنترل‌شده
  - Lifecycle موردی
  - Hold غیر وثیقه‌ای
  - Bulk Action
- Closure/Reopening کنار Step 08 / Maturity قرار گرفت تا ترتیب Workspace با مرجع همسو شود.
- عنوان‌های Stepهای 03 تا 17 با عنوان‌های فرم مرجع همسان شدند.
- ComboBoxهای دارای raw technical value در رابط کاربر بانک فارسی شدند. مقدار `value` فنی برای API بدون تغییر باقی می‌ماند.
- خدمات حساب، طرف/دسترسی/ابزار و مقررات از input آزاد کد به ComboBoxهای کنترل‌شده و فارسی نزدیک شدند.
- دیکشنری Presentation برای status/channel/action/type و سایر codeهای پرکاربرد اضافه شد.
- CSS مخصوص Reference parity برای Page Head، Scope Banner، Panel، Split، Form Grid و Toolbar اضافه شد.

## کنترل‌های Static
- `verify-dps2-account-operations-reference-parity.mjs`
  - 18 عنوان اصلی Reference
  - Step 01/02 UX markers
  - Reference CSS contract
  - عدم نمایش static raw latin technical code در ComboBoxها
  - unique literal IDs
  - ngModel field resolution
  - event handler resolution
  - technical-value preservation

## Regression نتیجه‌شده در Work Tree
- Phase 8: 18/18 PASS
- Phase 11N-A Step 00: 36/36 PASS
- Phase 11N-B Steps 01–02: 78/78 PASS
- Phase 11N-C Steps 03–04: 72/72 PASS
- Phase 11N-D Steps 05–10: 64/64 PASS
- Phase 11N Steps 11–17: 39/39 PASS
- Account Operations UI Alignment: 103/103 PASS
- Account Operations Reference Parity V2: 72/72 PASS
- TypeScript transpile: PASS

## Build
Full Angular/production build در محیط تولید Patch اجرا نشده است؛ `node_modules` در Work Tree این محیط موجود نبود. `build-production.cmd` و `build-production.sh` طوری به‌روزرسانی شده‌اند که Reference Parity verifier را نیز قبل از build اجرا کنند. Build نهایی روی محیط پروژه با `build-production.cmd` انجام شود.
