# DPS2 0.10.0 — Phase 10F Final Runtime Qualification

Date: 2026-09-21

## Result

Real Oracle/Spring Boot runtime qualification completed successfully for all four target deposit families.

| Family | Opening Request | Account | Result |
|---|---:|---:|---|
| QARD_SAVINGS | 4 | 4 | PASS |
| CURRENT_ACCOUNT | 5 | 5 | PASS |
| SHORT_TERM_DEPOSIT | 6 | 6 | PASS |
| LONG_TERM_DEPOSIT | 7 | 7 | PASS |

Successful sequence for every family:

`Validate → Persist → Create Account → Settlement → Readiness → Activate`

Final marker:

```text
PHASE10F_RUNTIME_E2E_PASS
```

## Runtime issues discovered and closed during qualification

- Phase 7 unique/index idempotency naming drift.
- Phase 10 covering-index idempotency drift.
- Missing Phase 10 reference tables in the runtime allow-list.
- CASH funding / cash-management transaction reference mismatch.
- Missing `CREATED_AT DEFAULT SYSTIMESTAMP` on installed opening tables.
- Obsolete created-account ID/number pair constraints.
- Installed `DEPOSIT_OPENING_FUND_ALLOC` column drift (`ALLOCATED_AMOUNT` vs legacy `ALLOCATION_AMOUNT`).
- Opening-check result-status constraint drift.
- Controlled Readiness evidence expiry after a long debugging interval; fresh evidence produced READY and the test harness is now hardened against stale controlled evidence.

No final business pass was claimed until the actual marker `PHASE10F_RUNTIME_E2E_PASS` was produced.
