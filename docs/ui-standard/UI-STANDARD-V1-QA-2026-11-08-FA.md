# گواهی کنترل فنی اصلاح UI سراسری — SYS-UI-V1 / 2026-11-08

**Baseline سورس:** 12:19 مورخ ۸ نوامبر، `0.11.0`; **دامنه تغییر:** ۸۶ Template از ۲۳ Feature Domain + UI Shared/Docs/Tools.  
**وضعیت:** Source QA PASS / Browser UAT NOT_RUN / Windows Build NOT_RUN / Oracle Write NOT_RUN.

## کنترل‌های اجراشده در محیط ساخت بسته

| کنترل | نتیجه | معنای نتیجه |
|---|---|---|
| شمارش همه ۸۶ قالب Feature و وجود Scope استاندارد | PASS: 86/86 | پوشش CSS و قواعد مشترک؛ نه گواهی UX هر فرم |
| شمارش Template مشترک Shell | PASS: 1 | پوسته و ناوبری مرکزی بدون تغییر |
| پیمایش تمام فیلدهای `date/time/datetime-local` بومی | PASS: 0 باقی‌مانده | مؤلفه مشترک شمسی در Templateهای تغییرکرده فعال است؛ runtime نیازمند UAT |
| بررسی نحو TypeScript کامپوننت تاریخ | PASS: 0 parse diagnostics | صحت نحو فایل، نه Angular Template Type Check |
| کنترل ساختاری `tools/ui-standard/verify-system-ui.cjs` | PASS: 21/21 | فایل‌ها، قراردادهای HTML و سند Roadmap |
| CLT R3 `tools/clt/ui-standard/verify-ui-standard.cjs` | PASS: 20/20 | قراردادهای ایستای قبلی وثایق حفظ شده |
| `verify-global-breadcrumb.mjs` | PASS | Breadcrumb مشترک سامانه |
| `verify-calendar-display-labels.mjs` | PASS | برچسب‌های تقویم |
| `verify-pb-r15.mjs` و `verify-rgl-lite-r20.mjs` | PASS | سازگاری محصول‌ساز/سیاست |
| `verify-dps2-opening-ui-reference-parity.mjs` | PASS | قرارداد نمای مرجع افتتاح |
| `verify-dps2-account-operations-ui-alignment.mjs` | PASS: 103/103 | قرارداد نمای عملیات سپرده |
| `verify-dps2-account-operations-reference-parity.mjs` | PASS: 72/72 | هم‌راستایی فرم‌ها و خدمات عملیات سپرده |
| `verify-fee2-studio.mjs` و `verify-fee2-forms.mjs` | PASS | قواعد اصلی FEE2 |
| `verify-fee-admin-baseline.mjs` | PASS | قواعد مدیریت کارمزد |
| `verify-calendar2-reference.mjs` | PASS | قرارداد Calendar2 |
| `verify-cif-persisted-grids.mjs` | PASS | جدول‌های عملیاتی CIF |
| `verify-pdl-product-builder.mjs` | PASS | قرارداد اصلی PDL |
| آزمون نصب Node روی ساختار سورس 12:19 شبیه‌سازی‌شده | PASS: 101/101 فایل + reinstall=0 | نصب، backup، SHA و idempotency؛ بدون اجرای Windows Build |

> اجرای یکپارچه کل ۱۳۰ verifier موجود در `build-production.sh` به محدودیت زمان اجرای محیط بررسی برخورد کرد؛ گواهی موفقیت آن داده نمی‌شود. همچنین Build واقعی Angular/Maven/JAR، راه‌اندازی Spring، آزمون Chrome در ۸۶ فرم و تراکنش‌های واقعی Oracle در این محیط انجام نشده‌اند.

## یافته‌های باز در ممیزی سراسری

- ۱۷ Template به بررسی موردی رنگ ثابت و Contrast در تم تیره نیاز دارند.
- ۲۶ مورد به بررسی **قرارداد** صفحه‌بندی (نه لزوماً افزودن Paginator) نیاز دارند.
- ۱۵ Template دارای متن یا شناسه فنی‌اند؛ موارد ابزارهای DBA، تشخیص فنی و UAT از نظر هدف کاربری مستثنی هستند، سایر موارد نیازمند بازبینی متن اختصاصی‌اند.
- برای تمام فرم‌های حاوی عملیات مالی، بدون قرارداد تصویب‌شده API/تراکنش و UAT، کنترل‌های بصری مجوز عملیات جدید ایجاد نمی‌کنند.
- مجموعه فرم‌های CLT هنوز باید در UAT طراح/تحلیلگر با V1.42 و مراحل ثابت R0..R7 کنترل شوند.

## تصمیم Release

`SYS-UI-V1` **نامزد نصب و آزمون پذیرش** است. پذیرش نهایی UI سامانه نیازمند Angular Build رسمی، Chrome UAT برای هر مسیر مؤثر، آزمون RTL در عرض‌های ۱۳۶۶ و ۷۶۸ پیکسل، تم روشن/تیره و عدم ایجاد نقص رگرسیون است. پذیرش کسب‌وکار وثایق به گیت‌های مستقل CLT وابسته باقی می‌ماند.
