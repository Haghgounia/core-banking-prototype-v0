# DPS2 0.11.0 — Phase 11L Wave C QA

## Scope
Operational Steps 09–13 from the canonical Trace Guide: account services, party/access, regulatory/compliance, pricing and tax.

## Boundaries
- No Regulatory/Fee/Tax policy rule is fabricated by migration or runtime.
- Compliance Evaluation requires an ACTIVE governed `DEPOSIT_REGULATORY_RULE`.
- Tax Calculation requires an ACTIVE governed `DEPOSIT_TAX_RULE` and must trace to a Profit Payment or Transaction.
- Pricing Override uses `DEPOSIT_OPERATION_APPROVAL_REQUEST`; maker and approver must differ and the override remains `PENDING_APPROVAL` until the designated approver approves it.
- Fee Assessment and Profitability are exposed as trace evidence in this phase; Fee Rule/Tier authoring remains in pricing/product configuration.
- Reserve calculation/reporting and tax adjustment/liability/payment/reconciliation are not claimed complete by 11L.

## Runtime coverage
The runtime qualifier creates and validates: inquiry, confirmation, notification preference/event, API access grant/revoke, signature rule, authorized user, beneficiary, payment instrument, regulatory restriction/release, maker-checker pricing override, tax exemption and tax certificate. Governed-rule dependent evaluation/calculation subflows are explicitly deferred when no real active rule exists.

## Gates
1. Static source verifier.
2. Oracle table/sequence reconciliation with no business DML.
3. DB integrity verifier.
4. Production build and existing regression gates.
5. Runtime E2E on an ACTIVE account with an ACTIVE Account Party.
