# DPS2 0.11.0 — Phase 11I Transaction-aware Subledger Hotfix QA

Date: 2026-09-24
Status: IN_PROGRESS until Windows Oracle + Runtime qualification

## Observed Runtime Failure

Step 05 passed Initiate, Validation and Maker/Checker Authorization, then `POST /api/v1/deposit-accounts/{accountId}/transactions/{transactionId}/post` returned HTTP 409 `DUPLICATE_VALUE`.

## Defect Removed

The Step 05 integration previously called the generic 11D posting primitive with `TRANSACTION_ID=NULL`, `TRANSACTION_LEG_ID=NULL` and `ENTRY_SEQUENCE_NO=1`, then updated transaction trace after insert. This design cannot safely represent a two-leg internal transfer under the existing unique transaction/entry-sequence index and also leaves posting-reference collisions in the global Subledger namespace harder to isolate.

## Hotfix

1. `PostEntryRequest` supports optional `transactionId`, `transactionLegId`, `entrySequenceNo`.
2. The existing 10-argument constructor remains for Opening/Profit/Closure and other callers.
3. Step 05 writes transaction trace atomically with Subledger INSERT.
4. `ENTRY_SEQUENCE_NO` equals `DEPOSIT_TRANSACTION_LEG.LEG_NO`.
5. Posting references are namespaced as `DPS2-TX-*` and `DPS2-REV-*`.
6. 11I migration reconciles `SEQ_DEPOSIT_SUBLEDGER_ENTRY` above `MAX(SUBLEDGER_ENTRY_ID)`.
7. DB verifier checks the new sequence and Leg/Entry sequence coherence.

## Local Evidence

```text
PHASE11I_STATIC_VERIFIER_PASS=64
PHASE11I_STATIC_VERIFIER_FAIL=0
PHASE11I_STATIC_BASELINE_PASS
PHASE11I_POST_REQUEST_COMPAT_PASS
```

Full Maven compilation remains a Windows qualification gate because Maven is not installed in this workspace.

## Required Windows Gates

```bat
node tools\verify-dps2-step05-transaction-processing-11i.mjs
tools\apply-dps2-phase11i.cmd
bin\stop.cmd
build-production.cmd
bin\start.cmd
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11i-e2e.mjs
```

Expected DB target: `PHASE11I_DB_VERIFIER_PASS=29`, `PHASE11I_DB_VERIFIER_FAIL=0`.
Phase 11I is not CLOSED until `PHASE11I_RUNTIME_E2E_PASS`.
