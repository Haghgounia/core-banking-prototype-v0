# FEE 0.3.68 / FIX79 — Visual Flowchart QA

## هدف
بازطراحی بخش «روال تعریف اطلاعات کارمزد» در صفحه `/fee` به‌گونه‌ای که از نظر بصری یک Diagram/Flowchart واقعی دیده شود، نه صرفاً مجموعه‌ای از Cardها.

## تغییرات UI
- هر مرحله داخل یک Stage Box مستقل نمایش داده می‌شود.
- هر فرم به شکل یک Node/Shape مستقل با آیکون، عنوان فارسی و نام جدول نمایش داده می‌شود.
- Nodeهای هر Stage روی یک Backbone افقی قرار دارند.
- بین Stageها Connector عمودی و فلش واضح نمایش داده می‌شود.
- Nodeها قابل کلیک هستند و Route واقعی فرم را باز می‌کنند.
- رنگ‌بندی سه نوع Node:
  - Core Configuration
  - Support/Test
  - Runtime/Read-only
- Node جدول ناموجود در Oracle Disabled است.

## مسیرها
- فرم جدولی: `/fee/tables/:table`
- شبیه‌ساز: `/fee/simulator`

## Regression
- 47 فرم Baseline حفظ شده‌اند.
- 574 رکورد Seed و Contract دیتابیس تغییر نکرده است.
- Quick Access Cardها و جستجوی 47 فرم حفظ شده‌اند.
- هیچ DDL/Seed/API جدیدی در FIX79 وجود ندارد.

## کنترل استاتیک
`tools/verify-fee-admin-baseline.mjs` اکنون وجود Flow Canvas، Stage Connector و لینک Arrangement/Simulator را نیز کنترل می‌کند.
