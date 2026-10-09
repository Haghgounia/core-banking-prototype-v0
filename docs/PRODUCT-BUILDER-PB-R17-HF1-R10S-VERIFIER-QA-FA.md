# PB-R17-HF1 — اصلاح سازگاری Verifier قدیمی R10S با بارگذاری ایمن گرید

## نشانه خطا در ویندوز

`verify-pb-r17.mjs`: 40/40 PASS؛ اما در `build-production.cmd`، آزمون R10S با پیام `FK labels load before initial grid rendering` و نتیجه 53 PASS / 1 FAIL متوقف می‌شد.

## ریشه مشکل

در نسخه R17 برای جلوگیری از تداخل پاسخ قدیمی درخواست‌های ناهم‌زمان پس از `await this.loadLookups()`، شرط `if (generation !== this.navigationGeneration) return;` قرار دارد؛ سپس `await this.search(0)` اجرا می‌شود. ترتیب صحیح و ایمن حفظ شده است. آزمون R10S قبلی صرفاً تطابق عین دو خط مجاور را می‌خواست و با وجود شرط صحیح محافظ، خطا می‌داد.

## اصلاح

فقط فایل `tools/verify-pdl-common-rules-r10s.mjs` تغییر کرد. به جای مقایسه متن دو خط پشت‌سرهم، آزمون در بدنه متد `initialize()` بررسی می‌کند که `loadLookups()` قبل از `search(0)` باشد. این آزمون در صورت معکوس شدن ترتیب، حذف بارگذاری Lookup یا نبود متد initialize مردود خواهد شد. هیچ تغییری در Reactivity، API، قرارداد Oracle یا UI اعمال نشده است.

## نتایج اجرا در محیط تولید Patch

- `node tools/verify-pdl-common-rules-r10s.mjs`: **54 PASS, 0 FAIL**
- `node tools/verify-pb-r17.mjs`: **40 PASS, 0 FAIL**
- `node tools/verify-release-layout.mjs`: **PASS**
- کنترل محتویات ZIP و استخراج مستقل: **PASS**

## محدودیت

به دلیل نبود وابستگی‌های Frontend در این محیط، Build کامل Angular/Maven و تست Oracle/مرورگر اجرا نشده است؛ کنترل آن باید با `build-production.cmd` در ویندوز انجام شود. موفقیت این Patch رفع خطای مشخص‌شده در R10S را اثبات می‌کند، نه تضمین موفقیت همه مراحل بعدی Build.

## نصب روی PB-R17

Patch را با حفظ مسیر پوشه‌ها در ریشه پروژه استخراج کنید و اجرا کنید:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-common-rules-r10s.mjs
node tools\verify-pb-r17.mjs
build-production.cmd
```
