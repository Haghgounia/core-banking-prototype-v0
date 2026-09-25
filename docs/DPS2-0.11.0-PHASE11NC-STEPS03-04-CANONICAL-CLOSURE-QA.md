# DPS2 0.11.0 — Phase 11N-C — Steps 03–04 Canonical Coverage Closure QA

## Scope

Phase 11N-C closes the remaining canonical coverage gaps for Operational Step 03 (Term Deposit Operations) and Step 04 (Deposit Profit Operations). It does not introduce a new business domain or duplicate existing canonical tables.

## Source order

1. `Deposit_Account_Operations_6_RC_2026-09-22.xml`
2. `Deposit_Account_Operations_Traceability_Guide_FA_v2_2026-09-22.html`
3. `Deposit_Account_Operations_Operational_v11_RC_2026-09-22.html`
4. `DPS2-CANONICAL-ROADMAP-FA.md`

## Proven gaps closed

### Step 03

- Executed Partial Withdrawal must create a canonical Step 05 `DEPOSIT_TRANSACTION`, transaction legs and Package 17 subledger trace.
- Financial maturity instructions (`PAY_TO_ACCOUNT` / `CLOSE_AND_SETTLE`) must execute through a POSTED `MATURITY_SETTLEMENT` transaction, persist `DEPOSIT_TERM_SETTLEMENT`, update `DEPOSIT_MATURITY_EVENT.SETTLEMENT_TRANSACTION_ID`, and finish through controlled Package 16 closure.
- `RENEW_PRINCIPAL` remains a non-settlement renewal branch; `RENEW_PRINCIPAL_PAY_PROFIT` pays outstanding profit via Step 05 before renewal.
- Executed Early Termination must use a POSTED `TERM_EARLY_TERMINATION` transaction, a POSTED term settlement and controlled Package 16 closure with the same transaction reference.

### Step 04 / AD-01

Canonical ownership remains:

`DEPOSIT_ACCOUNT_PROFIT_PROFILE -> DEPOSIT_PROFIT_PERIOD -> DEPOSIT_PROFIT_ACCRUAL / DEPOSIT_PROFIT_ADJUSTMENT / DEPOSIT_PROFIT_PAYMENT`

The 11G `DEPOSIT_PROFIT_CONTRACT / DEPOSIT_PROFIT_POSTING` structures remain compatibility/runtime projection and do not replace canonical Period/Payment ownership.

- `SAME_DEPOSIT` payment retains the established direct same-account Package 17 posting path.
- `LINKED_ACCOUNT` and `CUSTOMER_SELECTED_ACCOUNT` use canonical Step 05 `PROFIT_PAYMENT` transaction processing and store `TX-<TRANSACTION_ID>` in `DEPOSIT_PROFIT_PAYMENT.POSTING_REFERENCE`.
- Maturity settlement can record outstanding profit into canonical Profit Payment using the owning maturity transaction.

## Schema decision

No new DDL is required. The Phase 11N-C migration is a fail-fast schema-contract reconciliation only and performs no business DML.

## Qualification order

`Static -> Oracle DB -> Production Build -> Runtime E2E`

Required final markers:

- `PHASE11NC_STATIC_BASELINE_PASS`
- `PHASE11NC_SCHEMA_RECONCILIATION_PASS`
- `PHASE11NC_DB_BASELINE_PASS`
- `PHASE11NC_IMPLEMENTATION_PASS`
- `PHASE11NC_RUNTIME_E2E_PASS`
- `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`

## Runtime positive branches

The 11N-C runtime creates governed term-account fixtures through the normal Opening API and validates:

1. external/customer-selected Profit Payment through Step 05;
2. Partial Withdrawal through Step 05;
3. due Maturity `PAY_TO_ACCOUNT` through Step 05 + Term Settlement + controlled Closure;
4. Early Termination through Step 05 + Term Settlement + controlled Closure.

No direct Oracle business DML is used for runtime fixture creation.


## Windows qualification finding — DB verifier compatibility hotfix

The first Windows qualification after R1 passed Static `58/58` and the schema reconciliation, but the DB verifier found one previously executed Partial Withdrawal with no `TRANSACTION_ID`. This row is historical 11F evidence, not a new 11N-C execution: 11F posted it directly to Package 17 with `POSTING_REFERENCE='TERM-PW-<PARTIAL_WITHDRAWAL_ID>'`, `SOURCE_ENTITY_TYPE='TERM_PARTIAL_WITHDRAWAL'`, and `SOURCE_ENTITY_ID=<PARTIAL_WITHDRAWAL_ID>`.

11N-C must not fabricate a retroactive `DEPOSIT_TRANSACTION` for that historical operation because the 11N-C migration is explicitly no-business-DML. The verifier therefore uses a cutover-compatible rule:

- evolved executions with `TRANSACTION_ID` must resolve to a POSTED Step 05 `TERM_PARTIAL_WITHDRAWAL` transaction;
- historical executions with null `TRANSACTION_ID` are accepted only when the original direct 11D subledger evidence is present and coherent;
- all new runtime qualification branches still require the Step 05 transaction path.

