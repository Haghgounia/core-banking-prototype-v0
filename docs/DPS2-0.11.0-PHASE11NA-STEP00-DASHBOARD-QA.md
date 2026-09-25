# DPS2 0.11.0 — Phase 11N-A — Step 00 Read-only Dashboard QA

## Canonical scope

Phase 11N-A is the first slice of **Phase 11N — Canonical Coverage Closure** and closes only Step 00 when all gates pass.

Canonical read sources:

- `DPS2.DEPOSIT_ACCOUNT`
- `DPS2.DEPOSIT_ACCOUNT_HOLD`
- `DPS2.DEPOSIT_TRANSACTION`
- `DPS2.DEPOSIT_ACCOUNT_BALANCE`

No Step 00 business table, mutation API, seed data, migration, approval, or idempotency record is introduced. Dashboard read actions are projections over already-owned domain state.

## Runtime contract

`GET /api/v1/deposit-accounts/dashboard`

The response contains:

1. account counts by canonical lifecycle status;
2. active Hold counts by Hold type;
3. transaction counts by operational status;
4. ledger/available/blocked/pending balance totals **grouped by currency**.

Currency balances are intentionally not summed across currencies.

## No-write guard

`DepositAccountDashboardRepository` is a dedicated SELECT-only repository. `DepositAccountDashboardService.get()` is marked `@Transactional(readOnly = true)`. The REST surface has only `GET /dashboard` and the Runtime E2E sends only GET requests.

## Qualification gates

```text
Static Gate
  -> Oracle DB read-model Gate
  -> Production Build
  -> Runtime restart / health
  -> Read-only Runtime E2E
```

Expected markers:

```text
PHASE11NA_STATIC_BASELINE_PASS
PHASE11NA_DB_BASELINE_PASS
PHASE11NA_IMPLEMENTATION_PASS
PHASE11NA_RUNTIME_E2E_PASS
DPS2_PHASE11NA_STEP00_DASHBOARD_QUALIFICATION_PASS
```

## Runtime E2E assertions

- Dashboard account total equals account search total.
- Active Hold count equals the aggregate of real account detail Hold records.
- Currency balance aggregates equal the aggregate of real account detail balances.
- Transaction total equals account transaction lists when none are truncated by the 100-row list cap.
- Two consecutive dashboard reads do not change account/Hold/transaction business counts.

## Closure rule

Only after all five gates pass may:

- technical Phase `11N-A` be marked `CLOSED`;
- Document Step `00` move from `PARTIAL` to `DONE`.
