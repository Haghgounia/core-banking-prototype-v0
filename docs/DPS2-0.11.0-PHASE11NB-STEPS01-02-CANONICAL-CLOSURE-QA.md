# DPS2 0.11.0 — Phase 11N-B QA

## Scope

Canonical Coverage Closure for Operational Steps 01–02 only.

Source authority:
1. `docs/dps2/reference/Deposit_Account_Operations_6_RC_2026-09-22.xml`
2. `docs/dps2/reference/Deposit_Account_Operations_Traceability_Guide_FA_v2_2026-09-22.html`
3. `docs/dps2/reference/Deposit_Account_Operations_Operational_v11_RC_2026-09-22.html`
4. `docs/DPS2-CANONICAL-ROADMAP-FA.md`

## Canonical gaps closed by source

### Step 01 — Account servicing

- `OPENING_ORG_UNIT_CODE` is immutable Opening evidence.
- `ORG_UNIT_CODE` is the mandatory current servicing/owning organization unit.
- Migration reconciles evolved rows without losing the pre-11N mutable servicing value.
- New account creation initializes both values from `DEPOSIT_OPENING_REQUEST.ORG_UNIT_CODE`.
- Basic servicing changes only `ORG_UNIT_CODE`; `OPENING_ORG_UNIT_CODE` is read-only.
- Product Version change must execute a real same-Product positive branch with Maker/Checker approval.
- Condition Override and Signatory Authority remain traceable to approval / active Party and Signatory.

### Step 02 — Lifecycle / Hold / Bulk

- Activation closure requires real source readiness, canonical Activation Run/Checks, then `READY -> ACTIVE` execution.
- Lifecycle and Bulk retain idempotency, audit and per-item transaction isolation.
- Generic CLOSE is not reintroduced; Step 08 owns account closure.
- Collateral Hold contract is `ORIGIN_SYSTEM_CODE=COLLATERAL`, `HOLD_REASON_CODE=COLLATERAL_PLEDGE`, `ORIGIN_EXECUTION_MODE_CODE=SERVICE`, `RELEASE_POLICY_CODE=ORIGIN_ONLY`.
- Collateral create/release replay is resolved by the business origin tuple as well as transport Idempotency-Key, preventing duplicate requests when a caller retries with a different transport key.

## Migration policy

`database/oracle/dps2/migrations/0.11.0-phase11nb-steps01-02-canonical-closure.sql`

This migration is a canonical schema/data reconciliation, not a business seed:

1. add `DEPOSIT_ACCOUNT.ORG_UNIT_CODE VARCHAR2(30 CHAR)` if missing;
2. preserve the pre-11N mutable unit into current `ORG_UNIT_CODE`;
3. restore immutable `OPENING_ORG_UNIT_CODE` from the Opening source;
4. fail if a current org unit cannot be sourced;
5. enforce `ORG_UNIT_CODE NOT NULL`.

No Product Version, Condition, Signatory, Activation, Hold, Bulk or Collateral business record is fabricated by migration.

## Oracle declaration-order hotfix

The first 11N-B qualification attempt reached the Oracle migration after `PHASE11NB_STATIC_BASELINE_PASS` and failed during anonymous-block compilation with `PLS-00103` at `v_missing`. The cause was declaration order: `v_missing` appeared after the local `col_exists` function. Oracle PL/SQL requires ordinary variable declarations before local subprogram declarations in this block structure.

The hotfix moves `v_missing NUMBER;` before `FUNCTION col_exists` and adds a static regression guard. The failed attempt executed no migration body statement because the anonymous block did not compile, so rerunning the normal 11N-B qualifier is safe.


## Oracle new-column name-resolution hotfix

The second 11N-B qualification attempt passed the updated static gate (`64/64`) and reached the same Oracle migration, but the anonymous block still failed to compile with `ORA-00904: A.ORG_UNIT_CODE invalid identifier`.

Root cause: `ORG_UNIT_CODE` is introduced by dynamic DDL inside the anonymous block. Oracle compiles all static SQL in the block before executing that DDL, so later static `UPDATE` / `SELECT` statements cannot resolve the new column during block compilation even though the `ALTER TABLE` appears earlier at runtime.

The hotfix therefore:

