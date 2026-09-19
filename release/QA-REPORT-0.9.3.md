# QA Report — 0.9.3

## Scope
DPS2 Deposit Opening reference-data and foreign-key reconciliation.

## Source evidence
- Supplied Oracle DDL export: all 66 `REF_DEP_OPEN_*` reference tables exist; Missing Table = 0.
- Supplied diagnostic workbook: Snapshot Type, Audit Actor Type, Audit Event Type, Change Reason and Channel/Org Map are empty; Batch Error Code has three Phase 6 rows.
- Historical Phase 5 migration defines Audit Actor/Event and Snapshot codes.
- Historical Phase 6 migration defines the three provisional Batch Error codes.
- Application service explicitly validates `CHANGE_REASON_CODE` and requires a note for `OTHER`.

## Static gates
- Master seed is fully `DPS2.` qualified.
- Seed preflight uses `ALL_TABLES` with `OWNER='DPS2'`; no executable `USER_TABLES` dependency remains.
- Environment-owned Channel/Org mapping is not invented.
- Six correct FK pairs are declared.
- Three known wrong XMI relations are detected/removed only if present.
- Orphan-code guard runs before each FK creation.
- Historical Phase 5/6 migrations are preserved.
- Phase 8 regression: 18/18 PASS.
- Phase 9 regression: 18/18 PASS.
- 0.9.2 reconciliation regression: 20/20 PASS.
- 0.9.3 dedicated verifier: 19/19 PASS.

## Package QA
- Full source ZIP integrity: PASS.
- Patch ZIP integrity: PASS.
- 0.9.2 + 0.9.3 patch overlay versus 0.9.3 full source: Missing=0, Extra=0, Different=0.
- Full-source and patch-overlay trees both pass Phase 8, Phase 9, 0.9.2 and 0.9.3 dedicated verifiers.
- Clean source excludes runtime logs, build caches, document-storage runtime data, generated static UI and packaged JAR artifacts.

## Build qualification
All static production gates pass through the runtime-artifact contract. The packaging environment cannot complete the Maven wrapper download for Maven 3.9.16, so Java/Angular full build and runtime remain the Windows qualification gate.

## Oracle runtime qualification
Database execution of 0.9.3 is pending on the user's Oracle environment. Recommended order:
1. run the 2026-09-19 master reference seed for complete catalog normalization;
2. run `0.9.3-reference-data-fk-reconciliation.sql` for the corrected physical FK contract.

Expected final reference counts include at least:
- Snapshot Type: 5
- Audit Actor Type: 2
- Audit Event Type: 7
- Change Reason: 1 (`OTHER`) unless bank-approved reasons already exist
- Batch Error Code: 3

`REF_DEP_OPEN_CHANNEL_ORG_MAP` may legitimately remain 0 until organizational mapping is approved.
