# PB-R24-HF1 — اصلاح آزمون قدیمی خانواده محصول (بدون تغییر سامانه)

## علت توقف

R24 خانواده‌های تسهیلات را با استفاده از `PDL.LOAN_TYPE` و گروه عقود فعال تعیین می‌کند. مقدار تاریخی `RETAIL_LOAN` برای خواندن اطلاعات قدیمی باقی می‌ماند، ولی در فهرست ثابت ایجاد محصول نیست. آزمون پایه قدیمی `tools/verify-pdl-product-builder.mjs` هنوز وجود `RETAIL_LOAN` را در فهرست ثابت الزام می‌کرد. این تعارض باعث `Error: Target family missing: RETAIL_LOAN` شد.

## محدوده تغییر

فقط **بلوک بررسی خانواده‌ها در آزمون قدیمی** اصلاح می‌شود: هفت خانواده سپرده را در فهرست ثابت کنترل می‌کند؛ چهار خانواده تسهیلات را در نگاشت گروه‌ها و منبع زنده Oracle بررسی می‌کند؛ ورود `RETAIL_LOAN` را برای محصولات جدید ممنوع نگه می‌دارد. فایل‌های Angular، Java، Oracle، `build-production.cmd` و خود داده‌ها تغییر نمی‌کنند.

## نصب

۱. فایل ZIP را در ریشه پروژه استخراج کنید. دو اسکریپت جدید زیر `tools` اضافه می‌شود. **فایل `verify-pdl-product-builder.mjs` را دستی جایگزین نکنید.**

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\repair-pb-r24-hf1-family-verifier.mjs --check
node tools\repair-pb-r24-hf1-family-verifier.mjs --apply
node tools\repair-pb-r24-hf1-family-verifier.mjs --check
node tools\verify-pb-r24-hf1-family-verifier.mjs
node tools\verify-pdl-product-builder.mjs
node tools\verify-pb-r24-loan-integrity.mjs
build-production.cmd
```

پیش از اصلاح از آزمون قدیمی در `docs/patch-backups/pb-r24-hf1-family-verifier/<timestamp>/` پشتیبان گرفته می‌شود. نصب مجدد بدون تغییر اضافه خاتمه می‌یابد. در صورت اختلاف ساختار آزمون، هیچ تغییری اعمال نمی‌شود.

## آزمون

- R24 پیش از اصلاح طبق لاگ شما `65/65` موفق است.
- آزمون یکپارچه قدیمی در کپی مرجع R24 پس از اصلاح موفق شد: ۵۱ جدول کسب‌وکار و ۵۴ جدول فیزیکی PDL.
- ۷۷ آزمون بعدی فهرست‌شده در Windows Build روی کپی مرجع کامل‌شده بدون خطا اجرا شدند.
- اجرای Batch Windows، Angular و Maven و Oracle در این محیط انجام نشده است؛ برای نتیجه قطعی به اجرای شما نیاز است.

## نکته

**نیازی به تغییر `PRODUCT_FAMILY_CODE`، افزودن `RETAIL_LOAN` به فهرست ثابت، اجرای SQL یا نصب مجدد R24 نیست.**
