# DPS2 0.11.0 — Remaining Business Gaps Implementation

این بسته فقط Gapهای باقی‌مانده‌ی فایل‌های مرجع عملیات حساب سپرده را پیاده‌سازی می‌کند. طبق تصمیم پروژه، Runtime/Qualification این بسته به مرحله بعد موکول شده است.

## Step 14
- اجرای تطبیق در UI به صورت یک Action: Run + Item.
- ایجاد Discrepancy در mismatch حفظ شد.
- ایجاد خودکار `DEPOSIT_SUSPENSE_OPEN_ITEM` از مسیر mismatch حذف شد.
- Action مستقل `POST /api/v1/deposit-accounts/{accountId}/suspense-items` اضافه شد.
- Source/Target پیش‌فرض مطابق RC: `DEPOSIT_SUBLEDGER -> GL`.
- گزینه `SUSPENSE` در نوع تطبیق UI اضافه شد.

## Step 17
- Rule اهلیت اکنون `MIN_ACTIVE_DAYS` را نیز در ارزیابی لحاظ می‌کند.
- Action «بررسی اهلیت و عضویت» نتیجه `ELIGIBLE/INELIGIBLE` را در Enrollment ثبت/به‌روزرسانی می‌کند و `MIN_ACTIVE_DAYS` را نیز لحاظ می‌کند.
- Action یکپارچه `POST /api/v1/deposit-accounts/{accountId}/lottery-run` اضافه شد.
- Action قرعه‌کشی تمام Enrollmentهای فعال و واجد شرایط Program را می‌خواند، `ENTRY_COUNT` را از `ELIGIBLE_BALANCE / ENTRY_UNIT_AMOUNT` محاسبه می‌کند، Entryها را ثبت می‌کند، انتخاب وزنی Winner را انجام می‌دهد و Draw را `VERIFIED` می‌کند.
- UI با RC هم‌راستا شد: `تعریف برنامه`، `بررسی اهلیت و عضویت`، `تثبیت & قرعه‌کشی` و `ثبت پرداخت آخرین برنده`.
- پرداخت جایزه همچنان فقط از Step 05 Transaction engine انجام می‌شود.

## Test status
- بنا به درخواست پروژه، در این بسته هیچ Runtime/Qualification/Regression test جدیدی اجرا نشده است.
- وضعیت این بسته: `IMPLEMENTATION_COMPLETE_TEST_DEFERRED`.

## Step 15 — Exceptions & Corrections
- چهار نوع اصلاح RC در UI و Service پوشش داده شد.
- `REVERSAL` و `DUPLICATE_CANCEL` مسیر مالی non-destructive Step 05 دارند.
- `CORRECTION` و `BACKDATED_CORRECTION` بدون جعل posting مالی، Correction Entry ممیزی‌شده ایجاد می‌کنند.
- Runtime/Qualification بنا بر تصمیم پروژه به بعد موکول شد.
