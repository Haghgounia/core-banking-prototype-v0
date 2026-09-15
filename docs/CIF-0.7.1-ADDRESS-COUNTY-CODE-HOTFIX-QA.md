# CIF 0.7.1 — Address COUNTY_CODE Hotfix QA

## Scope
Hotfix for Party > Contact & Address where the UI could display a selected county while `CIF.ADDRESS.COUNTY_CODE` was not reliably the same persisted form value.

## Fix
- Province, county and district selects now bind the reference **code** directly to the reactive form.
- Surrogate IDs are retained only for loading child geography lookups.
- For `IRN`, backend validation requires province/county/city before SQL execution.
- Backend validates the active GEO hierarchy `COUNTRIES -> PROVINCES -> COUNTIES -> DISTRICTS -> CITIES`.
- Oracle `ORA-01400 COUNTY_CODE` should no longer be the user-facing validation path for this scenario.

## Regression scenario
`IRN -> تهران -> تهران -> تهران -> create address` must submit non-null `provinceCode`, `countyCode`, and `cityCode` and pass the active GEO hierarchy check.

## Database
No database migration is required for 0.7.1.
