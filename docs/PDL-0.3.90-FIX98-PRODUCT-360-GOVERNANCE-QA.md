# PDL 0.3.90 / FIX98 — Product 360, Rule Validation و Governed Lifecycle

## 1. هدف Release

FIX98 ادامه مستقیم FIX97 است و مرحله ۶ Product Workspace را از Review محلی به **Product 360 مبتنی بر Backend** ارتقا می‌دهد. این Release چرخه `Validate → Approve → Publish` را روی Stateهای موجود `PRODUCT`, `PRODUCT_VERSION`, `PRODUCT_VERSION_MODULE` اجرا می‌کند و **DDL جدید ندارد**.

## 2. Product 360 Backend Aggregation

Endpoint جدید:

`GET /api/v1/product-builder/products/{productId}/360?versionId={productVersionId}`

خروجی شامل Product، Version، Moduleها، Rule Coverage، Validation هر Module، Readiness، مجوز Approve/Publish و Lifecycle State است.

## 3. Validation واقعی Ruleها

Validation دیگر از UI به‌صورت دستی `VALID/INVALID` نمی‌شود. Action زیر داده واقعی Rule Tableها را می‌خواند و `PRODUCT_VERSION_MODULE.VALIDATION_STATUS_CODE` را ثبت می‌کند:

`POST /api/v1/product-builder/products/{productId}/versions/{versionId}/validate`

کنترل‌های سلسله‌مراتبی FIX98:

| Module | Validation Coverage |
|---|---|
| CHANNEL | `PRODUCT_CHANNEL_RULE` + حداقل یک `PRODUCT_CHANNEL_OPERATION` |
| PRICING | `PRODUCT_PRICING_RULE` + حداقل یک `PRODUCT_PRICING_COMPONENT` |
| TERM | `DEPOSIT_PRODUCT_TERM_RULE` + حداقل یک `DEPOSIT_PRODUCT_ALLOWED_TERM` |
| CORRESPONDENT | Profile + حداقل یک Settlement Rule |
| LOAN_ELIGIBILITY | Parent `PRODUCT_ELIGIBILITY_RULE` + Child `LOAN_ELIGIBILITY_EXTENSION` |

سایر Moduleها بر اساس Root Rule Table خود کنترل می‌شوند. Cross-field validation رکوردها همچنان در `ProductBuilderBusinessValidator` اعمال می‌شود.

## 4. Family Baseline / Mandatory Modules

برای جلوگیری از Publish محصول بدون حداقل پیکربندی عملیاتی، FIX98 یک Baseline حاکمیتی دارد:

- همه محصولات: `ELIGIBILITY`, `CHANNEL`
- Deposit: `DEPOSIT_PROFILE`, `OPENING`, `TRANSACTION`, `CLOSURE`
- Term Deposit / CD: علاوه بر موارد بالا `TERM`, `PROFIT_PAYMENT`
- NOSTRO/VOSTRO: علاوه بر Deposit baseline، `CORRESPONDENT`
- Loan: `LOAN_PROFILE`, `FINANCIAL`, `REPAYMENT`, `PROCESS`

این Baseline در Application Governance است و Table جدید ایجاد نمی‌کند.

## 5. Governed Lifecycle

### Validate
- Rule Coverage زنده محاسبه می‌شود.
- Moduleهای فعال باید `CONFIGURED` باشند.
- نتیجه هر Module در `VALIDATION_STATUS_CODE` با `VALID/INVALID` ثبت می‌شود.

### Approve
Endpoint:

`POST /api/v1/product-builder/products/{productId}/versions/{versionId}/approve`

پیش‌شرط‌ها:
- Version در `DRAFT` باشد.
- Validation زنده دوباره اجرا شود.
- Readiness = 100% باشد.

اثر:
- `VERSION_STATUS_CODE = APPROVED`
- `APPROVED_AT = current timestamp`
- `APPROVED_BY = X-User-Name`

### Publish
Endpoint:

`POST /api/v1/product-builder/products/{productId}/versions/{versionId}/publish`

پیش‌شرط‌ها:
- Version = `APPROVED`
- Validation زنده مجدداً موفق باشد.

