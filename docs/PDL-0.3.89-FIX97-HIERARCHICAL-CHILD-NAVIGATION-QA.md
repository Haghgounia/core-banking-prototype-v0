# PDL 0.3.89 / FIX97 — Unified Product Builder Wizard + Child Editing QA

## 1. هدف Release

FIX97 ادامه مستقیم Baseline نسخه 0.3.88/FIX96 است و Product Workspace را مطابق نقشه‌راه پروژه به Wizard شش‌مرحله‌ای تبدیل می‌کند. این Release هیچ DDL جدیدی ندارد و از همان جداول FIX96 استفاده می‌کند.

## 2. Wizard شش‌مرحله‌ای

| مرحله | Source of Truth | مسئولیت |
|---|---|---|
| 1 | `PDL.PRODUCT` | هویت، Domain، Family، ارز، وضعیت و ماهیت ترازنامه‌ای |
| 2 | `PDL.PRODUCT_VERSION` | Version، اعتبار، نسخه جاری، Version Status، Origination/Servicing |
| 3 | `PDL.PRODUCT_VERSION_MODULE` | فعال/غیرفعال، Configuration Status و Validation Status هر Module |
| 4 | Common Rule Tables | Eligibility، Channel، Org Scope، Documents، Inquiry، Pricing، Relationship |
| 5 | Deposit/Loan Rule Tables | قواعد تخصصی بر اساس Product Family |
| 6 | PRODUCT + VERSION + MODULE | Review و Configuration Readiness بدون State موازی |

## 3. قرارداد مرحله ۳ — مدیریت مستقیم Module

- برای هر Module، رکورد واقعی `PRODUCT_VERSION_MODULE` خوانده/ایجاد/ویرایش می‌شود.
- `IS_ENABLED`، `CONFIGURATION_STATUS_CODE` و `VALIDATION_STATUS_CODE` در دیتابیس Persist می‌شوند.
- UI هیچ آرایه یا Payload موقت را به‌عنوان Source of Truth ماژول‌ها نگه نمی‌دارد.
- نمای Generic جدول `PRODUCT_VERSION_MODULE` برای Audit و ویرایش مستقیم همچنان قابل دسترس است.

## 4. Family Applicability

- Common Moduleها برای هر دو Domain قابل استفاده‌اند.
- `TERM` و `PROFIT_PAYMENT` فقط برای `SHORT_TERM_DEPOSIT`, `LONG_TERM_DEPOSIT`, `CERTIFICATE_OF_DEPOSIT`.
- `CORRESPONDENT` فقط برای `NOSTRO_ACCOUNT`, `VOSTRO_ACCOUNT`.
- Loan Moduleها فقط برای Product Class = `LOAN`.
- Deposit Moduleها فقط برای Product Class = `DEPOSIT`.

## 5. Child Editing فعال

| Parent | Child | FK Context |
|---|---|---|
| PRODUCT_CHANNEL_RULE | PRODUCT_CHANNEL_OPERATION | CHANNEL_RULE_ID |
| PRODUCT_PRICING_RULE | PRODUCT_PRICING_COMPONENT | PRICING_RULE_ID |
| PRODUCT_PRICING_COMPONENT | PRODUCT_RATE_TIER | PRICING_COMPONENT_ID |
| PRODUCT_ELIGIBILITY_RULE | LOAN_ELIGIBILITY_EXTENSION | ELIGIBILITY_RULE_ID |
| DEPOSIT_PRODUCT_TERM_RULE | DEPOSIT_PRODUCT_ALLOWED_TERM | TERM_RULE_ID |
| DEPOSIT_PRODUCT_CLOSURE_RULE | DEPOSIT_PRODUCT_CLOSURE_PRECHECK | CLOSURE_RULE_ID |
| DEPOSIT_PRODUCT_CLOSURE_RULE | DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE | CLOSURE_RULE_ID |
| DEPOSIT_PRODUCT_CLOSURE_RULE | DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE | CLOSURE_RULE_ID |
| CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE | CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE | CORRESPONDENT_PRODUCT_PROFILE_ID |

UX Contract:
- عملیات Child از رکورد Parent آغاز می‌شود.
- `filterColumn/filterValue` به Route فرزند منتقل می‌شود.
- FK Parent در Create Form خودکار مقداردهی و Lock می‌شود.
- Grid فرزند فقط رکوردهای همان Parent را نمایش می‌دهد.

## 6. Review / Readiness

Readiness از State واقعی موجود محاسبه می‌شود و جدول جدید ندارد. کنترل‌های فعلی:
1. Product ذخیره و هویت/Family معتبر باشد.
2. Product Version انتخاب شده باشد.
3. حداقل یک Module قابل‌اعمال فعال باشد.
4. همه Moduleهای فعال `CONFIGURED` باشند.
5. همه Moduleهای فعال Validation معتبر داشته باشند.

همچنین `PRODUCT_STATUS_CODE`, `VERSION_STATUS_CODE`, `ORIGINATION_STATUS_CODE`, `SERVICING_STATUS_CODE` در صفحه Review یکجا نمایش داده می‌شوند.

## 7. Database

- Migration جدید: **ندارد**.
- پیش‌نیاز: Migrationهای FIX96 / 0.3.88 باید قبلاً اجرا شده باشند.
- هیچ Table/Column جدیدی برای Wizard یا Readiness ایجاد نمی‌شود.

## 8. کنترل پذیرش

1. یک Product جدید بسازید و Family را انتخاب کنید.
2. یک Version ایجاد و Origination/Servicing را ثبت کنید.
3. در مرحله ۳ Moduleها را فعال و وضعیت Configuration/Validation را تغییر دهید؛ Reload باید همان مقادیر دیتابیس را نشان دهد.
4. مرحله ۴ فقط Common Ruleهای Moduleهای فعال را نمایش دهد.
5. مرحله ۵ بر اساس Family فقط Ruleهای قابل‌اعمال را نمایش دهد.
6. از Parentهای سلسله‌مراتبی وارد Child شوید و قفل بودن FK Parent را کنترل کنید.
7. مرحله ۶ Readiness را از State واقعی محاسبه کند.
8. اجرای `node tools/verify-pdl-product-builder.mjs` باید OK باشد.

## 9. خارج از Scope FIX97

- Publish/Approval workflow تراکنشی چندمرحله‌ای سازمانی
- Product 360 Backend Aggregation مستقل
- Workflow Engine / Maker-Checker کامل
- Normalization تولیدی Multi-selectهای Eligibility

این موارد می‌توانند Scope نسخه بعدی باشند و نباید با Readiness فعلی اشتباه شوند.
