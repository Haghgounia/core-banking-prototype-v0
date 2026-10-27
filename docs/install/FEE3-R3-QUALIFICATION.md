# FEE3-BE-R3 | Non-mutating UAT qualification kit (R3-QA1)

Target: Core Banking Prototype with UI-R8+HF1 and BE-R1/R2/R3 already applied.
This is an additive, **verification-only** kit. It does not install Java or Angular sources,
change configuration, enable workflow, create Oracle objects, or modify bank data.

## Installation (Windows CMD)

Extract the ZIP in the project root; ensure it adds only files below
`tools\fee3-backend\qualification\r3`, `docs\install` and `release`.
Do **not** replace modified FEE3 application sources with an old baseline.

```bat
cd /d D:\Projects\core-banking-prototype-v0
call tools\fee3-backend\qualification\r3\run-source.cmd
```

`run-source.cmd` checks SHA-256 of installed R3 files against its release manifest,
then runs the R1/R2/R3 structural validators, the R2 mapping tests, and the standalone
Java math/governance probe. Java 21+, Node and Python 3.11+ should be on PATH.
It writes a local JSON report `docs\install\FEE3-R3-SOURCE-RESULT.json`.
The script handles UTF-8 explicitly on Windows.

## Full project build (must be run by the local development team)

```bat
cd /d D:\Projects\core-banking-prototype-v0
bin\stop.cmd
build-production.cmd
```

Do not claim `BUILD SUCCESS` unless both the backend Maven build and the frontend
Angular AOT production build complete on the Windows machine. Dependency download
may be necessary on the first run. The local qualification kit cannot substitute
for the actual Maven and Angular build.

## Oracle metadata and baseline (no writes)

Open `docs\install\FEE3-R3-ORACLE-UAT-BASELINE-READONLY.sql` in SQL Developer.
Run all sections against the intended UAT connection, preferably with a read-only user.
Save results before and after the HTTP smoke, including row totals and status counts.
All 11 named tables and both named sequences should be visible; constraints relevant
to state transitions should be enabled and validated. The three UAT safeguards
(activated/published tariffs, Maker=Checker decisions, applied change requests)
should each return zero. If historical data already violates a safeguard, stop and
reconcile without running destructive cleanup SQL.

## HTTP verification (non-mutating)

Only after the backend is started, run:

```bat
cd /d D:\Projects\core-banking-prototype-v0
call tools\fee3-backend\qualification\r3\run-runtime-readonly.cmd
```

This verifies Oracle readiness, real catalog/reference GET endpoints, default-deny
flags, one positive validation and preview, and negative date, currency and formula
cases. It checks item/version counts before and after. **The only HTTP POST calls
are to `/api/v1/fee3/validation` and `/api/v1/fee3/preview`.** There are no
`POST /drafts`, `PUT /drafts`, `POST /workflow/submit` or decision requests.
It allows only local loopback endpoints and never sends credentials or UAT tokens.

The test stops before sending POSTs if draft-writing is enabled or any governance
flag/authentication-role check fails. Verify the Oracle baseline again afterward.
The resulting JSON report is `docs\install\FEE3-R3-RUNTIME-RESULT.json`.

## Acceptance conditions

- Source checks 100% PASS on current R3 files (or reconcile version drift first).
- Both Maven/Spring Boot and Angular production builds successful in the target environment.
- R3 HTTP qualification passes with `draftWritesEnabled=false`, three workflow flags
  disabled and `postingEnabled=false`.
- Oracle pre/post row counts and state counts are unchanged.
- No UAT tariff is ACTIVE or published; no UAT change request is APPLIED.
- Preview is marked `SIMULATED`, `persisted=false`, `financialPosting=false`.
- No assumption that a simulated calculation constitutes certified bank execution.

**Not included:** real Maker/Checker transaction test, publishing, fee collection,
posting, security certification or oracle write UAT. Those require a separate
approved UAT window, backup and real organizational identities.

## Evidence handoff

Keep `FEE3-R3-SOURCE-RESULT.json`, `FEE3-R3-RUNTIME-RESULT.json`, the final
`build-production.cmd` output, and both copies of the Oracle read-only query results.
Do not include passwords, tokens, personal data or full connection strings in logs.

## Optional smoke-tool selftest

```bat
python tools\fee3-backend\qualification\r3\selftest.py
```

This starts a temporary local HTTP stub on a random loopback port, then checks
23 successful read-only scenarios and three fail-closed situations. This test
does not connect to Oracle or use the actual Spring Boot application.
