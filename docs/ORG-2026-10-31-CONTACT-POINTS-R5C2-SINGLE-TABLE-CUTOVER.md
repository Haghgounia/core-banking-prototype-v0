# ORG R5-C2 / R5-C3 - `CONTACT_POINTS` retirement (source + Oracle)

## Authoritative architecture decision

`ORG.ORGANIZATION_UNIT_CONTACT_POINTS` is the **active single-table system of record** for organization-unit contacts. `ORG.CONTACT_POINTS` is only a historical master and will be renamed to `ORG.DEPRECATED_CONTACT_POINTS` **after** the approved cutover. `ORG.CONTACT_POINTS_V2` is not modified or renamed in this release; its migration statistics become historical evidence, not a gate for new OUCP writes. The old R5-B V2 pilot is **not** the approved contact system of record.

### Live evidence supplied by DBA, 2026-10-31

- `CONTACT_POINTS=6427`; `ORGANIZATION_UNIT_CONTACT_POINTS=6930`.
- All 6,930 unit associations have complete contact snapshots; missing master, missing required/optional snapshot, and snapshot drift all **0**.
- Three historic master IDs are shared across six OUCP assignments. Shared IDs do **not** imply duplicate or broken data. Never update the historic master when editing one assignment.
- Inbound FK: `FK_OUCP_CONTACT_MASTER` (mandatory detach) and `FK_SPCP_CONTACT_MASTER` (user-approved exempt service contacts); other inbound FK 0.
- Stored dependencies, synonyms, triggers 0 in supplied DBA audit. Dynamic SQL, external apps, and external jobs still require separate approval.
- `ORG.CONTACT_POINTS_V2` is out of scope and is not touched by the Oracle scripts.

## Source behavior

- The **default** `ORG_CONTACT_STORAGE_MODE=legacy` now maps to the OUCP-only service for compatibility with existing configuration. It returns readiness `storageMode=UNIT`. Do not start the obsolete `canonical`/V2 pilot to validate OUCP.
- Reads query only OUCP + active unit/role tables; no live joins to `CONTACT_POINTS`.
- New records use `SEQ_ORGANIZATION_UNIT_CONTACT_POINTS`, omit historical `CONTACT_POINT_ID` (NULL), and write their contact snapshot directly into OUCP.
- Updates preserve historical `CONTACT_POINT_ID`, never insert/update a master, and use unit row lock plus optimistic `ROW_VERSION`. Editing one of the six assignments associated with a shared master does not touch other assignments.
- OUCP generic editing is permanently hidden; dedicated `/organization/unit-contacts` UI remains.
- `ORG_CONTACT_UNIT_WRITES_ENABLED=false` **by default** even if source is installed; the backend also requires both master FKs detached and nullable historic ID. Set to true only after successful Oracle detach verify and approved UAT.
- Old `/api/v1/organization/unit-contacts/migration/readiness` measures historical **V2** parity with OUCP snapshots; after legitimate OUCP edits or new records this historical parity may diverge. It is not an operational gate.

## Execution order

1. Stop the application and **install source** with `INSTALL.cmd`. Installer performs guarded byte-copy and full Windows build; no SQL is executed. If target files differ from recognized R5-A/R5-B baselines it aborts before modification.
2. Restart the new backend in **read-only OUCP mode** (`ORG_CONTACT_STORAGE_MODE=legacy`, `ORG_CONTACT_UNIT_WRITES_ENABLED=false`). Query `GET /api/v1/organization/unit-contacts/readiness`: `storageMode=UNIT`, `ready=true`, `writesEnabled=false`. Verify representative unit contacts in UI, including shared old-master IDs.
3. Run SQL `01-ORG-R5C2-OUCP-DETACH-OPT-IN.sql` with **all five flags NO** (F5). It checks data parity and exact FK inventory; no DDL.
4. Obtain DBA backup and approval, stop all writers (including external writers), approve read UAT and external consumers. Explicitly set all five flags YES and run SQL 01. It **only** makes historical CONTACT_POINT_ID nullable and drops the **two known** FKs. It neither updates data nor renames any table. Oracle DDL commits immediately; partial DDL is not rolled back.
5. Run SQL `02-ORG-R5C2-OUCP-DETACH-VERIFY-READONLY.sql` from a fresh connection. Require old-master inbound FK count **zero**, nullable historical ID, five snapshot columns, and 0 required missing snapshots.
6. Restart app with writes initially false, recheck read and unit contact data. For **explicitly approved** test writes set `ORG_CONTACT_UNIT_WRITES_ENABLED=true` and use dedicated UI. Verify create and update, row-version conflict, same-unit duplicate protection, and that editing one of six shared-old-master associations does not modify any other.
7. After signoff, run SQL `03-ORG-R5C3-CONTACT_POINTS-RENAME-OPT-IN.sql` **PREVIEW** (all six flags NO). A zero-blocker preview is not permission to rename. Verify source is installed, UI UAT is completed, all runtime writers are stopped, DBA backup and external consumers are approved.
8. In an authorized maintenance window only, set all six flags YES, execute SQL 03. It renames **only** `ORG.CONTACT_POINTS` -> `ORG.DEPRECATED_CONTACT_POINTS`. `CONTACT_POINTS_V2` and OUCP remain unchanged.
9. Run SQL `04-ORG-R5C3-RENAME-VERIFY-READONLY.sql`, restart application, check readiness and read/write behavior. In case of approved urgent rollback, `05-ORG-R5C3-MASTER-RESTORE-OPT-IN.sql` provides a preview-first **name-only** restore; code/flags must also be restored appropriately.

### Safety notes

- User-approved exclusion of `SERVICE_POINT` and `SELF_SERVICE` contact data does not authorize deleting **primary** point-of-service or terminal tables. SQL 01 **only detaches** `FK_SPCP_CONTACT_MASTER` and preserves service data in place; no DROP TABLE.
- Never run obsolete scripts that recreate old foreign keys or old master writes after R5-C2.
- The installer builds on the user's Windows machine. Local static checks do not prove JDBC behavior; Oracle/DB and functional UAT must still run.
- No script in this package changes `CONTACT_POINTS_V2`, CIF data, role catalog, unit assignments, contact values, or historical IDs.
