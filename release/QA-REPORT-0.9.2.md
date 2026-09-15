# QA Report — Core Banking Prototype 0.9.2

## Scope
Maintenance synchronization release for DPS2 Phase 4/9 account schema reconciliation and clean source layout.

## Findings before correction
- Application source was version 0.9.0 and Phase 9 static guards passed.
- Oracle had already been reconciled with the 0.9.2 repair, but that successful migration was not present in the uploaded source tree.
- Root contained stale `README-FA.txt` from 0.3.2.
- Stale `application.yml_` backup files remained in active configuration paths.
- The uploaded ZIP was a raw working-tree archive and contained runtime/generated material that the official source packager is designed to exclude (logs, document storage, Angular cache, generated static frontend, BUILD-VERSION, DB exports, upgrade backup).
- Unix build script was missing three guards present in Windows build (`migrate-source-layout`, CIF religion verifier, CAL current-year verifier).

## Corrections
- Added canonical 0.9.2 DPS2 reconciliation migration and verifier.
- Synchronized all version markers to 0.9.2.
- Added automatic root-layout migration and clean layout verification.
- Restored Windows/Unix build-guard parity.
- Produced a clean source package without runtime/private generated artifacts.

## Database/Application Contract
`DEPOSIT_ACCOUNT` and `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT` columns used by Phase 4/8/9 repositories match the successfully reconciled Oracle schema. Phase 9 remains `ACTIVE -> CLOSED`, with one append-only `CLOSE` event and `RECORD_VERSION` concurrency protection.
