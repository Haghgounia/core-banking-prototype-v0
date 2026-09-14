# Core Banking Prototype 0.5.1 — DPS2 Phase 5 Build Hotfix

0.5.1 is a non-functional hotfix that repairs the production build/release path discovered by the first real Windows build of Phase 5.

## Fixed

- Cumulative patch now carries every Phase 4 Account Lifecycle source required by Phase 5.
- `DepositOpeningAuditService` is aligned with Jackson 3 used by Spring Boot 4.1.0.
- Production build performs explicit completeness checks for the Phase 4 account package.
- Phase 5 verifier and dedicated 0.5.1 build-hotfix verifier run before Maven compilation.

## Compatibility

The cumulative source patch is intended to be safe for the current mixed 0.5.0 tree produced by overlaying Phase 5 on an older 0.3.99-era source, and can also be applied to a complete 0.4.0/0.5.0 source tree.

## Database

No 0.5.1 migration exists. Required schema changes remain:

- `database/oracle/dps2/migrations/0.4.0-phase4-deposit-account-lifecycle.sql`
- `database/oracle/dps2/migrations/0.5.0-phase5-opening-audit-change-management.sql`

Run only migrations that have not already been applied.

## Next verification

After overlaying 0.5.1, run `build-production.cmd`. A successful build must produce `app/core-banking-prototype.jar` and `app/BUILD-VERSION` containing `0.5.1`.
