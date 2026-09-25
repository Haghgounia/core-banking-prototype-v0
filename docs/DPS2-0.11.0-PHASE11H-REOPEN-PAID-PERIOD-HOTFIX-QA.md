# DPS2 0.11.0 — Phase 11H Reopen Paid Profit Period Hotfix QA

## Document trace
This hotfix remains inside canonical Step 04 — Deposit Profit Operations. It does not implement Step 05 Transaction Processing.

## Runtime defect
A MATURITY profit period backfilled from an already-posted Phase 11G account could be `PAID`. A later canonical Step 04 accrual or approved adjustment increases `PAYABLE_AMOUNT`, but the repository previously left `PERIOD_STATUS_CODE='PAID'`. Posting eligibility filtered out `PAID`, causing `Profit Period قابل پرداخت وجود ندارد` even though `PAYABLE_AMOUNT > PAID_AMOUNT`.

## Fix
- Accrual reopens any non-CLOSED period with new outstanding payable amount as `CALCULATED`.
- Posted adjustment reopens any non-CLOSED period with new outstanding payable amount as `APPROVED`.
- Payment selection is amount-driven (`PAYABLE_AMOUNT > PAID_AMOUNT`) and excludes only `CLOSED` periods.
- Migration reconciles pre-hotfix rows where `PAID` still has an outstanding balance.
- DB verifier rejects `PAID` periods with `PAYABLE_AMOUNT > PAID_AMOUNT`.

## Gate
Phase 11H remains IN_PROGRESS until DB verifier and runtime E2E both pass after this hotfix.

## Runtime qualification
The Phase 11H runtime harness first looks for an existing non-CLOSED period with `PAYABLE_AMOUNT > PAID_AMOUNT`, posts that legacy/backfilled outstanding amount, verifies canonical Payment, Period, ledger delta and idempotent replay, and emits `PHASE11H_RUNTIME_REOPENED_PERIOD_PASS`. It then continues the fresh accrual/adjustment/payment E2E flow.
