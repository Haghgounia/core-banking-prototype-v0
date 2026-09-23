# DPS2 0.11.0 - Phase 11G Legacy PROFIT_PERIOD_ID Hotfix QA

## Context

Runtime qualification reached governed pricing resolution, account bootstrap, and operational Profit Contract creation, then failed on the first accrual insert because the Oracle table `DPS2.DEPOSIT_PROFIT_ACCRUAL` contained a legacy mandatory column `PROFIT_PERIOD_ID` that is not part of the canonical Phase 11G repository contract.

The current 11G accrual model is date-range based (`ACCRUAL_FROM_DATE`, `ACCRUAL_TO_DATE`, `DAY_COUNT`) and `DepositProfitRepository.createAccrual` intentionally does not fabricate or write a period identifier.

## Reconciliation policy

The migration now checks whether `PROFIT_PERIOD_ID` exists.

- If it does not exist: no action.
- If it exists and is already nullable: no action.
- If it exists as `NOT NULL` and `DEPOSIT_PROFIT_ACCRUAL` is empty: relax the obsolete legacy `NOT NULL` requirement.
- If it exists as `NOT NULL` and accrual rows already exist: fail with `ORA-21182` and require explicit legacy-period mapping. No financial/period identity is fabricated.

## Verification

The Phase 11G DB verifier now asserts that a legacy `PROFIT_PERIOD_ID` cannot remain mandatory and block canonical accrual inserts.

Static qualification after the hotfix:

```text
PHASE11G_STATIC_VERIFIER_PASS=86
PHASE11G_STATIC_VERIFIER_FAIL=0
PHASE11G_STATIC_BASELINE_PASS
```
