# CIF 0.3.22 Fix20 - Risk Assessment QA

## Baseline
- Source baseline: `core-banking-prototype-v0-2026-08-22-fix19.zip`
- CIF schema metadata baseline: `CIF-tables-2026-08-22-1200.xlsx`
- Scope: Risk Assessment only.

## Root causes confirmed
1. `PARTY_RISK_ASSESSMENT.SCORE_VALUE` is mandatory but the supplied metadata has no governed minimum/maximum.
2. `PARTY_RISK_ASSESSMENT.MODEL_VERSION` is mandatory, while the supplied `REF_RISK_MODEL` has no model-version attribute; therefore the UI required a value it could not derive from Reference Data.
3. The Risk submit button was disabled whenever `riskForm.invalid`; a missing model version therefore prevented submit without giving actionable feedback.

## Fix20 design
- `CIF.REF_RISK_MODEL` is extended with:
  - `MODEL_VERSION VARCHAR2(30)`
  - `MIN_SCORE NUMBER(12,6)`
  - `MAX_SCORE NUMBER(12,6)`
  - `CHECK (MIN_SCORE <= MAX_SCORE)`
- Existing prototype models are initialized to version `1.0` and domain `0..100`. These are data-governance values and can be changed in the reference table without code changes.
- New endpoint: `GET /api/v1/cif/risk-models/{modelCode}/profile`.
- Selecting a Risk Model loads model version and score domain automatically.
- Model Version is read-only in the operational form.
- Score input receives dynamic Angular min/max validators and displays the current permitted range.
- Risk submit button is disabled only while a model profile is loading or a save operation is in progress. Invalid submit gives a field-specific Persian message.
- Backend validates KYC ownership, active Risk Type, Risk Level, Decision, model/version consistency, and score range before persistence.

## Migration
Run before testing against an existing CIF schema:
`database/oracle/cif/migrations/0.3.22-fix20-risk-model-profile.sql`

## QA performed
- Runtime reference metadata JSON parsed successfully.
- `REF_RISK_MODEL` metadata checked for MODEL_VERSION/MIN_SCORE/MAX_SCORE and score-range constraint.
- Fresh-install compliance-risk DDL checked to ensure the new columns exist only on REF_RISK_MODEL.
- Seed SQL checked for all three prototype risk-model rows with version and score domain.
- Angular Risk form checked for dynamic min/max, read-only model version, and non-silent submit behavior.
- TypeScript parser check found no syntax diagnostics in the changed CIF frontend files. Missing Angular/RxJS modules are expected because release packages intentionally exclude `node_modules`.
- Backend source and API mapping checked statically for profile retrieval and server-side validation.
- Full Maven/Angular build could not be executed in this container because Maven/npm dependencies are not locally available and outbound package download is blocked.
