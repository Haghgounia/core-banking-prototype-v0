# DPS2 0.11.0 — Phase 11I — Step 05 Deposit Transaction Processing QA

## مرجع Scope

این فاز فقط از Operational Step 05 در `Deposit_Account_Operations_Traceability_Guide_FA_v2_2026-09-22` استخراج شده است.

Trace رسمی:

`Step 05 -> Package 03 Deposit Transaction Processing -> Package 17 Balance/Subledger`

و Validation به Packageهای زیر وابسته است:

- `01C - Deposit Account Hold Control`
- `15 - Deposit Account Limits & Restrictions`
- `07 - Deposit Regulatory & Compliance`
- `00 - Deposit Operation Governance & Control`

## Actionهای مستند که این فاز پوشش می‌دهد

1. اعتبارسنجی تراکنش؛
2. تأیید/مجوز؛
3. ثبت تراکنش و Legها؛
4. اتصال مالی به `DEPOSIT_SUBLEDGER_ENTRY` و Balance؛
5. برگشت non-destructive تراکنش.

## جداول Canonical Step 05

- `DEPOSIT_TRANSACTION`
- `DEPOSIT_TRANSACTION_VALIDATION`
- `DEPOSIT_TRANSACTION_AUTHORIZATION`
- `DEPOSIT_TRANSACTION_STATUS_HISTORY`
- `DEPOSIT_TRANSACTION_LEG`
- `DEPOSIT_TRANSFER_DETAIL`
- `DEPOSIT_CASH_TRANSACTION_DETAIL`
- `DEPOSIT_TRANSACTION_REVERSAL`
- `DEPOSIT_SUBLEDGER_ENTRY`
- `DEPOSIT_ACCOUNT_BALANCE`

## State Machine

```text
INITIATED
  -> VALIDATED
  -> PENDING_AUTH
  -> AUTHORIZED
  -> POSTED
  -> REVERSED

Validation failure -> REJECTED
Technical posting failure -> transaction remains AUTHORIZED and may be retried idempotently.
```

## Validation Evidence

برای هر تراکنش جدید این کنترل‌ها Persist می‌شوند:

- `ACCOUNT_STATUS`
- `ACTIVE_HOLD`
- `AVAILABLE_BALANCE` برای Debit
- `TRANSACTION_RESTRICTION`
- `ACCOUNT_LIMIT`
- `REGULATORY_RESTRICTION`

Posting دوباره همان کنترل‌های جاری را ارزیابی می‌کند؛ بنابراین تغییر Hold/Limit/Balance بعد از Authorization قابل دورزدن نیست.

## Maker / Checker

تمام تراکنش‌های عادی بعد از Validation به `PENDING_AUTH` می‌روند. Authorizer باید با `CREATED_BY` متفاوت باشد و Approval در `DEPOSIT_TRANSACTION_AUTHORIZATION` ذخیره می‌شود.

## Cash / Transfer

- Cash: یک Deposit Leg + `DEPOSIT_CASH_TRANSACTION_DETAIL`؛ `CASH_MANAGEMENT_TXN_REF` الزامی است.
- Internal Transfer: Source Debit Leg + Destination Credit Leg و هر دو از 11D Posting primitive عبور می‌کنند.
- External Transfer: Source Deposit Leg post می‌شود و Destination Leg با Account Reference خارجی ثبت می‌شود؛ settlement rail خارجی خارج از Package 03 prototype است.

## Reversal

Reversal تراکنش اصلی را حذف یا overwrite نمی‌کند. Transaction جدید `REVERSAL` ساخته می‌شود، Legهای معکوس با `REVERSAL_OF_ENTRY_ID` به Subledger اصلی وصل می‌شوند و `DEPOSIT_TRANSACTION_REVERSAL` Trace رسمی را نگه می‌دارد.

## Idempotency

تمام mutationها از `DEPOSIT_OPERATION_IDEMPOTENCY` استفاده می‌کنند. `DEPOSIT_TRANSACTION.IDEMPOTENCY_KEY` نیز برای Initiation حفظ شده است.

## Gates

