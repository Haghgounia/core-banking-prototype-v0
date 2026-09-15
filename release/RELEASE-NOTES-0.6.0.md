# Release Notes — 0.6.0

## Four Deposits / Deposit Account Opening — Phase 6
- Dedicated Bulk Opening UI matching the supplied `Deposit_Account_Opening.html` flow.
- Batch Header / Item / Error persistence and workflow APIs.
- `FILE / API / MANUAL` source modes and source-defined Batch/Item lifecycle states.
- Party validation through CIF and Product Version validation through PDL integration contracts.
- Per-item isolated processing (`REQUIRES_NEW`).
- One Opening Request per valid row with `REQUEST_TYPE_CODE=BULK` and `BATCH_ITEM_ID` traceability.
- Existing Account Lifecycle reused for create and activation; accounts are created as `PENDING_ACTIVATION`.
- Batch terminal status `COMPLETED / PARTIAL / FAILED` based on item outcomes.
- Minimal safe retry behavior for transient processing failures without duplicating already successful rows.
- Reference-data migration only; the three existing Batch operational tables are not recreated.
- Dedicated Phase 6 regression verifier added to the production build gate.

## Governance note
`ROW_INVALID` is explicitly aligned with the supplied HTML prototype. `PROCESSING_FAILED` and `ACTIVATION_FAILED` are provisional technical prototype codes; the final bank-wide error-code taxonomy remains Data-Governance owned.
