# EDU -> CIF Application Cutover R5 (Source-only)

Baseline: core-banking-prototype-v0-2026-11-01-1800.zip, with R3 lookup and optionally R4 frontend already installed.

- Swaps canonical six-table descriptor schema EDU to CIF, preserving route keys and menu labels.
- Replaces four lookup SQLs with equivalent read-only SELECT statements against CIF tables. Existing API URLs remain stable.
- Does NOT change legacy descriptors, GEO, Oracle DDL/DML, grant permissions, or delete EDU objects.
- If R4 test-panel source files exactly match shipped R4, installer detaches the disposable panel without deleting files or changing unrelated features; otherwise leaves frontend untouched.
- Exact SHA-256 guards stop installation when an unexpected variant of an overwritten source is found.
- Backups under tools/edu-cif-r5/backups, only on the user's machine.

Installation from repository root: tools\edu-cif-r5\INSTALL.cmd
Then stop, build-production.cmd, start (only on successful build).

Oracle prerequisite: application account must SELECT six CIF tables and access their sequences for INSERT operations; sequence names need verification from ALL_SEQUENCES (SEQ_EDUCATION_*). No privilege changes are included.

Test existing four HTTP API endpoints /api/v1/education/lookups/{fields,levels,institutions,field-levels}.
Also test GET/list/edit/create on six reference tables from UI and confirm CIF receives changes (no write to EDU). Preserve EDU until content parity and dependent consumers are verified.

Status: source/static checked only; full backend/frontend build and Oracle smoke test unavailable in this environment.
