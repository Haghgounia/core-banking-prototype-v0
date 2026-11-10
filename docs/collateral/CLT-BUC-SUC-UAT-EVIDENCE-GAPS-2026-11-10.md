# CLT — BUC/SUC UAT evidence and gap management

**Date:** 2026-11-10  | **Source baseline:** confirmed installed `CLT-R6-BUC-SUC-UAT-TIMELINE-2026-11-10`
**Release identifier:** `CLT-BUC-SUC-UAT-EVIDENCE-GAPS-2026-11-10`
**Canonical roadmap:** `CLT-ROADMAP-R1.0-FA.md`, unchanged phases R0..R7. The historic phase name **R6** means analyst UI/UAT, *not* a software version, an API domain, or a Business Use Case.

## Applied naming rule

- New public Java types: `CollateralBusinessReadinessController`, `CollateralBusinessReadinessPolicy`.
- New Angular types and files: `collateral-business-uat-*`, new tag `app-collateral-business-uat`.
- Business UAT scenarios: `BUC-01-P` / `BUC-01-N` through `BUC-16-P` / `BUC-16-N`.
- New JSON evidence format: `CLT-BUC-SUC-UAT-EVIDENCE-2`; gap export: `CLT-BUC-SUC-UAT-GAPS-1`. The obsolete phase-tagged evidence format (`CLT-R6-BUC-EVIDENCE-1` with scenario IDs `R6-BUC-01-P`, etc.) is **import-only** for previously exported analyst results. Strictly reject duplicate, wrong BUC/SUC association, forged bank/Oracle acceptance, unsupported versions.
- Read-only API remains `GET /api/v1/collaterals/uat/business-readiness`; no breaking URL change. New server response schema `CLT-BUC-READINESS-1`, while the client can read the older response schema for rolling-window compatibility.
- The old R6 implementation **source files are deleted** by guarded installer, not retained as stale parallel components. Legacy `tools/clt/r6/verify-source.cjs` delegates to the new `tools/clt/business-uat/verify-source.cjs`.
- Historical roadmap notes, existing older toolkit paths `r6a/r6c/r6d/r6e`, existing BUC phase traceability labels and old test runs remain unchanged: no mass rename of prior deliveries.

## User-facing change

The UAT workbench shows a collapsible list of all analyst-declared FAIL/BLOCKED results with their associated BUC, scenario kind, manual evidence and link to the actual operational screen. A separate JSON gap register can be exported for offline defect triage. It does **not** assume or create a real Oracle gap, bank decision or approval. Results stay in-memory unless the analyst exports their file.

## Not delivered / bank gates

No Oracle DDL or DML, no finance flag activation, no bank acceptance, no security elevation. Full end-to-end banking UAT and actual Oracle data validation still require the authorized bank environment.
