# DPS2 0.11.0 - Phase 11G Legacy Accrual Dual-Write Compatibility

## Evidence from the deployed Oracle DDL

`DPS2.DEPOSIT_PROFIT_ACCRUAL` contains both the pre-11G accrual projection and the canonical Phase 11G projection in the same physical table.

Legacy projection:

- `PROFIT_PERIOD_ID` (now nullable for canonical rows)
- `ACCRUAL_DATE`
- `BALANCE_BASIS_AMOUNT`
- `RATE_VALUE`
- `DAY_FRACTION`
- `ACCRUAL_AMOUNT`
- `ACCRUAL_STATUS_CODE`
- `POSTING_REFERENCE`
- `CREATED_BY`

Canonical Phase 11G projection:

- `PROFIT_CONTRACT_ID`
- `ACCRUAL_FROM_DATE`
- `ACCRUAL_TO_DATE`
- `DAY_COUNT`
- `BASIS_AMOUNT`
- `ANNUAL_RATE`
- `ACCRUED_AMOUNT`
- `DAY_COUNT_BASIS_CODE`
- `CALCULATION_METHOD_CODE`
- `STATUS_CODE`
- `PROFIT_POSTING_ID`
- `CALCULATED_AT`
- `CALCULATED_BY`

## Decision

Do not relax the legacy financial columns to nullable and do not fabricate `PROFIT_PERIOD_ID`.

When the complete legacy projection is present, `DepositProfitRepository.insertAccrual` writes both projections atomically:

| Legacy | Canonical source |
|---|---|
| `ACCRUAL_DATE` | `ACCRUAL_TO_DATE` |
| `BALANCE_BASIS_AMOUNT` | `BASIS_AMOUNT` |
| `RATE_VALUE` | `ANNUAL_RATE` |
| `DAY_FRACTION` | `DAY_COUNT / 365` or `DAY_COUNT / 360` |
| `ACCRUAL_AMOUNT` | `ACCRUED_AMOUNT` |
| `ACCRUAL_STATUS_CODE` | `CALCULATED` for canonical `ACCRUED` |
| `CREATED_BY` | `CALCULATED_BY` |

At posting, canonical `STATUS_CODE/PROFIT_POSTING_ID` and legacy `ACCRUAL_STATUS_CODE/POSTING_REFERENCE` are updated together.

`PROFIT_PERIOD_ID` stays nullable because Phase 11G uses the operational `DEPOSIT_PROFIT_CONTRACT` date-range model and must not invent an identity in the older `DEPOSIT_PROFIT_PERIOD -> DEPOSIT_ACCOUNT_PROFIT_PROFILE` aggregate.

## Portability

The repository detects the physical schema through `ALL_TAB_COLUMNS`:

- 0 legacy compatibility columns -> canonical-only insert.
- all 7 compatibility columns -> dual-write insert.
- partial legacy projection -> fail fast as schema corruption/mismatch.

This supports both clean Phase 11G installations and the current evolved prototype database.
