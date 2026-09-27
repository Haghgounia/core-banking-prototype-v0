# DPS2 0.11.0 - UAT Static Verifier Alignment Hotfix

Date: 2026-09-27

## Scope

This hotfix aligns two static verifiers with the already-implemented RC business behavior. No production business logic is changed.

### Phase 11M correction verifier

The historical verifier required the exact source literal `new ReversalRequest("CORRECTION_REQUEST")`. The current implementation deliberately chooses the reason based on correction type: `REVERSAL -> CORRECTION_REQUEST`, `DUPLICATE_CANCEL -> DUPLICATE_POSTING`. Both still use the canonical Step 05 `transactions.reverse(...)` path and persist a correction entry.

The verifier now checks the semantic contract instead of the obsolete exact literal.

### Phase 11N rapid UI verifier

The historical verifier required the exact English phrase `Pay via Step 05`. The current RC UI uses Persian explanatory text while retaining Step 05 and the real `runLottery` / `payLotteryWinner` actions. The verifier now checks those actual actions plus the Step 05 boundary and payment label.

## Local static result

```text
PHASE11M_STATIC_VERIFIER_PASS=92
PHASE11M_STATIC_VERIFIER_FAIL=0
PHASE11M_STATIC_BASELINE_PASS

PHASE11N_RAPID_STATIC_VERIFIER_PASS=39
PHASE11N_RAPID_STATIC_VERIFIER_FAIL=0
PHASE11N_RAPID_STATIC_BASELINE_PASS
```
