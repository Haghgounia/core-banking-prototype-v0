# QA Report — Core Banking Prototype 0.4.0

Date: 2026-09-14
Scope: DPS2 Four Deposits Phase 4 — Account Creation & Activation
Baseline: 0.3.99
Target: 0.4.0

## Result summary

| Gate | Result | Evidence |
|---|---|---|
| Source version alignment | PASS | VERSION, Maven POM, package.json/package-lock = 0.4.0 |
| Phase 4 API/source contract | PASS | Dedicated Phase 4 verifier |
| Account creation idempotency | PASS (static contract) | `CREATED_ACCOUNT_ID` replay + Opening row lock |
| Concurrency guard | PASS (static contract) | `SELECT ... FOR UPDATE` on Opening |
| Activation state guard | PASS (static contract) | only `PENDING_ACTIVATION -> ACTIVE` |
| Opening completion/history | PASS (static contract) | `APPROVED -> COMPLETED` + status history |
| Lifecycle events | PASS (static contract) | `CREATE`, `ACTIVATE` |
| Angular Step 7 integration | PASS (static contract) | three-stage UI + API client |
| Oracle migration contract | PASS (static contract) | Phase 4 migration present and verified |
| Full regression verification | PASS | all 33 `verify-*.mjs` scripts passed |
| Patch overlay byte comparison | PASS | 0 missing, 0 extra, 0 different in managed source scope |
| Patch overlay regression | PASS | all 33 `verify-*.mjs` scripts passed on overlay |
| Full/Patch ZIP integrity | PASS | ZIP CRC test, extracted Full regression and Patch-ZIP overlay equivalence |
| Java syntax-level check | PASS | no syntax-family javac diagnostics; dependency symbols unavailable |
| TypeScript syntax-level check | PASS | no syntax-family TypeScript diagnostics; Angular dependencies unavailable |
| Backend production Maven build | BLOCKED / NOT VERIFIED | Maven distribution/dependencies cannot be fetched in isolated environment |
| Frontend Angular production build | BLOCKED / NOT VERIFIED | NPM registry DNS/network unavailable (`EAI_AGAIN`) |
| Oracle runtime integration | NOT EXECUTED | no target Oracle DPS2 connection in this environment |

## Regression suite

33 of 33 verifier scripts passed on the target source and again on the patch-overlay source tree.

## Patch overlay

The patch was generated from the uploaded 0.3.99 baseline. The final candidate patch contains 31 managed files: 28 Phase 4/source changes (10 added + 18 changed) plus 3 release-metadata files; no managed source file is deleted. Overlay verification compares managed source content and excludes generated/runtime artifacts such as `app/BUILD-VERSION`, JARs, frontend build output, Maven target output, logs and caches.

## Production build blocker

The environment cannot resolve/fetch external build dependencies:

- Maven wrapper cannot fetch the configured Apache Maven distribution from Maven Central.
- NPM requests to `registry.npmjs.org` fail with DNS/network `EAI_AGAIN`.

This is an environment/network qualification blocker. It is not recorded as a source compile PASS or FAIL.

## Release decision

**QA Candidate: PASS for static regression + overlay.**

**Final production release: NOT YET QUALIFIED** until Maven package, Angular production build and target Oracle integration/smoke tests pass in a networked/build-capable environment.
