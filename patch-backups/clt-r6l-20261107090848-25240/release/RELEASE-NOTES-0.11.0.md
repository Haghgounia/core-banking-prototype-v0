# Release Notes — 0.11.0 (Development Baseline)

## Phase 11A — Account Operations Schema Reconciliation

- Started the post-0.10.0 Account Operations implementation line.
- Preserved 0.10.0 Opening/Activation runtime baseline unchanged.
- Reconciled the legacy three-state `DEPOSIT_ACCOUNT` status guard with the canonical five-state contract.
- Expanded lifecycle event type contract to support `SUSPEND`, `REACTIVATE`, `MARK_DORMANT` and `REOPEN` in addition to existing events.
- Corrected `DEPOSIT_ACTIVATION_RUN.RUN_STATUS_CODE` default to `STARTED`.
- Corrected `DEPOSIT_ACCOUNT_EXT_REGISTRY.REGISTRATION_STATUS_CODE` default to `NOT_SENT`.
- Added a static Phase 11A verifier and a read-only Oracle DB verifier.
- Added Phase 11A verification to Windows/Unix production build gates and source packaging.

No Account Operations business API is enabled by 11A; those behaviors begin in Phase 11B.

## Phase 11B — Account Servicing Core + Account Party
- Promotes Party relationships from opening-only data to persistent account-level servicing data.
- Adds account basic-info, account-party and account-contact APIs.
- Adds Servicing History read/write integration.
- Updates Account 360 UI for Party, Contact and servicing changes.
- Keeps Hold/Lifecycle state transitions out of scope until Phase 11C.

## Phase 11C — Lifecycle + Hold/Block
Account Operations now supports controlled suspension, dormancy and reactivation plus persistent Hold/Block controls with idempotency and history. Balance/Subledger effects are intentionally deferred to Phase 11D; Controlled Closure/Reopening remains Phase 11E.

### Phase 11C Angular compile hotfix
- Fixed Deposit Opening funding-account ownership verification to use active Account Party rows (`details.parties`) instead of the removed legacy `details.owners` contract.
- Added fail-fast detection for a locked runtime JAR on Windows production builds.
- No database change is required.
