# QA Report 0.9.0

Release: **0.9.0**  
Scope: **Four Deposits — Phase 9 Account Servicing / Closure**

## Result

- Dedicated Phase 9 verifier: **18/18 PASS**.
- Historical Phase 8 verifier: **18/18 PASS** after forward-compatible evolution of the API contract.
- Production static regression gates: **PASS through the runtime-artifact guard**.
- Full-source package and `0.8.0 + Patch` overlay are source-identical to the QA worktree: **0 Missing / 0 Extra / 0 Different** excluding generated/build/runtime artifacts.
- ZIP integrity: PASS for Full and Patch archives.

## Delivered

- First post-opening mutating Servicing command.
- Controlled `ACTIVE -> CLOSED` transition.
- Oracle row locking with `FOR UPDATE`.
- Optimistic concurrency using `DEPOSIT_ACCOUNT.RECORD_VERSION`.
- Idempotent Close replay: an already CLOSED account does not receive a second lifecycle event.
- `CLOSE` lifecycle event with actor and correlation ID.
- Append-only trigger protecting lifecycle history against UPDATE/DELETE.
- Account Operations UI action and lifecycle timeline update.
- Explicit model-gap policy: no Hold/Dormancy/Reactivation invented outside the current physical model.

## Database gate

Migration required:

`database/oracle/dps2/migrations/0.9.0-phase9-account-closure-servicing.sql`

No business table or new `DEPOSIT_ACCOUNT` column is introduced.

## Local build limitation

The production build reaches the Maven compile gate only after all static/regression guards pass. In this execution environment Maven Wrapper cannot download Apache Maven 3.9.16 from Maven Central, and Angular dependencies are not locally cached. Therefore Java/Angular compile, Oracle migration execution and runtime closure smoke tests remain qualification gates on the target Windows environment.

## Runtime acceptance

1. Apply the 0.9.0 migration.
2. Build and start version 0.9.0.
3. Select an ACTIVE account in `/four-deposits/account-operations`.
4. Close it and verify `ACCOUNT_STATUS_CODE=CLOSED`.
5. Verify `RECORD_VERSION` increments by one.
6. Verify exactly one `CLOSE` event exists.
7. Replay the command and verify no duplicate event is created.
8. Verify stale Record Version is rejected before mutation.
