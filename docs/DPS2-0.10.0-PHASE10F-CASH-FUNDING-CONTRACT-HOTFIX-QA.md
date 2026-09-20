# DPS2 0.10.0 Phase 10F - Cash Funding Contract Hotfix

## Root cause
Runtime persistence failed with:

`ORA-02290: check constraint (DPS2.CHK_DEPOSIT_OPENING_FUNDING_FUNDING_METHOD_CODE_CASH_MANAGEMENT_TXN_REF) violated`

The Phase 10F qualification fixture used `FUNDING_METHOD_CODE=CASH` but omitted `CASH_MANAGEMENT_TXN_REF`.
The Angular wizard also serialized `CASH_MANAGEMENT_TXN_REF` as null for every funding method, while the backend pre-persistence validation did not guard the CASH-specific contract.

## Changes
- Phase 10F prepare tool now emits a non-placeholder `CASH_MANAGEMENT_TXN_REF` for CASH funding.
- Angular wizard maps the entered funding reference to `CASH_MANAGEMENT_TXN_REF` when the funding method is CASH.
- Backend aggregate validation requires `CASH_MANAGEMENT_TXN_REF` when `FUNDING_METHOD_CODE=CASH`.
- Phase 10E/10F verifier adds regression checks for all three points.

## Static verification
`DPS2 Operational Opening v5 Phase 10E/10F verification: 35/35 passed.`

`node --check tools/prepare-dps2-phase10f-runtime.mjs` passed.

## Build note
The delivery environment used to assemble this patch does not have Maven 3.9.16 or frontend node_modules available offline, so Maven/Angular compilation must be run in the target project environment.

## Runtime qualification sequence
1. Extract this patch at the project root.
2. Run `node tools\verify-dps2-opening-v5-phase10e10f.mjs` and expect 35/35.
3. Rebuild frontend and backend for release correctness.
4. Restart the runtime.
5. Set `CORE_BANKING_BASE_URL=http://127.0.0.1:8091`.
6. Run `node tools\prepare-dps2-phase10f-runtime.mjs`.
7. Run `node tools\runtime-dps2-phase10f-e2e.mjs docs\examples\phase10f-e2e-fixture.runtime.json`.
