# FEE 0.3.67 / FIX78 — Fee Home Flow Diagram QA

## هدف تغییر
در صفحه اصلی ماژول کارمزد (`/fee`) یک Diagram تعاملی اضافه شد تا «ترتیب روال تعریف اطلاعات در فرم‌های کارمزد» را نمایش دهد و کاربر بتواند با کلیک روی هر Shape مستقیماً به فرم مربوط هدایت شود.

## دامنه تغییر
فقط UI صفحه Home ماژول FEE تغییر کرده است و هیچ تغییر DDL/Seed/API در این Release وجود ندارد.

## منطق روال پیشنهادی
Diagram جدید مسیر تعریف را در 9 گام نمایش می‌دهد:
1. اطلاعات پایه و دامنه‌ها
2. داده‌های مستقل برای تست و شبیه‌سازی
3. سیاست، ویژگی و چارچوب مقرراتی
4. تعریف سرویس کارمزد و نسخه آن
5. قواعد اعمال و محاسبه
6. اخذ، ارز، زمان‌بندی و ثبت
7. تخفیف، تعدیل و تسهیم
8. Arrangement اختصاصی
9. شبیه‌سازی و مشاهده خروجی اجرایی

## ویژگی‌های UI
- هر Node یک لینک مستقیم به فرم عملیاتی مربوط دارد.
- Nodeهای Runtime با برچسب «فقط مشاهده» مشخص شده‌اند.
- Nodeهای پشتیبان/تست از گام‌های اصلی تفکیک بصری دارند.
- اگر جدولی در Oracle در دسترس نباشد، Node مربوط به صورت Disabled نمایش داده می‌شود.
- Quick Access Cardهای قبلی حفظ شده‌اند.

## فایل‌های تغییر یافته
- `frontend/src/app/features/fee-admin/fee-admin-home.component.ts`
- `frontend/src/app/features/fee-admin/fee-admin-home.component.html`
- `frontend/src/app/features/fee-admin/fee-admin-home.component.scss`
- `tools/verify-fee-admin-baseline.mjs`
- `CHANGELOG.md`

## کنترل استاتیک
Verifier فعلی اکنون علاوه بر کنترل Baseline 47 Table / 574 Seed، وجود Diagram تعاملی Home را نیز بررسی می‌کند.

## انتظار عملکرد
- با باز کردن `/fee`، Diagram زیر Hero و Quick Cardها دیده می‌شود.
- کلیک روی هر Shape کاربر را به `/fee/tables/:table` یا `/fee/simulator` هدایت می‌کند.
- ساختار کلی صفحه Home و جستجوی فرم‌ها بدون Regression حفظ شده است.
