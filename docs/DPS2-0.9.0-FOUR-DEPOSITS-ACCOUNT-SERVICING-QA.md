# DPS2 0.9.0 — Four Deposits / Account Servicing Phase 9 QA

## Scope
Phase 9 starts the **mutating Deposit Account Operations / Servicing** bounded context after Opening is complete.
The implementation is intentionally constrained by the physical model already present in DPS2.

The current `DEPOSIT_ACCOUNT` status constraint supports only:
- `PENDING_ACTIVATION`
- `ACTIVE`
- `CLOSED`

Therefore Phase 9 implements only the model-supported transition:

`ACTIVE -> CLOSED`

It does **not** invent Hold/Blocked/Dormant/Reactivated states or tables.

## Backend contract
- `POST /api/v1/deposit-accounts/{accountId}/close`
- Request body: `expectedRecordVersion`
- `X-User-Id` is captured as actor.
- `X-Correlation-Id` is captured/generated for lifecycle traceability.
- Row is locked with `FOR UPDATE`.
- `RECORD_VERSION` is checked before mutation.
- Replaying Close for an already `CLOSED` account is idempotent and does not create another lifecycle event.
- Accounts not in `ACTIVE` cannot be closed.

## Oracle migration
`database/oracle/dps2/migrations/0.9.0-phase9-account-closure-servicing.sql`

The migration:
1. extends `CK_DEP_ACCT_EVT_TYPE` from `CREATE/ACTIVATE` to `CREATE/ACTIVATE/CLOSE`;
2. adds `TRG_DEP_ACCT_EVT_APPEND_ONLY` to reject UPDATE/DELETE on lifecycle history;
3. creates no new business table and adds no new `DEPOSIT_ACCOUNT` column.

## UI
The Account Operations workspace now includes a Servicing card:
- only ACTIVE accounts expose the Close command;
- CLOSED accounts show terminal/idempotent state;
- PENDING_ACTIVATION remains outside Servicing and is handled by Opening activation;
- the physical-model gap for Hold/Dormancy/Reactivation is displayed explicitly.

## Verification
Run:

```bat
node tools\verify-dps2-deposit-account-servicing-phase9.mjs
```

Expected:

`DPS2 Deposit Account Servicing Phase 9 verification OK: 18/18 controlled Account Closure checks passed.`

## Runtime acceptance
After applying the migration and building the application:
1. select an ACTIVE account;
2. close it from `/four-deposits/account-operations`;
3. verify account status becomes `CLOSED` and `RECORD_VERSION` increments;
4. verify exactly one `CLOSE` event exists in `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT`;
5. replay Close and verify no duplicate event is created;
6. attempt stale version mutation and verify the API rejects it.

## Local QA result
- Phase 9 verifier: 18/18 PASS.
- Phase 8 regression verifier: 18/18 PASS.
- All production static gates pass before Maven bootstrap.
- Local compile is blocked only because Maven 3.9.16 cannot be downloaded in this environment; Angular dependencies are not cached locally.
