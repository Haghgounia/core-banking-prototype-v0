# DPS2 0.10.0 Phase 10A — Operational Opening v5 Foundation QA

## Scope

Authoritative functional source: `Deposit_Account_Opening_Operational_Final_2026-09-17.html` (Deposit Account Opening v5).

This step is the **database/model foundation only** for Phase 10. It intentionally precedes Backend/API/UI changes so the Oracle physical model can be qualified first.

## Delta implemented

### DEPOSIT_OPENING_REQUEST
Added, idempotently:
- `CUSTOMER_RISK_LEVEL_CODE`
- `RISK_ASSESSMENT_REFERENCE`
- `EXPECTED_ACTIVITY_REFERENCE`
- `JOINT_ACCOUNT_BASIS_CODE`
- `JOINT_BASIS_REFERENCE`
- `ACTIVATION_STATUS_CODE`
- `ACTIVATION_DEADLINE_AT`

FKs:
- `JOINT_ACCOUNT_BASIS_CODE -> REF_DEP_OPEN_JOINT_BASIS`
- `ACTIVATION_STATUS_CODE -> REF_DEP_OPEN_ACTIVATION_STATUS`

`CREATED_ACCOUNT_NO` is deliberately **not** persisted because `ACCOUNT_NO` is resolved by `CREATED_ACCOUNT_ID -> DEPOSIT_ACCOUNT.ACCOUNT_ID`. This avoids duplicate account-number state.

### DEPOSIT_OPENING_FUNDING
Added:
- `SOURCE_PARTY_ID`
- `SOURCE_ACCOUNT_ID`
- `FUNDING_PURPOSE_CODE`
- `SOURCE_OWNERSHIP_VERIFIED_FLAG`
- `SOURCE_VERIFICATION_REFERENCE`
- `CASH_MANAGEMENT_TXN_REF`
- `ATTEMPT_AT` (only if missing)

The sequence `SEQ_DEPOSIT_OPENING_FUNDING` is created only if absent and starts above the current max ID.

### DEPOSIT_OPENING_CHECK
Added:
- `CHECK_PHASE_CODE`
- `BLOCKING_SCOPE_CODE`
- `REQUIRED_FLAG`
- `RECHECK_REQUIRED_FLAG`
- `VALID_UNTIL`
- `SOURCE_EVALUATION_REFERENCE`

Existing rows are backfilled from `REF_DEP_OPEN_CHECK` where a catalog row exists.

### New operational tables
- `DEPOSIT_OPENING_OBLIGATION`
- `DEPOSIT_OPENING_FUND_ALLOC`

With explicit sequences, PKs, request/funding/obligation FKs, activation-mandatory flag, settlement status and audit columns.

### DEPOSIT_OPENING_BATCH
Added:
- `BULK_OPENING_BASIS_CODE`
- `LEGAL_BASIS_REFERENCE`
- `CDD_APPROVAL_REFERENCE`

The v5 business restriction to the government-employee savings exception will be enforced in the Backend phase; the database does not hard-code a single legal basis as a permanent physical-model limitation.

### DEPOSIT_ACCOUNT
Added:
- `OPENED_PRODUCT_VERSION_ID`
- `CURRENT_PRODUCT_VERSION_ID`
- `OWNERSHIP_TYPE_CODE`
- `OPENED_ON`
- `ACTIVATION_DEADLINE_AT`
- `ACTIVATION_POLICY_VERSION`
- `LEDGER_BALANCE`
- `AVAILABLE_BALANCE`
- `DEBIT_CAPABILITY_CODE`

The legacy `PRODUCT_VERSION_ID` remains for backward compatibility during the transition.

Historical financial balances and activation deadlines are **not fabricated**. Only product-version/ownership/opened-on metadata that can be deterministically derived from existing data is backfilled.

## Reference-data alignment

- Adds v5 funding purposes: `OPENING_TOTAL`, `INITIAL_BALANCE`, `CHARGES`.
- Deactivates legacy active funding-purpose codes: `OPENING_BALANCE`, `OBLIGATION_SETTLEMENT`, `COMBINED` (records are retained for history).
- Adds missing v5 check types: `REGULATORY`, `FINANCIAL`, `PRODUCT`, `CONTRACTUAL`, `NOTIFICATION`.
- Reconciles the 23 Create-Gate checks from v5.
- Adds 14 Activation-Readiness requirements to the same check catalog for persistence through `DEPOSIT_OPENING_CHECK` with `PRE_ACTIVATION` / relevant blocking scope.
- Deactivates legacy `SANCTIONS` and `OPENING_RULES`; historical rows are retained. `PEP_SANCTIONS` is the v5 replacement for the former sanctions check.

## Safety

- Idempotent object/column/constraint/index creation.
- Fully schema-qualified `DPS2` references; independent of `CURRENT_SCHEMA`.
- No table drops.
- No historical transaction generation.
- No fabricated SIAH/reference-system results.
- No fabricated balances for pre-existing accounts.

## Static QA

`node tools/verify-dps2-opening-operational-v5-phase10.mjs`

Result:

`DPS2 Operational Opening v5 Phase 10 verification OK: 34/34 schema-foundation checks passed.`

## Runtime Gate

Run the migration in Oracle with **Run Script / F5**. Phase 10A is runtime-qualified only after the final verification queries and SUCCESS marker complete without Oracle errors.