1. keeps the `ALTER TABLE ... ADD ORG_UNIT_CODE` dynamic;
2. changes the current-org backfill statement to dynamic SQL;
3. changes the null-count verification on `ORG_UNIT_CODE` to dynamic SQL;
4. leaves the immutable Opening snapshot reconciliation static because it references only pre-existing columns;
5. adds a static regression guard proving the newly-added column is not referenced by compile-time static SQL in this block.

This failure also occurred before the block body executed, so the rerun is safe. The normal qualification command remains unchanged.

## Oracle NOT NULL rerun hotfix

A later 11N-B qualification successfully added/backfilled `ORG_UNIT_CODE`, enforced `NOT NULL`, passed the DB verifier, and reached the production build. The subsequent qualification rerun then failed in the migration with `ORA-01442: column to be modified to NOT NULL is already NOT NULL`.

Root cause: the migration always executed `ALTER TABLE ... MODIFY (... NOT NULL)` even when `ALL_TAB_COLUMNS.NULLABLE='N'`. This made an otherwise-correct reconciliation non-rerunnable after its first successful application.

The hotfix reads `ALL_TAB_COLUMNS.NULLABLE` after the null-value guard and only issues the `MODIFY ... NOT NULL` DDL when the column is still nullable. If it is already mandatory, the migration emits `KEEP | ... already NOT NULL` and continues. A static regression guard now requires this dictionary check. No business data rollback is needed because the prior schema reconciliation and commit were valid.

## Qualification order

```text
Static
  -> Oracle migration
  -> Oracle DB verifier
  -> stop runtime
  -> production build + tests
  -> start runtime
  -> Runtime E2E
```

Run on Windows:

```bat
cd /d D:\Projects\core-banking-prototype-v0
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
set "CORE_BANKING_ORACLE_CONNECT=<real Oracle connect string>"
tools\qualify-dps2-phase11nb.cmd
```

## Required Runtime evidence

The Runtime E2E must emit all of the following through real operations:

```text
PHASE11NB_RUNTIME_READY_ACTIVE=<activationRunId>:<accountId>
PHASE11NB_RUNTIME_ORG_UNIT=<openingSnapshot>-><temporaryCurrentOrg>
PHASE11NB_RUNTIME_CONDITION=<conditionId>
PHASE11NB_RUNTIME_SIGNATORY=<authorityId>
PHASE11NB_RUNTIME_PRODUCT_CHANGE=<historyId>:<fromVersion>-><toVersion>
PHASE11NB_RUNTIME_LIFECYCLE_BULK=<suspendBulkId>,<reactivateBulkId>
PHASE11NB_RUNTIME_COLLATERAL=<holdId>
PHASE11NB_RUNTIME_ACCOUNT=<accountId>
PHASE11NB_RUNTIME_ACTIVATED_ACCOUNT=<accountId>
PHASE11NB_RUNTIME_E2E_PASS
```

### Governed Product Version precondition

Unlike 11J, Phase 11N-B does **not** accept `SKIP_NO_SAME_PRODUCT_CANDIDATE`. At least one ACTIVE account must have a governed alternate Product Version belonging to the same Product. If none exists, Runtime stops with:

```text
PHASE11NB_NO_SAME_PRODUCT_VERSION_CANDIDATE
```

This is a controlled configuration blocker, not permission to fabricate Product configuration in DPS2 Runtime.

## Closure markers

Expected final markers:

```text
PHASE11NB_STATIC_BASELINE_PASS
PHASE11NB_SCHEMA_RECONCILIATION_PASS
PHASE11NB_DB_BASELINE_PASS
PHASE11NB_IMPLEMENTATION_PASS
PHASE11NB_RUNTIME_E2E_PASS
DPS2_PHASE11NB_STEPS01_02_QUALIFICATION_PASS
```

Only after all markers PASS may the canonical roadmap change Step 01 and Step 02 to `DONE` and Phase 11N-B to `CLOSED`.

## Phase 11J historical verifier compatibility hotfix

After the NOT NULL rerun fix, the 11N-B Oracle gate completed successfully, but the production build stopped in the historical Phase 11J static verifier at `activation execution delegates established lifecycle activation`.

