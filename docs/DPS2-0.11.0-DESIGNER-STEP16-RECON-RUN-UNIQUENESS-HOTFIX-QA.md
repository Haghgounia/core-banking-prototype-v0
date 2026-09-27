# DPS2 0.11.0 - Designer Step 16 Reconciliation Run Uniqueness Hotfix QA

## Scope
Fix the Step 16 runtime collision when NOSTRO and VOSTRO correspondent accounts execute BALANCE reconciliation on the same business date.

Observed runtime evidence:

- NOSTRO account creation and reconciliation reached a real `DEPOSIT_RECONCILIATION_RUN`.
- VOSTRO then failed on `POST /reconciliation-runs` with `409 DUPLICATE_VALUE`.

## Root cause
`DPS2.DEPOSIT_RECONCILIATION_RUN` has the canonical unique key:

`(BUSINESS_DATE, RECONCILIATION_TYPE_CODE, SOURCE_SYSTEM_CODE, TARGET_LEDGER_CODE)`

The Step 16 UI and runtime harness used the same fixed `SOURCE_SYSTEM_CODE = CORRESPONDENT_STATEMENT` for every correspondent account. Two accounts reconciled on the same day therefore collided even though they were different account masters.

## Correction
- Derive a stable account-scoped reconciliation source code from:
  - `ACCOUNT_ID`
  - `STATEMENT_SOURCE_CODE`
- Format: `CORR_<ACCOUNT_ID>_<STATEMENT_SOURCE_CODE>`, normalized to the Oracle 50-character contract.
- Apply the same contract in the designer UI and Step 16 runtime qualification.
- In the designer UI, if the same account/source/type/target has already completed reconciliation for the current business date, reuse that completed run instead of attempting a duplicate insert.
- No DDL, constraint relaxation, direct Oracle seed, or fabricated business data is introduced.

## Verification
Expected static markers:

- `DESIGNER_STEP16_STATIC_PASS=71`
- `DESIGNER_STEP16_STATIC_FAIL=0`
- `DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_STATIC_PASS`
- `DESIGNER_BUSINESS_GAP_CLOSURE_A_PASS=76`
- `PHASE11M_STATIC_VERIFIER_PASS=92`
- `PHASE11N_RAPID_STATIC_VERIFIER_PASS=39`

Expected runtime markers include distinct sources for both branches:

- `DESIGNER_STEP16_RUNTIME_NOSTRO_RECON_SOURCE=CORR_<id>_SWIFT_MT940`
- `DESIGNER_STEP16_RUNTIME_VOSTRO_RECON_SOURCE=CORR_<id>_SWIFT_MT940`
- `DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_RUNTIME_PASS`
- `DPS2_DESIGNER_STEP16_QUALIFICATION_PASS`
