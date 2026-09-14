# DPS2 Four Deposits — Phase 5 — Opening Audit & Change Management — QA

Version: `0.5.0`

## Scope

Phase 5 converts the five controlled Opening tables from schema-only structures into a domain-managed workflow:

- `DEPOSIT_OPENING_CHANGE_SET`
- `DEPOSIT_OPENING_AUDIT_EVENT`
- `DEPOSIT_OPENING_AUDIT_FIELD_CHANGE`
- `DEPOSIT_OPENING_SNAPSHOT`
- `DEPOSIT_OPENING_STATUS_HISTORY`

These tables remain outside Generic CRUD. The implementation follows the supplied Deposit Account Opening EA/Oracle model: audit/status/snapshot records are append-only evidence, while Change Set is the controlled mutable header until it reaches `APPLIED` or `REJECTED`.

## Domain lifecycle

`DRAFT -> APPROVED -> APPLIED`

or

`DRAFT/APPROVED -> REJECTED`

Every Change Set stores `BASE_REQUEST_VERSION`. Approval and Apply fail if the Opening root has moved to a newer `RECORD_VERSION`.

## Aggregate-aware controlled change

Apply is not limited to `DEPOSIT_OPENING_REQUEST`. The service supports business-field corrections on the Opening aggregate whitelist, including Party, Signatory, Signatory Authority, Delegation, Beneficiary, Term, Maturity Instruction, Profit Instruction, Funding, Check, Document, Tax, Services, Payment Instrument, Pricing Override, Rewards, Terms Acceptance and Decision.

Controls:

1. Entity name must be in the server-side whitelist.
2. Child `entityId` must resolve to the same `OPENING_REQUEST_ID`.
3. Indirect children (`SIGNATORY_AUTHORITY`, `MATURITY_INSTRUCTION`) are validated through their parent Opening child.
4. Oracle `ALL_TAB_COLUMNS` metadata validates field existence, datatype and nullable contract.
5. PK, aggregate ownership keys, `RECORD_VERSION`, `CREATED_*` and `UPDATED_*` are protected.
6. Root technical/system-managed fields remain excluded; only the explicit root mutable field set is accepted.
7. Each changed entity receives its own `CHANGE_APPLIED` Audit Event under the same Change Set.
8. All effective changes share one Opening aggregate version increment.

## Audit evidence

For an Apply operation:

1. canonical `PRE_CHANGE` JSON snapshot is persisted;
2. entity rows are locked and updated with optimistic row-version guards;
3. Opening root version advances exactly once;
4. one Audit Event per changed entity is persisted;
5. every changed field records old/new canonical values plus SHA-256 hashes; fields classified through `DEPOSIT_OPENING_AUDIT_SENSITIVE_FIELDS` are stored as `[MASKED]` while hashes retain evidence without exposing the value;
6. canonical `POST_CHANGE` JSON snapshot is persisted;
7. Change Set is finalized as `APPLIED` with `RESULT_REQUEST_VERSION`.

Initial Opening creation is connected to a `CREATE` Audit Event and Status History. Phase 4 account linking and completion are also represented by `ACCOUNT_LINKED` and `STATUS_CHANGE` audit events. A `COMPLETED` Opening rejects material change through this bounded context; subsequent account changes belong to Deposit Account Operations.

## API contract

- `POST /api/v1/deposit-opening/requests/{id}/change-sets`
- `POST /api/v1/deposit-opening/requests/{id}/change-sets/{changeSetId}/approve`
- `POST /api/v1/deposit-opening/requests/{id}/change-sets/{changeSetId}/reject`
- `POST /api/v1/deposit-opening/requests/{id}/change-sets/{changeSetId}/apply`
- `GET /api/v1/deposit-opening/requests/{id}/audit-trail`
- `GET /api/v1/deposit-opening/requests/{id}/snapshots/{snapshotId}`

Apply mutation shape:

```json
{
  "changes": [
    {
      "entityName": "DEPOSIT_OPENING_PARTY",
      "entityId": 101,
      "fieldName": "OWNERSHIP_PERCENT",
      "newValue": "50"
    }
  ],
  "operationName": "APPLY_CHANGE_SET"
}
```

For the root request, `entityName` may be omitted and `entityId` defaults to the current `OPENING_REQUEST_ID` for backward compatibility with the Phase 5 root-only draft contract.

## Oracle migration

`database/oracle/dps2/migrations/0.5.0-phase5-opening-audit-change-management.sql`

The migration seeds the Phase 5 lifecycle/reference codes and creates append-only guards for Audit Event, Audit Field Change, Snapshot and Status History. It does not create duplicate business tables because the supplied DPS2 schema already contains them.

## Static QA gates

- source version alignment = `0.5.0`
- dedicated Domain Service / Repository / API contract present
- aggregate whitelist and ownership checks present
- Oracle metadata field validation present
- optimistic locking on Opening and target child rows present
- canonical PRE/POST snapshots present
- SHA-256 field evidence present
- append-only database triggers present
- dedicated Angular change-management route present
- audit/change tables not exposed as Generic CRUD
- completed Opening mutation guard present

Production Maven/Angular compilation and Oracle runtime integration remain environment-dependent release gates and must not be marked PASS unless executed successfully.