اثر:
- نسخه‌های قبلی `IS_CURRENT=0`
- نسخه انتخابی `VERSION_STATUS_CODE=ACTIVE`, `IS_CURRENT=1`
- `PRODUCT.PRODUCT_STATUS_CODE=ACTIVE`

`ORIGINATION_STATUS_CODE` و `SERVICING_STATUS_CODE` عمداً مستقل باقی می‌مانند و Publish آنها را به‌زور Enable نمی‌کند.

## 6. Return to Draft

نسخه `APPROVED` که هنوز Publish نشده، می‌تواند از Action Governance به `DRAFT` برگردد. `APPROVED_AT/APPROVED_BY` پاک می‌شوند. Version فعال از این مسیر قابل برگشت نیست.

## 7. جلوگیری از Bypass و Immutable Published Configuration

کنترل فقط در UI نیست و در Backend نیز enforce می‌شود:
- `PRODUCT_STATUS_CODE` فقط از Lifecycle Action قابل تغییر است.
- `VERSION_STATUS_CODE`, `IS_CURRENT`, `APPROVED_AT`, `APPROVED_BY` از Generic CRUD قابل تغییر نیستند.
- `PRODUCT_VERSION_MODULE.VALIDATION_STATUS_CODE` فقط می‌تواند در اثر تغییر Configuration به `NOT_VALIDATED` برگردد؛ ثبت `VALID/INVALID` فقط از Action Validate انجام می‌شود.
- نسخه `APPROVED` یا `ACTIVE`، Moduleها و Ruleهای وابسته آن از Generic Create/Update/Delete **Read-only** می‌شوند؛ برای اصلاح باید APPROVED به DRAFT برگردد یا Version جدید ایجاد شود.
- Child Ruleها نیز از طریق Parent Chain به Version Resolve می‌شوند؛ بنابراین قفل Governance با ورود مستقیم به Child API قابل دورزدن نیست.
- تغییر `PRODUCT_CLASS_CODE`, `PRODUCT_FAMILY_CODE`, `BALANCE_NATURE_CODE`, `DEFAULT_CURRENCY_CODE` و `PRODUCT_CODE` پس از وجود Version غیر-DRAFT رد می‌شود تا Applicability منتشرشده تغییر نکند.
- Approve و Publish همیشه Backend Validation را دوباره اجرا می‌کنند.

## 8. Database

- DDL جدید: **ندارد**.
- Migration جدید: **ندارد**.
- پیش‌نیاز دیتابیس: **FIX96 / 0.3.88**.
- پیش‌نیاز Source/UI: **FIX97 / 0.3.89**.

## 9. Acceptance Test

1. Product و Version در DRAFT بسازید.
2. Moduleهای Base مربوط به Family را Enable و `CONFIGURED` کنید.
3. Ruleهای مرتبط را ثبت کنید؛ برای Moduleهای Parent/Child، Child موردنیاز را نیز ایجاد کنید.
4. در مرحله ۶ «اعتبارسنجی Ruleها» را اجرا کنید.
5. برای داده ناقص باید Module = `INVALID` و Missing Table نمایش داده شود.
6. پس از تکمیل Ruleها، Validation باید `VALID` و Readiness = 100% شود.
7. «تصویب نسخه» باید Version را `APPROVED` و Audit Approval را پر کند.
8. «Publish نسخه» باید Version را `ACTIVE/IS_CURRENT=1` و Product را `ACTIVE` کند.
9. Publish بدون APPROVED یا با Rule ناقص باید از Backend رد شود.
10. تلاش برای تغییر مستقیم Statusهای Lifecycle با Generic CRUD باید رد شود.
11. بعد از APPROVE، Create/Update/Delete روی Module/Rule همان Version باید رد شود؛ بعد از Return-to-Draft دوباره مجاز باشد.
12. Child API مانند `PRODUCT_CHANNEL_OPERATION` روی Version غیر-DRAFT نیز باید به‌واسطه Parent Chain قفل باشد.
13. `node tools/verify-pdl-product-builder.mjs` باید OK باشد.
