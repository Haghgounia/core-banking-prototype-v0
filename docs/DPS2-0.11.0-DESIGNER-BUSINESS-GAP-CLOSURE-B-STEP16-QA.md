# DPS2 0.11.0 — Designer Business Gap Closure B — Step 16 Nostro/Vostro

Date: 2026-09-27

## Scope

This closure is restricted to the business behavior explicitly required by the supplied Deposit Account Operations RC and Traceability Guide for Step 16. No unrelated feature is introduced.

Required business flow:

`Create DEPOSIT_ACCOUNT master -> CORRESPONDENT_ACCOUNT_EXTENSION -> CORRESPONDENT_RECONCILIATION_PROFILE -> Reconciliation Run/Item -> completion trace`

## Implemented contract

1. A Nostro/Vostro registration creates a new, independent `DPS2.DEPOSIT_ACCOUNT` master. It does not repurpose the currently selected retail/customer account.
2. The product version is resolved from governed `PDL.CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE` by `ACCOUNT_TYPE_CODE` and `SETTLEMENT_CURRENCY_CODE`.
3. No synthetic Opening Request is fabricated. Correspondent account masters use `OPENING_REQUEST_ID = NULL`; account search/detail joins therefore tolerate an absent opening request.
4. The new account is created ACTIVE with zero opening/ledger/available balance and receives the canonical `DEPOSIT_ACCOUNT_BALANCE` infrastructure through `DepositBalanceService.initializeAccount`.
5. `CORRESPONDENT_ACCOUNT_EXTENSION` and `CORRESPONDENT_RECONCILIATION_PROFILE` are created only after the account master exists.
6. Designer UI exposes the RC fields: account type, correspondent party, BIC, external account reference, settlement currency, statement source, reconciliation frequency, and tolerance.
7. Designer UI no longer asks for a manually entered reconciliation run id. `Run Reconciliation` creates a real Step 14 run and item, then completes the correspondent reconciliation trace.
8. Create is idempotent under operation `CORRESPONDENT_ACCOUNT_CREATE` and returns the already-created account on an exact replay.

## Product Builder dependency

Step 16 intentionally does not create or seed a correspondent product profile. At least one matching governed row must already exist in `PDL.CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE` for the requested NOSTRO/VOSTRO type and settlement currency.

If no matching profile exists, runtime qualification fails with:

`STEP16_NO_GOVERNED_CORRESPONDENT_PRODUCT_PROFILE`

This is a business/configuration readiness failure, not a reason to fabricate seed data.

## Static verification

Run:

```bat
node tools\verify-dps2-designer-step16-correspondent-account.mjs
```

Expected:

```text
DESIGNER_STEP16_STATIC_PASS=41
DESIGNER_STEP16_STATIC_FAIL=0
DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_STATIC_PASS
```

## Runtime qualification

Run on the Windows/Oracle environment:

```bat
cd /d D:\Projects\core-banking-prototype-v0
set "CORE_BANKING_ORACLE_CONNECT=SYSTEM/Oracle123@//10.1.60.51:1522/FREEPDB1"
tools\qualify-dps2-designer-step16.cmd
```

Expected final marker:

`DPS2_DESIGNER_STEP16_QUALIFICATION_PASS`

The runtime proves:

- governed PDL product-profile resolution;
- creation of a new account master;
- persisted correspondent extension/profile;
- account read/search visibility without a fake opening request;
- zero canonical balance initialization;
- real reconciliation run and item;
- correspondent reconciliation completion trace.

## Final qualification integration

Both `build-production.cmd` and `tools\qualify-dps2-phase11nk-final.cmd` now include the Step 16 static gate. The final qualifier also executes the positive Step 16 runtime flow, so the historical `DEFERRED_NO_EXISTING_CONFIG` path can no longer silently qualify Step 16 as designer-ready.
