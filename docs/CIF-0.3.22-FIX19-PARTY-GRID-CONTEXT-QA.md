# CIF 0.3.22 Fix19 — Party Operational Grid & Role Context QA

## Baseline
- Source: `core-banking-prototype-v0-2026-08-22-1200.zip`
- CIF metadata: `CIF-tables-2026-08-22-1200.xlsx`
- Output version: `0.3.22-prototype-fix19`

## Requested grid enhancements
- CONTACT_POINT: type/title, value, purpose, owner, dial/area/extension, primary/verified, verification status/time, validity.
- FINANCIAL_PROFILE: as-of date, monthly/annual income, assets, liabilities, estimated net worth, expected turnover, financial capacity, verification status.
- PARTY_INCOME_SOURCE: source title, monthly amount/currency, status, documented flag.
- PARTY_ASSET_LIABILITY: business item title, amount/currency, assessment date, status, description.
- PARTY_IDENTIFIER: identifier type/value, issuing country/authority, issue/expiry, verification, primary/active, validity.
- PARTY_CLASSIFICATION: classification type/value titles, assignment reason, validity, description/version.
- Additional Party grids were reviewed and enriched: addresses/contact-address links, employment/licenses, roles/customer history, KYC/risk/screening/inquiries, relationships/beneficial ownership/authorities, documents, consents/preferences, lifecycle/merge, and Party 360 summaries.

## Party Role Context
Latest CIF metadata confirms `CIF.PARTY_ROLE` already contains:
- `CONTEXT_TYPE_CODE VARCHAR2` — nullable
- `CONTEXT_ID VARCHAR2` — nullable

No physical schema change is required. Backend already persists both fields and validates that type/id are provided together.

Fix19 UI behavior:
- Context Type: Reference Data Combo (`CIF.REF_CONTEXT_TYPE`).
- GLOBAL: fixed selectable global context.
- PRODUCT: selectable from the existing Deposit Product reference endpoint.
- CASE: selectable from the Party's persisted KYC cases.
- ACCOUNT/BRANCH/CONTRACT: the current prototype has no authoritative operational domain table exposed for these contexts; the external/source-system identifier is entered and persisted to `PARTY_ROLE.CONTEXT_ID` rather than fabricating a local master table.

Migration `0.3.22-fix19-party-grid-role-context.sql` aligns Persian reference values for GLOBAL/ACCOUNT/PRODUCT/BRANCH/CONTRACT/CASE.

## Metadata cross-check
The requested displayed fields were cross-checked against `CIF-tables-2026-08-22-1200.xlsx` for:
- CONTACT_POINT
- FINANCIAL_PROFILE
- PARTY_INCOME_SOURCE
- PARTY_ASSET_LIABILITY
- PARTY_IDENTIFIER
- PARTY_CLASSIFICATION
- PARTY_ROLE
- REF_CONTEXT_TYPE

## Static QA
- TypeScript syntax parse: 44 files, 0 errors.
- CIF HTML parser: 12 templates, 0 parser errors.
- Removed the remaining legacy `*ngIf` in KYC template; no `*ngIf` remains under CIF templates.
- Removed corrupted technical display tokens (`CIF.شناسه...`, `MERGED_INTO_شناسه...`).
- `party-reference-model.json` parses successfully and is synchronized with the new REF_CONTEXT_TYPE seed set.
- VERSION/system specification synchronized to `0.3.22-prototype-fix19`.

## Full build limitation in this environment
A full Angular/Maven build could not be executed in this container because the clean source package intentionally excludes dependencies and this runtime cannot resolve `registry.npmjs.org` / Maven Central. Maven Wrapper download therefore also fails. No backend Java source was changed in Fix19; frontend changes passed the static checks above.

Run on the project Windows environment:
```cmd
bin\stop.cmd
rmdir /s /q frontend\dist
rmdir /s /q backend\src\main\resources\static
build-production.cmd
```

## Release packaging
`package-release.cmd` is included to create future clean source ZIPs while excluding node_modules, dist, target, generated static assets, JARs, logs, runtime document storage, IDE metadata and temp files.
