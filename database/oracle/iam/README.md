# IAM Oracle — User Management Foundation R0.1

**Status:** source-only SQL design. **No database operations have been performed**.

- Proposed schema owner: `IAM` (DBA provisions manually); not legacy `USR`.
- 14 reference tables, 16 foundation tables, 5 governance tables = **35 tables**, 415 columns.
- Keys: `NUMBER(19,0)` + `IAM.SEQ_*`, UTF-8, Oracle 19c+ target. All PK/FK/UQ/CK names <= 30 bytes.
- Identity-provider secrets, password hashes, tokens, MFA credentials are **not** stored in IAM.
- Source code unchanged; **no RUN/COMMIT** executed by this package.

## Review and optional controlled execution

1. Examine `docs/iam/IAM-NAMING-DATA-MODEL-FA.md` and approve ADR / DBA governance.
2. DBA provisions an independent IAM schema with approved privileges/tablespace/secret management, without using SYSTEM as application login.
3. Run `00-iam-precheck-readonly.sql` (Run Script/F5). Stop if IAM tables already exist.
4. Optional DBA-approved run `01-iam-foundation-ddl-opt-in.sql`, then `02-iam-governance-ddl-opt-in.sql` only if those governance tables are approved.
5. Optional `03-iam-reference-seed-opt-in.sql`; this script does not issue COMMIT.
6. Run `99-iam-verify-readonly.sql` and attach verification evidence to IAM roadmap.

**Warning:** Oracle DDL automatically commits; `WHENEVER SQLERROR` cannot roll back successful prior CREATE statements. Make a backup/snapshot and execute on an isolated schema. The guard rejects existing target objects; it is not a resumable migration.

**Do not execute DDL from the project application or installer.** Any change to live login is postponed to IAM-02.
