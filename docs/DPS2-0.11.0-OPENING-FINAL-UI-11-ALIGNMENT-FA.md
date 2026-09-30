# DPS2 Opening Final UI Alignment — 11.html

مبنای تغییر: فایل نهایی `11.html` ارائه‌شده توسط کاربر.

## تغییرات اصلی
- بازطراحی پوسته فرم افتتاح: Topbar، Page Head، Badgeها، Wizard Shell و Stepper دوخطی.
- هم‌ترازی کارت‌های خانواده محصول و چیدمان بخش‌های عملیاتی با UI نهایی.
- بازطراحی Payment Instrument مطابق UI نهایی:
  - Holder Role سیستمی و read-only.
  - دسته‌چک بدون Party Holder.
  - REQUESTED_QUANTITY=1 برای دسته‌چک.
  - CHEQUEBOOK_LEAF_COUNT مستقل با نمونه Policy 10/20/50.
- Payload و Backend برای INSTRUMENT_HOLDER_ROLE_CODE و CHEQUEBOOK_LEAF_COUNT تکمیل شد.
- SETTLEMENT_ACCOUNT_ID و DESTINATION_ACCOUNT_ID از UI تا Repository عبور داده می‌شوند.
- منطق و Runtime Integration موجود حفظ شده و UI standalone صرفاً به‌صورت Mock کپی نشده است.

## Verification
`node tools/verify-dps2-opening-reviewed-alignment.mjs`

نتیجه در زمان بسته‌بندی: 78 PASS / 0 FAIL.

Build کامل Angular/Maven در محیط بسته‌بندی اجرا نشد، چون node_modules و Maven در محیط موجود نبودند. Build نهایی باید در محیط پروژه با `build-production.cmd` انجام شود.
