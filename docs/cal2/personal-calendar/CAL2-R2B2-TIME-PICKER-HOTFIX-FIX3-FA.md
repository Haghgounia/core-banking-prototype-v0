# CAL2 R2-B2 / FIX3 — اصلاح خطای کنترل ساعت

این Patch برای نسخه `CAL2-R2B2-PATCH-LAYOUT-FIX2-2026-10-24.zip` تهیه شده است.

خطای `no browser-native type=time inputs remain` هنگام اجرای `build-production.cmd`
به دلیل وجود دو ورودی `type="time"` در فرم شخصی `calendar2-month-view.component.html` بود.

## تغییرات

- هر دو ورودی «ساعت شروع کار یا قرار» و «ساعت نمایش پیام» با کامپوننت مشترک `app-time-input` جایگزین شدند.
- `TimeInputComponent` به Imports کامپوننت ماهانه افزوده شد.
- قراردادهای `ngModel`, `name`, `required`, مقدار پیش‌فرض و ارسال `HH:mm` حفظ شدند.
- هیچ فایل SQL یا Backend تغییر نکرده است.
- هیچ فایل جدیدی در ریشه پروژه قرار نمی‌گیرد.

## نصب در Windows

1. مطمئن شوید نسخه FIX2 قبلاً روی `D:\Projects\core-banking-prototype-v0` استخراج شده است.
2. محتویات ZIP را **در ریشه همان پروژه** استخراج و جایگزینی دو فایل `ts/html` را تأیید کنید.
3. در CMD اجرا کنید:

```cmd
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-time-picker.mjs
node tools\verify-cal2-personal-r2b2.mjs
call tools\RUN-CAL2-R2B2-PILOT.cmd
```

اسکریپت Launcher موجود FIX2 همچنان معتبر است و نیاز به کپی Launcher دیگری نیست.

## کنترل کیفیت

در این محیط، کنترل `verify-time-picker`, `verify-cal2-personal-r2b2` و `verify-release-layout` اجرا می‌شود.
این کنترل‌ها Build کامل Angular/Java یا تست اجرای Oracle نیستند. اگر خطای جدیدی پس از این مرحله مشاهده شود، خروجی Build مبنای اصلاح بعدی خواهد بود.
