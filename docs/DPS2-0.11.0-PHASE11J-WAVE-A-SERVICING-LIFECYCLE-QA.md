# DPS2 0.11.0 — Phase 11J — Wave A Servicing & Lifecycle Completion QA

**Status:** IN_PROGRESS  
**Date:** 2026-09-24  
**Canonical source:** `docs/DPS2-CANONICAL-ROADMAP-FA.md`

## Scope

Phase 11J is a consolidated implementation wave for the remaining document-traced gaps in Operational Steps 01 and 02. It does not redefine the source roadmap and it does not imply that Phase 11I is closed.

Trace:

`Step 01 + Step 02 -> Packages 01A/01B/01C/05/06/00/17 -> canonical DPS2 tables -> API/Service/UI -> Static/DB/Runtime gates`

Implemented in this wave:

- `DEPOSIT_ACCOUNT_ATTRIBUTE` upsert with idempotency.
- `DEPOSIT_ACCOUNT_CONDITION_OVERRIDE` with `DEPOSIT_OPERATION_APPROVAL_REQUEST` maker/checker trace.
- Same-product version change through `DEPOSIT_ACCOUNT_PRODUCT_HISTORY`; `OPENED_PRODUCT_VERSION_ID` is not overwritten.
- `PARTY_ID -> SIGNATORY_ID` resolution and `DEPOSIT_ACCOUNT_SIGNATORY_AUTHORITY` persistence.
- Controlled activation snapshot through `DEPOSIT_ACTIVATION_RUN` and `DEPOSIT_ACTIVATION_CHECK`, sourced from existing Opening Readiness evidence; the wave does not fabricate PASS evidence.
- Activation execution delegates the established account activation service after a READY canonical run.
- `DEPOSIT_ACCOUNT_BULK_ACTION` + `DEPOSIT_ACCOUNT_BULK_ACTION_ITEM` with per-item isolation for HOLD/RELEASE_HOLD/SUSPEND/REACTIVATE/MARK_DORMANT.
- Bulk hold/release delegates existing hold ownership/release-policy enforcement; release requires an explicit Hold ID per account.
- Angular controls for the above actions.

## Oracle baseline

The 2026-09-24 DPS2 export already contains all Step 01/02 canonical tables. The 11J migration therefore creates no business tables. It only validates required tables and reconciles sequence high-water marks above current PK maxima.

## Gates

1. `node tools/verify-dps2-wave-a-servicing-lifecycle-11j.mjs`
2. `tools\apply-dps2-phase11j.cmd`
3. `build-production.cmd`
4. `node tools\runtime-dps2-phase11j-wave-a-e2e.mjs`

Phase 11J can be marked CLOSED only after Static, Oracle DB and Runtime gates pass. A Runtime result of `SKIP_NO_PENDING_ACCOUNT` or `BLOCKED_BY_SOURCE_READINESS` proves canonical activation snapshot behavior but does not independently prove the READY -> ACTIVE execution branch; that branch must remain explicitly noted until a source-valid READY case is exercised.

## Local source evidence — 2026-09-24

- Static verifier: `PHASE11J_STATIC_VERIFIER_PASS=73`, `FAIL=0`.
- Local Java 21 contract checks passed for the new Wave A repository and service (`PHASE11J_REPOSITORY_JAVAC_PASS`, `PHASE11J_SERVICE_JAVAC_PASS`).
- TypeScript no-resolve parse check found only unavailable Angular/RxJS module imports because `frontend/node_modules` is not present in the container; no additional syntax/type diagnostics were found in the changed files.
- Regression source gates remained green: 11I `64/64`, 11H `64/64`, 11G `92/92`, 11F `64/64`, 11E `49/49`, 11D `56/56`.
- Full Maven/Angular production build is intentionally not claimed here: the container Maven wrapper cannot download Maven from Maven Central, and frontend dependencies are not installed. The authoritative production build gate must run on the Windows project environment.


## Consolidated runner return-code hardening — 2026-09-24

- Phase 11I Oracle requalification has independently passed `29/29`; the Wave A resume path therefore starts from Phase 11J rather than repeating 11I DB work.
- `apply-dps2-phase11i.cmd` and `apply-dps2-phase11j.cmd` now return explicit `exit /b 0` on success.
- `qualify-dps2-wave-a-11i11j.cmd` captures each child return code explicitly instead of inheriting a stale `ERRORLEVEL`.
- `tools/resume-dps2-wave-a-after-11i.cmd` performs: 11J Oracle -> stop -> one production build -> start -> 11I Runtime -> 11J Runtime.

## Node verifier path portability hotfix — 2026-09-24

- Windows production build exposed a portability guard failure because `verify-dps2-wave-a-servicing-lifecycle-11j.mjs` derived the repository root from `process.cwd()`.
- The verifier now derives the repository root from `import.meta.url` through `fileURLToPath()`, matching the project-wide verifier convention.
- Local evidence after the fix:
  - `Node verifier path-portability verification OK: 58 verifier scripts are cwd-independent and Windows-safe.`
  - `PHASE11J_STATIC_VERIFIER_PASS=73`
  - `PHASE11J_STATIC_VERIFIER_FAIL=0`
  - `PHASE11J_STATIC_BASELINE_PASS`
- This hotfix changes no Java, Angular, Oracle schema, migration, or business behavior. Existing 11I Oracle `29/29` evidence remains valid.


## Activation snapshot database hotfix — 2026-09-24

Runtime evidence passed Attribute, Condition, Signatory and Bulk but failed on `POST /activation-runs` with `DATABASE_ERROR`. Source/DDL audit found an unsafe mapping: `DEPOSIT_OPENING_CHECK.WAIVER_REASON` is free text up to 500 chars, while `DEPOSIT_ACTIVATION_CHECK.RESULT_REASON_CODE` is a machine-readable code limited to 60 chars. The snapshot now leaves `RESULT_REASON_CODE` null when no machine-readable source code exists, preserves readable evidence in bounded `DETAILS` (max 500), and prefers `SOURCE_EVALUATION_REFERENCE` over `RESULT_REFERENCE` for `SOURCE_REFERENCE`. No Oracle DDL change is required.


## Wave A runner start hotfix — 2026-09-24

The consolidated runner previously used backslash-escaped quotes inside Windows `cmd /c`, which did not reliably start the runtime although the built JAR started normally when invoked manually. Both Wave A runners now use `start ... /d "%ROOT%" cmd /c "call bin\start.cmd"` so the child command starts from the project root with native CMD quoting.


## Targeted activation requalification

Because Attribute, Condition, Signatory and Bulk runtime paths already passed before the activation snapshot fix, `tools\qualify-dps2-phase11j-activation-hotfix.cmd` rebuilds/restarts and runs only `runtime-dps2-phase11j-activation-only.mjs`. A pending account is mandatory; absence of one is a qualification failure rather than a skip.
