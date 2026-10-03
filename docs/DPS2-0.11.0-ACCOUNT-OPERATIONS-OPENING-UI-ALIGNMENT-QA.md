# DPS2 0.11.0 — Account Operations / Opening UI Alignment

## Scope

This patch aligns the Deposit Account Operations workspace with the reviewed Deposit Opening visual language without changing Account Operations business ownership or service contracts.

### UI alignment

- Same visual tokens as Opening: navigation, background, primary/deposit accents, radius and state colors.
- Internal Operations sidebar with grouped navigation for Step 00, Steps 01–02, Steps 03–05, Steps 06–10, Steps 11–17 and Closure/Hold/History.
- Opening-style sticky top bar, page head, scope note and technical-evidence area.
- Sticky selected-account context ribbon with account number, lifecycle status, family, currency, product version and available balance.
- Cards, status pills, tables, timeline rows, notices, form controls and buttons aligned to the Opening UI language.
- Responsive collapse for tablet/mobile widths.

## Business boundary preserved

No Account Operations endpoint or domain mutation was replaced by UI-only state. Existing APIs remain the execution source for Activation, Lifecycle, Bulk, Hold, Balance/Subledger, Term, Profit, Transaction Processing, Servicing Steps 06–10, Governance Steps 11–17, Closure/Reopening and account-party/contact/history operations.

## Static qualification

Run:

```bat
node tools\verify-dps2-account-operations-ui-alignment.mjs
node tools\verify-dps2-deposit-account-operations-phase8.mjs
node tools\verify-dps2-phase11na-step00-dashboard.mjs
node tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs
node tools\verify-dps2-phase11nc-steps03-04-canonical-closure.mjs
node tools\verify-dps2-phase11nd-steps05-10-canonical-audit.mjs
node tools\verify-dps2-phase11n-steps11-17-rapid-completion.mjs
```

The new verifier is wired into both production build scripts. On Windows you may run the single qualification helper:

```bat
tools\qualify-dps2-account-operations-ui.cmd
```

Or run the authoritative build directly:

```bat
build-production.cmd
```

## Manual smoke test

1. Open `#/four-deposits/account-operations`.
2. Confirm Dashboard is visible and the Operations sidebar opens/docks.
3. Search an account and open it using the eye action.
4. Confirm the selected-account context bar appears and the Step navigation becomes enabled.
5. For `PENDING_ACTIVATION`, execute Activation Checks before activation.
6. For an `ACTIVE` account, verify lifecycle, Hold, transaction and history cards load.
7. For a term account, verify Term and Profit sections load and retain Step 03/04 behavior.
8. Verify Closure/Reopening remains controlled by Package 16 and no generic direct-close action appears.

## Runtime note

Static qualification proves source/API wiring and canonical coverage. Environment-specific runtime E2E must still be executed against the user's Oracle/backend before production release.
