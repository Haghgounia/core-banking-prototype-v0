# Core Banking Prototype 0.10.0 — Final Freeze / Closure Record

## Release identity

- Product: Core Banking Prototype
- Release: `0.10.0`
- Backend qualified artifact convention: `0.10.0-SNAPSHOT`
- Canonical runtime JAR: `app/core-banking-prototype.jar`
- Build marker: `app/BUILD-VERSION` must equal `0.10.0`
- Closure baseline date: 2026-09-21

## Scope frozen in 0.10.0

The release contains the accumulated prototype capabilities through Phase 10F. For the Four Deposits track, the frozen business sequence is:

`Validate → Persist Opening → Create Account → Settlement → Readiness → Activate`

Supported qualification families:

- `QARD_SAVINGS`
- `CURRENT_ACCOUNT`
- `SHORT_TERM_DEPOSIT`
- `LONG_TERM_DEPOSIT`

Phase 9 servicing remains intentionally limited to the implemented account lifecycle, including `ACTIVE → CLOSED`. New Hold/Block, Dormancy/Reactivation, extended transaction posting, profit engine, cheque lifecycle and similar servicing work are **not** retrofitted into 0.10.0 and belong to later versions.

## Closure evidence

- Phase 10A: CLOSED
- Phase 10B: CLOSED
- Phase 10C: CLOSED
- Phase 10D: CLOSED
- Phase 10E: CLOSED
- Phase 10F: `PHASE10F_RUNTIME_E2E_PASS`
- Four-family runtime E2E: PASS
- Final Phase 10E/10F static gate: expected 35/35 after absorbed runtime hotfixes
- Final DB baseline gate: `database/oracle/dps2/verification/0.10.0-final-verifier.sql`
- Final source/runtime gate: `tools/verify-final-closure-0.10.0.mjs`
- Single Windows orchestration gate: `tools/final-verify-0.10.0.cmd`

## Freeze policy

Once `FINAL_RELEASE_CLOSURE_PASS` is produced on the target workstation:

1. `VERSION`, backend/frontend source, Oracle migrations, reference seed, verifier scripts and release documents become immutable for `0.10.0`.
2. A defect requiring functional source/DDL behavior change must be assigned to a later release (or an explicitly numbered maintenance release), not silently patched into this baseline.
3. Generated runtime data, logs and Phase 10F fixture output are not part of the immutable source baseline.
4. The canonical JAR may be rebuilt from the frozen source, but its `BUILD-VERSION` must remain `0.10.0` and the complete closure verifier must pass after rebuild.

## Current closure state

The four-family business runtime qualification is complete. The only remaining release-management action is to run the single final verifier on the actual Windows/Oracle/JAR environment after overlaying this closure package. A successful run prints:

```text
FINAL_SOURCE_BASELINE_PASS
FINAL_DB_BASELINE_PASS
FINAL_RUNTIME_HEALTH_PASS
FINAL_RELEASE_CLOSURE_PASS
```

At that point `0.10.0` is formally frozen/closed.
