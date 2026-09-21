# Release Notes — 0.10.0 FINAL

## Deposit Opening Operational v5

- Added the Phase 10 Operational Opening v5 schema foundation and semantic FK/index reconciliation.
- Expanded the Opening aggregate for risk, expected activity, joint-account, obligation, funding allocation and activation metadata.
- Implemented guarded account lifecycle ordering: Create Account → Settlement → Activation Readiness → Activate.
- Account creation remains `PENDING_ACTIVATION`; Activation requires a successful `READY` decision.
- Expired PASS evidence is rejected by Readiness.
- Angular Opening Wizard is aligned with Operational v5: CIF Party search, Product-driven Withdrawal Media, cheque quantity, ACTIVE funding-account selection/ownership verification, Obligation coverage and explicit Create-Gate evidence.
- Fee/Tax/Service-Charge obligations are not fabricated while external service contracts are absent.
- Operational Batch is intake/orchestration only for the approved legal basis/family and does not bypass the individual Opening workflow.
- Existing Phase 9 account servicing remains intentionally limited; new Hold/Block/Dormancy/Reactivation capabilities are not introduced by 0.10.0.

## Phase 10E / 10F final qualification

- Four-family real runtime E2E qualification completed for:
  - `QARD_SAVINGS`
  - `CURRENT_ACCOUNT`
  - `SHORT_TERM_DEPOSIT`
  - `LONG_TERM_DEPOSIT`
- Every family passed `Validate → Persist → Create Account → Settlement → Readiness → Activate`.
- Final runtime marker: `PHASE10F_RUNTIME_E2E_PASS`.

## Runtime hotfixes absorbed into the final baseline

The final source package no longer depends on keeping separate ad-hoc runtime ZIPs. It incorporates:

- semantic Phase 7 idempotency/index detection;
- Phase 10 semantic covering-index rerun handling;
- full Operational v5 runtime reference allow-list;
- CASH funding / cash-management transaction reference contract;
- final metadata-aware Phase 10F fixture preparation;
- `CREATED_AT DEFAULT SYSTIMESTAMP` reconciliation;
- final created-account ID integration-reference constraint;
- canonical FUND_ALLOC `ALLOCATED_AMOUNT NUMBER(19,4)` plus safe legacy-column cleanup;
- canonical Opening Check result statuses `PASS`, `FAIL`, `PENDING`, `WAIVED`, `NOT_APPLICABLE`;
- qualification-tool evidence-validity hardening.

## Final closure artifacts

- `release/FINAL-MIGRATION-MANIFEST-0.10.0.md`
- `release/FINAL-FREEZE-0.10.0.md`
- `release/FINAL-BASELINE-HASHES-0.10.0.sha256`
- `database/oracle/dps2/verification/0.10.0-final-verifier.sql`
- `tools/verify-final-closure-0.10.0.mjs`
- `tools/final-verify-0.10.0.cmd`
- `docs/DPS2-0.10.0-PHASE10F-FINAL-RUNTIME-QUALIFICATION-QA.md`

No new business feature should be added to this release after the final closure marker. New servicing or domain work belongs to the next version.
