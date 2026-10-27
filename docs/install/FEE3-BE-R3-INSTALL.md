# FEE3-BE-R3 | Controlled Validation, Preview and Maker/Checker Foundation

Target: Core Banking Prototype 0.11.0 + FEE3-UI-R8/HF1 + FEE3-BE-R1 + FEE3-BE-R2.
Scope: development/UAT only; never use this package in a production banking system.

## Install (Windows CMD)

Extract the ZIP to the project root. This initially adds files only below
`tools/fee3-backend/releases/r3/` and does NOT replace application sources.

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\releases\r3\install.py --preview
python tools\fee3-backend\releases\r3\install.py --apply
node tools\fee3-backend\verify-r3.mjs
bin\stop.cmd
build-production.cmd
bin\start.cmd
```

If the preview reports CONFLICT, do not force file replacement. R3 requires an
exact R2 FEE3 baseline; it does not require ORG or other modules to match old ZIPs.
The installer adds new files only and checks SHA-256 of the R2 FEE3 anchors.

## HTTP endpoints

```
POST /api/v1/fee3/validation
POST /api/v1/fee3/preview
GET  /api/v1/fee3/workflow/readiness
GET  /api/v1/fee3/workflow/requests?itemId=...
POST /api/v1/fee3/workflow/submit
POST /api/v1/fee3/workflow/requests/{requestId}/decision
```

`/validation` accepts the same `DraftRequest` JSON as the R2 draft-save API.
`/preview` accepts `{ "draft": <DraftRequest>, "inputs": {"amount": 1000000} }`.
The `PreviewResult` has `status=SIMULATED|BLOCKED`, `persisted=false`, and
`financialPosting=false` without writing any Oracle table.

The engine intentionally supports FIXED, PERCENT and MIXED for general service
fees. GUARANTEE_FEE supports fixed/percentage single collateral and percentage
mixed portfolios with allocated/covered-value bases, explicit coverage and rate.
For portfolios, shares must sum exactly to 100%; each distinct collateral type
must have a configured tariff rule. Caps, rounding, tax are applied once to the
TOTAL. Unsupported scenarios return BLOCKED, never a guessed financial result.

Loan profit, Qard fees, late damages, arbitrary expressions, PMT/IPMT and
uncertified daily calendar conventions remain BLOCKED on the server in R3.
The existing offline UI calculator is NOT proof of backend financial acceptance.

## Non-mutating HTTP verification

After a successful build and start, run:

```bat
python tools\fee3-backend\releases\r3\verify_api_readonly.py
```

It uses the Oracle business date and an active SERVICE_FEE operation for an
unpersisted request. It calls GET and simulation/validation POST endpoints only.
It never calls an insert/update/submit/decision endpoint.

## Workflow: default DENY

Three feature flags are independent, and all default to false:

```
FEE3_R3_WORKFLOW_ENABLED=false
FEE3_R3_CHECKER_DECISIONS_ENABLED=false
FEE3_R3_UAT_APPROVAL_ENABLED=false
```

Do not enable them until the environment provides a VERIFIED authenticated
`Principal`, a real servlet `getAuthType()`, and container role mapping for
`FEE3_MAKER` and `FEE3_CHECKER`. No client-controlled `X-Actor` or the R1 UAT
draft token is accepted as a Maker/Checker identity. The prototype does not
provide such an organizational identity system by itself: workflow mutations
therefore remain unavailable under its default authentication setup.

With verified identity/roles and an explicit UAT change window, submit moves
an owned `F3-UAT-*` initial inactive DRAFT to REVIEW and inserts a SUBMITTED
FCE_CHANGE_REQUEST plus audit in one transaction. Rejected decisions restore
DRAFT. Approved decisions (separate flag, disabled) can only set APPROVED,
never ACTIVE or PUBLISHED, and require every test case to carry PASS+TESTED_AT
evidence. The R3 preview does NOT set any test-case PASS flags; it is not
certification evidence. Banker-approved testing and access/entitlement review
must be completed before enabling UAT approval.

Only a different authenticated Checker may decide a submitted Maker request.
Optimistic record-version checks and Oracle row locks reject stale or duplicate
transitions. Decision reasons are retained in FCE_AUDIT_EVENT.DETAILS_JSON.

## Oracle safety

Run `docs/install/FEE3-BE-R3-ORACLE-READONLY-CHECK.sql` in SQL Developer for
schema inspection. R3 has no DDL, no seed, no data migration, no posting,
no fee collection, and no automatic version activation or effective-dated
publication. Do not run direct SQL to set test PASS, change request statuses,
or approval flags. Existing FEE and FEE2 stay independent.

## Acceptance gates

- Java stub signature compilation is NOT a Maven build.
- Standalone arithmetic checks are NOT Oracle or browser E2E tests.
- Run full Maven/Angular build and confirm the non-mutating HTTP smoke.
- Validate real RBAC/identity segregation and change-request SQL transitions
  in isolated UAT using separate Maker and Checker accounts before activation.
- Do not claim banking/financial certification from preview results.
