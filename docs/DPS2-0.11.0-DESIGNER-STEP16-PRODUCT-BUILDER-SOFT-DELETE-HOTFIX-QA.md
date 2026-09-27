# DPS2 0.11.0 - Designer Step 16 Product Builder Soft Delete Hotfix QA

## Scope
Fix the Step 16 qualification fixture failure when creating the QA-scoped correspondent products through the official Product Builder API.

Observed runtime failure:

`PRODUCT_BUILDER_VALIDATION_FAILED: Required column is missing: IS_DELETED`

## Root cause
`PDL.PRODUCT.IS_DELETED` is a soft-delete infrastructure column. Product Builder already uses it to hide deleted rows and to implement logical delete (`IS_DELETED = 1`), but insert validation treated it as an ordinary required business field. The Product Workspace intentionally does not expose this infrastructure field, so both the real Product Builder create flow and the Step 16 qualification fixture could fail when the database column is NOT NULL without a database default.

## Correction
- Mark `IS_DELETED` as system-managed in `PdlProductBuilderRepository`.
- On insert, when a supported PDL table has `IS_DELETED`, persist `0` automatically.
- Preserve existing logical delete behavior (`IS_DELETED = 1`).
- Keep the Step 16 qualification fixture on the official Product Builder REST API; no direct Oracle DML, migration seed, or fabricated correspondent business data was introduced.
- Extend static guards so the insert default and system-managed contract cannot regress silently.

## Verification
Expected static markers after this hotfix:

- `PDL unified product builder static verification: OK`
- `DESIGNER_STEP16_STATIC_PASS=65`
- `DESIGNER_STEP16_STATIC_FAIL=0`
- `DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_STATIC_PASS`
- `DESIGNER_BUSINESS_GAP_CLOSURE_A_PASS=76`
- `PHASE11M_STATIC_VERIFIER_PASS=92`
- `PHASE11N_RAPID_STATIC_VERIFIER_PASS=39`

## Runtime qualification
Run:

```bat
cd /d D:\Projects\core-banking-prototype-v0
set "CORE_BANKING_ORACLE_CONNECT=SYSTEM/Oracle123@//10.1.60.51:1522/FREEPDB1"
tools\qualify-dps2-designer-step16.cmd
```

The Product Builder fixture must create or reuse the two QA-scoped governed profiles and Step 16 must then positively exercise both NOSTRO/USD and VOSTRO/EUR.
