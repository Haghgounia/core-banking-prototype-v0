# DPS2 0.11.0 - Phase 11M Reconciliation Run Read-after-create Hotfix QA

## Defect

Wave D qualification reached runtime after Static 90/90, Oracle DB 40/40 and a successful production build, but failed with `RECON_RUN_MISSING` immediately after `POST /api/v1/deposit-accounts/{accountId}/reconciliation-runs`.

## Root cause

`DEPOSIT_RECONCILIATION_RUN` is a run header and does not carry `ACCOUNT_ID`. The account-scoped read model originally returned run headers only by joining `DEPOSIT_RECONCILIATION_ITEM`. A newly-created run has no item yet, so the command response inserted the row successfully but omitted it from `reconciliation.runs`.

## Fix

The account-scoped run reader now recognizes ownership from either:

1. an existing reconciliation item for the account, or
2. the completed shared idempotency trace created by the start-run command: `ACCOUNT_ID`, `OPERATION_TYPE_CODE='RECONCILIATION_RUN'`, and `RESULT_REFERENCE='RECONRUN:<runId>'`.

This preserves the canonical schema, avoids placeholder items, supports read-after-create, and also makes idempotent replay return the same run before the first item exists.

## Qualification

Oracle DB 40/40 evidence from the immediately preceding 11M qualification is retained because this hotfix changes no DDL or migration. The targeted qualifier runs Static -> Production Build -> Restart -> full 11M Runtime E2E.
