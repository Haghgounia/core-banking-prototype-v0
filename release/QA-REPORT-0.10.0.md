# QA Report — Core Banking Prototype 0.10.0 FINAL

**Closure date:** 2026-09-21  
**Release:** `0.10.0`  
**Qualified backend runtime:** `0.10.0-SNAPSHOT` under the existing project versioning convention  
**Target runtime:** Spring Boot 4.1.0 / Java 21 / Oracle 26ai Free / port 8091

## 1. Final status

The Four Deposits Phase 10F business qualification is complete. The real target runtime produced:

```text
PHASE10F_RUNTIME_E2E_PASS
```

for all four families:

| Family | Opening Request | Account | Final result |
|---|---:|---:|---|
| `QARD_SAVINGS` | 4 | 4 | PASS / ACTIVE |
| `CURRENT_ACCOUNT` | 5 | 5 | PASS / ACTIVE |
| `SHORT_TERM_DEPOSIT` | 6 | 6 | PASS / ACTIVE |
| `LONG_TERM_DEPOSIT` | 7 | 7 | PASS / ACTIVE |

Every family completed the same mandatory sequence:

`Validate → Persist → Create Account → Settlement → Readiness → Activate`

## 2. Final static/source gates on the consolidated baseline

The final source baseline, after absorbing the runtime hotfixes, passed the closure verifier with:

```text
FINAL_SOURCE_VERIFIER_PASS=30
FINAL_SOURCE_VERIFIER_FAIL=0
FINAL_SOURCE_BASELINE_PASS
```

Key component results:

- Phase 7 E2E/runtime hardening: **22/22 PASS**
- Account schema reconciliation 0.9.2: **20/20 PASS**
- Reference/FK reconciliation 0.9.3: **19/19 PASS**
- Phase 10 foundation: **34/34 PASS**
- Phase 10B workflow/runtime alignment: **31/31 PASS**
- Phase 10D UI/integration: **25/25 PASS**
- Phase 10E/10F: **35/35 PASS**
- Phase 10F FUND_ALLOC contract: **16/16 PASS**
- Runtime artifact contract: **11 checks PASS**
- Release layout: **PASS**

The complete `build-production.sh` static sequence was also executed in the isolated packaging environment. All source/static checks reached the Maven bootstrap boundary successfully. The only failure was external bootstrap access:

```text
wget: Failed to fetch https://repo.maven.apache.org/.../apache-maven-3.9.16-bin.zip
```

Therefore no source/static regression was observed in the consolidated baseline. Final Java/Angular packaging remains a Windows workstation gate where Maven/npm dependencies are available.

## 3. Oracle runtime qualification evidence

Earlier runtime readiness was confirmed as `READY_WITH_WARNINGS`; the non-blocking warnings represented external service contracts (`ACCOUNT_SERVICE`, `TAX_PROFILE_SERVICE`) rather than fabricated integrations.

During Phase 10F, real Oracle execution uncovered and resolved these installed-schema/source contract drifts:

1. Phase 7 idempotency/index name drift.
2. Phase 10 existing-index rerun drift (`ORA-01408`).
3. Missing Operational v5 reference tables in the runtime allow-list.
4. CASH funding `CASH_MANAGEMENT_TXN_REF` contract mismatch.
5. `CREATED_AT NOT NULL` columns without `DEFAULT SYSTIMESTAMP`.
6. Obsolete `CREATED_ACCOUNT_ID` + `CREATED_ACCOUNT_NO` paired constraints.
7. `DEPOSIT_OPENING_FUND_ALLOC` installed-schema drift: missing `ALLOCATED_AMOUNT` and legacy `ALLOCATION_AMOUNT`.
8. Opening-check result constraint not accepting the canonical `FAIL` value.
9. Controlled qualification evidence expired during the extended debugging window; fresh evidence passed. The test harness is now hardened so controlled evidence validity is refreshed immediately before Readiness.

## 4. Final database closure gate

The frozen source now contains a read-only Oracle verifier:

`database/oracle/dps2/verification/0.10.0-final-verifier.sql`

It checks, among other items:

- Deposit Account tables/sequences/status constraint.
- Phase 5 append-only triggers.
- Semantic idempotency unique guards.
- Canonical `ALLOCATED_AMOUNT NUMBER(19,4)` and absence of legacy `ALLOCATION_AMOUNT`.
- Final created-account integration-reference constraint.
- Five canonical Opening Check result statuses.
- Operational v5 reference catalogs.
- `CREATED_AT` default coverage.
- Four Phase 10F PDL products/current versions.
- Evidence that all four P10F product families have reached `ACTIVE` accounts.

Expected marker:

```text
FINAL_DB_BASELINE_PASS
```

## 5. Runtime artifact closure gate

The single Windows closure command is:

```bat
tools\final-verify-0.10.0.cmd
```

It requires:

```bat
set CORE_BANKING_ORACLE_CONNECT=<user/password@//host:port/service>
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
```

and validates:

1. Frozen source/static baseline plus runtime JAR/BUILD-VERSION/log version.
2. Oracle DB/reference/constraint baseline.
3. Spring Boot Actuator health `UP`.

Final marker:

```text
FINAL_RELEASE_CLOSURE_PASS
```

## 6. Release decision

Business Phase 10F is **CLOSED**. Release-management closure is **prepared** and becomes formally frozen when the target Windows/JAR/Oracle environment produces `FINAL_RELEASE_CLOSURE_PASS` after the final rebuild/restart.
