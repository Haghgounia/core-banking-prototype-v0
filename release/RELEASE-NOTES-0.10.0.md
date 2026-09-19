# Release Notes — 0.10.0

## Deposit Opening Operational v5 Alignment

- Added Phase 10A Oracle foundation for Operational Opening v5 and semantic FK reconciliation.
- Added Phase 10B runtime alignment: pre-settlement Funding Plans can keep `ATTEMPT_AT` null until an actual settlement attempt.
- Extended the Opening aggregate with risk, expected-activity, joint-account and activation metadata.
- Converted Funding persistence to a collection and added Obligation / Funding Allocation persistence.
- Expanded Opening Check persistence with phase, blocking scope, recheck and validity metadata.
- Added a hard Create Gate recheck immediately before Account creation.
- Account creation remains `PENDING_ACTIVATION` and sets Opening activation status to `PENDING_READINESS`.
- Added separate Settlement and Activation Readiness endpoints; Activation requires `READY`.
- Expired PASS evidence is rejected by the readiness evaluator.
- Phase 10D aligns the Angular Opening Wizard with Operational v5: CIF Party search, Product-driven Withdrawal Media, cheque quantity, live ACTIVE funding-account selection/ownership verification, Obligation coverage summary and explicit Create-Gate evidence capture.
- The UI no longer treats required external Compliance/Inquiry checks as automatically PASS.
- Fee/Tax/Service-Charge obligations are not fabricated; only currently supported obligations are materialized until the relevant service contracts are connected.
- Digital channels route the prototype org unit to the configured virtual branch code `0205`.
- Operational v5 Batch is restricted to `GOV_EMPLOYEE_SAVINGS_1376` / `QARD_SAVINGS`; direct group processing/activation is disabled.
- Existing Phase 9 `ACTIVE -> CLOSED` servicing remains unchanged.


## Final Phase 10E / 10F alignment
- Batch Opening is now an intake/orchestration flow for the legal `GOV_EMPLOYEE_SAVINGS_1376` / `QARD_SAVINGS` exception only.
- Every valid Batch Item is completed through an independent Opening aggregate and the standard Create -> Settlement -> Readiness -> Activate sequence.
- Added runtime E2E qualification harness covering QARD_SAVINGS, CURRENT_ACCOUNT, SHORT_TERM_DEPOSIT and LONG_TERM_DEPOSIT without fabricated external evidence.

## Release-candidate hardening
- Updated legacy Phase 6 and Phase 10D regression guards to validate the current Operational v5 Batch contract instead of obsolete direct Batch Process/Activate UI actions.
- Strengthened project-root hygiene: unexpected root files now fail release-layout verification, and Phase 10D patch artifacts live under `docs/patches`.
- Windows/Linux production builds and clean packaging now require the Phase 10E/10F static gate (30/30) before compile/archive progression.
- Final release status is intentionally not claimed until the four-family Phase 10F Oracle runtime harness returns `PHASE10F_RUNTIME_E2E_PASS` in the target environment.

