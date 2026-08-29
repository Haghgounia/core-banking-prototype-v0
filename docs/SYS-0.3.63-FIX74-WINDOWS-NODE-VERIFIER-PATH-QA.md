# QA — SYS 0.3.63 / FIX74 — Windows Node Verifier Path

## مسئله مشاهده‌شده

در Windows اجرای `build-production.cmd` در مرحله `verify-cif-isic2.mjs` با خطای `ENOENT` متوقف می‌شد و مسیر زیر ساخته می‌شد:

```text
D:\D:\Projects\core-banking-prototype-v0\database\oracle\cif\isic2\01-create-isic2-tables.sql
```

علت، استفاده از `new URL('..', import.meta.url).pathname` بود. در Windows مقدار URL pathname برای Drive Letter به شکل `/D:/...` برمی‌گردد و استفاده مستقیم از آن در `path.join()` می‌تواند Drive را دوباره به مسیر اضافه کند.

## اصلاح

`verify-cif-isic2.mjs` اکنون ریشه پروژه را با API استاندارد Node محاسبه می‌کند:

```js
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
```

همین الگو برای `verify-pdl-product-builder.mjs` نیز اعمال شد تا Verifier به `process.cwd()` وابسته نباشد. همچنین Build Scriptهای Windows/Linux ابتدا Working Directory را به Root پروژه تغییر می‌دهند.

## Regression Guard

ابزار `tools/verify-node-tool-path-portability.mjs` تمام Verifierهای Node را بررسی می‌کند و در صورت مشاهده یکی از الگوهای زیر Build را Fail می‌کند:

- استفاده از `URL.pathname` برای مسیر فایل‌سیستم
- استفاده از `process.cwd()` به‌عنوان Root Verifier

## Stop Script

در `bin/stop.cmd` PIDهای تکراری خروجی `netstat` فقط یک بار Kill می‌شوند. این اصلاح پیام خطای دوم `taskkill` را که پس از Kill موفق همان PID نمایش داده می‌شد حذف می‌کند.

## تست‌های انجام‌شده

- اجرای `verify-cif-isic2.mjs` از Root پروژه: PASS
- اجرای `verify-cif-isic2.mjs` از Working Directory متفاوت (`/tmp`): PASS
- اجرای Guard جدید Path Portability: PASS
- اجرای تمام Static Verifierهای CIF/CAL/CAL2/PDL/EA-Oracle/Runtime: PASS
- Maven compile در محیط ساخت به علت عدم دسترسی Maven Wrapper به Maven Central اجرا نشد.

## اثر دیتابیس

هیچ DDL/Migration/Seed جدیدی در FIX74 وجود ندارد. اسکریپت‌های ISIC2 نسخه FIX73 بدون تغییر قابل استفاده‌اند.
