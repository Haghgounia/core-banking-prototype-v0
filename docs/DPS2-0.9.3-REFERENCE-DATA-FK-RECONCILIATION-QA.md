# DPS2 0.9.3 — Reference Data & FK Reconciliation QA

## Scope

This maintenance release reconciles Deposit Account Opening reference catalogs and the affected semantic foreign-key relationships against the supplied XMI, current Oracle DDL export, Phase 5/6 migrations and application behavior.

## Database observations used

The target database contains all 66 `REF_DEP_OPEN_*` tables. The diagnostic workbook showed the following row counts:

- `REF_DEP_OPEN_SNAPSHOT_TYPE` = 0
- `REF_DEP_OPEN_AUDIT_ACTOR_TYPE` = 0
- `REF_DEP_OPEN_AUDIT_EVENT_TYPE` = 0
- `REF_DEP_OPEN_CHANGE_REASON` = 0
- `REF_DEP_OPEN_BATCH_ERROR_CODE` = 3
- `REF_DEP_OPEN_CHANNEL_ORG_MAP` = 0

The three operational tables exist, but the expected semantic FKs for Request Type/Status, Decision/Decision Reason and Acceptance Source/Status were absent.

## Reconciled reference values

### Phase 5 source-defined catalogs

`REF_DEP_OPEN_AUDIT_ACTOR_TYPE`:
- USER
- SYSTEM

`REF_DEP_OPEN_AUDIT_EVENT_TYPE`:
- CREATE
- CHANGE_REQUESTED
- CHANGE_APPROVED
- CHANGE_REJECTED
- CHANGE_APPLIED
- ACCOUNT_LINKED
- STATUS_CHANGE

`REF_DEP_OPEN_SNAPSHOT_TYPE`:
- SUBMITTED
- APPROVED
- COMPLETED
- PRE_CHANGE
- POST_CHANGE

`REF_DEP_OPEN_CHANGE_REASON`:
- OTHER

Only `OTHER` is seeded for Change Reason because the XMI/application explicitly require a note for `OTHER`; a broader bank-specific taxonomy is not invented.

### Phase 6 source-defined provisional codes

`REF_DEP_OPEN_BATCH_ERROR_CODE` is reconciled to:
- ROW_INVALID
- PROCESSING_FAILED
- ACTIVATION_FAILED

The supporting stages `VALIDATION`, `PROCESSING`, `POSTING` are also reconciled.

### Environment-governed catalog

`REF_DEP_OPEN_CHANNEL_ORG_MAP` intentionally remains unseeded. `DEFAULT_ORG_UNIT_CODE` must use an approved bank organizational-unit code; a portable release must not invent it.

## Correct FK contract

0.9.3 installs the six semantic relations:

1. `DEPOSIT_OPENING_REQUEST.REQUEST_TYPE_CODE` -> `REF_DEP_OPEN_REQUEST_TYPE.REQUEST_TYPE_CODE`
2. `DEPOSIT_OPENING_REQUEST.REQUEST_STATUS_CODE` -> `REF_DEP_OPEN_REQUEST_STATUS.REQUEST_STATUS_CODE`
3. `DEPOSIT_OPENING_DECISION.DECISION_CODE` -> `REF_DEP_OPEN_DECISION.DECISION_CODE`
4. `DEPOSIT_OPENING_DECISION.DECISION_REASON_CODE` -> `REF_DEP_OPEN_DECISION_REASON.DECISION_REASON_CODE`
5. `DEPOSIT_OPENING_TERMS_ACCEPTANCE.ACCEPTANCE_SOURCE_CODE` -> `REF_DEP_OPEN_ACCEPTANCE_SOURCE.ACCEPTANCE_SOURCE_CODE`
6. `DEPOSIT_OPENING_TERMS_ACCEPTANCE.ACCEPTANCE_STATUS_CODE` -> `REF_DEP_OPEN_ACCEPTANCE_STATUS.ACCEPTANCE_STATUS_CODE`

If any of the three known incorrect XMI mappings is present in another environment, the migration removes only that exact incorrect relationship before installing the correct relations.

## Safety behavior

- All Oracle objects are explicitly `DPS2.` qualified.
- The master seed uses `ALL_TABLES` with `OWNER='DPS2'`; it does not depend on `USER_TABLES` or `CURRENT_SCHEMA`.
- Migration is safe to rerun.
- FK creation checks for orphan source values first and aborts with a clear error instead of enabling an invalid relationship.
- No operational/business row is altered by this migration.
- Historical migrations 0.5.0 and 0.6.0 remain unchanged.

## Static verification

Dedicated verifier:

`tools/verify-dps2-reference-fk-reconciliation-093.mjs`

It validates version sync, schema-qualified seed behavior, Phase 5/6 source-code alignment, `OTHER` semantics, environment-specific Channel/Org handling, six correct FKs, wrong-FK guards, orphan detection, build integration and source-layout hygiene.

## Runtime qualification

Runtime qualification requires execution of:

`database/oracle/dps2/migrations/0.9.3-reference-data-fk-reconciliation.sql`

Expected final reference counts include at least:

- Snapshot Type: 5
- Audit Actor Type: 2
- Audit Event Type: 7
- Change Reason: 1
- Batch Error Code: 3

`REF_DEP_OPEN_CHANNEL_ORG_MAP` may legitimately remain 0 until organizational mapping is approved.
