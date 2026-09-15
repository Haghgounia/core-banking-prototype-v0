# QA Report — 0.7.0

## Phase
DPS2 Deposit Account Opening — Phase 7 End-to-End Integration & Runtime Hardening.

## Scope
- real Oracle/CIF/PDL contract validation for Single Opening
- Batch runtime-reference hardening
- database-level idempotency/concurrency guards
- runtime readiness metadata checks
- isolated non-destructive rollback probe
- Step 5 runtime preflight and dedicated readiness UI
- no Deposit Account Servicing scope added

## Static / regression qualification
- Phase 7 verifier: **22/22 PASS**
- Complete repository verifier suite: **38/38 PASS**
- Historical Deposit Opening Phase 1–6 guards: PASS
- Four Deposits form/wizard guard: PASS
- Release/version/runtime-artifact guards: PASS

## Runtime qualification model
Target Oracle must execute:
`database/oracle/dps2/migrations/0.7.0-phase7-opening-e2e-hardening.sql`

Then verify:
- `GET /api/v1/deposit-opening/readiness`
- `POST /api/v1/deposit-opening/readiness/rollback-probe`
- `/four-deposits/runtime-readiness`

Expected minimum readiness after successful migration is `READY_WITH_WARNINGS`; Account Service and Tax Profile/Withholding remain explicit external contracts and may legitimately remain WARN until connected.

## Build status in packaging environment
A full Maven/Angular production build could not be completed in the packaging environment because Maven Wrapper dependency download from Maven Central is unavailable there. No Java compiler error was observed because Maven did not start. The target Windows environment previously built 0.6.0 successfully with Java 21, Angular production build and 51/51 backend tests. Version 0.7.0 must therefore still pass `build-production.cmd` on the target Windows workstation before its build gate is closed.

## Release packaging results
- Patch manifest: **15 added / 20 changed / 0 removed**
- Patch overlay against exact 0.6.0 baseline: **PASS**
- Exact overlay tree comparison: **MISSING=0 / EXTRA=0 / DIFFERENT=0**
- Full ZIP integrity: **PASS**
- Patch ZIP integrity: **PASS**
- Complete verifier suite on patched overlay: **38/38 PASS**
- Complete verifier suite on extracted Full ZIP: **38/38 PASS**

Runtime qualification is not claimed until the target Oracle migration, Readiness and Rollback Probe are executed successfully. Build qualification is not closed until `build-production.cmd` succeeds on the target Windows workstation.
