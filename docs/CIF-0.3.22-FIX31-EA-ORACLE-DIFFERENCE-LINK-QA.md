# CIF 0.3.22 Fix31 - EA / Oracle Difference Link QA

## Scope

Minimal frontend-only correction on top of Fix30/Fix29. The EA/Oracle comparison engine, Oracle metadata queries and XML parser are unchanged.

## Implemented behavior

- Added a `جزئیات` column to the table-level comparison grid.
- Rows with `TableStatus.DIFFERENT` show an explicit `جزئیات اختلاف` link-style button.
- Matching rows and missing-table rows show no difference link.
- Clicking the action selects the corresponding table, prevents row-click bubbling, and scrolls to the existing `comparison-detail` per-column difference grid.
- Existing row click behavior remains unchanged.

## Static verification

- Fix31 EA/Oracle verifier checks the link label, `DIFFERENT` condition and handler binding.
- Existing table/tag balance checks continue to run.
- Existing persisted-grid verifier continues to run.
- TypeScript source was syntax-checked with the available TypeScript compiler; dependency-resolution errors are ignored because frontend node_modules are intentionally absent from the release package.
- No database migration is required.
