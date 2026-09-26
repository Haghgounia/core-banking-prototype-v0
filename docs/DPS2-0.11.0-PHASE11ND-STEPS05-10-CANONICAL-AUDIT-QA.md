# DPS2 0.11.0 — Phase 11N-D — Steps 05–10 Canonical Regression / Audit

## Status

`IN_PROGRESS` until Windows emits `DPS2_PHASE11ND_STEPS05_10_QUALIFICATION_PASS`.

## Canonical scope

- Step 05 — Transaction Processing: Package 03 + 17 + 01C + 15 + 07 + 00.
- Step 06 — Statements: Package 14 + 03 + 17.
- Step 07 — Account Limits & Restrictions: Package 15 + 00.
- Step 08 — Closure & Maturity: Package 16 + 01A + 01B + 01C + 13 + 03 + 17 + 00.
- Step 09 — Account Services: Package 05.
- Step 10 — Party Access & Payment Instruments: Package 06.

Source precedence: XML → Traceability Guide → Operational HTML → Canonical Roadmap → this QA.

## Coverage audit

| Step | Audit result | 11N-D action |
|---|---|---|
| 05 | 11I covers validation, authorization, lifecycle, legs, cash/transfer detail, posting and reversal | Regression only |
| 06 | 11K covers template/version, request, header/items, reproducible balances and delivery | Regression only |
| 07 | 11K covers limit/usage and non-destructive restriction lifecycle | Regression only |
| 08 | Real gap: generic positive Closure bypasses Step05; settlement item has canonical `TRANSACTION_ID` but 11E leaves it null | Fix in 11N-D |
| 08 | Real gap: approval-governed Closure/Reopening status-history rows omit canonical `APPROVAL_REQUEST_ID` | Fix in 11N-D |
| 09 | 11L covers all five actions exposed by Traceability Guide/Operational HTML | Regression only |
| 10 | 11L covers all five documented actions and active-party validation | Regression only |

`DEPOSIT_ACCOUNT_SERVICE_SUBSCRIPTION` remains a model table without an independent Operational HTML action; 11N-D does not invent a new action for it.

## Step 08 resolution

1. Positive generic Closure uses internal Step05 `ACCOUNT_CLOSURE_SETTLEMENT` transaction.
2. Transaction legs are balanced: deposit source DEBIT + settlement reference CREDIT. External settlement remains a non-deposit leg boundary; no external bank integration is fabricated.
3. Deposit source leg posts through Package17 with transaction/leg trace.
4. `DEPOSIT_ACCOUNT_CLOSURE_SETTLEMENT_ITEM.TRANSACTION_ID` stores the owning transaction.
5. `DEPOSIT_ACCOUNT_CLOSURE.SETTLEMENT_TRANSACTION_REFERENCE` stores `TX-<id>`.
6. Maturity/Early-Termination pre-settled closures project their existing `TX-<id>` into closure settlement items when such items exist.
7. Closure and Reopening status history persist the same `APPROVAL_REQUEST_ID` used by the governed workflow.
8. Historical direct 11E closure postings are preserved; no fabricated transaction backfill is permitted.
9. Closure-to-Step05 child idempotency keys are bounded to the Step05 80-character contract, including long parent closure keys.

## Qualification gates

1. Static 11N-D with an explicit scope guard: any reference from the 11N-D qualifier to full 11L apply/runtime is a failure.
2. Oracle regression: Step05 (`11I`) + Steps06–08 (`11K`) + scoped 11N-D Steps05–10 audit. The 11N-D migration reconciles only Step09–10 sequences and never touches Step11–13 objects.
3. Oracle execution prefers local SQL*Plus when installed; otherwise it uses the project's Oracle JDBC driver against `CORE_BANKING_ORACLE_CONNECT`. Docker is not required for a remote Oracle database.
4. Production backend/frontend build and Maven tests.
5. Runtime Step05 (`11I`).
6. Runtime Steps06–08 (`11K`).
7. Dedicated Runtime Steps09–10 only (`runtime-dps2-phase11nd-steps09-10-e2e.mjs`); it contains no Step11/12/13 actions. Delegation is exercised only when a second active account party already exists and is otherwise explicitly deferred rather than fabricated.
8. Runtime controlled positive-balance Closure/Reopening (`11E` evolved): must prove Step05 transaction, settlement-item transaction ID, source subledger trace and approval-linked status history.
9. Final marker: `DPS2_PHASE11ND_STEPS05_10_QUALIFICATION_PASS`.

## R1 scope-drift correction

The first 11N-D R1 qualifier incorrectly called the full 11L Oracle/runtime qualification even though 11L owns Steps09–13. This was detected before any Oracle script ran in the user's Windows execution: Step05 static passed, then the run stopped while attempting the old Docker fallback. R2 removes full 11L from 11N-D completely. No Step11–13 migration/runtime belongs to 11N-D.

Step 08 becomes `DONE` and Phase 11N-D becomes `CLOSED` only after the final Windows marker.


## R3 — Historical pre-settled Closure compatibility + JDBC diagnostics

Windows R2 proved the remote Oracle path is correct: 11I and 11K static/DB gates passed through JDBC against `CORE_BANKING_ORACLE_CONNECT`, and 11N-D reached its scoped DB verifier. The verifier then raised `ORA-20439`, but the JDBC runner discarded `DBMS_OUTPUT` on exception, hiding the exact failing invariant.

The audit identified a deterministic historical compatibility gap: successful 11N-C maturity/early-termination closures created before 11N-D already have an owning `DEPOSIT_TRANSACTION` referenced by `DEPOSIT_ACCOUNT_CLOSURE.SETTLEMENT_TRANSACTION_REFERENCE='TX-<id>'`, while their already-settled `DEPOSIT_ACCOUNT_CLOSURE_SETTLEMENT_ITEM.TRANSACTION_ID` remains null because 11N-D had not yet projected the transaction id into that item. These records must not be backfilled or treated as corrupt.

R3 therefore:

1. preserves the new 11N-D rule for all new closures (`SETTLEMENT_ITEM.TRANSACTION_ID` required/projected by implementation);
2. accepts historical 11N-C pre-settled rows only when the closure header resolves to a real POSTED `MATURITY_SETTLEMENT` or `TERM_EARLY_TERMINATION` transaction and that transaction reaches the same source account through transaction leg + subledger evidence;
3. retains the exact legacy 11E direct Package17 compatibility predicate;
4. performs no historical business DML/backfill;
5. drains `DBMS_OUTPUT` before a JDBC verifier exception is rethrown so any future failing check is visible in Windows qualification logs.

Phase 11N-D remains `IN_PROGRESS` until the full Windows final marker is emitted.
