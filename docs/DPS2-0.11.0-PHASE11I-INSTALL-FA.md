# نصب و Qualification فاز 11I — Step 05 Transaction Processing

1. Patch را روی ریشه پروژه Extract/Replace کنید.
2. Static Gate:

```bat
node tools\verify-dps2-step05-transaction-processing-11i.mjs
```

3. Oracle reconciliation + DB verifier:

```bat
tools\apply-dps2-phase11i.cmd
```

4. چون Backend و Angular تغییر کرده‌اند:

```bat
bin\stop.cmd
build-production.cmd
bin\start.cmd
```

5. Runtime E2E:

```bat
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11i-e2e.mjs
```

Phase 11I فقط زمانی `CLOSED` می‌شود که Static + DB + Runtime همگی PASS باشند.

## Expected Source Gate

```text
PHASE11I_STATIC_VERIFIER_PASS=64
PHASE11I_STATIC_VERIFIER_FAIL=0
PHASE11I_STATIC_BASELINE_PASS
```

Expected DB gate after `tools\apply-dps2-phase11i.cmd`:

```text
PHASE11I_DB_VERIFIER_PASS=29
PHASE11I_DB_VERIFIER_FAIL=0
PHASE11I_DB_BASELINE_PASS
PHASE11I_IMPLEMENTATION_PASS
```

Expected final runtime marker:

```text
PHASE11I_RUNTIME_E2E_PASS
```
