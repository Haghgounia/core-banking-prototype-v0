# NAV-R22 — گزارش آزمون سازگاری با نسخه ۲۱:۵۳

**مبنای آزمون:** `core-banking-prototype-v0-2026-10-11-2153.zip`، شامل تغییرات Product Builder R29،‏ FEE R2.1 و حذف میانبر استودیو FEE2 از منوی اصلی.

## آزمون‌های اجراشده

- نصب اسکریپتی روی نسخه ۲۱:۵۳: **PASS**؛ ۲۰ لینک موجود با همان ترتیب حفظ شدند.
- مقایسه محتوای ۱۸۷۴ فایل مبنا: تنها دو فایل `app-shell.component.html` و `app-shell.component.ts` تغییر کردند؛ هفت فایل افزوده شدند. هیچ فایل دیگری حذف یا اصلاح نشد.
- لینک Product Builder در AppShell پس از نصب **بایت‌به‌بایت بدون تغییر** باقی ماند.
- آزمون توسعه هم‌زمان: افزودن لینک فرضی دوم Product Builder، نصب R22 و برگشت از آن، بدون حذف لینک: **PASS**.
- محافظت در برابر تغییر غیرمنتظره عنوان بخش منو (Fail-closed): **PASS**، بدون تغییر فایل‌ها.
- `verify-r22-sidebar.mjs`: **PASS**.
- `verify-release-layout.mjs`: **PASS**.
- `verify-fee-admin-baseline.mjs` و `verify-fee-ux-r1/r2/r21.mjs`: **PASS**.
- `verify-fee2-forms.mjs`، `verify-fee2-studio-menu-cleanup.mjs` و `verify-fee2-studio.mjs`: **PASS**.
- `verify-pdl-product-builder.mjs`، `verify-pdl-r28-loan-code-eligibility.mjs` و `verify-pdl-r29-finalization.mjs`: **PASS**.
- `verify-oracle-ea-xmi-export.mjs`: **PASS**؛ عبارات قدیمی مورد نیاز تست در Tooltip حفظ شده‌اند.
- تست‌های ناوبری موجود برای CIF، DPS2، Global Breadcrumb، ORG و EA/Oracle: **PASS**.
- پارس نحوی TypeScript با کامپایلر سراسری برای دو فایل AppShell و R22: **PASS**.
- سلامت هر دو بسته ZIP و تطبیق باینری با نسخه ۲۱:۵۳: **PASS**؛ ۱۸۷۴ فایل اولیه، ۱۸۸۱ فایل نهایی، ۲ فایل اصلاح‌شده، ۷ فایل افزوده، ۰ فایل حذف‌شده.

## موارد تأییدنشده

- Build کامل Angular (وابستگی‌های `node_modules` در بسته سورس موجود نیست).
- Build کامل Java/Maven و آزمون واقعی Runtime و Oracle.
- مشاهده رفتاری منو با مرورگر روی سیستم Windows مقصد.

**مهم:** PASS آزمون‌های استاتیک به‌معنای تأیید عملکرد Runtime نیست. `build-production.cmd` باید روی ماشین مقصد اجرا شود.
