# Collateral Domain Naming & Compatibility Decision – 2026-11-10

## Decision

Never encode delivery-phase identifiers (`R1`, `R2`, `R3`, `R4`, `R5`, `R6`, `R7`) in **new permanent Java classes, Angular components/services, public API paths or persisted domain concepts**. Instead, use stable bounded-context capability names (`CollateralWorkflowService`, `CollateralValuationCommitService`, `CollateralInsuranceCancellationService`). Document versions, migration scripts and QA fixtures may retain phase names for historical traceability.

## Migration applied to this source

- 22 active Collateral Java production classes renamed (plus 7 JUnit classes), all in-place references updated; original obsolete `.java` paths **must be removed** by the installer.
- 6 Angular file stems (and their associated `.ts/.html/.scss` files) renamed, selectors/imports/type exports updated, old paths removed. Business UI templates for sponsor/insurance/valuation unchanged.
- Both `GET/POST /api/v1/collaterals/workflow/...` and legacy `/api/v1/collaterals/r3-workflow/...` route to **one** controller; no duplicate handler or bean. Both lifecycle evidence aliases are supported.
- No Oracle table, column, index or constraint renamed: this is source/API-facing refactoring; altering physical audit records is forbidden without approved Migration.

## Safe deployment and rollback

- SHA-256 checked baseline must be user `2026-11-10-0815.zip`; any unknown local edits or unexpected old paths are conflicts, not overwritten. Installer supports `write` and `delete` operations and prepares backups before mutation; rollback restores previous source files and removes new ones.
- Preserve route compatibility until downstream API consumers migrate, then consider a separately approved breaking release.
- Build the frontend and backend from source after installing; do not run previous JAR/UI build output as if it were current.

## Limitations

- Rename provenance is not an attestation of a successful real Maven/Angular build or Oracle operation. Real runtime verification is required on the target Windows project.
- Versioned developer-only tools and historical source comments may still contain the old phase IDs intentionally.

## 2026-11-10 corrective checkpoint: UAT capability naming

BUC/SUC UAT was erroneously introduced under phase `R6` Java/Angular names in an earlier delivery. The next `CLT-BUC-SUC-UAT-EVIDENCE-GAPS-2026-11-10` release corrects those active names, removes superseded files in one guarded patch, keeps the public `/api/v1/collaterals/uat/business-readiness` URL, accepts previous UAT JSON via legacy import, and exports only capability-based identifiers. Phase labels remain in the approved R0..R7 roadmap and historical documentation, **not in new persistent domain identifiers**.
