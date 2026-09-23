# DPS2 0.11.0 — Phase 11C — Lifecycle + Hold/Block QA

## Scope
Phase 11C operationalizes the existing DPS2 lifecycle/hold physical model without redesigning the tables.

Implemented lifecycle transitions:
- `ACTIVE -> SUSPENDED` (`SUSPEND`)
- `ACTIVE -> DORMANT` (`MARK_DORMANT`)
- `SUSPENDED -> ACTIVE` (`REACTIVATE`)
- `DORMANT -> ACTIVE` (`REACTIVATE`)

Every transition uses row locking, `RECORD_VERSION`, `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT`, `DEPOSIT_ACCOUNT_STATUS_HISTORY`, servicing history and `DEPOSIT_OPERATION_IDEMPOTENCY`.

Implemented hold operations:
- Create `FULL`, `PARTIAL`, `DEBIT_ONLY`, `CREDIT_ONLY` hold.
- Full release of any active hold.
- Partial release of a `PARTIAL` hold.
- Hold history persistence.
- Origin/release-policy enforcement.
- Collateral rule: `ORIGIN_SYSTEM_CODE=COLLATERAL`, `HOLD_REASON_CODE=COLLATERAL_PLEDGE`, `ORIGIN_EXECUTION_MODE_CODE=SERVICE`, `RELEASE_POLICY_CODE=ORIGIN_ONLY`.

## Deliberate boundaries
- Hold does **not** change `DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE`.
- Phase 11C does not yet recalculate `AVAILABLE_BALANCE`; balance/subledger enforcement belongs to 11D.
- Transaction authorization against holds belongs to 11H.
- Direct Phase 9 close remains temporarily available and will be replaced by Controlled Closure in 11E.
- Reopening is 11E.

## Schema readiness DDL
`0.11.0-phase11c-lifecycle-hold-runtime-readiness.sql` creates only operational indexes and performs prerequisite checks. It contains no business DML.

## Phase 11B runtime correction included
The named SQL parameter typo `:expectedRecordVersionsion` in Basic Info update was corrected to `:version`. This is an application-source fix and needs no DB migration.

## Qualification markers
Expected:
- `PHASE11C_STATIC_BASELINE_PASS`
- `PHASE11C_SCHEMA_RUNTIME_READINESS_PASS`
- `PHASE11C_DB_BASELINE_PASS`
- `PHASE11C_IMPLEMENTATION_PASS`
