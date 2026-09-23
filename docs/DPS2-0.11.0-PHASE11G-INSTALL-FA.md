# راهنمای نصب DPS2 0.11.0 — Phase 11G

## ترتیب اجرا

این فاز Java source، Angular source و Oracle schema جدید دارد؛ بنابراین Build/Restart الزامی است.

```bat
cd /d D:\Projects\core-banking-prototype-v0

set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
node tools\verify-dps2-profit-processing-11g.mjs

bin\stop.cmd

rem CORE_BANKING_ORACLE_CONNECT باید مطابق محیط فعلی Oracle تنظیم شده باشد.
tools\apply-dps2-phase11g.cmd

build-production.cmd

bin\start.cmd
```

`bin\start.cmd` برنامه را در همان پنجره اجرا می‌کند. پس از مشاهده Start موفق، در یک CMD دوم اجرا شود:

```bat
cd /d D:\Projects\core-banking-prototype-v0
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091

rem ابتدا Configuration حاکمیتی Pricing / Profit Payment را کنترل کنید.
node tools\reconcile-pdl-profit-config-phase11g.mjs
rem در صورت نمایش Missing configuration، سپس:
rem node tools\reconcile-pdl-profit-config-phase11g.mjs --apply

node tools\runtime-dps2-phase11g-e2e.mjs
```

## Gateهای مورد انتظار

Static:

```text
PHASE11G_STATIC_VERIFIER_PASS=92
PHASE11G_STATIC_VERIFIER_FAIL=0
PHASE11G_STATIC_BASELINE_PASS
```

Database:

```text
PHASE11G_DB_BASELINE_PASS
PHASE11G_IMPLEMENTATION_PASS
```

Runtime:

```text
PHASE11G_RUNTIME_E2E_PROFIT_CONTRACT=...
PHASE11G_RUNTIME_E2E_ACCRUAL=...
PHASE11G_RUNTIME_E2E_POSTING=...
PHASE11G_RUNTIME_E2E_PASS
```

## سازگاری با DDL Legacy فعلی

در دیتابیس فعلی `DEPOSIT_PROFIT_ACCRUAL` هم‌زمان ستون‌های Legacy و Canonical 11G را دارد. Repository فاز 11G در صورت تشخیص کامل Projection قدیمی، هر دو Projection را در یک INSERT می‌نویسد. بنابراین ستون‌های Legacy مالی مانند `ACCRUAL_DATE`, `BALANCE_BASIS_AMOUNT`, `RATE_VALUE`, `DAY_FRACTION`, `ACCRUAL_AMOUNT` و `ACCRUAL_STATUS_CODE` نباید صرفاً nullable شوند.

`PROFIT_PERIOD_ID` استثنا است: هویت آن متعلق به Aggregate قدیمی `DEPOSIT_PROFIT_PERIOD -> DEPOSIT_ACCOUNT_PROFIT_PROFILE` است و 11G آن را جعل نمی‌کند؛ برای ردیف‌های Canonical nullable باقی می‌ماند.

بعد از نصب Hotfix Dual-write، Build/Restart الزامی است چون `DepositProfitRepository` تغییر کرده است.

## مرزبندی

- Accrual بر اساس Operational Profit Contract Snapshot انجام می‌شود.
- Same-deposit posting فقط از primitive فاز 11D عبور می‌کند.
- 11G مستقیماً `DEPOSIT_TRANSACTION` ایجاد نمی‌کند.
- پرداخت به `LINKED_ACCOUNT` یا `CUSTOMER_SELECTED_ACCOUNT` تا Phase 11H Deferred است.
