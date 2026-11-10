# CLT Oracle Reference Reconciliation - P1 source checkpoint

**Date:** 2026-11-10 | **Product:** 0.11.0 | **Canonical roadmap:** CLT-ROADMAP-R1.0 (R0 through R7 unchanged)
**Integration source:** core-banking-prototype-v0-2026-11-10-1658.zip
**Install status at packaging:** DELIVERED / INSTALL_NOT_CONFIRMED
**Oracle live:** NOT_VERIFIED | **Bank approval:** NOT_ATTESTED | **Financial DML:** NONE

## Scope of changes

- Adds `GET /api/v1/collaterals/uat/oracle-reconciliation` to the existing CLT UAT API. The endpoint requires a signed authenticated principal, `SCOPE_clt.collateral.read` and a server-trusted, validated branch claim.
- Uses the *application's existing DataSource*; there is no CLT-specific connection pool, launcher, schema migration, second JAR, local-token minting or financial posting.
- Reports 17 physical CLT reference catalogs, actual row totals and ACTIVE counts if readable; otherwise `UNKNOWN` (not zero). `ALL_TABLES` visibility is privilege-filtered, so `NOT_VISIBLE` is **not** equivalent to physically missing.
- Reports DB identity (when observable), observation time, validated branch, visible sequence count, own-branch collateral count and dictionary checks for NUMBER(19,0), an enabled unique constraint and G-16 default/status reference consistency.
- Gives analyst a compact expandable panel inside the existing 16-BUC UAT view, with timestamped JSON evidence export. No replacement of the three customized Sponsor/Insurance/Valuation HTML forms.
- Includes an independent DBA read-only query script. Executing it is optional and not part of installation.

## Evidence boundaries

The report is a *technical observation*, not 16 BUC acceptances and not a permission or verification to mutate Oracle. `bankAccepted=false` is constant. The `BANK_POLICY_AND_ORACLE_WRITE_UAT` gate remains BLOCKED. G-03/G-12/G-14/G-17/G-16 and related Domain decisions remain OPEN. BUC-02/CIF integration and BUC-03/BUC-13 UI->API->Oracle readback still require real user-driven evidence.

Partial/unavailable observations are not promoted to PASS. A zero count is only emitted after a successful COUNT(*). Displayed customer-collateral totals are scoped to the authenticated branch. No personal details, collateral numbers, account numbers or customer IDs are exported.

## Verification

| Gate | Status at package creation |
|---|---|
| Java domain policy smoke | PASS (local JVM, no Oracle) |
| Java syntax/API-shape stubs | PASS (not a real Spring compilation) |
| TypeScript contract strict compilation / component syntax | PASS in isolated toolchain |
| Static no-DML, authorization and file-safety checks | PASS |
| Production Maven and Angular build | NOT_VERIFIED (dependencies unavailable in packaging environment) |
| Runtime JAR / Windows startup / shared Oracle probe | NOT_VERIFIED |
| Analyst UAT, 17 technical catalogs and 32 scenarios | NOT_EXECUTED |
| DBA approval and any Oracle migration | NOT_GRANTED / NONE |

## Next action

After normal project build and startup, the analyst opens the existing CLT BUC UAT workbench and expands **Oracle Reconciliation P1**, executes the read-only probe and exports JSON. Reconcile actual missing permissions, active reference data and physical numbering/status requirements with bank/DBA. Only then evaluate authorized BUC-03 introduction + BUC-13 direct lookup through real Oracle. Do not close a gap or a canonical phase from this source checkpoint alone.
