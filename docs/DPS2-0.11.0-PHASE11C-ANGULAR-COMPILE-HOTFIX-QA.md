# DPS2 0.11.0 Phase 11C - Angular Compile / Runtime JAR Lock Hotfix QA

## Trigger
Runtime build on Windows passed Phase 11C DB/static checks and Maven compile, then exposed two build-path issues:

1. `deposit-opening-wizard.component.ts` still referenced the removed `DepositAccountDetails.owners` property.
2. The running application kept `app/core-banking-prototype.jar` locked while `build-production.cmd` attempted to delete the stale runtime artifact.

## Fixes
- Funding source ownership verification now uses `DepositAccountDetails.parties` and requires the matching Account Party row to be `ACTIVE`.
- `DepositAccountParty` is imported explicitly in the wizard.
- Phase 11C static verifier rejects any reintroduction of `details.owners.some(...)` and verifies the active Account Party contract.
- Windows production build now fails fast with an explicit message when the canonical runtime JAR is locked instead of continuing with a stale executable artifact.

## Data / Schema Impact
None. No Oracle migration is required and no business data is changed.

## Qualification
- Phase 11C static verifier: 38/38 PASS after hotfix.
- Existing Phase 11C DB qualification remains valid; this hotfix changes only source/build behavior.
- Final Angular production build must be rerun on the target Windows environment after stopping the running Java process.

## Expected Runtime Follow-up
After a successful production build and restart, run:

`node tools\\runtime-dps2-phase11c-e2e.mjs`

Expected marker:

`PHASE11C_RUNTIME_E2E_PASS`
