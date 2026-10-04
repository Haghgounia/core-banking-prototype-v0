# Patch R10P — Account Operations Workspace Navigation

Baseline: `core-banking-prototype-v0-2026-10-04-1409`

این Patch تجمعی شامل R10N + R10O + R10P است و UI عملیات حساب را از مدل «نمایش همه عملیات پشت سر هم» به مدل Workspace تک‌عملیاتی با Navigation واقعی Dockable تبدیل می‌کند.

نکات کلیدی:
- نمایش تنها یک Operation Workspace در هر لحظه.
- Context حساب انتخاب‌شده ثابت و دارای عنوان عملیات جاری.
- عملیات گروهی مستقل، فقط روی حساب‌های همان مشتری و با Backend scope validation.
- حفظ مشتری مبنای جستجو برای حساب‌های مشترک.
- امکان انتخاب حساب همان مشتری در عملیات موردی چرخه عمر.
- Step 16 نوسترو/وسترو در Scope مستقل عملیات ویژه بانکی.
- بدون DDL/Migration.
