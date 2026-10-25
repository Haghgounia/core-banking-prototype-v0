# FEE3 UI R2 - Identity Step Alignment

Baseline: `core-banking-prototype-v0-2026-10-25-1200-FEE3-UI-R1.zip`
Reference: `FCE_v13_8_84_Program_REDOWNLOAD.html`, stage-one identity view and later V13.8.28 / V13.8.39 / V13.8.40 overrides.

## UI acceptance contract

The first step follows the eight business inputs of the reference in *DOM keyboard/tab order*, not in database-column order.

| Position in RTL desktop layout | Field | UI model | Note |
| --- | --- | --- | --- |
| 1: right | Banking module | `Fee3Item.module` | Dependent operation list |
| 1: center | Banking operation | `Fee3Item.operation` | Filtered by module |
| 1: left | Calculation amount category | `Fee3Item.amountKind` | Filtered by operation |
| 2: full width | Expert-visible title | `Fee3Item.title` | Required, 250 chars |
| 3: right | Responsible owner unit | `Fee3Item.ownerDomain` | Source HTML owner vocabulary, 15 options |
| 4: right | Tariff origin and obligation | `Fee3Version.origin` | Context-sensitive classification |
| 5: right | Fee/output currency | `Fee3Item.feeCurrency` | Provisional, UI-only metadata |
| 5: left | Account/service currency | `Fee3Item.serviceCurrency` | Provisional, UI-only identity metadata |

Currency fields are separated from the upper fields by a horizontal divider. The original screenshot uses right-to-left reading order and Material forms use the application's established styles.

Removed from **stage-one UI only**: business ID input, service-variant input, free-form description, `IS_ENABLED` checkbox and the extra owner-domain field. Existing seed or session data for these technical properties is **not** deleted; the checkbox flag cannot activate a fee in this UI phase.

## Contract details and pending Oracle adapter decisions

- FEE3's seeded `OWNER_DOMAIN_CODE` values overlap the owner-unit vocabulary of the HTML reference. R2 uses the existing `ownerDomain` value to avoid introducing a second independently editable owner field. Backend integration must confirm the *business owner domain vs maintaining organizational unit* vocabulary with analysts.
- The HTML reference contains central tariff management owner code `TARIFF`, which was **not** found in the supplied FEE3 reference Seed (`FCE_REF_CODE` / `OWNER_UNIT`). R2 includes that option **only** in the frontend for screenshot fidelity. Do **not** assume it is an Oracle reference row; confirm/enrich master data before connecting the backend.
- The reference has separate service/account and output/fee currencies. The supplied physical Oracle model has only `FCE_VERSION_SCOPE.CURRENCY_CODE` in scope and IRR-named rule amount columns. Two-currency persistence/FX conversion requires an explicitly approved backend mapping; R2 does not invent schema columns or execute conversions.
- Currency identity selectors are editable for a **new DEMO item with a single draft version** and otherwise read-only, because changing the currency pair changes business identity. This avoids changing previously seeded identities silently.
- Changing a new item's service currency also updates the single model's active draft scopes and matching test-request currency values. A new scope inherits the same service currency. The scope editor now displays this currency read-only to avoid contradictory selections.
- Tariff origin is maintained in `FCE_TARIFF_VERSION` semantics. The authoritative source field is in step one and no longer appears as a second editable field under validity. Loan-profit/financing-profit is restricted to the contract-regulated origin; standard service fees use internal-bank or central-bank-mandatory origins. Existing source-code evidence remains under validity.
- The UI still does not call HTTP or Oracle, approve or publish tariffs, compute FX, or persist browser storage.

## Checks executed

- Source-first HTML parity: 23 static assertions in `tools/fee3-ui/verify-identity-r2.py`.
- Original R1 FEE3 package verification: 34 static checks.
- TypeScript no-emit compilation of model, validation and Seed; component TypeScript syntax transpilation.
- Runtime validation scenarios: 11 assertions for required owner/origin/currencies, loan-profit origin selection, duplicate-identity/currency discrimination, and scope/identity mismatch.
- Unchanged: Oracle scripts, FEE/FEE2 modules, main navigation, all 297 Seed items and their related versions/rules/scopes.

**Not yet verified:** complete Angular production build and pixel/browser E2E review (Angular npm packages were not available to this environment). Obtain a local `npm run build` result and inspect in the target browser before formally accepting the UI.

## Apply the patch

Extract `FEE3-UI-R2-IDENTITY-PATCH-2026-10-25.zip` at the **project root**, retaining relative paths and replacing only the listed files. Or use the full-package archive for a clean review. No SQL should be run.
