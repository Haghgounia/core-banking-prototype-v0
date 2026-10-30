# FEE3 F7 domain integration (2026-10-29)

Reference: FCE V13.8.84. Fixed phases: F5, F6, F7, F8, F9, F10.

## Implemented in source

- REST `POST /api/v1/fee3/simulations/domain-preview`, controlled dispatch to the existing independent, read-only R4C1 (exchange-loan profit), R4C2 (participatory / debt-purchase / Salam models), R4C3 (Qard), R4B2 (guarantee issue / collateral portfolio) services.
- The R4 services retain their Oracle source, version and business-date, CAL2, and contract-evidence guards. Domain outcomes are normalized to exact money strings, breakdown and diagnostic codes in the F7 UI. Nothing is written, authorized or posted.
- The Angular tariff wizard sends domain evidence from explicit analyst fields and shows raw amount, rounded amount, contract version, fingerprint and breakdown. No false local PASS for domain operations without a reviewed Backend contract.
- The F6 generic API also explicitly rejects specialized banking modules and amount kinds. Its other 9 general methods remain unchanged.
- No manual API, build, CLI or Git actions are required from the business analyst for UAT; acceptance is through the tariff screen.

## Known limits (not implemented/accepted)

- R4 service previews **can block** if the Oracle/CAL2 reference, account role, applicable rule or official provenance is unqualified. A successful install must not be confused with domain certification.
- LC, guarantee renew/change, late-damage, cheque issuance and collateral valuation, and noncovered services await independent F7 contracts.
- Bank-sanctioned tariff approval, charge, repayment schedule, anti-duplicate charge events, financial posting, and Oracle write remain outside this release.
- Analyst UI UAT has not been performed. Full Maven and Angular production builds must complete in the deployment environment; a successful isolated Java compilation is not a substitute.

## UI handoff

Open the existing FEE3 tariff wizard, use the domain-specific form and select «محاسبه تخصصی Backend F7». The analyst supplies genuine contract references and inputs, reviews calculation trace or blocking diagnostics, and may confirm numerical equality. This confirmation is **not** bank authorization.
