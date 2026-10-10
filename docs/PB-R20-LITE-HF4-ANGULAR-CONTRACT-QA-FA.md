# PB-R20 Lite HF4 - رفع خطای TypeScript در ماتریس حاکمیت

## علت
در مدل `PdlEffectiveRule.presencePolicy` فقط `REQUIRED | OPTIONAL` تعریف شده بود، ولی Backend و Template مقدار `WHEN_ENABLED` را نیز پشتیبانی می‌کنند. Angular با TS2367 متوقف شد.

## تغییرات
- یک خط مدل TypeScript اصلاح شد (افزودن مقدار `WHEN_ENABLED`).
- Verifier مستقل برای تطبیق قرارداد Frontend و Backend اضافه شد.
- هیچ تغییر Oracle یا Java، هیچ تغییر منطق انتشار و هیچ حذف گزینه تجاری انجام نشده است.

## چک‌لیست نصب در ویندوز
1. ZIP را در ریشه پروژه استخراج کنید و مسیرهای داخلی را حفظ کنید.
2. `node tools\verify-pb-r20-hf4-angular-contract.mjs` را اجرا کنید؛ انتظار `PB_R20_HF4_STATIC_PASS=4 FAIL=0`.
3. `build-production.cmd` را اجرا کنید.
4. پس از Build موفق، Backend را اجرا و ماتریس Rule Governance مرحله ششم Product Builder را بررسی کنید.

## آزمون انجام‌شده
- بررسی چهار شرط سازگاری Frontend و Backend: موفق.
- بررسی تفاوت دقیقاً یک خطی فایل مدل: موفق.
- ZIP: تست سلامت موفق.
- Build کامل Angular در محیط ساخت اصلاحیه موفق به اجرا نشد (وابستگی Angular قابل دریافت نبود)؛ نتیجه Build مقصد تعیین‌کننده است.

## نکته
اجرای مجدد DDL یا نصب Oracle لازم نیست. اگر در مدل TypeScript فعلی تغییرات محلی دیگری دارید، فقط همان یک خط را مطابق این گزارش اصلاح کنید و کل فایل را جایگزین نکنید.
