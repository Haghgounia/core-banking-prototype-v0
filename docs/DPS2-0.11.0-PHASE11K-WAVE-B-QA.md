# DPS2 0.11.0 — Phase 11K Wave B QA

## Scope

Phase 11K is a consolidated implementation wave for canonical Deposit Account Operations Steps 06–08 without rewriting the already-closed 11E closure/reopening engine.

- Step 06 / Packages 14 + 03 + 17: statement template/version, statement request/header/items, delivery attempts.
- Step 07 / Packages 15 + 00: account limits, periodized usage, transaction/channel restrictions and non-destructive release/revoke.
- Step 08 / Packages 16 + 01A + 01B + 01C + 13 + 03 + 17 + 00: maturity process run/item/event materialization integrated with the existing term workflow.

## Source-of-truth rules

1. Statement balances and lines are derived from the operational subledger, with `TRANSACTION_ID` retained when available.
2. Limit usage is not incremented blindly. It is recomputed from transactions currently in `POSTED` status for the relevant period/scope; reversal therefore removes the reversed transaction from current usage.
3. Revoke/release changes status/validity and never physically deletes the governed record.
4. Maturity Batch may materialize a `PENDING` maturity event but does not invent renewal, transfer, settlement or closure completion. Existing Step 03/05/11E workflows own final financial execution.
5. Existing term renewal progresses an already-materialized maturity event for the same term/maturity cycle rather than inserting a duplicate.

## Source-fidelity refinements

- Statement `EMAIL/SMS` destinations must resolve to an active `DEPOSIT_ACCOUNT_CONTACT`; `PORTAL` and `API` use the system destinations defined by the Operational prototype.
- Current UI/API limit choices are intentionally limited to the four options present in the Operational prototype: `DAILY_AMOUNT`, `DAILY_COUNT`, `MONTHLY_AMOUNT`, `TRANSACTION_AMOUNT`.
- Maturity completed-run counters are verified exactly against process items; a `PENDING` maturity event is never treated as financial completion.
- One-command Windows qualification is provided by `tools\qualify-dps2-wave-b-11k.cmd`.

## Qualification gates

Expected source gate:

```text
PHASE11K_STATIC_VERIFIER_PASS=88
PHASE11K_STATIC_VERIFIER_FAIL=0
PHASE11K_STATIC_BASELINE_PASS
```

Expected Oracle gate:

```text
PHASE11K_DB_VERIFIER_PASS=34
PHASE11K_DB_VERIFIER_FAIL=0
PHASE11K_DB_BASELINE_PASS
PHASE11K_IMPLEMENTATION_PASS
```

Expected Runtime E2E:

```text
PHASE11K_RUNTIME_STATEMENT=...
PHASE11K_RUNTIME_LIMIT=...
PHASE11K_RUNTIME_RESTRICTION=...
PHASE11K_RUNTIME_MATURITY_RUN=...
PHASE11K_RUNTIME_E2E_PASS
```

Technical Phase 11K remains `IN_PROGRESS` until all gates pass on the Windows/Oracle runtime baseline.
