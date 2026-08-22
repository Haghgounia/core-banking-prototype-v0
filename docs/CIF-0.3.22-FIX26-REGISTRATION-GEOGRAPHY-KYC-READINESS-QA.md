# CIF 0.3.22 Fix26 QA - Registration Geography and KYC Readiness

## Findings

1. `ORGANIZATION.REGISTRATION_PLACE_CODE` was rendered as free text before registration country. This allowed a city/code inconsistent with the selected country.
2. The GEO model already has the required hierarchy: `GEO.COUNTRIES`, domestic `GEO.CITIES` through province/county/district, and `GEO.FOREIGN_CITIES` with `COUNTRY_ID`. No new reference table is required.
3. `EMPLOYEE_COUNT` is a numeric current-state attribute and the current schema has no per-field source/as-of columns. Fix26 documents customer declaration as the onboarding source, with verified documentary values preferred when available.
4. Readiness API already returned `PartyReadinessItem.detail`, but Party 360 did not display it. The KYC rule requires a KYC case with both `FINAL_RISK_LEVEL_CODE` and `DECISION_CODE`, while the full KYC form did not require final risk level.

## Fixes

- Country is selected before registration city.
- Registration city is a searchable controlled selection: Iran -> `GEO.CITIES`; other countries -> `GEO.FOREIGN_CITIES` filtered by country.
- Backend validates active country and city-country membership before persisting `ORGANIZATION`.
- Changing registration country clears the previous city to prevent stale mismatches.
- Readiness cards display the requirement detail and blockers include the exact reason.
- KYC final risk level is required. If a linked risk assessment exists but KYC final fields are missing, readiness explicitly says that the final KYC result must be completed in section 8.1.

## Database migration

None. Existing GEO reference tables and current CIF columns are used.

## Regression boundary

The Person/Natural-customer registration geography flow is unchanged.
