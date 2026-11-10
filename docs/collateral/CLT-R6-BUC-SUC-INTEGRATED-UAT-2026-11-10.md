# CLT R6 business analyst UAT cockpit: 16 BUC / 20 SUC / 32 scenarios

Date: 2026-11-10. Canonical roadmap: CLT-ROADMAP-R1.0-FA.md (phase names R0..R7 unchanged).
Baseline: user-confirmed installed CLT-BUC09-10-11-12-15-LIFECYCLE-2026-11-10.

## Delivered in source

- A new R6 tab in the existing Collateral UAT Workbench covers the 16 BUCs with their referenced business rules, 20 SUC identifiers, 16 positive and 16 negative/barring scenarios.
- Each scenario is an independently recorded manual analyst result: NOT_RUN, PASS, FAIL or BLOCKED with non-sensitive evidence note. A BUC is counted as manually evidenced only if BOTH positive and negative scenarios are PASS with notes. This is not bank acceptance.
- Open-operation navigation routes each BUC to the existing operational page and detail section; amendment navigation is now supported.
- The UAT panel exports/imports a strict versioned JSON local manual evidence snapshot. It rejects forged bank/oracle attestation and unknown/duplicate scenarios. State is in-memory only and deliberately not posted to any server.
- GET /api/v1/collaterals/uat/business-readiness queries Oracle ALL_TABLES on the application's existing shared DataSource, and reports each BUC's table-presence status (PRESENT/MISSING/UNKNOWN), plus configured financial feature flag state. Metadata unavailability maps to UNKNOWN, never MISSING. No DML/DDL.
- The existing BUC-16 timeline read API is now embedded in the selected Collateral profile history tab, scoped to registration number and collateral ID, with pagination, stale-request suppression and explicit evidence coverage.

## Interpretation boundaries

The dictionary probe attests *table visibility only*, not schema compatibility, data migration, valid business decisions, successful transactions, policy approvals, live Oracle UAT or bank acceptance. Financial POST feature flags are unchanged and default-disabled. Local prototype analysts cannot issue bank-owned financial instructions.

## R6 required exit evidence (OPEN)

1. All 16 BUCs and 20 SUC mappings reviewed, positive and negative scenarios executed by a business analyst, observations and non-sensitive evidence captured.
2. Existing 10 official UI scenarios and 17 R1 reference table UAT tests completed independently.
3. RTL/1366px review and cross-screen regression.
4. Actual UI -> Spring Boot -> approved Oracle end-to-end tests (including write operations where applicable) on bank-approved schema/flags, with authorization and data provenance.
5. Independent bank decision, security/audit, concurrency, rollback, and R7 controls satisfied.

## Not performed

No new Oracle migration, test bank-data mutation, approval grant or feature flag activation. The local QA includes source verification, type-checking with mocks, pure Java contract smoke, negative UAT file contract tests and guarded patch installation/rollback; it does **not** constitute a complete Maven+Angular production build or live Oracle/UAT.

## Verification entrypoints

- tools/clt/r6/verify-source.cjs (source-contract verifier)
- tools/clt/r6/R6BusinessReadinessPolicySmoke.java (pure Java policy smoke)
- PATCH RUN-QA.cmd for source/runtime verifiers (requires installed target)

Next development in R6: analyst-run UI scenario results -> resolve demonstrated functional gaps, then R7 bank acceptance and deployment readiness, without renaming R0..R7.
