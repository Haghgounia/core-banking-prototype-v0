# FEE3 R3-QA1 | Verification record

**Source:** Core Banking Prototype 0.11.0 / UI-R8+HF1 / BE-R1 / BE-R2 / BE-R3.

- R2 safe installer: preview pass, apply pass on an isolated reconstructed copy.
- R3 safe installer: preview pass, apply pass on the R2 copy, no application overwrite.
- R1 structural verifier: 52/52 PASS.
- R2 structural verifier: 48/48 PASS.
- R2 roundtrip mapping: 17/17 PASS.
- R3 structural verifier: 31/31 PASS.
- R8 UI static verifier: 59/59 PASS.
- Standalone Java R3 arithmetic/policy probe: 33/33 PASS (Java 21).
- R3-QA1 source qualification manifest hash/check gates: 25/25 PASS.
- Oracle DDL check: R3 governance status codes and required sequences found in the
  provided FEE3 2026-10-25 schema text. This does NOT replace live database introspection.
- Full Maven build: NOT VERIFIED. Local Maven 3.9.16 distribution unavailable and
  Maven Wrapper could not retrieve the distribution (no external download).
- Full Angular build: NOT VERIFIED. The current container has no installed project
  node_modules; static source checks do not replace Angular AOT.
- Oracle integration / actual HTTP runtime: NOT VERIFIED. No live connection or
  running project backend provided to this environment.
- Maker/Checker and write UAT: NOT PERFORMED, intentionally default-denied.

The scripts are fail-closed and read-only by design. A passed static test does not
establish banking, security, accounting or runtime acceptance.
- R3-QA1 HTTP mock contract: 23/23 PASS with a local stub HTTP service.
- Three negative smoke tests: write flag ON prevents all POSTs; workflow ON prevents
  all POSTs; non-loopback host is rejected before network requests.
- Angular offline dependency installation attempt: blocked by uncached package
  `zod-to-json-schema@3.25.2` (`npm ci --offline` -> `ENOTCACHED`).
- These outcomes are local verification only and not evidence of live Oracle UAT.
