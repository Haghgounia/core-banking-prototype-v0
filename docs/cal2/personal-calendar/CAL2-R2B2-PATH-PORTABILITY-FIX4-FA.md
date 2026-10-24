# CAL2 R2-B2 — FIX4 (2026-10-24)

## علت
در اجرای `tools/verify-node-tool-path-portability.mjs`، ابزار قدیمی
`tools/verify-cal2-personal-r1.mjs` به علت محاسبه ریشه پروژه با `process.cwd()` رد شد.
این روش به پوشه جاری CMD وابسته است و با الزام قابلیت جابه‌جایی ابزارهای پروژه سازگار نیست.

## تغییر دقیق
فقط فایل `tools/verify-cal2-personal-r1.mjs` اصلاح شد. اکنون ریشه پروژه
با `dirname(fileURLToPath(import.meta.url))` و `resolve(..., '..')` محاسبه می‌شود.
این تغییر صرفاً مربوط به تشخیص مسیر فایل است؛ قواعد آزمون R1 تغییر نکرده‌اند.

## نصب
1. فایل ZIP را در `D:\Projects\core-banking-prototype-v0` استخراج کنید و در صورت سؤال، جایگزینی فایل را مجاز کنید.
2. در CMD دستورات زیر را اجرا کنید:

```cmd
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-node-tool-path-portability.mjs
node tools\verify-time-picker.mjs
node tools\verify-cal2-personal-r2b2.mjs
call tools\RUN-CAL2-R2B2-PILOT.cmd
```

## کنترل‌های انجام‌شده
- کنترل مسیر ابزارهای Node: `OK` برای 179 فایل؛ این آزمون ایستا است و به معنی اجرای تک‌تک ابزارها نیست.
- ساعت‌گزین مشترک: 19/19 موفق.
- قرارداد ایستای R2-B2: موفق.
- کنترل ساختار ریشه پروژه: موفق.
- هیچ دستور Oracle یا DDL در این Patch وجود ندارد.

**محدودیت:** Build کامل Java/Angular و اجرای Windows/Oracle در این محیط اثبات نشده است. اگر در ادامه خطای دیگری ظاهر شد، خروجی آن باید جداگانه بررسی شود.