Root cause: the 11J verifier encoded the legacy two-step activation pattern (`lifecycleService.activateAccount` followed by `linkLatestActivationEvent`). Phase 11N-B intentionally replaced that with the canonical atomic overload `lifecycleService.activateAccount(account.openingRequestId(), activationRunId, actor, correlation)` so the READY Activation Run is written with the ACTIVATE lifecycle event in the same transaction.

The compatibility hotfix changes only the historical verifier: it accepts either the original 11J delegation pattern or the newer atomic 11N-B delegation. It does not relax the business rule, because the 11N-B verifier separately requires the activation-run argument, atomic lifecycle event persistence, and status-history trace. No Oracle change or business-data rollback is required.

## R7 — Qualification Product Version Preparation

آخرین qualification پس از عبور Static/Oracle/Build و اجرای واقعی `READY -> ACTIVE` با marker زیر متوقف شد:

```text
PHASE11NB_RUNTIME_READY_ACTIVE=6:13
PHASE11NB_NO_SAME_PRODUCT_VERSION_CANDIDATE
```

این failure یک Gap دادهٔ runtime بود، نه failure منطق Activation. برای بستن Positive Product Version branch بدون ساخت config در Product واقعی، R7 ابزار `tools/prepare-dps2-phase11nb-product-version.mjs` را اضافه می‌کند.

قواعد R7:

- فقط Productهایی که `PRODUCT_CODE` آنها با `P10F_` شروع می‌شود قابل seed هستند؛ روی Product واقعی fail-fast می‌شود.
- نسخه جدید از همان Product و با `SOURCE_VERSION_ID` نسخه fixture ساخته می‌شود.
- `IS_CURRENT=0` است؛ بنابراین current Product را جابه‌جا نمی‌کند.
- `ORIGINATION_STATUS_CODE=CLOSED` است؛ نسخه qualification برای افتتاح جدید استفاده نمی‌شود.
- `SERVICING_STATUS_CODE=ACTIVE` است تا migration حساب موجود قابل آزمون باشد.
- نوشتن فقط از Product Builder REST API انجام می‌شود؛ Direct DML روی PDL وجود ندارد.
- Runtime ابتدا همان حسابی را که در 11N-B از Opening به ACTIVE رسانده برای Product Change انتخاب می‌کند؛ fallback فقط برای compatibility باقی می‌ماند.

Qualification order از 6 gate به 7 step تبدیل شده و Product Version preparation بین Runtime start و Runtime E2E اجرا می‌شود.


## Final Qualification Closure — 2026-09-24

Phase 11N-B completed the exact closure order and is now `CLOSED`. Document Steps 01 and 02 are `DONE`.

Verified evidence:

```text
PHASE11NB_STATIC_VERIFIER_PASS=77
PHASE11NB_STATIC_VERIFIER_FAIL=0
PHASE11NB_STATIC_BASELINE_PASS
PHASE11NB_SCHEMA_RECONCILIATION_PASS
PHASE11NB_DB_VERIFIER_PASS=20
PHASE11NB_DB_VERIFIER_FAIL=0
PHASE11NB_DB_BASELINE_PASS
PHASE11NB_IMPLEMENTATION_PASS
PHASE11NB_QUALIFICATION_PRODUCT_VERSION_CREATED=1:1->5
PHASE11NB_RUNTIME_READY_ACTIVE=7:14
PHASE11NB_RUNTIME_ORG_UNIT=001->11NB-QA
PHASE11NB_RUNTIME_CONDITION=4
PHASE11NB_RUNTIME_SIGNATORY=4
PHASE11NB_RUNTIME_PRODUCT_CHANGE=3:1->5
PHASE11NB_RUNTIME_LIFECYCLE_BULK=5,6
PHASE11NB_RUNTIME_COLLATERAL=4
PHASE11NB_RUNTIME_ACCOUNT=4
PHASE11NB_RUNTIME_ACTIVATED_ACCOUNT=14
PHASE11NB_RUNTIME_E2E_PASS
DPS2_PHASE11NB_STEPS01_02_QUALIFICATION_PASS
```

The next canonical phase is **11N-C — Steps 03–04 Canonical Closure**. Work begins with source coverage audit only; implementation is permitted only for genuine XML/Trace/Operational gaps.
