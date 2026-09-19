# DPS2 0.10.0 — Phase 10E Batch Opening Hardening

## Scope
Operational v5 Batch is restricted to `GOV_EMPLOYEE_SAVINGS_1376` and `QARD_SAVINGS`. Batch remains an intake/orchestration surface; it cannot bypass the individual Opening aggregate gates.

## Implemented
- Batch row validation remains Oracle/CIF/PDL-backed.
- A VALID Batch Item can hand off into the normal seven-step Opening Wizard.
- The Wizard receives `BATCH_ITEM_ID`, Party, Product Version, currency and opening amount as controlled prefill only.
- The linked Opening is persisted as `REQUEST_TYPE_CODE=BULK` and `DEPOSIT_OPENING_REQUEST.BATCH_ITEM_ID`.
- Transactional linking requires the Batch Item to still be `VALID`, match the Product Version and have no existing Opening.
- Once linked, Batch Item becomes `PROCESSING`; it only becomes `SUCCESS` after its individual Account is `ACTIVE` (or subsequently `CLOSED`).
- Batch header status/counters are refreshed from individual row progress.
- Direct Batch `process` and `activate` remain deliberately blocked; no group shortcut exists around Funding/Settlement/Readiness.
- No CDD, sanctions, SIAH, tax or readiness evidence is fabricated by Batch handoff.

## Database
No new DDL is required. Existing `DEPOSIT_OPENING_REQUEST.BATCH_ITEM_ID`, `DEPOSIT_OPENING_BATCH_ITEM.OPENING_REQUEST_ID` and `ACCOUNT_ID` contracts are used.
