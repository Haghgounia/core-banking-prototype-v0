# CIF 0.3.22 Fix24 - Party Authority Reference & Currency QA

## Scope

Section C: **وکالت، نمایندگی و حدود اختیار** (`PARTY_AUTHORITY`) only. No Person or Organization identity form/model was changed.

## Baseline findings

- `CIF.PARTY_AUTHORITY.DOCUMENT_REF` is mandatory in the current schema but the Fix23 UI collected it as free text (`textarea`).
- `MAX_AMOUNT` is `NUMBER(20,2)` and the UI used a plain numeric input without thousands grouping.
- `CURRENCY_CODE` is optional at column level, while `CK_AUTH_AMOUNT_CURRENCY` enforces the pair: both amount/currency null, or both populated.
- Currency lookup already comes from general Reference Data `GEO.CURRENCIES`, but the Party Authority form had no default currency.
- Authority type and authority scope already have governed CIF Reference Data tables.

## Fix24 design

### 1. Governed authority-document reference

Added dedicated Reference Data table:

`CIF.REF_AUTHORITY_DOCUMENT_TYPE`

The user selects the Persian title, while the code is persisted in the existing `PARTY_AUTHORITY.DOCUMENT_REF` field. The API field remains `documentRef` for compatibility, but its semantics are now a governed reference code.

Initial values:

- `POWER_OF_ATTORNEY` - وکالت‌نامه رسمی
- `BOARD_RESOLUTION` - مصوبه یا صورتجلسه هیئت‌مدیره
- `COURT_ORDER` - حکم یا دستور قضایی
- `GUARDIANSHIP_ORDER` - حکم قیمومت
- `EXECUTORSHIP_DOCUMENT` - سند وصایت یا وصیت‌نامه
- `ARTICLES_OF_ASSOCIATION` - اساسنامه یا سند تأسیس
- `OFFICIAL_DELEGATION` - ابلاغ یا تفویض اختیار رسمی
- `OTHER_OFFICIAL_DOCUMENT` - سایر اسناد رسمی اختیار

Backend validates new/updated values against active Reference Data. The migration creates a `NOVALIDATE` foreign key so legacy free-text rows do not block deployment; when a legacy row is edited it must be aligned to a governed reference value.

### 2. Maximum amount formatting

The plain `<input type="number">` was replaced with the existing shared `FormattedAmountInputComponent`:

- thousands grouping is visible while typing;
- Persian/Arabic digits and separators are normalized;
- maximum two decimal places are retained;
- the API/database receives a numeric value, not formatted text.

### 3. Currency default and validation

- UI default: `IRR` / ریال ایران.
- If `MAX_AMOUNT` is empty, `currencyCode` is sent as `null`, preserving `CK_AUTH_AMOUNT_CURRENCY`.
- If `MAX_AMOUNT` exists and currency is missing at API level, Backend defaults it to `IRR`.
- Backend validates the currency against active `GEO.CURRENCIES.CURRENCY_ALPHABETIC_ISO`.

### 4. Reference validation

Backend now validates:

- `AUTHORITY_TYPE_CODE` -> `CIF.REF_AUTHORITY_TYPE`
- `SCOPE_CODE` -> `CIF.REF_AUTHORITY_SCOPE`
- `DOCUMENT_REF` -> `CIF.REF_AUTHORITY_DOCUMENT_TYPE`
- `CURRENCY_CODE` -> active `GEO.CURRENCIES`

### 5. Saved-record readability

The authority history row now resolves Persian titles for authority type, scope, document reference and currency when lookup data is available. Amount is shown formatted rather than as an unformatted raw number.

## Database migration

`database/oracle/cif/migrations/0.3.22-fix24-party-authority-reference-currency.sql`

Migration actions:

1. Create `CIF.REF_AUTHORITY_DOCUMENT_TYPE` if absent.
2. Add Persian table/column comments.
3. Seed eight governed values idempotently.
4. Update `PARTY_AUTHORITY.DOCUMENT_REF` comment.
5. Create supporting index.
6. Add `FK_PARTY_AUTHORITY_REF_AUTH_DOC ... ENABLE NOVALIDATE` for forward integrity without rejecting legacy data.

## Static verification completed

- Party Reference JSON parses successfully.
- Reference catalog contains 103 active CIF tables.
- New reference descriptor has 10 columns and 8 seed rows.
- Fresh-install identity DDL contains one table definition and one seed block (8 MERGE statements).
- Authority UI uses `app-formatted-amount-input` for `MAX_AMOUNT`.
- Authority form default currency is `IRR`.
- UI sends currency `null` when no amount is present.
- Backend defaults IRR only when an amount exists.
- Backend reference/currency validation is present.
- `VERSION` and generated system specification are synchronized to `0.3.22-prototype-fix24`.

## Build environment note

Full Angular/Maven execution could not be completed in the artifact environment because the npm cache does not contain all packages and Maven Wrapper cannot download Maven from Central. `npm ci --offline` failed with `ENOTCACHED`; `./mvnw -o test` could not fetch the Maven distribution. Run `build-production.cmd` in the normal Windows build environment after applying the migration.
