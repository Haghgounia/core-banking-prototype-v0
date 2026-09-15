# Release Notes — 0.9.2

0.9.2 is a synchronization and packaging maintenance release. It incorporates the successful DPS2 Phase 4 + Phase 9 account-schema reconciliation into the canonical source tree and removes stale root/backup artifacts from the active layout.

No new Deposit Servicing business state is added. Phase 9 continues to support controlled Account Closure only (`ACTIVE -> CLOSED`).

The release also aligns Windows and Unix production build guards and ensures clean source packaging excludes runtime logs, uploaded document storage, Angular cache, generated frontend static files, runtime BUILD-VERSION/JAR artifacts, database exports and upgrade backups.
