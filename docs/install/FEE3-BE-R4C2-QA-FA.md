# FEE3 R4-C2 — QA Evidence / Acceptance Gates

**Source review:** FCE_v13_8_84 HTML/Oracle Seed + R4C0 Oracle read-only output (`2026-10-27`) + R4C1 actual Java source. No new external banking policy inferred.

## Local qualification (not an Oracle sign-off)

| Gate | Expected evidence | Actual in local QA |
|---|---|---|
| Actual Java contract arithmetic and negative bounds | `run-pure-java.py` | `FEE3_R4C2_PURE_JAVA_PASS=76 FAIL=0` |
| Java Spring/JUnit signatures with framework stubs | `verify-java.py` | `FEE3_R4C2_JAVA_SIGNATURE_PASS` |
| Strict tool transport/mocked fail-closed | `selftest.py` | `FEE3_R4C2_SELFTEST_PASS=17 FAIL=0` |
| Static source safeguards | `verify-r4c2.py` | Run after extracting/installation |
| R4C1 regression | R4C1 `run-pure-java.py` / source checks | Must still pass |
| Full Maven package, real Oracle/CAL2 | `build-production.cmd` + `run-runtime-readonly.cmd` | **NOT RUN in local authoring environment** |

This stage never implies approval to recognize profit or change financial ledgers. Java standalone tests are not a substitute for Spring Boot compile/JUnit on the user's Windows system.

## Exact business dimensions

1. `PARTICIPATION_REALIZED`: a **reported** realized profit and contractual bank share percentage. Cannot substitute annual interest rate. Client evidence is unverified.
2. `PARTICIPATION_ESTIMATE`: annual contract rate and CAL2-backed Jalali actual-year factor. Must be marked ESTIMATE, not realized.
3. `DEBT_PURCHASE_SPREAD`: face amount less purchase price, **gross difference**, not a blanket revenue-recognition rule.
4. `SALAM_SPREAD`: sale realization less purchase amount, **gross difference**, not a blanket revenue-recognition rule.
5. `SALAM_ESTIMATE`: same static mathematical day factor as R4C1 but a separate Salam business profile and resultKind, never conflated with Salam realized sale.

## Negative/boundary testing

Loss requires a separate policy (negative spread BLOCKED); positive amounts bound to `NUMBER(22,2)` conservative limit, percentages `NUMBER(18,8)`. Other rejection cases: missing documentary IDs, non-bank event date, policy mismatch, rate spoof, wrong formula code, editable expression, client `yearFraction`, tax/waiver, partial ID, invalid CAL2 slices, cross-model event, published/version active, and nondeterministic hash replay.

## Oracle qualification gate

Before POST, the runner checks Oracle 11-table readiness, `draftWritesEnabled=false`, all workflow/auth/posting flags false, baseline 297/300, active `LOAN_PROFIT` reference and four exact active formula codes. The POST is only to the new **nonpersisting** preview controller and is denied by the harness if the initial gate fails. It evaluates all five models and Mudaraba, two CAL2-backed estimates, 19 negative cases, SHA-256 replay, and post-readiness item/version counts.

**Caveat:** Count consistency only proves no change in the two checked counters, not forensic immutability of every Oracle table. Persisted/published flags are model API attestations, not a formal database audit.

## Business UAT still open

The actual contract/source-of-truth for profit statement, share agreement, purchase price/face value, settlement status, tax, loss allocation, and timing rules remains unverified. No source can be treated as approved merely because `FCE_REF_FORMULA.IS_ACTIVE=1`. Only after business sign-off can the operational design for recognition/posting be considered separately. Independent FEE/FEE2 and Product Builder remain untouched.
