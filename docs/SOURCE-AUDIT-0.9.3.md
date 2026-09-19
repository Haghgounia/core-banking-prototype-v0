# Source Audit — 0.9.3

## Baseline

0.9.3 is built on the user's 0.9.0 source package plus the accepted 0.9.2 source/schema reconciliation overlay.

## Root hygiene

The active source root is restricted to normal project entrypoints and metadata. Historical patch/readme notes are stored below `docs/patches`.

- legacy `README-FA.txt`: not in root
- `PATCH-LAYOUT-MIGRATION-0.9.2.txt`: moved to `docs/patches`
- `config/application.yml_`: archived/removed from active source
- `backend/src/main/resources/application.yml_`: archived/removed from active source
- runtime logs/build caches/document-storage: excluded from clean source packaging

`tools/migrate-root-layout.mjs` now also relocates future `PATCH-LAYOUT-MIGRATION-*.txt` files left by raw overlay extraction.

## DPS2 consistency

- 0.9.2 account lifecycle reconciliation remains present.
- 0.9.3 adds the corrected Deposit Opening Reference Data/FK contract.
- Historical Phase 5 and Phase 6 migrations are unchanged and are used as source authority for the recovered audit/snapshot and batch-error codes.
- No Hold/Block/Dormancy/Reactivation account states are introduced.

## Static regression

Production static build gates pass through the runtime-artifact contract, including:

- Phase 7: 22/22
- Phase 8: 18/18
- Phase 9: 18/18
- 0.9.2 reconciliation: 20/20
- 0.9.3 reference/FK reconciliation: 19/19

Local full build remains environment-dependent because Maven wrapper download is unavailable in the packaging environment; Windows build/runtime remains the qualification gate.

## Package equivalence

The final 0.9.3 patch was overlaid on the 0.9.2 synchronized clean-source baseline and normalized with the standard source-layout tools. Comparison against the 0.9.3 full clean-source tree produced:

- Missing: 0
- Extra: 0
- Different: 0

Both extracted trees pass the Phase 8, Phase 9, 0.9.2 reconciliation and 0.9.3 reference/FK verifiers.
