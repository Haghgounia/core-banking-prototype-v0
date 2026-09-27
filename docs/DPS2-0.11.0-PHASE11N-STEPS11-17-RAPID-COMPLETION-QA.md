# DPS2 0.11.0 — Phase 11N Steps 11–17 Rapid Completion QA

## Status

`IMPLEMENTED / QUALIFICATION_DEFERRED_TO_11N_K`

This delivery intentionally defers repeated Oracle/Production-Build/Runtime qualification to the single final Phase 11N-K gate. No Step 11–17 slice is declared CLOSED by this patch.

## Canonical scope

- Step 11: Regulatory Restriction / Compliance / Reserve Requirement / Reserve Position / Regulatory Report Item trace.
- Step 12: governed Fee Rule/Tier consumption, Pricing Override, Fee Override Maker/Checker, Fee Assessment, Pricing Package/Benefit/Enrollment, Profitability.
- Step 13: Tax Exemption / Calculation / Adjustment approval / Liability / Payment / Certificate / Tax Reconciliation.
- Steps 14–15: retain the qualified Wave-D Reconciliation / Suspense / Discrepancy / Exception / Assignment / RCA / Correction behavior; only REVERSAL owns financial execution through Step 05.
- Step 16: Correspondent Extension/Profile plus reconciliation completion from an already COMPLETED Step-14 run for the same account. The canonical extension status remains READY/SUSPENDED/CLOSED; reconciliation completion is recorded in `LAST_RECONCILED_AT`.
- Step 17: governed Reward Program/Eligibility consumption, Enrollment, audited Draw/Entry/Winner registration, and Prize Payment through the Step-05 derived transaction `REWARD_PRIZE_PAYMENT`.

## Bounded-context rules retained

- Regulatory Rule, Fee Rule/Tier, Pricing Package, Tax Rule and Reward Program are governed configuration and are not authored by Account Operations.
- External correspondent settlement network remains outside the deposit-account bounded context.
- Tax Liability/Payment/Reconciliation are projected through tax types actually referenced by the account's governed Tax Calculations; no synthetic liability/payment is created.
- Lottery Draw requires explicit random-seed and audit references. Account Operations does not calculate a random winner; it only persists an audited winner result supplied to the operation.
- Prize payment is a balanced Step-05 transaction with an external funding leg and a CREDIT leg to the winning deposit account, and the winner stores `PAYMENT_TRANSACTION_ID`.

## Database change

`database/oracle/dps2/migrations/0.11.0-phase11n-steps11-17-rapid-completion.sql`

The migration contains no business INSERT/UPDATE/DELETE/MERGE. It asserts canonical tables and reconciles only operational sequence high-water marks needed by the new commands.

## Deferred final qualification

The final 11N-K qualification must run, in one pass:

1. Static gates for 00–17, including this Rapid Completion verifier.
2. Oracle reconciliation and DB verifiers.
3. One Production Build.
4. Runtime matrix across the canonical slices.
5. Final cross-step trace checks for Approval, Idempotency, Transaction, Subledger, Balance and source-key coherence.

Until that final marker exists, Steps 08 and 11–17 remain IMPLEMENTED rather than DONE/CLOSED.


### Post-R1 final-qualification compatibility hotfix

- Historical 11N-D static assertions were updated to accept the post-Rapid-Completion roadmap state while still enforcing the original Steps 05-10 scope.
- Step 08 and Steps 11-17 remain `IMPLEMENTED` until 11N-K passes; they are not prematurely marked DONE/CLOSED.
- `tools/run-oracle-sql.cmd` is JDBC-first by default for the remote Oracle topology. SQL*Plus is now opt-in only via `CORE_BANKING_ORACLE_CLIENT=SQLPLUS`; Docker is not used.
- This hotfix changes no banking business DML and no canonical ownership boundary.
