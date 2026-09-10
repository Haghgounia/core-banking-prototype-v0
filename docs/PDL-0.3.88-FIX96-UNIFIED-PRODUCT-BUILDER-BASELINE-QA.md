# PDL 0.3.88 / FIX96 — Unified Product Builder Baseline QA

## 1. مبنای تغییر

Baseline این تغییر فایل `Unified_Product_Builder_Deposit_Loan.html` مورخ 2026-09-10 است.
مدل Target شامل 50 جدول کسب‌وکاری است. سه جدول Code Management موجود پروژه به‌عنوان Infrastructure حفظ شده‌اند؛ بنابراین Catalog فیزیکی PDL شامل 53 جدول است.

## 2. Coverage

| Package | Target Business Tables | Physical PDL |
|---|---:|---:|
| 01 Core | 5 | 5 |
| 02 Common Rules | 9 | 9 |
| 03 Deposit Module | 14 | 14 |
| 04 Loan Module | 6 | 6 |
| 05 Reference Data | 14 | 14 |
| 11 Correspondent Accounts | 2 | 2 |
| 90 Code Management Infrastructure | 0 | 3 |
| **جمع** | **50** | **53** |

## 3. Objectهای جدید

### PDL.DEPOSIT_PROFIT_PAYMENT_RULE
Source of Truth زمان‌بندی عملیاتی پرداخت سود سپرده است؛ نرخ و روش محاسبه همچنان در `PRODUCT_PRICING_*` باقی می‌ماند.

Business Rules پیاده‌شده:
- `MATURITY => MATURITY_DATE`
- `PAYMENT_DAY_NO` فقط برای `FIXED_DAY_OF_MONTH` و در بازه 1..31
- `CAPITALIZATION_DEFAULT => CAPITALIZATION_ALLOWED`
- `VALID_TO >= VALID_FROM`
- یک رکورد برای هر `PRODUCT_VERSION_ID` مطابق Unique مشخص‌شده در مدل Target

### PDL.CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE
پروفایل تخصصی NOSTRO/VOSTRO شامل نوع حساب، ارز و سامانه تسویه، روش Reconciliation، فرمت Statement، Overdraft و Limitها.

### PDL.CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE
قواعد Frequency، Cut-off، Holiday Adjustment، Netting و بازه اعتبار.
`CUTOFF_TIME` با قرارداد `HH:mm` و 24 ساعته نگهداری می‌شود.

## 4. Product Family Baseline

DPS Seed جدید کدهای زیر را بدون حذف داده Legacy اضافه می‌کند:

Deposit:
- `CURRENT_ACCOUNT`
- `QARD_SAVINGS`
- `SHORT_TERM_DEPOSIT`
- `LONG_TERM_DEPOSIT`
- `CERTIFICATE_OF_DEPOSIT`
- `NOSTRO_ACCOUNT`
- `VOSTRO_ACCOUNT`

Loan:
- `RETAIL_LOAN`
- `QARD_HASAN_LOAN`

همچنین Referenceهای `DEPOSIT_GROUP_CODE` و `DEPOSIT_TYPE_CODE` مطابق Target تکمیل می‌شوند.

## 5. UI / UX

- تمام DATEهای Generic PDL Form با `app-persian-date-input` وارد می‌شوند و Grid نیز تاریخ شمسی نمایش می‌دهد؛ Backend همچنان ISO/Gregorian Canonical دریافت می‌کند.
- تمام فیلدهای `*_TIME` غیر-TIMESTAMP، از جمله `CUTOFF_TIME`، با `app-time-input` Clock/Dial مشترک نمایش داده می‌شوند.
- در Context نسخه محصول، FK مربوط به نسخه به‌صورت Lock‌شده از Query Context اعمال می‌شود و کاربر ID عددی را ویرایش نمی‌کند.
- Check Codeهای سه جدول جدید با عنوان فارسی در ComboBox نمایش داده می‌شوند.
- فرم `DEPOSIT_PROFIT_PAYMENT_RULE` Conditional UI برای روز پرداخت و Capitalization دارد.

## 6. Workspace Applicability

در Workspace فعلی:
- TERM و PROFIT_PAYMENT: فقط `SHORT_TERM_DEPOSIT`, `LONG_TERM_DEPOSIT`, `CERTIFICATE_OF_DEPOSIT`
- CORRESPONDENT: فقط `NOSTRO_ACCOUNT`, `VOSTRO_ACCOUNT`
- خانواده‌های Target در انتخاب Product Family تفکیک شده‌اند.

## 7. Backend Validation

`ProductBuilderBusinessValidator` به مسیر Create/Update وصل شده است. Update ابتدا رکورد جاری را خوانده و Payload Patch را merge می‌کند تا Cross-field Validation روی state نهایی انجام شود.

## 8. Database Migration

الزامی:
- `database/oracle/pdl/migrations/0.3.88-fix96-unified-product-builder-baseline.sql`
- `database/oracle/dps/migrations/0.3.88-fix96-unified-product-builder-reference-seed.sql`

Migration PDL در حالت وجود کامل Tableها ORA-00955 را نادیده می‌گیرد؛ برای دیتابیسی که Object نیمه‌کاره از اجرای قبلی دارد، ابتدا وضعیت واقعی Object/Constraint بررسی شود.

## 9. موارد عمداً موکول‌شده به فاز بعدی

این نسخه معادل کامل Prototype HTML مستقل نیست و فعلاً این موارد را وارد Runtime Production-style نکرده است:
- Wizard شش‌مرحله‌ای کامل با Draft/Commit تراکنشی برای همه 50 جدول
- Child navigation مستقیم `PRODUCT_CHANNEL_RULE -> PRODUCT_CHANNEL_OPERATION`
- Child navigation `PRICING_RULE -> PRICING_COMPONENT -> RATE_TIER`
- Child navigation `TERM_RULE -> ALLOWED_TERM`
- Child navigation `CLOSURE_RULE -> PRECHECK / SETTLEMENT / APPROVAL`
- Child navigation `CORRESPONDENT_PROFILE -> CORRESPONDENT_SETTLEMENT_RULE`
- `LOAN_ELIGIBILITY_EXTENSION` از مسیر Parent `PRODUCT_ELIGIBILITY_RULE`
- Product 360 متصل به Backend و Readiness/Publish workflow
- Normalization تولیدی Multi-selectهای Eligibility

همچنین در فایل Target برای `PRODUCT_VERSION.ORIGINATION_STATUS_CODE` و `SERVICING_STATUS_CODE` بین Default (`DISABLED`) و Option Set جدید ناسازگاری وجود دارد. FIX96 این قسمت را بدون Migration قطعی Check Constraint دیتابیس تغییر نمی‌دهد.

## 10. Verification

Verifier اصلی:

`node tools/verify-pdl-product-builder.mjs`

انتظار:
- Business model tables = 50
- Physical PDL catalog tables = 53
- Package distribution = `01:5, 02:9, 03:14, 04:6, 05:14, 11:2, 90:3`
