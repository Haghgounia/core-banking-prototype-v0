# DPS2 0.11.0 — Phase 11M Wave D QA

## Scope

Wave D covers canonical Operational Steps 14–17:

- Step 14: reconciliation run/item, discrepancy, suspense and exception handoff.
- Step 15: exception assignment/RCA and maker-checker correction. Financial REVERSAL delegates to Step 05 and remains non-destructive.
- Step 16: NOSTRO/VOSTRO account extension and reconciliation profile. External correspondent settlement remains an integration boundary.
- Step 17: consume governed reward program/eligibility configuration, enroll accounts, and expose lottery entry/winner/payment trace. No program/draw/winner fabrication.

## Safety / source-of-truth boundaries

- No business seed data is inserted by the 11M migration.
- A correction of type REVERSAL must reference a POSTED transaction of the same account.
- CORRECTION/BACKDATED_CORRECTION/DUPLICATE_CANCEL are recorded/governed but are not given fabricated financial posting semantics in this prototype.
- Reward enrollment requires an existing ACTIVE eligible program.
- Correspondent runtime does not turn an arbitrary retail account into NOSTRO/VOSTRO merely to make a test pass.

## Qualification

```bat
cd /d D:\Projects\core-banking-prototype-v0
set "CORE_BANKING_ORACLE_CONNECT=..."
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
tools\qualify-dps2-wave-d-11m.cmd
```

Expected gates:

```text
PHASE11M_STATIC_BASELINE_PASS
PHASE11M_WAVE_D_RECONCILIATION_PASS
PHASE11M_DB_BASELINE_PASS
PHASE11M_IMPLEMENTATION_PASS
PHASE11M_RUNTIME_E2E_PASS
DPS2_WAVE_D_11M_QUALIFICATION_PASS
```

Runtime proves a real Step 05 cash deposit, reconciliation MATCH/MISMATCH, discrepancy+suspense, discrepancy→exception, correction maker/checker, and non-destructive reversal with corrective transaction trace. Correspondent and reward subflows may explicitly defer when governed configuration is absent.

## Source gate before Windows qualification

- Phase 11M static verifier: `90/90 PASS`.
- Phase 11L regression: `89/89 PASS`.
- Phase 11K regression: `92/92 PASS`.
- Node verifier path portability: PASS (61 scripts).
- Wave D Domain/Repository/Service compile check: Java 21 PASS.
- TypeScript parse check: no TS1xxx syntax errors; full Angular dependency build remains the Windows production gate.
- Oracle verifier target: `40/40` checks.