The same qualification exposed invalid Oracle `raise_application_error` codes in the R1 scripts (`-214xx`). Oracle only accepts `-20000..-20999`; R2 changes these to `-20401`, `-20402`, and `-20420` without changing business behavior.


## Windows qualification finding — R3 DB verifier syntax hotfix

The Windows rerun after R2 passed Static `60/60` and schema reconciliation, then Oracle rejected the new legacy Partial Withdrawal verifier predicate before any DB check executed. The R2 predicate closed the outer `AND (` group twice: once on the `REQUESTED_AMOUNT))))` line and again on the following `);`, producing `ORA-03048`.

R3 removes the single extra closing parenthesis only. No business rule, migration, DDL, DML, or runtime behavior changes. The legacy compatibility rule remains exactly the R2 rule: historical 11F executions require coherent direct Package 17 evidence; every evolved execution with `TRANSACTION_ID` requires a POSTED Step 05 transaction.

A new static guard extracts this verifier predicate and checks balanced parentheses so the same packaging regression fails before Oracle qualification. Static baseline after R3 is `61/61 PASS`.

## Closure rule

Final Windows qualification emitted `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS` on 2026-09-25. Phase 11N-C is `CLOSED`; Step 03 and Step 04 are `DONE`.


## Windows qualification finding — R4 historical 11H verifier compatibility

Windows R3 completed the complete 11N-C Oracle gate: Static `61/61 PASS`, schema reconciliation PASS, DB verifier `16/16 PASS`, and `PHASE11NC_IMPLEMENTATION_PASS`. Production Build then stopped in the historical Phase 11H static verifier on exactly two stale expectations: external profit destinations were expected only to *depend on* Step 05, and the UI was expected to use the old literal dependency wording.

11N-C intentionally evolved this contract: `LINKED_ACCOUNT` / `CUSTOMER_SELECTED_ACCOUNT` internal destinations now execute through `transactionService.postDerived(..., "PROFIT_PAYMENT", ...)`, while unresolved external settlement is still not fabricated. The Account Operations UI correspondingly states that these destinations execute through `Step 05 Transaction Processing`.

R4 therefore changes only verifier compatibility:
- Phase 11H accepts either its original dependency marker or the evolved actual Step 05 `PROFIT_PAYMENT` execution.
- Phase 11H UI verification accepts the original literal or the evolved `Step 05` + `Transaction` wording.
- Phase 11N-C adds regression guards requiring those historical verifier compatibility branches.

No application business logic, DDL, DML, canonical ownership, or runtime behavior is changed by R4. Local static verification after R4: Phase 11H `64/64 PASS`; Phase 11N-C `63/63 PASS`.

## R5 — Phase 11K maturity verifier compatibility

Windows R4 qualification passed the 11N-C Oracle gate (`63/63` Static, `16/16` DB, `PHASE11NC_IMPLEMENTATION_PASS`) and the historical 11H regression gate, then stopped in the historical Phase 11K static verifier on `term workflow progresses existing maturity event`. The 11K verifier expected the former fixed `PROCESSED_AT=SYSTIMESTAMP` update. 11N-C intentionally evolved this repository path to `upsertMaturityEvent(...)` with `processedSql=processed?"SYSTIMESTAMP":"NULL"`, so `WAIT_INSTRUCTION` remains pending/non-financial while executed maturity branches receive `PROCESSED_AT`. R5 makes the historical 11K verifier accept either the original fixed-update contract or the evolved update-first/upsert contract. No business logic, DDL, DML, or Runtime semantics are changed. 11N-C now guards this compatibility explicitly.



## R6 — Derived authorization repository compile contract

Windows R5 qualification passed the full 11N-C Oracle gate (`64/64` Static, `16/16` DB, `PHASE11NC_IMPLEMENTATION_PASS`) and the historical 11H/11K regression gates, then reached Java compilation. Compilation failed because the new 11N-C `postDerived(...)` path called `DepositTransactionRepository.authorization(...)` with an explicit `AUTHORIZATION_TYPE_CODE`, while the repository still exposed only the historical six-argument maker-checker method.

R6 resolves the contract at the repository boundary without weakening legacy Step 05 behavior:

- the existing six-argument method remains and delegates with `AUTHORIZATION_TYPE_CODE='MAKER_CHECKER'`;
- a seven-argument overload persists the supplied authorization type through the canonical `DEPOSIT_TRANSACTION_AUTHORIZATION.AUTHORIZATION_TYPE_CODE` column;
- the 11N-C static verifier now guards both the legacy overload and the typed derived-transaction overload.

No DDL, DML migration, business-data backfill, or runtime workflow change is introduced by R6. Semantic normalization of the authorization-type catalogue itself belongs to the later Step 05 canonical audit (11N-D); R6 only makes the already-designed 11N-C derived transaction path compile and persist its explicit authorization evidence.

## R7 — Early Termination canonical status hotfix

