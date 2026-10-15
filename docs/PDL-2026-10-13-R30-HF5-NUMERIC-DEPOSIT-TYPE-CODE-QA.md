# R30-HF5 - Numeric deposit product codes by approved deposit type

Date: 2026-10-13 | Scope: PDL Product Builder, stage-six DEPOSIT finalization only.

## Approved mapping

| Persisted `PDL.DEPOSIT_PRODUCT_PROFILE.DEPOSIT_TYPE_CODE` | Numeric issuance prefix | First code in working year 1405 |
| --- | --- | --- |
| `CURRENT_NO_INTEREST` | `2510` | `25101405001` |
| `SAVINGS` | `3510` | `35101405001` |
| `SHORT_TERM_DEPOSIT` | `4510` | `45101405001` |
| `LONG_TERM_DEPOSIT` | `5510` | `55101405001` |
| `CERTIFICATE_OF_DEPOSIT` | `6510` | `65101405001` |

The prefix is the approved **four-digit numeric deposit TYPE code**. It is NOT `DEPOSIT_TYPE_ID`, any DPS surrogate key, the historical DPS code `2`, or `PRODUCT_FAMILY_CODE`. The PDL profile persists semantic business codes (e.g., `SAVINGS`); mapping to 3510 is explicitly governed in `DepositProductCodePolicy` and in the new Oracle table `CK_PDL_PDTCR_MAPPING` constraint. No guesswork or fallback is allowed.

Formula: `NUMERIC_TYPE_PREFIX(4) || ORACLE_BANK_WORK_YEAR(4) || SERIAL(3)`.
The serial begins with `001`, rises to `999`, and resets separately per numeric type and year. Every candidate is checked against **all** `PDL.PRODUCT.PRODUCT_CODE` values (both LOAN and DEPOSIT) before issuance. If a code is already in use by another product, its serial is skipped. The bank year comes from the same locked `PDL.PRODUCT_LOAN_WORK_YEAR` row that is used by loans; it is never inferred from the operating system clock.

## Legacy records and database compatibility

Existing `PDL.PRODUCT` values are never rewritten. Specifically, `QARD_SAVINGS1405001` on product 33 is retained, with the original legacy registry entry. Stage-six repeat requests return the same code without allocating a new serial.

The prior R30 `PDL.PRODUCT_DEPOSIT_CODE_REGISTRY` is **unchanged**. Its key `(PRODUCT_FAMILY_CODE, WORK_YEAR, SERIAL_NO)` is deliberately NOT reused to number new type-based codes. HF5 creates the independent `PDL.PRODUCT_DEPOSIT_TYPE_CODE_REGISTRY`, which stores `PRODUCT_ID`, original family, persisted business type, issued numeric prefix, bank year, serial, issued code and audit metadata. A `UNIQUE(ISSUANCE_TYPE_CODE, WORK_YEAR, SERIAL_NO)` key ensures the counter scope even when several legacy family aliases map to the same deposit type. An additional unique code key and the pre-existing global `PDL.PRODUCT.UQ_PRODUCT_CODE` protect against collision. Both registries are consulted for product-family immutability, and any previously issued product code itself also freezes the family (including historical rows not in either registry).

`NOSTRO_ACCOUNT` and `VOSTRO_ACCOUNT` have **no approved numeric deposit type mapping** in this scope, so issuance is refused with a validation error. No invented prefix or surrogate is used.

Loan finalization, `LOAN_TYPE_CODE + bank year + 001..999`, is unchanged; for instance `10 + 1405 + 002 = 101405002`.

## Installation order

1. Back up the application source/JAR and arrange a DBA-reviewed schema backup. Stop the backend service for deployment.
2. Install the HF5 source patch on top of **R30-HF4** (or use the full cumulative HF5 source).
3. Run the **new HF5 migration** against the correct Oracle PDL schema with DBA authorization, **before starting the HF5 backend**:
   `database/oracle/pdl/migrations/0.11.0-r30-hf5-numeric-deposit-type-product-code.sql`
