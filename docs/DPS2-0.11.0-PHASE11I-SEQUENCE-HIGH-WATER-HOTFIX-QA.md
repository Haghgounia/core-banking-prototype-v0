# DPS2 0.11.0 Phase 11I — Step 05 Sequence High-Water Hotfix QA

## Problem
Runtime qualification failed at `POST /api/v1/deposit-accounts/{accountId}/transactions` with HTTP 500 before a transaction could be initiated.

Phase 11I originally treated an existing Oracle sequence as valid when it merely existed. That is insufficient for an evolved schema because Step 05 tables already contain rows from earlier opening/runtime flows. An existing sequence can therefore have a `NEXTVAL` that is not greater than the current primary-key maximum.

## Fix
The 11I reconciliation now checks every Step 05 sequence against `MAX(primary-key)`.

For an existing sequence it obtains the current `NEXTVAL`; when it is behind the table high-water mark, it temporarily changes `INCREMENT BY`, advances the sequence above the current maximum, then restores `INCREMENT BY 1`.

Covered sequences:

- `SEQ_DEPOSIT_TRANSACTION`
- `SEQ_DEPOSIT_TRANSACTION_VALIDATION`
- `SEQ_DEPOSIT_TRANSACTION_AUTHORIZATION`
- `SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY`
- `SEQ_DEPOSIT_TRANSACTION_LEG`
- `SEQ_DEPOSIT_TRANSFER_DETAIL`
- `SEQ_DEPOSIT_CASH_TRANSACTION_DETAIL`
- `SEQ_DEPOSIT_TRANSACTION_REVERSAL`

The DB verifier consumes one `NEXTVAL` for each sequence and asserts that it is greater than the corresponding table maximum. Consuming a value is intentional and safe for a sequence.

## Qualification
Required gates after applying this overlay:

1. `PHASE11I_STATIC_VERIFIER_PASS=59`
2. `PHASE11I_STATIC_VERIFIER_FAIL=0`
3. Phase 11I Oracle reconciliation PASS
4. Phase 11I DB verifier includes eight sequence high-water PASS checks and `FAIL=0`
5. Production build PASS
6. `PHASE11I_RUNTIME_E2E_PASS`

Phase 11I remains `IN_PROGRESS` until the runtime gate passes.
