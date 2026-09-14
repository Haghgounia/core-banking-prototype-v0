# QA Report — Core Banking Prototype 0.5.0

## Phase

DPS2 Four Deposits — Phase 5 — Opening Audit & Change Management

## Source baseline

- Baseline package: `core-banking-prototype-v0.4.0-full-source-qa-candidate.zip`
- Target source version: `0.5.0`
- Oracle model basis: supplied DPS2 DDL and Deposit Account Opening EA/XMI model.

## Verification results

| Gate | Result |
|---|---|
| Phase 5 structural verifier | PASS |
| Full `verify-*.mjs` regression suite | PASS — 34/34 |
| Patch overlay regression suite | PASS — 34/34 |
| Patch overlay byte-level comparison | PASS — 0 missing / 0 extra / 0 different |
| Patch scope | 33 files — 14 added / 19 changed / 0 deleted |
| Version synchronization | PASS — 0.5.0 |
| Aggregate entity whitelist | PASS |
| Same-Opening child ownership guard | PASS |
| Oracle metadata field validation | PASS |
| Root/child optimistic locking contract | PASS |
| PRE_CHANGE / POST_CHANGE snapshots | PASS |
| Field Old/New + SHA-256 evidence | PASS |
| Configurable sensitive-field masking | PASS |
| Append-only trigger contract | PASS |
| Dedicated Angular route/UI contract | PASS |
| COMPLETED bounded-context mutation guard | PASS |
| Java syntax-oriented check | PASS subject to missing external dependencies |
| TypeScript syntax-oriented check | PASS subject to missing Angular/RxJS dependencies |
| Maven Production Build | BLOCKED / NOT VERIFIED |
| Angular Production Build | BLOCKED / NOT VERIFIED |
| Oracle runtime integration | NOT EXECUTED |

## Build environment blockers

Maven wrapper cannot obtain Apache Maven 3.9.16 from Maven Central in this environment. The offline attempt fails before project compilation because the wrapper distribution is absent.

`npm ci --offline` fails with `ENOTCACHED` because at least `zod-to-json-schema-3.25.2.tgz` is not present in the local npm cache. Therefore Angular production compilation cannot be truthfully marked PASS.

These are dependency/tooling availability blockers, not observed source compilation failures. A connected build environment must run the final Maven/Angular production gates.

## Oracle runtime gates for deployment environment

1. Execute `0.5.0-phase5-opening-audit-change-management.sql` against DPS2.
2. Verify append-only triggers return ORA-20051..ORA-20054 for UPDATE/DELETE attempts.
3. Create, approve and apply a Change Set on a root field.
4. Apply a Change Set on a child row owned by the same Opening.
5. Verify a child row owned by another Opening is rejected.
6. Verify protected/system columns are rejected.
7. Verify PRE/POST snapshots and per-entity Audit Events.
8. Configure a sensitive field and verify `[MASKED]` plus non-null SHA-256 hashes.
9. Verify Apply is rejected after Opening reaches `COMPLETED`.

## Conclusion

Phase 5 is **Source QA PASS / Packaging Candidate**. It is not marked Production Release until production builds and Oracle integration smoke tests are executed successfully.
