# Release Notes 0.7.1

Hotfix for CIF Party Address geography persistence.

- Province, county and district selectors now bind GEO reference codes directly to the reactive form.
- Surrogate geography IDs are used only to load child lookups.
- Backend validates the country and requires province/county/city for `IRN` before database DML.
- Backend verifies the active GEO hierarchy through COUNTRY -> PROVINCE -> COUNTY -> DISTRICT -> CITY.
- The reported `CIF.ADDRESS.COUNTY_CODE` null path is intercepted before Oracle insert/update.
- Deposit Opening Phase 7 is unchanged.
- No database migration is required.
