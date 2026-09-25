# DPS2 0.11.0 — Phase 11I Compile Hotfix QA

## Trigger

Windows `build-production.cmd` failed during backend compilation at `DepositTransactionService.java:130`:

```text
incompatible types: long cannot be converted to java.lang.String
```

## Root cause

`DepositAccountNotFoundException` exposes a constructor accepting `String message`, while the Step 05 service passed the numeric `accountId` directly.

## Fix

`requireAccount(long id, boolean lock)` now throws:

```java
new DepositAccountNotFoundException("حساب سپرده با شناسه "+id+" یافت نشد.")
```

This matches the established pattern in Deposit Balance, Profit, Closure, Term and Servicing services.

## Regression guard

`verify-dps2-step05-transaction-processing-11i.mjs` now rejects the numeric-constructor form and requires the String-message form.

Expected static gate after the hotfix:

```text
PHASE11I_STATIC_VERIFIER_PASS=57
PHASE11I_STATIC_VERIFIER_FAIL=0
PHASE11I_STATIC_BASELINE_PASS
```

## Database impact

None. The Oracle 11I migration and verifier had already passed:

```text
PHASE11I_DB_VERIFIER_PASS=19
PHASE11I_DB_VERIFIER_FAIL=0
PHASE11I_DB_BASELINE_PASS
PHASE11I_IMPLEMENTATION_PASS
```

No database re-apply is required for this hotfix.
