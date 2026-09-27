# DPS2 0.11.0 - Phase 11M Correction Verifier Hotfix

Date: 2026-09-27

## Symptom

`build-production.cmd` stopped at the Phase 11M static gate with:

```text
FAIL | correction execution is non-destructive Step05 reversal
```

while the following RC correction coverage check already passed.

## Root cause

The verifier still searched for the historical literal:

```text
new ReversalRequest("CORRECTION_REQUEST")
```

The current implementation intentionally maps `REVERSAL` to `CORRECTION_REQUEST` and `DUPLICATE_CANCEL` to `DUPLICATE_POSTING` through a conditional expression. Both financial correction types still execute through the canonical Step 05 `transactions.reverse(...)` path and persist `DEPOSIT_CORRECTION_ENTRY`.

## Fix

Only the static verifier contract was updated. It now verifies:

- `REVERSAL` branch
- `DUPLICATE_CANCEL` branch
- Step 05 `transactions.reverse(...)`
- both governed reversal reason codes
- `insertCorrectionEntry` audit persistence

No business logic, REST API, DDL, seed data, or runtime behavior was changed.
