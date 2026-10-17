# R1-D5: Acceptance Matrix for Cash Deposit/Withdrawal to Posting & General Ledger

**Baseline**: `core-banking-prototype-v0-2026-10-17-1443.zip`  
**Primary input**: `ACCOUNTING-2026-10-17.txt` (ACC, PST, LGR, RCN tables/constraints/sequences)  
**Reference pattern**: Formance Transaction + Posting (`source`, `destination`, `asset`, `amount`), implemented in existing Oracle schemas rather than deploying the Formance product.

## Business journal and balancing policy (subject to bank accounting sign-off)

| Cash operation | LGR source => destination | ACC debit | ACC credit |
|---|---|---|---|
| CASH_DEPOSIT | CASH_CLEARING => CUSTOMER_DEPOSIT | TELLER_CASH (asset) | CUSTOMER_DEPOSIT_LIABILITY |
| CASH_WITHDRAWAL | CUSTOMER_DEPOSIT => CASH_CLEARING | CUSTOMER_DEPOSIT_LIABILITY | TELLER_CASH (asset) |

GL role codes are **logical**; `ACC.ACCOUNT_ROLE_ASSIGNMENTS` resolves the approved, effective GL identifiers scoped by Book + Mapping Version + Currency + Product Version + Bank Unit. Required GL types: ASSET (normal DEBIT) and LIABILITY (normal CREDIT). Cash clearing position may be negative because it models external value source/sink, not the bank's cash asset account. Customer deposit position **must not** permit negative balance.

## UAT configuration preparation (no automatic seeds)

| Component | Exactly required for this cash slice |
|---|---|
| DPS2 | `DEPOSIT_ACCOUNT.ACCOUNT_NO`, `CURRENT_PRODUCT_VERSION_ID`, `ORG_UNIT_CODE`, posted/authorized transaction with cash details and meaningful `CASH_DESK_CODE` |
| PST rules | 2 rule codes for deposit/withdrawal with one active PUBLISHED version, valid CONTENT_HASH (64 hex), APPROVAL_REFERENCE and `event_type EQUALS CASH_DEPOSIT/CASH_WITHDRAWAL` conditions |
| PST templates | 2 published, effective versions; exactly one line per template with `CASH_PRINCIPAL` and `SAME_AS_INSTRUCTION`; source/destination role pairs as above |
| PST assignments | Approved published rule => template assignments with `ACCOUNTING_PURPOSE=CASH_PRINCIPAL` and effective date |
| PST approvals | One APPROVED/PUBLISH Maker-Checker record per template version, `PST.POSTING_TEMPLATE_VERSIONS:<version-id>` |
| ACC mappings | Two PUBLISHED mapping versions (configured codes), each with precisely two `ACCOUNTING_MAPPING_ENTRIES` of component `CASH_PRINCIPAL` and proper direction/role order |
| ACC GL | Active financial book (base currency same as transaction), chart, active GL accounts of exact classes and normal directions; published role assignments for both roles matching current account's product version/unit |
| ACC period | Exactly one OPEN fiscal period including BOOKING_DATE |
| LGR asset & ledger | Active currency asset (e.g. IRR, precision 0) and active ledger with given legal entity |
| LGR positions & bindings | `DPS2/accountNumber/CUSTOMER_DEPOSIT` to active customer position; `SYSTEM/cashDeskCode/CASH_CLEARING` to active clearing position; both same ledger, asset, globally distinguishable position codes |

No bank-approved Chart/GL/Role Assignment or seed data was present in the uploaded DDL. The latter defines **structure**, not actual active mappings, account balances or permissions. Therefore live journal generation cannot be accepted until the above data is confirmed in UAT.

## Critical transactional invariant

`DPS2.transaction.post` executes: Validate => existing DPS2 subledger postings => R1-D5 Posting Bridge => `PST` instruction/plan => `LGR` committed movement/position update => `ACC` fact/journal/projection => PST execution => DPS2 POSTED status => commit. All within the **same** Spring/Oracle transaction; R1-D5 writer has `Propagation.MANDATORY`. Kafka is not involved.

Errors in any stage roll back this unit of work; verified only at design/source level until Oracle integration is run. No credential or default GL numbers are embedded in Java code.

## Mandatory acceptance scenarios

| ID | Scenario | Expected |
|---|---|---|
| UAT-01 | cash deposit 100000 IRR, authorized by different actor | DPS2 POSTED; LGR 1 movement; ACC 1 journal/2 entries Dr Cash Cr Liability; amount exactly 100000 minor units |
| UAT-02 | withdraw 60000 IRR from same test account | DPS2 POSTED; LGR position reduces by 60000; ACC Dr Liability Cr Cash |
| UAT-03 | Read GET journal with private review token | Exactly 2 lines, `balanced=true`, `COMMITTED` and `POSTED` statuses |
| UAT-04 | Replay same `X-Idempotency-Key` on original post | Prior DPS2 result, no additional PST/LGR/ACC rows |
| UAT-05 | Reuse key with different payload | Rejected; no mutation |
| UAT-06 | Withdraw more than operational/position balance | Rejected; no POSTED status or newly committed ACC lines |
| UAT-07 | Force missing GL mapping or closed period in isolated UAT | Fail closed; DPS2 cash transaction still AUTHORIZED, no partial subledger change |
| UAT-08 | Try posting TRANSFER or DERIVED while Cash-only is enabled | Explicitly rejected before financial effect |
| UAT-09 | Try reversal of cash posting while Cash-only enabled | Explicitly rejected until double-sided reversal exists |
| UAT-10 | Kafka broker unavailable while Cash-only mode enabled | Cash journal is still transactionally committed; Event Streaming independent of cash GL journal |
| UAT-11 | Retry after forced Oracle constraint error | No duplicate PST/LGR/ACC/DPS2 financial effects |
| UAT-12 | Verify `ACC.ACCOUNT_BALANCE_PROJECTIONS` | Exactly one projection per configured GL/period/currency/dimension; debit/credit totals reconcile with entries |

## Non-goals / open bank design decisions

1. Reversal of ACC/LGR transactions (must create immutable compensating movement and opposite journal, not delete original). Current cash-only mode BLOCKS reversal; bank-grade UAT cannot sign off on entire accounting module until closed.
2. Official document numbering/Journal Batch linkage. Schema has `ACC.JOURNAL_BATCHES`, but `ACC.ACCOUNTING_TRANSACTIONS` does not reference batches; the first slice creates traceable `ACCOUNTING_TRANSACTION_IDENTIFIER`, not an official batch voucher.
3. Cash Management cash receipt verification and uniqueness across different DPS2 transactions. Cash reference is required but has no separate cross-transaction unique constraint in the supplied DDL.
4. Complete FX/base-currency translation, currency precision policy, product-specific charts/branch dimensions, subledger-to-LGR migration of opening balances, DLQ & reconciliation procedures.
5. Published configuration hashes are checked for SHA-256 shape and approval reference, not independently verified against canonical configuration contents; content attestation/immutable publication and authorization require separate hardening.
6. No Production deployment, no external Formance service API, and no Kafka-triggered financial GL commit. Real Kafka ES2 may proceed independently for audit/event delivery.

## Acceptance evidence

In the approved isolated UAT, save: build log, verifier output, setup metadata (with secrets masked), transaction ID, SQL evidence for LGR and ACC, GET journal JSON, negative test outcomes, full regression summary and expert accounting sign-off.

Acceptance status as of preparation: **SOURCE PATCH CREATED; PURE POLICY 21/21 PASS; SPRING STUB COMPILE PASS; MAVEN BUILD AND LIVE ORACLE UAT NOT YET EXECUTED**.
