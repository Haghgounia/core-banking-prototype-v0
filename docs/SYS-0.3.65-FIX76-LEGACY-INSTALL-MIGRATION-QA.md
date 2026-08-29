# Core Banking Prototype 0.3.65 / FIX76

## هدف

رفع توقف Build هنگام Upgrade در همان پوشه‌ای که فایل‌های `INSTALL-*.txt` متعلق به Releaseهای قدیمی هنوز در Root باقی مانده‌اند.

## علت خطا

ZIP نسخه 0.3.64 فایل INSTALL در Root نداشت، اما Extract روی پوشه قدیمی فایل‌های موجود را حذف نمی‌کند. Guard نسخه 0.3.64 وجود هر `INSTALL-*.txt` در Root را خطا محسوب می‌کرد و Build پیش از Compile متوقف می‌شد؛ بنابراین Runtime JAR قدیمی باقی می‌ماند.

## اصلاح

`tools/migrate-release-layout.mjs` قبل از `verify-release-layout.mjs` اجرا می‌شود. برای هر INSTALL قدیمی در Root:

- اگر فایل هم‌نام در `docs/install/` وجود داشته باشد، نسخه Root حذف می‌شود.
- اگر مقصد وجود نداشته باشد، فایل به `docs/install/` منتقل می‌شود.
- هیچ فایل INSTALL در Root باقی نمی‌ماند.

این رفتار در `build-production.cmd` و `build-production.sh` یکسان است.

## Regression Guard

`verify-release-layout.mjs` کنترل می‌کند Migration Script وجود داشته باشد و در هر دو Build Script پیش از Release Layout Verifier اجرا شود.

`verify-cif-isic2.mjs` نیز دیگر شماره Release را hard-code نمی‌کند و فقط Semantic Version `x.y.z` را می‌پذیرد.

## Database

هیچ DDL یا Import جدیدی در FIX76 وجود ندارد. ساختار ISIC و Seed همان FIX75 است.
