# راهنمای اجرای Phase 11H — Step 04 Canonical Profit Completion

## پیش‌نیاز

- Source پروژه شامل Phase 11G نهایی باشد.
- Oracle DPS2 در دسترس باشد.
- `CORE_BANKING_ORACLE_CONNECT` در همان CMD تنظیم شده باشد.
- Backend برای اجرای Runtime روی `http://127.0.0.1:8091` در دسترس باشد.

## 1. Static Gate

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-dps2-step04-canonical-profit-11h.mjs
```

باید `PHASE11H_STATIC_BASELINE_PASS` دریافت شود.

## 2. Oracle Reconciliation + DB Gate

```bat
tools\apply-dps2-phase11h.cmd
```

این مرحله Profile/Period/Detail/Payment evidence را برای داده‌های واجد شرایط موجود Backfill می‌کند و canonical invariants را Verify می‌کند.

## 3. Build و Restart

چون Java و Angular تغییر کرده‌اند:

```bat
bin\stop.cmd
build-production.cmd
bin\start.cmd
```

## 4. Runtime Qualification

در CMD دوم:

```bat
cd /d D:\Projects\core-banking-prototype-v0
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\runtime-dps2-phase11h-e2e.mjs
```

Closure فنی فقط با `PHASE11H_RUNTIME_E2E_PASS` مجاز است.

## 5. مرز سندی

11H فقط destination `SAME_DEPOSIT` را از Step 04 کامل می‌کند. `LINKED_ACCOUNT` و `CUSTOMER_SELECTED_ACCOUNT` به Step 05 Transaction Processing وابسته‌اند و Step 04 تا تکمیل آن dependency `PARTIAL` باقی می‌ماند.
