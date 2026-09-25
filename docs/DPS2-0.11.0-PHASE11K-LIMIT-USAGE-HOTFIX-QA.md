# DPS2 0.11.0 - Phase 11K Limit Usage Hotfix QA

## Trigger

Windows qualification passed Phase 11K Static (88/88), Oracle DB (34/34), backend/frontend production build and reached Runtime E2E, then failed with `LIMIT_USAGE_POST_MISMATCH`.

## Root cause addressed

1. Omitted `EFFECTIVE_FROM` / `VALID_FROM` was materialized from JVM UTC time into Oracle `TIMESTAMP` (no timezone). With a remote Oracle clock/timezone this can make a freshly-created control appear future-dated to Oracle-side `SYSTIMESTAMP` predicates.
2. Limit usage used `DEPOSIT_TRANSACTION.UPDATED_AT/CREATED_AT` as posting-time evidence. That is record-maintenance time, not the canonical POSTED transition time.

## Fix

- Default limit/restriction start time is now materialized by Oracle using `SYSTIMESTAMP` when omitted by the caller.
- Usage recomputation is grounded in `DEPOSIT_TRANSACTION_STATUS_HISTORY` rows where `TO_STATUS_CODE='POSTED'` and uses `EFFECTIVE_AT` as the posting boundary.
- `LAST_TRANSACTION_ID` is chosen from the latest POSTED event in the computed set.
- Runtime failure diagnostics now print the actual limit usage payload.
- No DDL change and no business-data migration is introduced. Existing Phase 11K Oracle 34/34 evidence remains applicable.

## Local gates

- Phase 11K Static: 92/92 PASS.
- Phase 11J regression: 78/78 PASS.
- Phase 11I regression: 64/64 PASS.
- Node path portability: PASS.

## Windows requalification

Run `tools\qualify-dps2-phase11k-limit-usage-hotfix.cmd`. It performs Static -> one Production Build -> Restart/health -> full Phase 11K Runtime E2E.
