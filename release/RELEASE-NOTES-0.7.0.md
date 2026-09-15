# Release Notes — 0.7.0

Phase 7 converts Deposit Account Opening from build-qualified flow to runtime-verifiable flow without expanding into Servicing.

Highlights:
- Single Opening validates DPS2 references, CIF Party and PDL Product Version before persistence.
- New validate-only endpoint for Step 5 preflight.
- New runtime readiness and rollback-probe endpoints/page.
- Oracle uniqueness guards close check-then-insert races for Opening/Batch idempotency and Batch external row keys.
- Concurrent duplicate inserts resolve as idempotent replay where appropriate.
- External Account Service and Tax Profile contracts remain visible WARNs rather than mock integration claims.
