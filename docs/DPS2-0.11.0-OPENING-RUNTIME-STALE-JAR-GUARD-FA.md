# Opening UI Runtime stale-JAR guard

این اصلاح روی سورس واقعی ارسالی 2026-09-29-2142 اعمال شده است.

## علت مشاهده UI قدیمی
سورس Angular جدید بود، اما build-production.cmd در verifier مربوط به Deposit Opening persistence متوقف می‌شد. حذف JAR قبلی بعد از verifierها انجام می‌شد؛ بنابراین app/core-banking-prototype.jar قدیمی باقی می‌ماند و bin/start.cmd آن را اجرا می‌کرد. در نتیجه مرورگر همچنان UI قدیمی را می‌دید.

## اصلاحات
- verifier persistence با قرارداد نهایی Opening هم‌راستا شد.
- build-production.cmd در شروع BUILD-DIRTY ایجاد می‌کند.
- bin/start.cmd در صورت وجود BUILD-DIRTY از اجرای JAR قدیمی جلوگیری می‌کند.
- BUILD-DIRTY فقط پس از ساخت موفق JAR و BUILD-VERSION حذف می‌شود.

## نتیجه static verification
- DPS2 Deposit Opening persistence verification OK
- DPS2_OPENING_UI_REFERENCE_PARITY_FAIL=0
