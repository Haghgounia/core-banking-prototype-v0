# DPS2 0.11.0 - Phase 11F Allowed Term Runtime Bootstrap Hotfix

## Problem observed

The Phase 11F runtime E2E correctly rejected legacy active term-family accounts that do not have an operational `DEPOSIT_TERM_CONTRACT`, then attempted to bootstrap a governed term account through the public Deposit Opening APIs. The bootstrap payload omitted `DEPOSIT_OPENING_TERM.ALLOWED_TERM_ID` while the DPS2 opening schema requires that column.

Observed failure:

`REQUIRED_DATABASE_VALUE_MISSING: ALLOWED_TERM_ID`

## Correction

1. `tools/runtime-dps2-phase11f-e2e.mjs` no longer fabricates a fixed 30-day term.
2. The runtime bootstrap resolves the product's active term configuration from PDL through the public Product Builder API:
   - `DEPOSIT_PRODUCT_TERM_RULE` filtered by Product Version.
   - `DEPOSIT_PRODUCT_ALLOWED_TERM` filtered by the selected `TERM_RULE_ID`.
3. The opening payload now carries the governed `ALLOWED_TERM_ID`, `TERM_CODE`, `TERM_VALUE`, and `TERM_UNIT_CODE` and derives `MATURITY_DATE` from that governed duration.
4. `DepositOpeningAggregateService` now fails fast when a term payload omits or supplies an invalid `ALLOWED_TERM_ID`, instead of allowing the request to reach a database NOT NULL failure.
5. If legacy term accounts point to Product Versions with no term configuration, the runtime now searches the full governed PDL product catalog for an opening-eligible SHORT_TERM_DEPOSIT/LONG_TERM_DEPOSIT Product Version with a usable TERM rule and allowed term.
6. The Phase 11F static verifier asserts these protections.

## Verification performed in package workspace

- `node --check tools/runtime-dps2-phase11f-e2e.mjs` - PASS
- `node tools/verify-dps2-term-operations-11f.mjs` - `60 PASS / 0 FAIL`
- Runtime script exercised against a local HTTP contract mock covering legacy-account skip, PDL term lookup, governed opening bootstrap, activation, maturity-instruction update, maker/checker partial withdrawal, and ledger/principal delta - `PHASE11F_RUNTIME_E2E_PASS`

## Environment limitation

A Maven package rebuild was attempted but the isolated build environment could not download Maven 3.9.16 from Maven Central. The Node/runtime hotfix is independently executable against the already-running 0.11.0 application. The Java source fail-fast improvement will take effect after the normal project Maven/package build in the target development environment.

## Target runtime gate

From the project root on Windows:

```bat
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11f-e2e.mjs
```

Expected final marker:

`PHASE11F_RUNTIME_E2E_PASS`
