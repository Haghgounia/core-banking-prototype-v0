# FEE3 F6-E2E-02: editable FORMULA in the backend-owned preview

Date: 2026-10-29. Project baseline: Core Banking Prototype 0.11.0.
Reference: FCE V13.8.84. Canonical phase map remains F5 -> F6 -> F7 -> F8 -> F9 -> F10.
Do not create or rename project phases.

## Delivered source behavior

- Existing POST `/api/v1/fee3/simulations/preview` now handles the `FORMULA` method for **general SERVICE_FEE previews**, in addition to the previous eight F6 general methods.
- Editable expression comes exclusively from `rules[0].parameters._fceEditableExpressionV1`, as serialized by the existing Angular wizard and `fee3-oracle-mapping.ts`.
- Evaluation runs in Java, using a bounded AST (no JavaScript, Java reflection, `eval`, scripting engines or dynamic code execution).
- Supports decimal arithmetic `+ - * / % ^`, parentheses and bounded named functions `min`, `max`, `abs`, `floor`, `ceil`, `round`, `sqrt`, `pow`, `pmt`, `ipmt`. Exponent must be an integer and bounded. PMT/IPMT use decimal installment computations, never client-provided yearFraction.
- Supports built-in variables and up to 30 explicitly declared `formulaParams` in `originalConfig`. Unsupported functions, undeclared variables, invalid input, division by zero, fractional exponents and oversized expressions are rejected. No guessed rates or contractual authorization.
- The existing `BigDecimal` normalization, limits, rounding, tax and no-posting gates of `Fee3PreviewEngine` still apply. `taxMode=UNREVIEWED` returns only `PRETAX_ONLY`; no payable amount or tax is manufactured.
- The main Angular wizard test action now routes general editable `FORMULA` to the same backend endpoint. Analyst must enter the expected amount and explicitly acknowledge numerical equality before a transient arithmetic test PASS is recorded.

## Still intentionally blocked

- Formula reference **codes** (`rules[0].formulaCode`), including the eleven F2 allowlisted profiles, continue to require the existing **separate F2** qualified-reference contract. The F6 editable interpreter does not certify reference provenance.
- Specialized LOAN/financing profit, QARD, late damage, GUARANTEE and LC calculations still require their separate F7 domain-specific contracts and remain excluded from general backend previews.
- No Oracle DML/DDL, write authorization, approval, tax determination, charge, accounting posting or publication is performed by this endpoint.
- Full Java Spring Boot + Angular production build and live Windows HTTP/browser run **are not verified in the package-building environment**. No functional/UAT PASS is asserted for the analyst.

## Engineering verification performed

- `javac` compilation (Java 21) of domain models, the F6 arithmetic interpreter, shared calculation engine and simulation service.
- 11 executable direct-Java numeric/safety scenarios: large exact decimal, mixed, compound, pmt, ipmt, custom parameter, divide-by-zero, unknown expression, F2 reference separation, undeclared parameter, fraction arithmetic.
- TypeScript syntax transpilation of modified Angular component (not a full Angular type-check).
- New JUnit tests for the same source behavior are supplied for execution during the project Maven build.

## Business analyst UAT (UI only)

1. Open an **unsaved/non-published** general `SERVICE_FEE` item in the FEE3 wizard (e.g. PAYMENT/TRANSFER).
2. In the calculation step, select `FORMULA`, enter `amount * rate / 100`, and choose the unreviewed tax state. Save the local draft.
3. In the wizard's Test & Summary step, enter amount `1000000`, rate `1.5`, click backend calculate and verify fee `15000 IRR`, no tax or payable total, with a `FORMULA_DECIMAL` trace.
4. Enter expected fee `15000`, explicitly accept numerical equality and review the session evidence. This does not constitute a bank tariff approval.
5. Edit the formula to `amount/0`; calculation must return `BLOCKED` and never a valid fee. The analyst may record a UI defect but should not run HTTP or source-level scripts.

## Next single work item, unchanged roadmap

F6 completion and integration defect fixes from analyst UAT; then F7 domain contracts, beginning with the existing loan, Qard, guarantee and LC backends. Keep the F7-A TypeScript build failure visible until the original build log establishes the exact fault; do not claim it is fixed by this release.

## Repository & release evidence

The installer checks source SHA-256 before modifying any file, reports stages, preserves prior files, calls the existing `build-production.cmd` and attempts rollback on build failure. It does not commit to Git. See `MANIFEST.json`, `DIFF-REPORT.json`, and the existing canonical roadmap checkpoint for the exact change set.
