# PB-R21 HF5 — اصلاح کرش Maven Wrapper در مرحله Package

## تشخیص

در گزارش کاربر، کنترل‌های R19، R20 و R21 همگی موفق شدند. Maven مرحله اول توانست ۲۱۸ فایل Java را Compile کند. Angular نیز با پیام `Application bundle generation complete` خروجی `frontend/dist/core-banking-ui` را تولید کرد. سپس هنگام فراخوانی دوم `mvnw.cmd`، پیام‌های `Process is terminated due to StackOverflowException` و `Cannot start maven from wrapper` ظاهر شدند.

این خطا نشانه قطعی مشکل در کد Java/Angular نیست؛ `Cannot start maven from wrapper` توسط Wrapper هنگام شکست اجرای PowerShell/resolve تولید شده است. بدون بررسی Windows Event Log، علت ریشه‌ای StackOverflowException را نمی‌توان با قطعیت تعیین کرد.

## نحوه اجرا

فایل ZIP را در ریشه پروژه باز کنید (فقط ابزارهای `tools` و سند `docs` اضافه می‌شوند). سپس در Command Prompt:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\repair-pb-r21-hf5-maven-wrapper.mjs --check
node tools\repair-pb-r21-hf5-maven-wrapper.mjs --apply
node tools\verify-pb-r21-hf5-maven-wrapper.mjs
build-production.cmd
```

## مکانیسم

- `build-production.cmd` همچنان ابتدا `mvnw.cmd -DskipTests clean compile` را اجرا می‌کند.
- Maven Wrapper مسیر اجرای `mvn.cmd` را در متغیر `__MVNW_CMD__` قرار می‌دهد.
- در مرحله `clean package` پس از Angular، اگر فایل اجرایی مورد اشاره وجود داشته باشد، مستقیم `call "%__MVNW_CMD__%" clean package` اجرا می‌شود. در نتیجه PowerShell برای بار دوم راه‌اندازی نمی‌شود.
- اگر مسیر موجود نبود، با پیام هشدار به Maven Wrapper قبلی برمی‌گردد تا سیستم بی‌صدا بدون Package ادامه ندهد.
- فایل قدیمی در `docs/patch-backups/pb-r21-hf5/build-production.cmd` پشتیبان‌گیری می‌شود.
- اسکریپتِ ادغام تکرارپذیر است و در ساختار ناشناخته از تغییر خودداری می‌کند.

## کنترل ایمنی

هیچ فایل Java، Angular، Oracle، FEE2 یا فرم تغییر نمی‌کند. مراحل Package، ساخت JAR رسمی، `BUILD-VERSION` و `BUILD-DIRTY` حذف نمی‌شوند. این Patch در محیط Linux به‌صورت ساختاری تأیید می‌شود؛ اجرای واقعی Windows/PowerShell و Maven باید در رایانه کاربر صورت گیرد.

**اگر Maven مجدداً خطا داد:** دستورات `where mvn`، `where powershell`، `cd backend` و `mvnw.cmd -version` را جداگانه آزمایش و خروجی را ارسال کنید. روی JAR خروجی قدیمی `app` تکیه نکنید تا Build کامل با پیام `Built version:` به پایان برسد.
