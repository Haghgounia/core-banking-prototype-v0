# FEE3-BE-R4A QA and residual risks (2026-10-27)

## Confirmed in this build environment

1. Actual pure Java compilation: `Fee3Models`, existing R3 arithmetic and new R4-A contract.
2. 51 standalone numeric/guard/evidence checks through `Fee3R4ContractProbe`, no mocking of arithmetic.
3. Strict source/SQL policy: R4-added repository uses read-only SELECT; no insert/update/delete/merge/DDL.
4. 14/14 HTTP checks with a **simulated local server only** validate the safety of the
   loopback qualification script; they do NOT certify the real Spring/Oracle endpoint.
5. SHA-256 gated additive installer; no existing Backend, Frontend, ORG, FEE, FEE2 or Product Builder file replaced.
6. R3 and QA3 anchor fingerprints are asserted before install.
7. Spring/JDBC/Jackson method-signature compilation with minimal artificial stubs is useful
   for syntax sanity only; a real Maven compile is explicitly **not** certified by stub testing.
8. Runtime loopback-only test checks unpersisted general service preview and blocked cases;
   no transport to nonlocal domains, redirects or proxies is permitted.

## Not certified here

- Maven `build-production.cmd` on the user's Windows workstation.
- Actual Oracle runtime persisted-draft snapshot comparison (requires a dedicated UAT-created R2 draft).
- Guarantee, multi-collateral, loan profit, Qard, late-payment loss, editable formulas and Jalali day-count.
- Operational tax determination or approved central-bank tariff rates.
- Any publication, approval, collection, booking or accounting posting.
- Independently bank-approved rounding rules; R4-A enforces the explicit version's selected policy
  rather than choosing a bank-wide rounding standard without a governance decision.

## Required final evidence

- Source verifier result `FEE3_R4A_SOURCE_PASS=... FAIL=0`.
- Real Maven build log.
- `FEE3-R4A-RUNTIME-RESULT.json` from actual local Spring/Oracle runtime.
- Read-only Oracle before-and-after row counts and invariant checks.