Windows R6 qualification passed the complete 11N-C Oracle gate (`66/66` Static, `16/16` DB, `PHASE11NC_IMPLEMENTATION_PASS`), the full Production Build, Angular build, and Maven `51/51` tests. Runtime then passed External Profit, Partial Withdrawal and Maturity, and failed only while requesting Early Termination. Service log showed `ORA-12899` because the implementation attempted to persist `STATUS_CODE='HANDED_OFF_TO_CLOSURE'` (21 chars) into `DEPOSIT_TERM_EARLY_TERMINATION.STATUS_CODE VARCHAR2(20 CHAR)`.

The canonical XML is stricter than the physical-length symptom: `DEPOSIT_TERM_EARLY_TERMINATION.STATUS_CODE` is length 20 and its documented vocabulary is `REQUESTED/APPROVED/EXECUTED/REJECTED/FAILED`. Therefore widening the Oracle column would preserve a non-canonical status and is rejected.

R7 restores the canonical state contract without DDL:

- Early Termination request remains `REQUESTED` after the related controlled Package 16 closure request is created.
- The related `DEPOSIT_ACCOUNT_CLOSURE` remains the approval source of truth. Execution is permitted only when its closure status is `APPROVED`.
- Final Early Termination execution transitions the term record directly from `REQUESTED` to canonical `EXECUTED` while writing the Step 05 settlement transaction and then executing the pre-settled closure.
- The Angular action is enabled only for `EarlyTermination.STATUS_CODE='REQUESTED'` plus `Closure.STATUS_CODE='APPROVED'`.
- The non-canonical `HANDED_OFF_TO_CLOSURE` state and the obsolete repository handoff mutation are removed.
- Static verification guards both the canonical vocabulary and the request/approval execution boundary.

No schema widening, migration DDL, business-data backfill, or new API is introduced by R7.

## R8 — Early Termination Closure Trace Correlation Hotfix

Qualification after R7 reached Runtime and failed only with `PHASE11NC_EARLY_CLOSURE_TRACE_MISSING`. The business request itself succeeded; the read projection failed to correlate the just-created Controlled Closure because it used the temporal heuristic `C.REQUESTED_AT >= E.CREATED_AT`, while `requestEarlyTermination` creates the Closure before inserting `DEPOSIT_TERM_EARLY_TERMINATION`.

R8 replaces that heuristic with the actual governed evidence relation already persisted by the workflow: `DEPOSIT_ACCOUNT_CLOSURE.APPROVAL_REQUEST_ID = DEPOSIT_TERM_EARLY_TERMINATION.APPROVAL_REQUEST_ID`, additionally scoped by account, closure type, and reason. No DDL and no business-data backfill are introduced. The DB verifier now also checks this shared-approval trace explicitly.

Expected R8 qualification baselines: `PHASE11NC_STATIC_VERIFIER_PASS=70`, `PHASE11NC_DB_VERIFIER_PASS=17`, followed by all four Runtime positive branches and `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`.


## R9 — Early Termination Status-History Reason Reconciliation

Runtime R8 reached Early Termination execute and exposed a canonical cross-package constraint mismatch: Package 13/16 requires `REASON_CODE=TERM_EARLY_TERMINATION` for full early termination closure, while `DEPOSIT_ACCOUNT_STATUS_HISTORY.REASON_CODE` in Package 01B omitted that documented reason from its CHECK vocabulary. R9 preserves the exact documented reason and minimally reconciles `CHK_DEPOSIT_ACCOUNT_STATUS_HISTORY_REASON_CODE`; no business rows are inserted/updated/deleted and no status-history rows are fabricated.

Qualification expectation after R9: Static and DB verifier must prove the reconciled reason contract before Runtime Early Termination executes through Step 05 and controlled Package 16 closure.


## Final qualification and closure — 2026-09-25

The complete Windows qualifier passed all required gates in canonical order:

- Static: `72/72 PASS`;
- schema reconciliation: PASS, including the documented `TERM_EARLY_TERMINATION` status-history reason CHECK reconciliation;
- DB verifier: `18/18 PASS`;
- `PHASE11NC_IMPLEMENTATION_PASS`;
- backend production compile/build: PASS;
- Angular production build: PASS;
- Maven: `51/51 PASS`;
- Runtime External Profit: `8:TX-39`;
- Runtime Partial Withdrawal: `5:TX-40`;
- Runtime Maturity: `10:TX-41`;
- Runtime Early Termination: `4:TX-42:CLOSURE-9`;
- `PHASE11NC_RUNTIME_E2E_PASS`;
- `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`.

Closure decision:

- Phase `11N-C = CLOSED`;
- Document `Step 03 = DONE`;
- Document `Step 04 = DONE`;
- highest verified runtime milestone moves to Phase 11N-C;
- next authorized scope is Phase 11N-D, Steps 05–10 canonical audit/regression.

Step 08 remains `PARTIAL` until 11N-D completes its own action-by-action audit even though the financial maturity execution path was successfully exercised by 11N-C.
