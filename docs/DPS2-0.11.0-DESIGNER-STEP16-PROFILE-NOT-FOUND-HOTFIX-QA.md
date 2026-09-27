# DPS2 0.11.0 - Designer Step 16 Product Profile Not Found Runtime Hotfix

## Trigger

Designer Step 16 qualification reached the real runtime after successful static verification, backend build, Angular build and 51/51 tests, but the first correspondent-account create attempt returned:

`500 / UNEXPECTED_ERROR`

The Step 16 service intentionally rejects a NOSTRO/VOSTRO + settlement-currency combination when no governed row exists in `PDL.CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE`. That rejection was implemented with `IllegalArgumentException`. The global exception handler had no dedicated mapping for that exception, therefore the expected business/configuration miss was rendered as an unexpected 500 response. The runtime harness consequently could not distinguish "no profile for this combination" from a genuine server failure and stopped on the first candidate.

## Fix

- Added `CorrespondentProductProfileNotFoundException` carrying the requested account type and settlement currency.
- Added a dedicated ProblemDetail mapping:
  - HTTP `409 CONFLICT`
  - `errorCode = CORRESPONDENT_PRODUCT_PROFILE_NOT_FOUND`
- Step 16 creation now throws the dedicated exception only when the governed PDL profile is absent.
- Runtime qualification continues to the next NOSTRO/VOSTRO + currency candidate only for that exact 409/errorCode pair.
- Any other 4xx/5xx response still fails qualification immediately; database/runtime errors are not suppressed.

## Static qualification

Expected marker:

`DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_STATIC_PASS`

Current static result after the hotfix:

- `DESIGNER_STEP16_STATIC_PASS=49`
- `DESIGNER_STEP16_STATIC_FAIL=0`

Regression guards retained:

- Business Gap Closure A: `76/76`
- Phase 11M Wave D: `92/92`

## Runtime expectation

Re-run:

```bat
cd /d D:\Projects\core-banking-prototype-v0
set "CORE_BANKING_ORACLE_CONNECT=SYSTEM/Oracle123@//10.1.60.51:1522/FREEPDB1"
tools\qualify-dps2-designer-step16.cmd
```

Successful runtime ends with:

`DPS2_DESIGNER_STEP16_QUALIFICATION_PASS`

If no governed correspondent product profile exists for any supported combination, runtime ends with the explicit non-fabrication marker:

`STEP16_NO_GOVERNED_CORRESPONDENT_PRODUCT_PROFILE`

That condition requires governed Product Builder configuration; the qualifier must not fabricate business seed data.