4. The migration is DDL only and guarded for repeat execution. It creates the numeric registry if absent and verifies its required columns and constraints if already present. It does not contain business DML or modify historical product codes. Oracle DDL is auto-committing; review first.
5. Run `node tools/verify-pdl-r30-hf5-deposit-type-code.mjs` and `node tools/verify-pdl-r30-deposit-finalization.mjs` (both are non-writing checks).
6. Run `build-production.cmd`; verify `BUILD_EXIT_CODE=0`, absence of `app/BUILD-DIRTY`, redeploy/restart the new backend JAR, and refresh the browser.

## Oracle read-only checks after DBA migration

```sql
-- Target registry exists (one row expected).
SELECT OWNER, TABLE_NAME FROM ALL_TABLES
WHERE OWNER = 'PDL' AND TABLE_NAME = 'PRODUCT_DEPOSIT_TYPE_CODE_REGISTRY';

-- Eight enabled/validated structural constraints expected.
SELECT CONSTRAINT_NAME, STATUS, VALIDATED
FROM ALL_CONSTRAINTS
WHERE OWNER = 'PDL' AND TABLE_NAME = 'PRODUCT_DEPOSIT_TYPE_CODE_REGISTRY'
ORDER BY CONSTRAINT_NAME;

-- Bank-controlled year source; do NOT update it for this test.
SELECT SETTING_ID, WORK_YEAR FROM PDL.PRODUCT_LOAN_WORK_YEAR WHERE SETTING_ID = 1;

-- Historical issued deposit codes must remain unchanged.
SELECT PRODUCT_ID, PRODUCT_CODE, PRODUCT_FAMILY_CODE
FROM PDL.PRODUCT
WHERE PRODUCT_ID IN (22,33);

-- New issued numeric deposit codes after authorized UAT finalization.
SELECT PRODUCT_ID, PRODUCT_FAMILY_CODE, DEPOSIT_TYPE_CODE,
       ISSUANCE_TYPE_CODE, WORK_YEAR, SERIAL_NO, PRODUCT_CODE
FROM PDL.PRODUCT_DEPOSIT_TYPE_CODE_REGISTRY
ORDER BY PRODUCT_ID;
```

## UAT and acceptance

- A **new, previously unissued** approved/current/active `SAVINGS` product with complete design governance obtains `35101405001` in year 1405 **only if that code is still available**; otherwise the next available serial is used.
- A second newly finalized SAVINGS product gets the next free serial. A CURRENT_NO_INTEREST product gets its own `2510` series independent of SAVINGS.
- Re-finalizing a product that already has a code (old OR new format) returns its existing code unchanged, without consuming another serial.
- Legacy product 33 retains `QARD_SAVINGS1405001` and the old registry remains intact.
- Issuance is blocked for unknown business type, correspondent types, mismatched version/profile type, unapproved/non-current version, and governance blockers.
- Concurrency: two different new products finalized simultaneously must not receive the same number; verify only in an isolated, authorized test schema.
- Insufficient permissions, missing migration or Oracle constraint failures must prevent finalization, not trigger silent fallback to the old R30 algorithm.
- A successful issuance marks the product `INACTIVE`; it never grants operational account opening.

**Test status:** HF5 Java policy and static source verifiers pass locally; broader HF1-HF4 / R29/R30 / FEE2 regressions pass. This is NOT evidence of successful Oracle migration, application build on Windows, or end-to-end issuing in the target environment.

## Rollback caveat

An application rollback from HF5 to HF4 is safe only **before** numeric HF5 codes have been issued. HF4 does not know the new type-based registry, so if HF5 has already issued codes, an unmodified HF4 backend would not enforce the new registry-based family immutability. A production rollback after issuance requires an independently DBA-reviewed compatibility plan. Do not drop the HF5 registry or rewrite issued codes as a rollback shortcut.

**Build note (this environment):** Maven wrapper compile could not start because the Maven 3.9.16 distribution was not cached and the container could not fetch it. Java policy compilation and non-writing regressions passed; full Maven/Angular build and Oracle E2E are to be executed on Windows.
