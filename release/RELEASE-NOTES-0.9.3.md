# Release Notes — 0.9.3

## DPS2 Deposit Opening Reference Data / FK Reconciliation

- Added a fully `DPS2`-qualified master reference-data seed: `database/oracle/dps2/reference-data/Deposit_Account_Opening_Reference_Data_Seed_2026-09-19.sql`.
- Fixed seed preflight to use `ALL_TABLES` + `OWNER='DPS2'`, so execution is independent of `SESSION_USER` / `CURRENT_SCHEMA`.
- Restored Phase 5 audit/snapshot reference codes and the source-required `OTHER` Change Reason.
- Reconciled the three Phase 6 provisional Batch Error codes already used by the application.
- Added six correct semantic FKs for Request Type/Status, Decision/Reason and Terms Acceptance Source/Status.
- Added a guard to remove only the three specifically-known wrong XMI FK mappings if they exist in another environment.
- `REF_DEP_OPEN_CHANNEL_ORG_MAP` remains intentionally unseeded because bank organization-unit mapping is environment-owned.
- Added dedicated static verifier and wired it into Windows/Unix production builds and release packaging.
- Moved `PATCH-LAYOUT-MIGRATION-0.9.2.txt` out of source root into `docs/patches` and hardened the root-layout migration guard.
- No new Account Servicing state is introduced; Phase 9 remains `ACTIVE -> CLOSED` only.
