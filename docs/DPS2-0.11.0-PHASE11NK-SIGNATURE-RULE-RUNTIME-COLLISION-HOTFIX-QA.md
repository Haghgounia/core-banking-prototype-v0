# DPS2 0.11.0 - Phase 11N-K Signature Rule Runtime Collision Hotfix QA

## Scope
Runtime-harness-only correction for the final Steps 00-17 qualification.

## Problem
Both the scoped 11N-D Steps 09-10 runtime and the historical 11L Wave C runtime can target the same ACTIVE account in one final qualification run. The canonical table `DEPOSIT_ACCOUNT_SIGNATURE_RULE` has a unique key on `(ACCOUNT_ID, VALID_FROM)`. Both harnesses previously used the current business date, so the later Wave C runtime could fail with HTTP 409 / DUPLICATE_VALUE. A rerun could also collide with evidence left by the prior attempt.

## Correction
- `runtime-dps2-phase11nd-steps09-10-e2e.mjs` reads current Wave C signature-rule evidence before mutation.
- `runtime-dps2-phase11l-wave-c-e2e.mjs` does the same.
- Each harness selects the first unused ISO date in a bounded 366-day window for `VALID_FROM`.
- No business service, repository, DDL, migration, or production API semantics are changed.
- Existing runtime evidence is preserved; no delete/backfill is performed.

## Verification
- Node syntax check: PASS for both runtime scripts.
- Phase 11N-D static verifier: 64/64 PASS.
- Phase 11L static verifier: 89/89 PASS.
- Rapid Completion static verifier: 39/39 PASS.

## Qualification
Re-run `tools\\qualify-dps2-phase11nk-final.cmd`. Final closure still requires:
`DPS2_PHASE11NK_STEPS00_17_FINAL_QUALIFICATION_PASS`.
