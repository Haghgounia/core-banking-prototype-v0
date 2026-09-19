# QA Report — 0.10.0

## Oracle runtime evidence
Target Oracle has successfully passed:
- 0.9.3 Reference Data / corrected FK reconciliation.
- Phase 10A Operational Opening v5 structural foundation.
- Phase 10A semantic constraint/index repair; all expected semantic FKs are ENABLED.
- Phase 10B runtime alignment; `DPS2.DEPOSIT_OPENING_FUNDING.ATTEMPT_AT` is nullable and Funding Plan can exist before a real settlement attempt.

## Static source gates
- Phase 10A verifier: 34/34 PASS.
- Phase 10B backend/workflow verifier: 31/31 PASS.
- Phase 10D UI/integration verifier: 25/25 PASS.
- All existing project Node/static guards passed before the Maven step.
- CIF Party search is wired into Opening instead of requiring only blind Party-ID entry.
- Account-based Funding loads ACTIVE deposit accounts and verifies Party ownership before accepting the source account.
- Withdrawal Media is Product-driven and is separated from Payment Instrument issuance.
- Cheque withdrawal requests require requested quantity.
- Opening Obligation summary is shown explicitly; no fabricated Fee/Tax obligations are created.
- Required Create-Gate Compliance/Inquiry checks require explicit evidence references and are no longer auto-PASSed by the UI.
- Create → Settlement → Readiness → Activate ordering remains enforced by Backend and UI.
- Operational v5 Batch remains restricted to the approved legal basis/family and direct group processing/activation is disabled.

## Build qualification
`build-production.sh` passed every static/release/runtime-artifact gate, including Phase 10A 34/34, Phase 10B 31/31 and Phase 10D 25/25. It then stopped at the Java compile step because Maven Wrapper 3.9.16 could not be downloaded from Maven Central in the isolated packaging environment. Frontend `node_modules` are not present in this environment, so a real Angular compile was not performed here. The Windows production build remains the compile/runtime qualification gate.


## Phase 10E / 10F
- Phase 10E Batch hardening implemented: controlled per-item handoff to the normal Opening Wizard, transactional `BATCH_ITEM_ID` linkage, row progress refresh, no direct bulk account creation/activation shortcut.
- Phase 10F runtime E2E harness added for all four deposit families. The harness requires real environment evidence and refuses placeholder values.
- Static Phase 10E/10F verifier: 30/30 PASS.

## Release-candidate regression cleanup
- Legacy Phase 6 compatibility guard now branches by version: pre-0.10.0 retains the historic direct-Batch expectations, while 0.10.0 verifies Validate + Refresh + per-item Opening handoff and the absence of direct group Process/Activate shortcuts.
- Phase 10D Batch UI guard now verifies the actual Operational v5 blocked-direct-processing notice rather than the obsolete `پردازش پس از CDD فردی` action label.
- `verify-release-layout.mjs` now rejects unexpected root files. Accidental zero-byte command artifacts were removed and Phase 10D patch notes were moved under `docs/patches`.
- `build-production.cmd`, `build-production.sh` and `package-release.cmd` now execute the Phase 10E/10F 30-check gate before compile/package progression.
- In the isolated packaging environment, the complete static suite reaches the Maven bootstrap boundary; no source/static verifier failure remains. Final Java/Angular build and Phase 10F business qualification remain Windows/Oracle runtime gates.

