# PDL 0.11.0 R10U-HF4 — Catalog Test Hotfix

## علت
R10U جدول نرمال‌شده `PRODUCT_ELIGIBILITY_CRITERION` را به پکیج Common Rules اضافه کرد. کاتالوگ اصلی و Verifierهای Product Builder به‌درستی 54 جدول فیزیکی، 51 جدول کسب‌وکاری و 10 جدول در Package 02 را گزارش می‌کردند؛ اما `PdlCatalogTest` هنوز اعداد قبل از R10U را انتظار داشت و در مرحله Maven test باعث شکست Build می‌شد.

## اصلاح
- تعداد کل کاتالوگ در تست: 53 → 54
- تعداد Common Rules در تست: 9 → 10
- تعداد Business tables در تست: 50 → 51
- Assertion صریح برای `PRODUCT_ELIGIBILITY_CRITERION`
- Guard جدید `verify-pdl-r10u-hf4-catalog-test.mjs` در Build ویندوز و Unix

## اثر
هیچ DDL، داده، API یا منطق کسب‌وکاری تغییر نکرده است. این Hotfix فقط Regression Test را با مدل R10U همگام می‌کند.
