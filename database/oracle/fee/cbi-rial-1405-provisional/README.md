# CBI Rial Banking Fees 1405 — Provisional Versioned Import

Source workbook SHA256: `bbe79ea223fabbfcddd83a402a70bc87f9c3fb3ad3d0de01369155240e4bd15e`  
Source PDF SHA256: `ea2e6a22e3b92b7ea6ef5da8cd6bfa20f918d0fc069c2e1bd46b89167a993e14`

## Scope
- 152 new rial-banking tariff definitions from the supplied 8-page attachment.
- 180 structured source calculation components.
- 156 prior **non-electronic** CBI-1404 fee definition versions are closed on 2026-09-09 and marked `SUPERSEDED`.
- 73 prior **electronic** CBI-1404 definitions are explicitly retained.
- No physical DELETE is executed. Historical definitions/rules remain queryable by effective date and regulatory source.

## Provisional regulatory metadata
The official cover letter/circular number and effective date were not supplied. For prototype execution only, this package uses:
- `SOURCE_CODE=CBI_RIAL_FEE_1405_PROVISIONAL`
- `CIRCULAR_NO=PROVISIONAL-RIAL-FEE-1405`
- `STATUS_CODE=PROVISIONAL`
- `EFFECTIVE_FROM=2026-09-10` (derived from the supplied workbook date, **not claimed as the official effective date**)

Replace/finalize these values after receiving the official CBI letter.

## Calculation policy
- Simple fixed/rate/per-unit rows are mapped to executable `FEE_CALCULATION_RULE` fields.
- Percent values are normalized from human percent to decimal (`0.5% -> 0.005`).
- Composite/conditional/reference/formula rows are not guessed. They use `COMPOSITE` or `EXTERNAL_VALUE`, and every source component is preserved in `FEE_RULE_COMPONENT` with source condition/reference text.
- Source component count is preserved exactly.

## Install
Run `00-install-cbi-rial-fee-1405-provisional.sql`. It imports, verifies, then commits. Any SQL error rolls back before commit.
