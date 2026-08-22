# CIF 0.3.22 Fix28 — Persisted Data Grids & Dockable Sidebar QA

## Baseline

- Source baseline: `0.3.22-prototype-fix27`
- Target: `0.3.22-prototype-fix28`
- Database migration: **none**
- Backend/database source: unchanged from Fix27

## User issue addressed

Persisted multi-record data was still rendered as stream/card rows in several CIF pages, especially Party / Customer 360. The left RTL sidebar also permanently consumed 290px of horizontal space.

## Implemented behavior

### Persisted data

All persisted multi-record collections in CIF are rendered as explicit HTML tables with named columns.

Operational forms covered:

1. Address / Contact / Contact-Address association
2. Financial profile
3. Employment history
4. Organization licenses
5. Income sources
6. Assets / liabilities
7. Additional identifiers
8. Documents
9. Classifications
10. Party relationships
11. Beneficial owners
12. Authorities
13. Party roles
14. Customer-role history
15. KYC cases
16. Risk assessments
17. Screening results
18. External inquiries
19. Consents
20. Communication preferences
21. General preferences
22. Lifecycle history
23. Merge history

Party / Customer 360 was also converted, including:

- Party names and identifiers
- addresses and contacts
- financial/employment/license/asset data
- classifications, relationships, UBO, authorities, roles and customers
- KYC/documents/risk/screening/external inquiries
- consent/preferences/lifecycle/merge
- source-system products/restrictions/limits
- interactions/journey
- complaints/alerts/status history
- segment/value score/metrics/recommendations
- organization officers/groups/signature specimens
- registration requests/audit events

Total column-grid/table blocks detected by the Fix28 verifier: **67**.

### Desktop width / horizontal scrolling

- Shared `.db-grid` and `.record-table` use `table-layout: fixed`.
- Cell content wraps inside the available column width.
- Desktop wrappers use `overflow-x: visible !important`.
- The application content area uses `overflow-x: clip` to prevent page-level horizontal scrolling.
- Only screens up to `820px` retain horizontal table scrolling as a responsive fallback.
- Party 360 source-system cards are single-column in Fix28 so their tables receive the full content width.

### Dockable left sidebar

The RTL `position="end"` sidenav (visually on the left) now has two states:

- Expanded: `290px`
- Compact/docked: `76px`

Behavior:

- Toggle available inside the sidebar and in the top toolbar.
- Material sidenav container uses `autosize`, so the content margin is recalculated when rail width changes.
- Compact state keeps icons and tooltips while hiding labels.
- State is persisted in `localStorage` key `core-banking.sidebar.collapsed`.
- First use defaults to compact mode to maximize form width.

## Regression safeguards

Added `tools/verify-cif-persisted-grids.mjs` and integrated it into `build-production.cmd` before npm/build execution.

The verifier checks:

- no exact stream/card persisted-record classes remain (`records`, `record`, `record-row`, `source-list`, `mini-row`)
- all table-related HTML tags are balanced
- minimum table coverage per operational component
- Party360 table bindings use fields declared by the TypeScript response interfaces
- sidebar collapse binding and two toggle controls exist
- sidenav `autosize` is enabled
- compact rail width and localStorage persistence exist
- desktop no-horizontal-scroll and mobile fallback rules exist
- generated system version matches `VERSION`

## Executed checks

### PASS — Fix28 static verifier

```text
Fix28 persisted-grid verification OK: 67 CIF grids, no stream/card record renderers, dockable sidebar verified.
```

### PASS — HTML structural checks

`table / thead / tbody / tr / th / td` open/close counts are balanced for all CIF component templates.

### PASS — Party360 field contract check

42 persisted/source-data loops were compared with fields declared in `cif.models.ts`; no invalid property reference remains.

### PASS — TypeScript syntax check

`app-shell.component.ts` was transpile/syntax checked with TypeScript and produced no syntax diagnostic.

### PASS — Version synchronization

`VERSION` and generated system specification both report:

```text
0.3.22-prototype-fix28
```

### Full Angular/Maven build status in this environment

A complete dependency-based build could not be executed in the artifact environment because external dependency retrieval is unavailable:

- online `npm ci` timed out while accessing npm registry
- offline `npm ci` stopped on an uncached `zod-to-json-schema` package
- Maven wrapper could not download Maven `3.9.16`

No backend or database source was changed in Fix28. `build-production.cmd` now runs the Fix28 regression verifier first and will then run the normal Angular and Maven build in the Windows development environment.

## Windows verification command

```bat
bin\stop.cmd
rmdir /s /q frontend\dist
rmdir /s /q backend\src\main\resources\static
build-production.cmd
```

The first visible Fix28 build line after the source checks should be:

```text
Fix28 persisted-grid verification OK: 67 CIF grids, no stream/card record renderers, dockable sidebar verified.
```
