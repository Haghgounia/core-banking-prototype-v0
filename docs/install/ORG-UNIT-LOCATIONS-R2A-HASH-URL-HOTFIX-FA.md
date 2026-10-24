# ORG R2-A — اصلاح نشانی مرورگر (Hash routing)

این بسته تنها مستندات URL و آزمون بازگشتی را اصلاح می‌کند؛ **هیچ کد اجرایی Frontend/Backend، SQL یا Oracle تغییر نمی‌کند**.

## علت خطا

`frontend/src/app/app.config.ts` شامل `provideRouter(routes, withHashLocation())` است.

به همین دلیل:

- آدرس صحیح: `http://localhost:8091/#/organization/unit-locations-canonical`
- آدرس اشتباه: `http://localhost:8091/organization/unit-locations-canonical`

آدرس اشتباه مستقیماً به Spring Boot ارسال می‌شود و نتیجه `No static resource ...` با وضعیت 404 است. آدرس صحیح در مرورگر به Angular می‌رسد؛ مرورگر Fragment را به سرور ارسال نمی‌کند.

## اجرا

۱. پیش از هر کار، آدرس صحیح بالا را امتحان کنید. نیازی به اعمال Patch یا Build برای رفع خطای ناشی از URL نیست.

۲. در صورت تمایل به اصلاح راهنما و افزودن تست، فایل‌های ZIP کوچک را روی پروژه دارای R2-A استخراج کنید و `node tools/verify-org-unit-location-r2a.mjs` را اجرا کنید.

۳. اگر آدرس صحیح، ولی صفحه قبلی/قدیمی باز شد، مطمئن شوید ZIP R2-A واقعاً نصب، `build-production.cmd` موفق و Java قدیمی متوقف و برنامه جدید از `bin/start.cmd` اجرا شده است.

۴. هیچ SQL جدیدی برای این موضوع اجرا نکنید.
