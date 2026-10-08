# PB-R12 — Product Builder UX and Grid Controls

Baseline: Core Banking Prototype 0.11.0; based on PB-R11 ZIP uploaded on 2026-10-08.

## Changes

- `PRODUCT_REQUIRED_DOCUMENT.PROCESS_STEP_NO`: optional internal sequencing field removed from ordinary editor/grid, without DDL/API removal.
- `PRODUCT_REQUIRED_INQUIRY.MAX_RESULT_AGE_MINUTES`: user inputs a positive integer value plus minute/hour/day; raw value persists in minutes. Existing minutes are rendered in the largest exact unit. Blank means unrestricted/unspecified. No invented month length.
- `DEPOSIT_PRODUCT_PROFILE`: group and type displayed read-only. Existing records/clones preserve stored codes; for new records from a version context, map supported product families to controlled deposit group/type using `PRODUCT_VERSION` -> `PRODUCT`. Unknown families are *not guessed* and cannot be saved without configured mapping. Map supports SAVINGS/QARD_SAVINGS, CURRENT/CURRENT_ACCOUNT, TERM/TERM_DEPOSIT; additional families require approved business mapping.
- `DEPOSIT_PRODUCT_OPENING_RULE`: STAMP_COUNT disabled and cleared for non-applicable stamp duty; when applicable, a strictly positive count is required.
- Generic PDL grid: Clone loads editable values and creates a **new** row rather than updating the original. For eligibility, child criterion records are copied into an independent aggregate through the existing API; no row IDs are copied. Unique constraints remain enforced by Backend.
- Generic PDL grid: delete invokes browser confirmation before calling API. This covers the Product Builder grid's delete action, *not every other module in the application*.
- Angular Router 21 Signal invocation corrected: `lastSuccessfulNavigation()?.previousNavigation`.
- Three PB documentation files moved from project root to `docs/` to satisfy release layout verifier.

## Verification

- PASS: `node tools/verify-pb-r12.mjs` — 12/12 static checks.
- PASS: `node tools/verify-pb-r11.mjs` — 8/8 static checks.
- PASS: TypeScript source syntax transpilation — no syntax diagnostics.
- NOT TESTED: Angular compiler with full node_modules; dependencies unavailable in the tool environment.
- NOT TESTED: Browser behavior (overlay, form accessibility, row cloning).
- NOT TESTED: Oracle persistence and reference-code mapping with production reference rows.
- NOT TESTED: Java production build/runtime; Backend not changed in PB-R12.

## Windows qualification

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pb-r12.mjs
build-production.cmd
bin\start
```

Do not apply migrations for PB-R12: no schema change is included. If Build succeeds, test one representative record for each form, cloning after changing at least one business key, decline and accept a delete confirmation, stamp duty both states, and deposit group/type against actual Oracle reference values.