```text
node tools\verify-dps2-step05-transaction-processing-11i.mjs
tools\apply-dps2-phase11i.cmd
build-production.cmd
node tools\runtime-dps2-phase11i-e2e.mjs
```

Runtime باید حداقل Cash Deposit، Cash Withdrawal، Internal Transfer، Reversal، Validation rejection و Idempotent replay را اثبات کند.

## Document Coverage

PASS فنی 11I به معنی DONE بودن Stepهای 07 و 11 نیست. در این فاز فقط داده‌های موجود Limit/Restriction/Compliance در Validation مصرف می‌شوند؛ CRUD کامل آن Domainها در فازهای مستند خودشان باقی می‌ماند.

## Source Qualification Evidence — 2026-09-24

```text
PHASE11I_STATIC_VERIFIER_PASS=64
PHASE11I_STATIC_VERIFIER_FAIL=0
PHASE11I_STATIC_BASELINE_PASS
PHASE11I_JAVAC_CONTRACT_PASS
```

Regression baseline after the Step 05 source changes:

```text
11H = 64/64 PASS
11G = 92/92 PASS
11F = 64/64 PASS
11E = 49/49 PASS
11D = 56/56 PASS
```

Additional hardening before Oracle qualification:

- Account-limit usage is calculated using the scope stored on each active limit (`TRANSACTION_TYPE_CODE` / `CHANNEL_CODE`), so a global limit is not accidentally reduced to the current transaction type/channel.
- DB verification isolates Step 05 operational evidence from legacy opening-funding transactions by excluding rows with `SOURCE_OPENING_FUNDING_ID IS NOT NULL` from Validation/Authorization/Leg assertions.
- Full Angular/Spring production compilation remains a Windows qualification gate because this workspace does not contain frontend `node_modules` or an executable Maven wrapper.


## Compile Hotfix Evidence — 2026-09-24

Windows production build exposed `long cannot be converted to java.lang.String` in `DepositTransactionService.requireAccount`. Root cause: `DepositAccountNotFoundException` accepts a String message. The service now uses the same canonical account-not-found message pattern as Balance/Profit/Closure, and the 11I static verifier has a dedicated regression guard. Oracle DB qualification had already passed 19/19 before this source-only hotfix.


## Oracle NUMBER RowMapper Runtime Hotfix — 2026-09-24

After Sequence high-water reconciliation passed 27/27, Runtime still failed at transaction initiation. The failure was traced to nullable Oracle `NUMBER` columns mapped with direct Java casts `(Long) ResultSet.getObject(...)` in `DepositTransactionRepository`. Initiation inserts transaction/leg/detail rows and then materializes a `TransactionView`; mapping `DEPOSIT_TRANSACTION_LEG.ACCOUNT_ID` could raise `ClassCastException` because Oracle JDBC may expose `NUMBER` as `BigDecimal`. The enclosing transaction therefore rolled back and the API returned HTTP 500.

The repository now uses typed JDBC extraction `getObject(index, Long.class)` for all nullable numeric identifiers/counts in Step 05. Static verification contains a regression guard prohibiting direct `(Long)r.getObject` casts in this repository.


## Transaction-aware Subledger Posting Hardening — 2026-09-24

Runtime reached `AUTHORIZED` but returned HTTP 409 `DUPLICATE_VALUE` during financial Posting. The Step 05/11D integration is hardened as follows:

- transaction/leg trace is written in the same `DEPOSIT_SUBLEDGER_ENTRY` INSERT rather than attached afterward;
- `ENTRY_SEQUENCE_NO` is the Step 05 `LEG_NO`, preserving the transaction-scoped uniqueness contract for internal transfers;
- posting references use `DPS2-TX-*` / `DPS2-REV-*` namespaces;
- `SEQ_DEPOSIT_SUBLEDGER_ENTRY` is reconciled above its table high-water mark by the 11I migration;
- DB verification checks both sequence high-water and leg/entry-sequence coherence;
- the legacy 10-argument `PostEntryRequest` constructor remains available to existing non-Step-05 callers.

Expected Oracle gate after this hotfix: `PHASE11I_DB_VERIFIER_PASS=29`, `FAIL=0`.
