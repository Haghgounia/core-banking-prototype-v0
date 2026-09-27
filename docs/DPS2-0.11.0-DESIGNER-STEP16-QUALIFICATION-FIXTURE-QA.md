# DPS2 0.11.0 — Designer Step 16 Qualification Fixture QA

## Scope
This hotfix closes the test-environment prerequisite exposed by the Step 16 runtime qualifier: the application code and Product Builder support NOSTRO/VOSTRO products, but the target Oracle database may contain no governed correspondent product/profile rows.

## Source boundary
The operational RC states that the correspondent product profile is defined in Product Builder and the Step 16 account form records account-level correspondent/reconciliation data. Therefore the qualifier must not insert a production seed directly into DPS2/PDL.

## Qualification-only fixture
`tools/prepare-dps2-designer-step16-correspondent-products.mjs` uses the normal Product Builder REST API to create/reuse:

- `QA_STEP16_NOSTRO` / family `NOSTRO_ACCOUNT` / USD
- `QA_STEP16_VOSTRO` / family `VOSTRO_ACCOUNT` / EUR
- one governed `PRODUCT_VERSION` for each QA product
- one `CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE` for each version

The fixture is rerunnable and does not use SQL INSERT/UPDATE/DELETE or production migration/seed files.

## Runtime qualification
The Step 16 runtime now positively executes both branches:

1. NOSTRO/USD account master -> extension/profile -> reconciliation run/item -> completion
2. VOSTRO/EUR account master -> extension/profile -> reconciliation run/item -> completion

Missing profile is no longer an accepted/deferred runtime branch.

## Integration
Both:

- `tools/qualify-dps2-designer-step16.cmd`
- `tools/qualify-dps2-phase11nk-final.cmd`

invoke the qualification fixture after runtime health is UP and before the Step 16 runtime E2E.

## Static evidence
Expected static marker:

`DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_STATIC_PASS`

with zero static failures.
