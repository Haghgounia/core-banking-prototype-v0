# DPS2 0.11.0 — Phase 11G Profit / Interest Processing

## هدف
Phase 11G لایه عملیاتی محاسبه و ثبت سود سپرده را روی Baseline چهار سپرده اضافه می‌کند، بدون ورود به Transaction Processing کامل فاز 11H.

## مرزبندی معماری
- `DEPOSIT_OPENING_PROFIT_INSTRUCTION` Snapshot قراردادی زمان افتتاح است.
- `DEPOSIT_PROFIT_CONTRACT` Snapshot عملیاتی حساب و منبع نرخ/روش محاسبه در 11G است.
- `DEPOSIT_PROFIT_ACCRUAL` Evidence محاسبه Accrual را با basis/rate/day-count نگهداری می‌کند.
- `DEPOSIT_PROFIT_POSTING` اتصال بین سود محاسبه‌شده و Subledger Entry فاز 11D است.
- 11G مستقیماً `DEPOSIT_TRANSACTION` ایجاد نمی‌کند.
- پرداخت `SAME_DEPOSIT` از `DepositBalanceService.post()` عبور می‌کند.
- پرداخت `LINKED_ACCOUNT` و `CUSTOMER_SELECTED_ACCOUNT` تا Phase 11H Deferred است.

## Product / Contract Traceability
در Activation، `DepositProfitContractProvisioningService` داده `DEPOSIT_OPENING_PROFIT_INSTRUCTION` را به Operational Contract تبدیل می‌کند. شناسه‌های `PRICING_RULE_ID`, `PRICING_COMPONENT_ID`, `RATE_TIER_ID`, `PROFIT_PAYMENT_RULE_ID` در Snapshot حفظ می‌شوند و Rate در زمان Accrual دوباره از PDL Resolve نمی‌شود.

برای `SHORT_TERM_DEPOSIT` و `LONG_TERM_DEPOSIT` وجود Profit Instruction در Activation اجباری است. Migration برای حساب‌های قبلی فقط وقتی Backfill انجام می‌دهد که Opening صریحاً Profit Instruction و `RATE_VALUE` داشته باشد؛ نرخ مصنوعی تولید نمی‌شود.

## محاسبه
Baseline این Increment:
- `CALCULATION_METHOD_CODE=PERCENTAGE`
- `DAY_COUNT_BASIS_CODE=ACT_365 | ACT_360`
- Basis = مانده Ledger عملیاتی 11D، حداقل صفر
- Accrual تا `EFFECTIVE_TO` Contract محدود است
- Monetary calculation با scale میانی 8 و خروجی 4 رقم اعشار انجام می‌شود.

## Idempotency
هر دو Mutation زیر `X-Idempotency-Key` می‌خواهند و از `DEPOSIT_OPERATION_IDEMPOTENCY` استفاده می‌کنند:
- `POST /api/v1/deposit-accounts/{id}/profit/accruals`
- `POST /api/v1/deposit-accounts/{id}/profit/postings`

## API
- `GET /api/v1/deposit-accounts/{id}/profit`
- `POST /api/v1/deposit-accounts/{id}/profit/accruals`
- `POST /api/v1/deposit-accounts/{id}/profit/postings`

## Regression 11F
از آنجا که Activation حساب مدت‌دار پس از 11G Profit Snapshot را اجباری می‌کند، Runtime Bootstrap فاز 11F نیز `DEPOSIT_OPENING_PROFIT_INSTRUCTION` معتبر ارسال می‌کند.

## Qualification
Static gate:
```bat
node tools\verify-dps2-profit-processing-11g.mjs
```

Oracle migration + verifier:
```bat
tools\apply-dps2-phase11g.cmd
```

Runtime E2E پس از Build/Restart نسخه جدید:
```bat
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11g-e2e.mjs
```

Runtime E2E باید Provisioning، Accrual، Idempotent replay، Same-Deposit Posting، تغییر Ledger، پاک‌شدن Accrued Amount و Trace به `DEPOSIT_SUBLEDGER_ENTRY` را اثبات کند.

## Hardening و Release Gate
- `DEPOSIT_PROFIT_ACCRUAL.PROFIT_POSTING_ID` دارای FK صریح به `DEPOSIT_PROFIT_POSTING` است.
- Bootstrap Runtime، `MATURITY_ACTION_CODE` را از `DEPOSIT_PRODUCT_TERM_RULE` فعال PDL حفظ می‌کند.
- UI، `Accrual Through Date` را به تاریخ جاری (با سقف `EFFECTIVE_TO`) مقداردهی می‌کند، نه به `LAST_ACCRUAL_DATE`.
- `build-production.cmd` اکنون Gateهای 11D، 11E، 11F و 11G را در مسیر Canonical Build اجرا می‌کند.
- Static Qualification نهایی این Increment: `72/72 PASS`.

## Deployment Order
به دلیل اضافه‌شدن Java source و schema جدید، ترتیب نصب این فاز با Hotfixهای صرفاً Script متفاوت است:
1. Overlay Source را اعمال کنید.
2. Static Gate را اجرا کنید.
3. Migration/DB Verifier را با `tools\apply-dps2-phase11g.cmd` اجرا کنید.
4. Runtime فعلی روی پورت 8091 را Stop کنید.
5. `build-production.cmd` را اجرا کنید.
6. نسخه جدید را با `bin\start.cmd` اجرا کنید.
7. Runtime E2E فاز 11G را اجرا کنید.

Build محلی تولیدکننده این Patch به دلیل عدم دسترسی محیط ایزوله به Maven Central قابل اجرای کامل نبود؛ بنابراین Java/Angular compilation باید در محیط پروژه با Wrapper و npm موجود به‌عنوان Deployment Gate اجرا شود. Static و Node syntax gates در Source Patch عبور کرده‌اند.
