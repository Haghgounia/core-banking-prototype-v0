# DPS2 0.11.0 — Phase 11H — Step 04 Canonical Profit Completion — QA

## 1. مبنای سندی

این فاز مستقیماً به **Operational Step 04 — مدیریت سود سپرده** و Packageهای **02 - Deposit Profit Operations** و **17 - Deposit Balance & Subledger** Trace می‌شود.

مدل canonical مستند Step 04 شامل این جداول است:

- `DEPOSIT_ACCOUNT_PROFIT_PROFILE`
- `DEPOSIT_PROFIT_PERIOD`
- `DEPOSIT_PROFIT_ACCRUAL`
- `DEPOSIT_PROFIT_ACCRUAL_DETAIL`
- `DEPOSIT_PROFIT_ADJUSTMENT`
- `DEPOSIT_PROFIT_PAYMENT`

Actionهای سندی تحت پوشش 11H:

1. بارگذاری/Provision پروفایل سود حساب؛
2. ایجاد/نگهداری دوره سود؛
3. شناسایی تدریجی سود با Detail محاسباتی؛
4. Adjustment غیرمخرب با Maker/Checker؛
5. Payment canonical برای مقصد `SAME_DEPOSIT` و Trace به Package 17.

`DEPOSIT_PROFIT_CONTRACT` و `DEPOSIT_PROFIT_POSTING` از Phase 11G به‌عنوان `MODEL_EXTENSION` حفظ می‌شوند و جایگزین مدل canonical بالا نیستند.

## 2. مرز Step 05

اجرای مالی destinationهای زیر در این فاز انجام نمی‌شود:

- `LINKED_ACCOUNT`
- `CUSTOMER_SELECTED_ACCOUNT`

این دو مقصد به **Step 05 / Package 03 - Deposit Transaction Processing** و Transaction validation/authorization/legs/reversal وابسته‌اند. به همین دلیل Step 04 حتی پس از PASS فنی 11H همچنان `PARTIAL` می‌ماند تا این dependency تکمیل شود.

## 3. Acceptance Criteria

### 3.1 Profile

- Activation برای حساب سوددار، `DEPOSIT_ACCOUNT_PROFIT_PROFILE` را از Opening Profit Snapshot provision می‌کند.
- Pricing trace، نرخ، day-count، frequency و payment destination بین Profile و operational Profit Contract سازگار می‌ماند.
- Migration برای حساب‌های 11G موجود Backfill غیرمخرب انجام می‌دهد.

### 3.2 Period / Accrual / Detail

- هر Accrual جدید به یک `PROFIT_PERIOD_ID` معتبر متصل است.
- برای qualification فعلی `MATURITY`، Period در بازه Contract ساخته می‌شود.
- برای frequencyهای تقویمی، Accrual از مرز Period عبور نمی‌کند.
- هر Accrual یک `BASE_PROFIT` در `DEPOSIT_PROFIT_ACCRUAL_DETAIL` دارد.
- Totalهای Period با Accrual atomically sync می‌شوند.

### 3.3 Adjustment

- Request باید دقیقاً به یک `ORIGINAL_ACCRUAL_ID` یا `ORIGINAL_PAYMENT_ID` متصل باشد.
- typeهای `INCREASE`, `DECREASE`, `REVERSAL` پشتیبانی می‌شوند.
- Request ابتدا `DRAFT` است و Approval در `DEPOSIT_OPERATION_APPROVAL_REQUEST` ثبت می‌شود.
- Approver نباید همان Maker باشد.
- Posting adjustment غیرمخرب است؛ source accrual/payment حذف یا overwrite نمی‌شود.
- Period و operational Contract با delta کنترل‌شده update می‌شوند.

### 3.4 Payment

- Payment مقصد `SAME_DEPOSIT` ابتدا در `DEPOSIT_PROFIT_PAYMENT` ثبت می‌شود.
- اثر مالی از primitive موجود Package 17 / 11D عبور می‌کند.
- `DEPOSIT_PROFIT_POSTING` فقط compatibility/trace extension باقی می‌ماند.
- Payment canonical به `PAID` می‌رسد و Period paid total و Contract paid/accrued state sync می‌شوند.
- replay با همان idempotency key نباید اثر مالی دوباره ایجاد کند.

## 4. Gates

### Static

```bat
node tools\verify-dps2-step04-canonical-profit-11h.mjs
```

انتظار:

```text
PHASE11H_STATIC_VERIFIER_FAIL=0
PHASE11H_STATIC_BASELINE_PASS
```

### Database

```bat
tools\apply-dps2-phase11h.cmd
```

انتظار:

```text
PHASE11H_STEP04_CANONICAL_PROFIT_RECONCILIATION_PASS
PHASE11H_DB_VERIFIER_FAIL=0
PHASE11H_DB_BASELINE_PASS
PHASE11H_IMPLEMENTATION_PASS
```

### Runtime

پس از Build/Restart:

```bat
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11h-e2e.mjs
```

انتظار حداقل:

```text
PHASE11H_RUNTIME_E2E_ACCOUNT=...
PHASE11H_RUNTIME_E2E_ACCRUAL=...
PHASE11H_RUNTIME_E2E_PERIOD=...
PHASE11H_RUNTIME_E2E_DETAIL=...
PHASE11H_RUNTIME_E2E_ADJUSTMENT=...
PHASE11H_RUNTIME_E2E_POSTING=...
PHASE11H_RUNTIME_E2E_PAYMENT=...
PHASE11H_RUNTIME_E2E_PASS
```

## 5. Closure Rule

Phase 11H فقط پس از Static + DB + Runtime PASS فنی `CLOSED` می‌شود. با این حال **Document Step 04** تا تکمیل external payment از مسیر Step 05، `PARTIAL` باقی می‌ماند.
