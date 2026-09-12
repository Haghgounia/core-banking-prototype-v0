# PDL 0.3.92 / FIX100 — Overlay Source Cleanup & Clean-Compile Guard

## مسئله مشاهده‌شده
Build نسخه 0.3.91 از Angular عبور کرد، اما در `mvn clean package` با 40 خطای Java در فایل زیر متوقف شد:

`backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductGovernanceService.java`

این Source در Baseline رسمی 0.3.91 وجود ندارد و به DTOهای قدیمی `Product360`، `ProductModuleValidation` و `ProductReadinessItem` وابسته است. در نصب‌های Overlay ممکن است فایل قدیمی از یک Build/پچ میانی روی دیسک باقی بماند، زیرا Overlay فایل حذف‌شده را از مقصد پاک نمی‌کند.

## علت اینکه Preflight قبلی خطا را نگرفت
`build-production.cmd` پیش از Angular فقط `mvn ... compile` اجرا می‌کرد. در محیط گزارش‌شده Maven اعلام کرد `Nothing to compile - all classes are up to date`. سپس مرحله نهایی `clean package`، `target` را پاک و کل 133 Source را دوباره Compile کرد و Source قدیمی را دید.

## اصلاح FIX100
1. ابزار `tools/migrate-source-layout.mjs` قبل از Verifierها اجرا می‌شود.
2. اگر `ProductGovernanceService.java` قدیمی وجود داشته باشد:
   - در `.upgrade-backup/<version>/obsolete-source/...` کپی می‌شود؛
   - سپس از Source فعال حذف می‌شود.
3. Maven preflight به `clean compile` تغییر کرده است تا Compile اولیه نیز incremental نباشد.
4. `verify-pdl-product-builder.mjs` نبودن Source منسوخ در Baseline canonical را کنترل می‌کند.
5. `package-release.cmd` مسیر `.upgrade-backup` را از Release ZIP حذف می‌کند.

## Regression / Data Safety
- هیچ DDL یا Migration دیتابیس ندارد.
- هیچ داده FEE، CAL2، CIF یا PDL تغییر نمی‌کند.
- رفتار Wizard و APIهای Product Builder تغییر نمی‌کند.
- Backup Source قدیمی نگهداری می‌شود تا در صورت نیاز قابل بررسی باشد.

## Verification انجام‌شده
- ابزار migration روی یک Source شبیه‌سازی‌شده قدیمی تست شد: فایل به Backup منتقل و از Source فعال حذف شد.
- PDL static verifier بعد از تغییر Pass شد.
- Node path-portability verifier Pass شد.
- Full Maven compile در محیط بسته‌بندی قابل اجرا نبود، زیرا Maven Wrapper نیازمند دانلود Maven از Repository خارجی بود؛ Compile نهایی با `build-production.cmd` در محیط پروژه انجام می‌شود.
