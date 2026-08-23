# CIF 0.3.24 FIX35 - Party onboarding required-column contract

## Symptom
After FIX34, legal-entity Party onboarding still returns the generic Persian database-integrity message:

`عملیات با محدودیت‌های پایگاه داده سازگار نیست.`

## Evidence reviewed
- `FK-CHECK.xlsx` confirms the onboarding FKs are enabled and the previously corrected values are valid:
  - `REF_SCRIPT`: `ARAB`, `LATN`
  - `REF_DATA_SOURCE`: includes `CORE`
  - Party base reference values include `ORGANIZATION`, `ACTIVE`, `NEW_REGISTRATION`, `UNVERIFIED`, `INCOMPLETE`
- `CIF-tables-2026-08-23-1624.xlsx` shows `CIF.ORGANIZATION.REGISTRATION_COUNTRY_CODE` as `NULLABLE = N`.

## Root cause
`onboardParty()` called `createParty()` first. For an ORGANIZATION, `createParty()` immediately inserted an intermediate `CIF.ORGANIZATION` row before the full organization payload was applied.

That intermediate `OrganizationRequest` set `REGISTRATION_COUNTRY_CODE` to `NULL`, while the current Oracle model requires the column to be NOT NULL. Oracle therefore raises `ORA-01400` during the intermediate insert and the transaction rolls back before `upsertOrganization()` can write the user-selected registration country.

This is not an `ORA-02291` missing-reference failure. The newly enabled reference FKs exposed several contract issues, but the remaining failure shown after FIX34 is a required-column mismatch in the intermediate Organization insert.

## Fix
- Extend the internal Party-create contract with `registrationCountryCode` for ORGANIZATION creation.
- During onboarding, propagate `organization.registrationCountryCode` into the intermediate Organization insert.
- Validate the registration country before persistence and align `OrganizationRequest.registrationCountryCode` with the Oracle NOT NULL contract.
- Validate all Party base FK-backed values before `PARTY` insert: Party type, lifecycle status, status reason, verification status, data-quality status and creation source.
- Validate additional `PARTY_IDENTIFIER` FK-backed fields before insert: identifier type, issuing country, issuing authority, verification status, verification source and verification method.
- Do not append synthetic fallback codes to the legal-entity onboarding selectors for ISIC/activity-status/enterprise-size/ownership. Only values returned by the active database Reference Data are selectable; fallback text may still improve labels for matching codes.
- Map `ORA-01400` separately as `REQUIRED_DATABASE_VALUE_MISSING` and include the Oracle column name in the user-visible detail when it can be extracted.
- Bump visible release to `0.3.24-prototype-fee-p1` so operators can distinguish the rebuilt JAR from the previous 0.3.23 runtime.

## Retest expectation
For the screenshot scenario (ORGANIZATION, source `CORE`, registration country `IRN`), the first Organization insert must receive `REGISTRATION_COUNTRY_CODE='IRN'` and must no longer fail on ORA-01400.

If a different database contract mismatch remains, the UI should now show a more specific field/reference error rather than the old generic integrity message.

## Verification performed
- Parsed `FK-CHECK.xlsx` and confirmed all 26 reported onboarding FKs are ENABLED.
- Parsed latest Oracle metadata and confirmed `ORGANIZATION.REGISTRATION_COUNTRY_CODE` is mandatory.
- Static project guards passed:
  - `tools/verify-cif-persisted-grids.mjs`
  - `tools/verify-ea-oracle-comparison.mjs`
- Full Angular build could not run in the isolated environment because `npm install` could not complete and Angular CLI was not available locally.
- Full Maven build could not be run because Maven dependencies were not locally cached.
