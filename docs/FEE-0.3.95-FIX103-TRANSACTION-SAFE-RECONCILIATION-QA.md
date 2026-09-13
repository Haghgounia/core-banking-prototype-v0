# FEE 0.3.95 / FIX103 — Transaction-safe Reconciliation & Recovery QA

## Trigger

Oracle reconciliation compiled successfully but returned `mismatches=419`. Every tariff/component/input/tier mismatch was `ROW expected=present actual=missing`; global counts were `SOURCE=0`, `DEFINITIONS=0`, `RULES=0`, `SOURCE_COMPONENTS=0`, `INPUT_DEFINITIONS=0`, `TIERS=0`.

This pattern is not a field-level tariff mismatch. It means the complete provisional 1405 dataset is absent from the current Oracle context. The earlier FIX98 structural verification had returned 1/152/152/180/15 with archive=156 and retained=73, so either: (a) the import was still uncommitted and a later failing reconciliation rolled it back, or (b) the current connection targets a different DB/PDB/service.

## FIX103 changes

| Artifact | Contract |
|---|---|
| `04-reconcile-...-core.sql` | Embedded source contract; raises ORA-20260 on mismatch |
| `04-reconcile-....sql` | Standalone wrapper; `WHENEVER SQLERROR CONTINUE NONE`; no COMMIT/ROLLBACK |
| `04-reconcile-...-enforced.sql` | Installer wrapper; rollback-on-error |
| `05-diagnose-...-state.sql` | Read-only DB/PDB/service/schema + FEE state report |
| `00-install-...sql` | Import → Structural Verify → Enforced Reconcile → COMMIT |

## Recovery acceptance

1. Run `05-diagnose-cbi-rial-fee-1405-state.sql`.
2. If Source/Definitions/Active Versions/Rules are all zero, run `00-install-cbi-rial-fee-1405-provisional.sql` — not `01` and `02` individually.
3. Confirm `CBI Rial Fee 1405 PROVISIONAL committed successfully`.
4. Open a fresh Oracle session and run `02-verify...sql`; all structural counts must match.
5. In the fresh session run standalone `04-reconcile...sql`; expected summary is 152/180/66/15 and `mismatches=0`.

No DDL or physical delete is introduced by FIX103.
