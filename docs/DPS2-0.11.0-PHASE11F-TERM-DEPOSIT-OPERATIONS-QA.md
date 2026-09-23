# DPS2 0.11.0 — Phase 11F Term Deposit Operations

Scope is strictly Four Deposits / DPS2, with runtime applicability only to `SHORT_TERM_DEPOSIT` and `LONG_TERM_DEPOSIT`.

## Contract
- `DEPOSIT_TERM_CONTRACT` is the operational source for term, principal, maturity and maturity instruction.
- Opening term data is idempotently backfilled into the operational contract with `SOURCE_OPENING_TERM_ID` / `SOURCE_OPENING_MATURITY_ID` traceability.
- Updating maturity instruction does not advance `START_DATE` or `MATURITY_DATE`.
- Partial withdrawal is maker/checker controlled, must remain below principal, posts through Phase 11D, and only then reduces `PRINCIPAL_AMOUNT`.
- Renewal is maker/checker controlled and cannot execute before current maturity. Actual execution updates term dates and emits `DEPOSIT_MATURITY_EVENT`.
- Conversion is limited to short/long-term products, updates `CURRENT_PRODUCT_VERSION_ID`, preserves `OPENED_PRODUCT_VERSION_ID`, and writes `DEPOSIT_ACCOUNT_PRODUCT_HISTORY`.
- Full early termination does not close the account directly. It creates Package 13 evidence/settlement calculation and hands off to Phase 11E Controlled Closure using `EARLY_TERMINATION / TERM_EARLY_TERMINATION`.
- Profit calculation remains Phase 11G. Full transaction processing remains Phase 11H; 11F uses the Phase 11D subledger posting primitive for financial impact.

## Gates
- Static: `node tools/verify-dps2-term-operations-11f.mjs`
- Oracle: `tools\apply-dps2-phase11f.cmd`
- Runtime: `node tools\runtime-dps2-phase11f-e2e.mjs`

Expected markers:
`PHASE11F_STATIC_BASELINE_PASS`, `PHASE11F_TERM_OPERATIONS_RECONCILIATION_PASS`, `PHASE11F_DB_BASELINE_PASS`, `PHASE11F_IMPLEMENTATION_PASS`, `PHASE11F_RUNTIME_E2E_PASS`.

## Runtime qualification correction — governed Term bootstrap

Phase 10F runtime qualification created SHORT_TERM_DEPOSIT and LONG_TERM_DEPOSIT accounts without `DEPOSIT_OPENING_TERM`; therefore those legacy prototype accounts cannot be converted into a truthful `DEPOSIT_TERM_CONTRACT` without fabricating term dates. Phase 11F does not fabricate those facts.

Forward contract after this correction:

- Activation of SHORT_TERM_DEPOSIT / LONG_TERM_DEPOSIT invokes `DepositTermContractProvisioningService` inside the same account-activation transaction.
- A real `DEPOSIT_OPENING_TERM` is mandatory for term-account activation.
- The operational `DEPOSIT_TERM_CONTRACT` is derived from opening term, opening maturity instruction, current Package 17 ledger principal and current product version.
- Non-term families are unaffected.
- Runtime E2E first reuses any valid funded operational term account. If none exists, it creates a new governed term opening through the public Opening APIs, including Term and Maturity Instruction, then performs settlement, readiness, activation and the 11F partial-withdrawal workflow.
- Existing legacy term accounts that never had Opening Term data are left unchanged rather than receiving invented maturity facts.

## 2026-09-23 runtime hotfix - governed Allowed Term

The bootstrap correction now also resolves a real active `DEPOSIT_PRODUCT_ALLOWED_TERM` from PDL for the selected Product Version. `ALLOWED_TERM_ID`, `TERM_CODE`, `TERM_VALUE`, and `TERM_UNIT_CODE` are propagated into `DEPOSIT_OPENING_TERM`; the maturity date is derived from that governed duration. The opening aggregate validator additionally rejects a missing/invalid `ALLOWED_TERM_ID` before persistence. See `DPS2-0.11.0-PHASE11F-ALLOWED-TERM-HOTFIX-QA.md`.

## PDL configuration reconciliation follow-up

Runtime qualification identified that the opening-eligible term Product Versions in the current prototype catalog had no governed TERM rule/allowed-term configuration. The controlled reconciliation procedure is documented in `DPS2-0.11.0-PHASE11F-PDL-TERM-CONFIG-RECONCILIATION-QA.md` and implemented by `tools/reconcile-pdl-term-config-phase11f.mjs`.
