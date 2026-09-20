# DPS2 0.10.0 - Phase 10F Runtime Qualification Runbook

## Purpose

This runbook closes the final runtime gate for the four deposit families against the live Oracle instance.

Families:

- `QARD_SAVINGS`
- `CURRENT_ACCOUNT`
- `SHORT_TERM_DEPOSIT`
- `LONG_TERM_DEPOSIT`

The qualification uses the application Product Builder API to create or reuse dedicated PDL qualification products and runtime-eligible product versions. It then validates four non-placeholder Deposit Opening aggregates and writes the fixture consumed by the existing Phase 10F harness.

## Preconditions

The following gates must already pass:

- application health = `UP`
- `/api/v1/deposit-opening/readiness` = `READY` or `READY_WITH_WARNINGS`
- Phase 7 rollback probe: `success=true`, `visibleInsideTransaction=true`, `remainedAfterRollback=false`
- `CIF.PARTY` contains the Party chosen for qualification (default: `PARTY_ID=1`)

## 1. Prepare PDL qualification data and fixture

From the project root:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\prepare-dps2-phase10f-runtime.mjs
```

Optional overrides:

```bat
set PHASE10F_PARTY_ID=1
set PHASE10F_OPENING_AMOUNT=1000000
set CORE_BANKING_BASE_URL=http://localhost:8091
node tools\prepare-dps2-phase10f-runtime.mjs
```

Expected final marker:

```text
PHASE10F_FIXTURE_READY ...\docs\examples\phase10f-e2e-fixture.runtime.json
```

The tool:

1. reads the actual `PRODUCT` and `PRODUCT_VERSION` descriptors from the running application;
2. creates/reuses dedicated products with codes `P10F_*`;
3. creates/reuses a runtime-eligible version for each family;
4. builds a fresh aggregate for each family with unique request/idempotency keys;
5. validates each aggregate through `POST /api/v1/deposit-opening/requests/validate`;
6. checks that the runtime-returned Product Family is exactly the requested family;
7. writes `docs/examples/phase10f-e2e-fixture.runtime.json` only after all four validations pass.

No direct SQL insert into `PDL.PRODUCT` or `PDL.PRODUCT_VERSION` is used.

## 2. Run final Phase 10F E2E

```bat
node tools\runtime-dps2-phase10f-e2e.mjs docs\examples\phase10f-e2e-fixture.runtime.json
```

The harness executes for every family:

`Validate -> Persist -> Create Account -> Settlement -> Readiness -> Activate`

Expected final marker:

```text
PHASE10F_RUNTIME_E2E_PASS
```

and four PASS rows, one for each family.

## 3. Final closure

Only after the real Oracle run emits `PHASE10F_RUNTIME_E2E_PASS` may Phase 10F and release 0.10.0 be marked closed/final.

## Notes

- Default Party is `PARTY_ID=1`; override with `PHASE10F_PARTY_ID` if required.
- The generated fixture contains controlled runtime evidence for the three external activation checks that are required by the current prototype contract: `CBI_SIAH_REGISTRATION`, `FINAL_COMPLIANCE_RECHECK`, and `RESTRICTIONS_READY`.
- The fixture contains all required Account Creation gate checks as PASS evidence, including `PRODUCT_ELIGIBILITY`, `KYC_CDD`, `SANCTIONS`, `DOCUMENTS`, `INQUIRIES`, `OPENING_RULES`, `SIGNATORY_AUTHORITY`, `TERMS_ACCEPTANCE`, and `DUPLICATE_REQUEST`.
- `ACCOUNT_OPENED_SMS` remains post-activation/non-blocking in the current runtime contract.
