# QA Report 0.7.1

## Scope
CIF Party Address hotfix for the reported `CIF.ADDRESS.COUNTY_CODE` null insert path.

## Result
- Dedicated hotfix verifier: 15/15 PASS.
- Windows production static verifier set: 39 verifier scripts PASS in the release source tree.
- Phase 6 and Phase 7 Deposit Opening verifiers remain PASS after the 0.7.1 version bump.
- Node verifier path-portability guard: PASS (38 verifier scripts scanned).
- No Oracle migration is required.

## Build qualification
A local Maven compile was attempted. The source reached Maven Wrapper bootstrap but this execution environment could not download Apache Maven 3.9.16 from Maven Central (`wget: Failed to fetch ...`). Therefore Java/Angular production compilation must be closed on the user's Windows build environment, as with previous releases.

## Functional regression target
`Party -> نشانی و تماس -> IRN -> تهران province -> تهران county -> تهران city -> ثبت نشانی`

Expected: `provinceCode`, `countyCode`, and `cityCode` are non-null reference codes; invalid or incomplete geography is rejected as business validation before Oracle DML.
