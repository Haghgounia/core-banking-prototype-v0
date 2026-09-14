# Core Banking Prototype 0.5.0 — DPS2 Four Deposits Phase 5

## Scope

Phase 5 implements **Opening Audit & Change Management** on top of the Phase 4 Deposit Account lifecycle.

### Delivered

- Domain-managed Change Set lifecycle: `DRAFT -> APPROVED -> APPLIED` or `REJECTED`.
- Base Opening `RECORD_VERSION` validation to reject stale Change Sets.
- Aggregate-aware controlled mutations across the Deposit Opening aggregate, not only the root request.
- Server-side entity whitelist and same-Opening child ownership validation.
- Oracle `ALL_TAB_COLUMNS` validation for field existence, datatype and nullable contract.
- Protection of PKs, aggregate ownership keys, row-version and audit metadata columns.
- Canonical `PRE_CHANGE` and `POST_CHANGE` JSON snapshots with SHA-256 hashes.
- One immutable `CHANGE_APPLIED` Audit Event per changed entity under the same Change Set.
- Field-level Old/New evidence plus SHA-256 hashes.
- Configurable sensitive-field masking through `DEPOSIT_OPENING_AUDIT_SENSITIVE_FIELDS`.
- Opening CREATE, Account Link and Opening Completion integrated into the Audit Trail.
- Append-only Oracle triggers for Audit Event, Audit Field Change, Snapshot and Status History.
- Dedicated Angular route `/four-deposits/change-management`.
- Existing five control/audit tables remain outside Generic CRUD.
- Material Opening changes are rejected after `COMPLETED`; later account servicing remains in Deposit Account Operations.

## API

- `POST /api/v1/deposit-opening/requests/{id}/change-sets`
- `POST /api/v1/deposit-opening/requests/{id}/change-sets/{changeSetId}/approve`
- `POST /api/v1/deposit-opening/requests/{id}/change-sets/{changeSetId}/reject`
- `POST /api/v1/deposit-opening/requests/{id}/change-sets/{changeSetId}/apply`
- `GET /api/v1/deposit-opening/requests/{id}/audit-trail`
- `GET /api/v1/deposit-opening/requests/{id}/snapshots/{snapshotId}`

## Database

Migration:

`database/oracle/dps2/migrations/0.5.0-phase5-opening-audit-change-management.sql`

The supplied DPS2 schema already contains the five Phase 5 business/control tables. The migration seeds lifecycle/reference codes and adds append-only guards; it does not duplicate the supplied schema objects.

## Release status

Source/regression QA is complete. The clean 0.4.0 -> 0.5.0 patch overlay is byte-identical to the target source and passes all 34 regression verifiers. Production Maven/Angular builds are **not verified in this environment** because Maven/NPM dependencies are not cached and external registries are unavailable. Oracle runtime integration is also not executed here. Therefore packages are marked **QA Candidate**, not Production Release.
