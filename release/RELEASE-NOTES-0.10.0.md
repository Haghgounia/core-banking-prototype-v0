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
