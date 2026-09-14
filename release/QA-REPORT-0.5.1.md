# QA Report — Core Banking Prototype 0.5.1

## Scope

Build hotfix for DPS2 Four Deposits Phase 5. No new functional scope and no new Oracle DDL.

## External production-build evidence

A Windows production build of 0.5.0 reached Maven `javac` and exposed two release defects:

- incomplete cumulative source after Phase 5 overlay (`deposit.account.domain`, `deposit.account.error`, `deposit.account.oracle` missing),
- Jackson 2 imports in `DepositOpeningAuditService` while the project uses Spring Boot 4.1.0 / Jackson 3.

The stale runtime guard correctly refused to start the old 0.3.99 JAR after the failed 0.5.0 build.

## Corrections

- Full Phase 4 Account Lifecycle source is guaranteed in the 0.5.1 cumulative patch.
- Phase 5 serialization now uses `tools.jackson.core.JacksonException` and `tools.jackson.databind.json.JsonMapper`.
- Windows and Unix production builds fail fast if Phase 4 Account source is incomplete.
- Production builds now execute Phase 5 and 0.5.1 hotfix verifiers before Maven compilation.
- Source version synchronized to 0.5.1.

## Verification status

| Gate | Result |
|---|---|
| Static/regression verifiers | PASS — 35/35 |
| Phase 4 source completeness guard | PASS |
| Phase 5 contract verifier | PASS |
| Jackson 3 namespace guard | PASS |
| Release layout/version synchronization | PASS |
| Cumulative patch overlay vs target | PASS — 0 missing / 0 extra / 0 different |
| ZIP integrity | PASS |
| Maven compile in this environment | BLOCKED — Maven wrapper distribution fetch unavailable after all static/verifier gates passed |
| User Windows Maven compile | REQUIRES RE-RUN with 0.5.1 |
| Angular production build | REQUIRES RE-RUN after Maven compile |
| Oracle runtime integration | NOT EXECUTED |

## Release gate

0.5.1 is a source QA candidate until `build-production.cmd` completes successfully on the user Windows environment and the resulting runtime reports `BUILD-VERSION=0.5.1`.
