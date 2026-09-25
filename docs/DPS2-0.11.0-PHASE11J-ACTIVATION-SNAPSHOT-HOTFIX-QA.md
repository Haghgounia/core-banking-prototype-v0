# DPS2 0.11.0 — Phase 11J Activation Snapshot Hotfix

Evidence before this patch:
- Phase 11I: Static 64/64 PASS, Oracle DB 29/29 PASS, Runtime E2E PASS — CLOSED.
- Phase 11J: Static 73/73 PASS, Oracle DB 29/29 PASS, Windows production build PASS, Maven tests 51/51 PASS.
- Phase 11J runtime: Attribute, Condition Override, Signatory Authority and Bulk PASS; activation-run creation failed with DATABASE_ERROR.

Root contract mismatch found by source/DDL audit:
- source `DEPOSIT_OPENING_CHECK.WAIVER_REASON` = free text up to 500 characters;
- target `DEPOSIT_ACTIVATION_CHECK.RESULT_REASON_CODE` = optional machine-readable VARCHAR2(60).

Fix:
- do not copy free-text WAIVER_REASON into RESULT_REASON_CODE;
- preserve readable evidence in DETAILS, bounded to Oracle VARCHAR2(500);
- prefer SOURCE_EVALUATION_REFERENCE, falling back to RESULT_REFERENCE, as SOURCE_REFERENCE;
- fix Wave A runner Windows `start` working-directory quoting;
- add targeted activation-only qualification so already-passed runtime paths are not repeated.

No Oracle migration is required.
