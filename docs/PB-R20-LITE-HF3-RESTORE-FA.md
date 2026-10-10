# PB-R20 Lite HF3 — اصلاح غیرمخرب دو قرارداد مفقود R19

تاریخ: ۱۴۰۵/۰۷/۱۸ (2026-10-10)

## تشخیص

گزارش کاربر نشان داد ابزار `diagnose-pb-r19-baseline.mjs` با `ERRORS=0 WARNINGS=3` پایان یافت و `verify-pb-r19.mjs` دو شکست داشت:

1. `approved versions permit only emergency reduction in permissions`
2. `REST exposes governed readiness matrix`

این موارد در دو فایل Backend قرار دارند و فایل‌های نسخه R18 را با وجود سایر اجزای R19/R20 در مقصد نشان می‌دهند. در فایل آزمون یا اسکریپت verifier دستکاری نمی‌کنیم.

## نصب روی سورس موجود (ویندوز)

1. از پروژه خود یک نسخه پشتیبان تهیه کنید و سرویس را متوقف کنید.
2. فایل ZIP را **در ریشه پروژه** `D:\Projects\core-banking-prototype-v0` استخراج کنید. صرفاً `tools` و `docs` ادغام می‌شوند؛ هیچ فایل Java در زمان Extract جایگزین نمی‌شود.
3. CMD:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\repair-pb-r19-contracts.mjs --check
node tools\repair-pb-r19-contracts.mjs
node tools\diagnose-pb-r19-baseline.mjs
node tools\verify-pb-r19.mjs
node tools\verify-rgl-lite-r20.mjs
build-production.cmd
```

انتظار داریم:

```text
PB_R20_HF3_MERGE_SUCCESS PATCHED=2
PB_R20_HF2_BASELINE_ERRORS=0 WARNINGS=0
PB_R19_STATIC_PASS=51 PB_R19_STATIC_FAIL=0
RGL_R20_STATIC_PASS=53 RGL_R20_STATIC_FAIL=0
```

اگر هریک از قراردادها از پیش موجود باشند، اسکریپت آن‌ها را مجدداً اعمال نمی‌کند. تکرار اجرا باید `PB_R20_HF3_ALREADY_APPLIED` بدهد.

## چه تغییر می‌کند؟

تنها دو سورس، **در صورت نیاز**:

- `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductGovernanceWriteGuard.java`: کنترل تغییرات محدود و کاهنده دسترسی نسخه تصویب‌شده.
- `backend/src/main/java/com/behsazan/corebanking/productbuilder/web/ProductBuilderController.java`: تزریق `ProductRuleGovernanceService` و ارائه مسیر `GET /api/v1/product-builder/versions/{versionId}/rule-governance`.

کد FEE2، منوها، Angular و Oracle تغییر نمی‌کند. همچنین فایل آزمون R19 و verifier تغییری نمی‌کنند.

## بازگشت / ایمنی

پیش از نوشتن، فایل‌های اولیه در این پوشه ثبت می‌شوند:

`docs/patch-backups/pb-r20-lite-hf3/`

نام فایل شامل SHA256 کوتاه از محتوای اولیه است، با پسوند `.java.txt` تا ابزار Build آن را به‌عنوان Java کامپایل نکند. در صورت نیاز می‌توان محتوای فایل پشتیبان را با همان نام اصلی به پوشه سورس برگرداند. پیش از بازگشت، از فایل اصلاح‌شده نیز نسخه نگه دارید.

اگر سورس با ساختار مورد انتظار R18/R19 تفاوت اساسی داشته باشد، ابزار با `PB_R20_HF3_ABORTED` **پیش از اصلاح فایل‌ها** متوقف می‌شود؛ در آن صورت فایل‌های Java واقعی پروژه را برای Merge دستی بررسی کنید و فایل‌های مرجع را کورکورانه جایگزین نکنید.

## آزمون‌های انجام‌شده در محیط تولید بسته

- بازتولید دقیق `PB_R19_STATIC_PASS=49 FAIL=2` با دو فایل R18 روی بدنه R20.
- اعمال HF3 و رسیدن به `PB_R19_STATIC_PASS=51 FAIL=0`.
- `RGL_R20_STATIC_PASS=53 FAIL=0`.
- تأیید تطابق Byte-for-byte هر دو سورس اصلاح‌شده با نسخه کامل R20.
- ۹ آزمون اجرایی Java برای Guard؛ جلوگیری از Reopen، تغییر تاریخ، تغییر سیاست از CRUD، و امکان Stop/Expire.
- آزمون Idempotency و Backup.
- آزمون Fail-closed با Constructor متفاوت، بدون تغییر هیچ فایل.

**حدود تأیید:** Build کامل Windows و ارتباط با Oracle کاربر هنوز اجرا نشده است. پس از این مرحله احتمال بروز خطاهای مستقل Maven/Angular وجود دارد و باید بر اساس خروجی واقعی جدا بررسی شود.
