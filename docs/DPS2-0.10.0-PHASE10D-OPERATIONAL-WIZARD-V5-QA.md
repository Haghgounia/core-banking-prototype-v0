# DPS2 0.10.0 — Phase 10D Operational Wizard/UI v5 QA

## Scope
Angular alignment of the Four Deposits Opening Wizard with the approved Operational v5 interaction model, on top of the already-qualified Phase 10A/10B Oracle and backend foundation.

## Implemented
- CIF Party search and selection; Opening persists Party ID/role/share only.
- Product-driven Withdrawal Media; Payment Instrument issuance remains a separate opening-time request.
- Cheque media requires requested leaf quantity.
- Digital channel org-unit routing to the prototype virtual branch (`0205`).
- Account-based Funding uses the existing Deposit Account Operations API to list ACTIVE accounts and verifies ownership against Account 360 before accepting the source.
- `SOURCE_VERIFICATION_REFERENCE` records the internal Account Operations verification reference rather than a fabricated evidence token.
- Explicit Obligation summary and Funding coverage gap. Only `INITIAL_BALANCE` is materialized until Fee/Tax contracts are connected.
- Create-Gate external evidence references for Identity, Mobile Ownership, Legal Capacity, KYC/CDD, PEP/Sanctions, Account Count, Inquiry Bundle and Sharia profile. Required external checks no longer auto-PASS.
- Final Compliance Activation evidence supports `VALID_UNTIL`; optional Account Opened SMS evidence is forwarded to readiness evaluation.
- Runtime sequence remains Opening → Account(PENDING_ACTIVATION) → Settlement → Readiness → Activate.
- Batch direct processing/group activation remains disabled by v5 policy.

## Verification
- Phase 10D static verifier: 25/25 PASS.
- Complete project build script: all static/project guards PASS through Phase 10D; Maven Wrapper download blocked by the isolated environment.
- Angular compile not executed in this environment because `frontend/node_modules` is absent.

## Runtime qualification still required
On the user's Windows environment run `build-production.cmd`, then execute E2E scenarios for all four deposit families.
