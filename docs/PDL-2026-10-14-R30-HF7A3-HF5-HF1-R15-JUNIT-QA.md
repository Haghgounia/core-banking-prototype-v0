# R30-HF7A3 / HF5-HF1 — R15 Maven Regression Hotfix

Date: 2026-10-14 | Baseline: R30-HF7A3-HF5 | Application version: 0.11.0

## Failure confirmed from Windows production build

- After successful production preflight checks, Maven Surefire reported 181 tests, one failed (`PdlPricingRelationshipR15Test.earlyTerminationRateIsConditionalAndBounded`, line 54), zero errors and one skipped Oracle-dependent FEE test.
- The old R15 positive fixture omitted `IS_TIERED_RATE`; the newer `ProductRuleSemanticContract` correctly requires an explicit binary decision. Field business label: پلکانی بودن نرخ.
- The negative R15 fixtures also omitted that flag, so they could succeed by throwing the *wrong* validation error.
- A second runtime issue was uncovered by an additional real-Java regression: Oracle `BigDecimal("1.0")` (or numeric text `"1.0"`) satisfied `ProductRuleSemanticContract.binary(...)` but was treated as false by `ProductBuilderBusinessValidator.bool(...)`. This incorrectly blocked an allowed early-termination rate.

## Surgical changes (8 files)

1. `PdlPricingRelationshipR15Test.java`: set `IS_TIERED_RATE=0` on all three original R15 scenarios and assert business-specific failure messages; add a missing/invalid tier flag test, an all-off positive test and Oracle decimal/numeric string positive scenarios.
2. `ProductBuilderBusinessValidator.java`: retain `true`/`Y` compatibility; evaluate numeric flags using `BigDecimal(...).compareTo(BigDecimal.ONE)` so `1`, `1.0`, and `1.00` are interpreted consistently. This method is only an adapter; the strict upstream semantic check for reviewed binary flags is unchanged.
3. `tools/java/HF7A3R15RegressionHarness.java`: 10 actual Java positive/negative assertions on the production validator (without Spring runtime or database writes).
4. `tools/verify-pdl-r30-hf7a3-hf5-hf1-r15.mjs`: 9 static contract checks, invokes real Java harness, and compiles the updated historical JUnit source against **compile-only** JUnit/AssertJ stubs.
5. `build-production.cmd` and `build-production.sh`: run the new verifier before Maven.
6. This QA note and a release manifest under permitted folders.

No modification to `ProductRuleSemanticContract`, deposit/loan governance gates, `canOpen`, Oracle DDL/DML, FEE, FEE2 or frontend.

## Verified locally

- Targeted Java execution: `PDL_R30_HF7A3_HF5_HF1_R15_JAVA_PASS=10 FAIL=0`.
- Static regression: `PDL_R30_HF7A3_HF5_HF1_R15_STATIC_PASS=9 FAIL=0`.
- Original Maven R15 test **source** compiles with minimal compile-time stubs; this does not replace Maven's JUnit execution.
- Additional previous HF5/HF4/HF7A1/HF7A2 checks and ZIP-overlay verification are run separately during packaging.
- A full `mvnw.cmd clean package` / Angular production build cannot be claimed as passed in this environment; the user's Windows build is the final integration check.

## Windows installation

If HF5 has already been installed (as confirmed in the supplied log), make a project backup, extract the small patch ZIP **over** `D:\Projects\core-banking-prototype-v0` preserving directories, then run from CMD:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf7a3-hf5-hf1-r15.mjs
node tools\verify-pdl-r30-hf7a3-hf5-compat.mjs
node tools\verify-release-layout.mjs
build-production.cmd
```

Do not delete `app/BUILD-DIRTY` manually. Its removal requires a fully successful production build. Do not apply any Oracle migration for this patch.

## Acceptance evidence requested

A successful Maven Surefire run should include `PdlPricingRelationshipR15Test` with no failing tests, followed by successful packaging and frontend production build. If another step fails, share its first diagnostic and final build summary.
