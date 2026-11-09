# CLT-R3 / Shared Oracle Intake & Worklists — Checkpoint 2026-11-09

**Baseline:** `CLT-ROADMAP-R1.0` (R0..R7 unchanged); project version `0.11.0`.
**Predecessor:** `CLT-R2-REVIEW-DECISION-INTEGRITY-2026-11-09` (user confirmed installer success).
**Delivery:** `CLT-R3-SHARED-DATASOURCE-INTAKE-2026-11-09`.
**Goal:** advance **R3 — guarantor, insurance, valuation** without enabling unapproved final-life-cycle transactions.

## Source contract and business traceability

| Domain | Business / System / Service | Source of truth | Implemented in this delivery | Not implemented |
|---|---|---|---|---|
| Guarantor | BUC-06, SUC-07, SVC-05 | `CLT.COLLATERAL_SPONSOR`; CIF Party | Reuse existing validation and DRAFT request POST, shared application Oracle DataSource in loopback; visible own-requests worklist | Definitive sponsor lifecycle, count/approval G-05/G-11, final sponsor INSERT |
| Insurance | BUC-07, SUC-09, SVC-07 | `CLT.COLLATERAL_INSURANCE`, insurer reference | Shared datasource staging request + user/branch worklist; active insurer filtering retained | Bank insurer policy G-04, cancellation/renewal approval G-06, final policy INSERT |
| Valuation | BUC-08, SUC-10, SVC-08 | `CLT.COLLATERAL_VALUATION_HISTORY` | Shared datasource staging request + user/branch worklist; current evidence remains read-only | G-07 FINAL/APPROVED, valuation period/amount policy, final posting or current valuation pointer UPDATE |

### R3 local unified data source

- `config/clt-unified-local.yml` enables R3 **request-stage** write flags only under the existing `CLT_UNIFIED_ENABLED` local overlay. All three R3 writer pools reuse the application's `DataSource` and a `DataSourceTransactionManager` in single-app, loopback mode.
- Shared datasource is **not closed** by Collateral writer bean shutdown. No new credentials, standalone starter, separate app, JDBC URL or database schema is introduced.
- The property toggle, principal/branch scope, required Oracle stage tables, CHECK constraints, idempotency, and transactional `REQUEST_CREATED` audit event are preserved.
- Outside unified local mode, existing dedicated-writer opt-in behavior and fail-closed configuration remain unchanged. `0.0.0.0` / non-loopback binding rejects shared write mode at bean creation.

### Persisted worklists

The `CollateralR3DraftWorklistComponent` uses the existing secured `GET .../requests/mine` endpoints. Each result is limited server-side by `MAKER_SUBJECT` and `BRANCH_CODE`; readback is paginated, with exact decimal-string identifiers and refresh after POST. The insurance `analystPreview` still cannot POST or load operational user requests. DRAFT is explicitly labeled **not final**.

## Oracle and architecture gates

The original EA XMI has 26 tables and contains no dedicated R3 staging tables. Existing source-controlled files under `database/oracle/clt/r3a`, `r3b`, and `r3c` are **non-executable candidate schemas**, intentionally failing before DDL. This package **does not** run them, approve them, or add new DDL. If these stage tables are absent or incomplete, each service readiness reports `stagingSchemaVerified=false` and save remains unavailable. No write to any of the three final domain tables is introduced.

**Pending:** G-04/G-05/G-06/G-07/G-09/G-11/G-18, plus G-14/G-17 and Oracle migrations for operational collateral registration. Branch/role rules remain with existing Collateral service boundaries; no IAM changes.

## Developer acceptance evidence and limitations

- Checked project Java policy with standalone Java 21 compiler (10 cases), TypeScript syntax for changed components/services, source contracts, installer SHA/rollback in an isolated source fixture.
- Windows Angular/Maven build, running Oracle Stage schema, UI UAT and end-to-end workflows are **NOT verified** in the package build environment. Installer invokes official `build-production.cmd` on user's system.
- User requested implementation-first: UAT/functional tests are deferred until the conclusion of R0..R7 development, but developer source/build verification is retained.

## Next concrete development slice

R3 final lifecycle contracts: approved insurer/guarantor eligibility and valuation state semantics, then command handlers for Oracle domain tables with explicit G-04/G-05/G-06/G-07/G-11 gate decisions. Do not close R2 or R3 until migrations, final registration and functional acceptance have evidence.
