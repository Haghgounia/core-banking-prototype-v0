# PDL 0.3.89 / FIX97 — Unified Product Builder Phase 2 QA

## Scope

این Release فاز دوم هم‌ترازی Product Definition Layer با Unified Product Builder است و روی Baseline دیتابیس FIX96 اجرا می‌شود.

## تغییرات اصلی

1. Product Workspace به Wizard شش‌مرحله‌ای تبدیل شد:
   - 1: PRODUCT
   - 2: PRODUCT_VERSION
   - 3: PRODUCT_VERSION_MODULE
   - 4: Common Rules
   - 5: Domain Rules
   - 6: Review

2. Module Applicability:
   - TERM و PROFIT_PAYMENT فقط برای SHORT_TERM_DEPOSIT / LONG_TERM_DEPOSIT / CERTIFICATE_OF_DEPOSIT.
   - CORRESPONDENT فقط برای NOSTRO_ACCOUNT / VOSTRO_ACCOUNT.
   - Loan Modules فقط در دامنه LOAN.

3. Hierarchical Child Editing:
   - PRODUCT_CHANNEL_RULE → PRODUCT_CHANNEL_OPERATION
   - PRODUCT_PRICING_RULE → PRODUCT_PRICING_COMPONENT
   - PRODUCT_PRICING_COMPONENT → PRODUCT_RATE_TIER
   - DEPOSIT_PRODUCT_TERM_RULE → DEPOSIT_PRODUCT_ALLOWED_TERM
   - DEPOSIT_PRODUCT_CLOSURE_RULE → PRECHECK / SETTLEMENT / APPROVAL
   - CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE → CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE
   - PRODUCT_ELIGIBILITY_RULE → LOAN_ELIGIBILITY_EXTENSION

4. Child FK از Parent Row به Query Context منتقل می‌شود و در فرم Child قفل است.

5. PRODUCT_VERSION UI:
   - VERSION_NO فقط‌خواندنی.
   - SOURCE_VERSION_ID فقط نسخه‌های همان Product و بدون Self-reference.
   - ORIGINATION_STATUS_CODE: OPEN / SUSPENDED / CLOSED.
   - SERVICING_STATUS_CODE: ACTIVE / SUSPENDED / CLOSED.
   - APPROVED_AT / APPROVED_BY توسط UI ارسال نمی‌شوند و System-managed باقی می‌مانند.
   - VALID_FROM / VALID_TO با Persian Date Picker.

6. Review Step:
   - نسخه انتخاب‌شده.
   - تعداد ماژول‌های فعال.
   - تعداد فرم‌های دارای داده.
   - تعداد رکوردهای Configuration.

## Database

این Release DDL یا Migration جدید ندارد. پیش‌نیاز آن اجرای Migrationهای 0.3.88 / FIX96 است.

## Static QA

- تمام 28 Verifier موجود پروژه Pass شدند.
- `verify-pdl-product-builder.mjs` برای Wizard، Module Toggle، Hierarchical Child Navigation و Product Version semantics توسعه یافت.
- فایل‌های TypeScript تغییرکرده با TypeScript transpile parser بدون خطای Syntax بررسی شدند.

## Runtime Acceptance Checklist

- باز کردن `/product-builder/products/{id}` باید Stepper شش‌مرحله‌ای نشان دهد.
- انتخاب نسخه باید مراحل 3 تا 6 را فعال کند.
- در Step 3 تغییر وضعیت Module باید در `PDL.PRODUCT_VERSION_MODULE` Persist شود.
- در `PRODUCT_PRICING_RULE` هر ردیف باید Action ورود به `PRODUCT_PRICING_COMPONENT` داشته باشد.
- در `PRODUCT_PRICING_COMPONENT` هر ردیف باید Action ورود به `PRODUCT_RATE_TIER` داشته باشد.
- در `DEPOSIT_PRODUCT_TERM_RULE` هر ردیف باید Action «مدت‌های مجاز» داشته باشد.
- در `DEPOSIT_PRODUCT_CLOSURE_RULE` سه Child Action وجود داشته باشد.
- در `CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE` Child Action Settlement وجود داشته باشد.
- در `PRODUCT_ELIGIBILITY_RULE` Child Action Loan Eligibility Extension وجود داشته باشد.
