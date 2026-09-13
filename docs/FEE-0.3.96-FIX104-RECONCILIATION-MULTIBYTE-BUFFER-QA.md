# FEE 0.3.96 / FIX104 — Reconciliation Multibyte Buffer Guard

## Incident
Oracle `04-reconcile-cbi-rial-fee-1405-provisional.sql` reached the tariff checks and then failed with:

`ORA-06502: PL/SQL: value or conversion error: character string buffer too small`

The failing call corresponds to the long Persian tariff title around `CBI1405R_CRD_8_24`. The title is 139 Unicode characters but 252 UTF-8 bytes, while the prior local variable was `VARCHAR2(250)`.

## Fix
The reconciliation generator no longer uses fixed-size local text buffers for values read from Oracle. Variables are anchored with `%TYPE` to their source columns, including:

- `FEE.FEE_DEFINITION.NAME_FA%TYPE`
- `FEE.FEE_RULE_COMPONENT.CONSTANT_TEXT%TYPE`
- `FEE.FEE_RULE_COMPONENT.DESCRIPTION%TYPE`
- `FEE.FEE_INPUT_DEFINITION.NAME_FA%TYPE`
- `FEE.FEE_CALCULATION_TIER.TIER_NAME_FA%TYPE`

Other code/status fields used by the same procedures are also column-anchored.

## Regression guards
`verify-cbi-rial-fee-1405-provisional.mjs` now requires the `%TYPE` declarations and rejects the previous fixed buffer declarations.

## Scope
No DDL, no fee values, no strategy mapping, and no archive/version data are changed by FIX104. This is a validation-script/generator fix only.

## Acceptance
Run the corrected reconciliation after the import and structural verification. Expected final status:

- `tariffs_checked=152`
- `components_checked=180`
- `inputs_checked=66`
- `tiers_checked=15`
- `mismatches=0`
- `source-to-Oracle reconciliation OK.`
