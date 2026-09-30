# DPS2 0.11.0 — Phase 10D Final Opening UI Verifier Alignment

Date: 2026-09-30
Scope: static verifier alignment only. No DDL, API, repository, business-rule or runtime behavior change.

## Problem
The final Deposit Opening UI had already moved to the reviewed/reference HTML contract, but the historical Phase 10D verifier still searched for old display strings such as:
- `جستجو و انتخاب Party`
- `حساب مبدأ ACTIVE`
- historical Withdrawal Media / Payment Instrument wording
- old Create-Gate evidence heading
- old lifecycle sequence sentence

This stopped `build-production.cmd` before Angular/Maven packaging, leaving the previous runtime JAR unavailable for replacement.

## Fix
`tools/verify-dps2-opening-operational-v5-phase10d.mjs` now verifies the same Phase 10D business guarantees against the final Opening implementation instead of historical labels.

The gate was not weakened:
- Party search requires the current search heading, search control/action and add-to-request action.
- Funding source requires the current source-account selector plus ACTIVE filtering in TypeScript.
- Withdrawal Media / Payment Instrument separation requires the final conceptual separation block.
- No-fabricated Fee/Tax obligation is checked directly against the `openingObligations` expression: only INITIAL_BALANCE is allowed there until real service contracts contribute obligations.
- Create-Gate evidence requires the final technical-evidence section and the core evidence controls.
- Tax verification requires read-only tax fields plus TAX_PROFILE validation evidence.
- Lifecycle order is verified by the actual Angular click-handler order: persist Opening -> create Account -> settlement -> readiness -> activation, plus PENDING_ACTIVATION/READY guards.

## Verification
- `node tools/verify-dps2-opening-operational-v5-phase10d.mjs` => 24/24 PASS.
- All static DPS2 verifiers that follow Phase 10D in `build-production.cmd`, from Phase 10E/10F through Phase 11N rapid completion, were executed successfully in the packaging environment.

A complete Angular/Maven production build still needs to be run on the Windows project environment.
