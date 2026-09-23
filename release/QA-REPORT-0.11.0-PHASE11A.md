# QA Report — 0.11.0 Phase 11A

## Scope

Account Operations Schema Reconciliation only. No Account Operations business API or UI behavior is enabled in this phase.

## Static result

`tools/verify-dps2-account-operations-schema-reconciliation-11a.mjs`

```text
PHASE11A_STATIC_VERIFIER_PASS=32
PHASE11A_STATIC_VERIFIER_FAIL=0
PHASE11A_STATIC_BASELINE_PASS
```

## Deposit regression result

The following existing gates remained PASS after starting 0.11.0:

- Deposit Account Phase 4: PASS
- Account Operations Phase 8: 18/18 PASS
- Account Servicing Phase 9: 18/18 PASS
- 0.9.2 Account Schema Reconciliation: 20/20 PASS
- 0.9.3 Reference/FK Reconciliation: 19/19 PASS
- Phase 10 Foundation: 34/34 PASS
- Phase 10B: 30/30 PASS (release-number assertion removed; business checks unchanged)
- Phase 10D: 24/24 PASS (release-number assertion removed; UI/integration checks unchanged)
- Phase 10E/10F: 35/35 PASS

## Full static build gate

`build-production.sh` was executed in the analysis environment. All static application gates passed through `verify-runtime-artifact-contract.mjs`.

The build then stopped at Maven bootstrap because this environment has no Internet access and Maven Wrapper attempted to fetch Maven 3.9.16 from Maven Central. This is an environment/network limitation, not a source or static-gate failure.

## Oracle runtime verification still required

Phase 11A is not CLOSED until the target Oracle database returns:

```text
PHASE11A_SCHEMA_RECONCILIATION_PASS
PHASE11A_DB_VERIFIER_FAIL=0
PHASE11A_DB_BASELINE_PASS
PHASE11A_IMPLEMENTATION_PASS
```

Recommended Windows command:

```bat
tools\apply-dps2-phase11a.cmd
```

The helper supports both local SQL*Plus and Oracle Docker fallback.
