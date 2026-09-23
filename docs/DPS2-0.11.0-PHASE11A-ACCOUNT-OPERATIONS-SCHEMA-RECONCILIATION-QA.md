# DPS2 0.11.0 — Phase 11A Account Operations Schema Reconciliation QA

## هدف

Phase 11A نقطه شروع توسعه Account Operations پس از Freeze نسخه 0.10.0 است. این فاز هیچ Feature کسب‌وکاری جدیدی فعال نمی‌کند؛ فقط Driftهای فیزیکی موجود در DPS2 را با Contract عملیاتی حساب سپرده هم‌راستا می‌کند تا Phaseهای بعدی روی Schema متناقض ساخته نشوند.

## مبنا

آخرین DDL واقعی DPS2 نشان می‌دهد مدل Account Operations از قبل شامل Hold، Balance/Subledger، Transaction، Term، Profit، Closure/Reopening، Limit/Restriction و سایر جداول عملیاتی است. در عین حال چهار ناسازگاری مانع اجرای صحیح Lifecycle و Activation بودند:

1. `DEPOSIT_ACCOUNT` همزمان Constraint قدیمی سه‌حالته و Constraint جدید پنج‌حالته داشت.
2. `DEPOSIT_ACCOUNT_LIFECYCLE_EVENT` هنوز Eventهای Phase 4 (`CREATE/ACTIVATE/CLOSE`) را محدود می‌کرد.
3. `DEPOSIT_ACTIVATION_RUN.RUN_STATUS_CODE` از نوع `VARCHAR2` بود ولی Default عددی `0` داشت.
4. `DEPOSIT_ACCOUNT_EXT_REGISTRY.REGISTRATION_STATUS_CODE` از نوع `VARCHAR2` بود ولی Default عددی `0` داشت.

## Contract نهایی Phase 11A

### Account Status

```text
PENDING_ACTIVATION
ACTIVE
SUSPENDED
DORMANT
CLOSED
```

Constraint قدیمی `CK_DEP_ACCOUNT_STATUS` حذف می‌شود و فقط Contract canonical باقی می‌ماند.

### Lifecycle Event

```text
CREATE
ACTIVATE
SUSPEND
REACTIVATE
MARK_DORMANT
CLOSE
REOPEN
```

Constraint قدیمی `CK_DEP_ACCT_EVT_TYPE` حذف می‌شود و Constraint canonical جدید روی همین هفت Event فعال/Validate می‌شود.

### Activation Run Default

```text
RUN_STATUS_CODE DEFAULT 'STARTED'
```

### External Registry Default

```text
REGISTRATION_STATUS_CODE DEFAULT 'NOT_SENT'
```

## Safety Contract

Migration:

`database/oracle/dps2/migrations/0.11.0-phase11a-account-operations-schema-reconciliation.sql`

ویژگی‌ها:

- `WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK`
- idempotent و safe-to-rerun
- بدون `INSERT / UPDATE / DELETE / MERGE`
- بدون تغییر Business Row
- قبل از DDL، وجود Table/Columnهای لازم را کنترل می‌کند
- Constraintهای canonical با `ENABLE VALIDATE` نصب می‌شوند

## Verification

Static verifier:

```bat
node tools\verify-dps2-account-operations-schema-reconciliation-11a.mjs
```

Marker مورد انتظار:

```text
PHASE11A_STATIC_BASELINE_PASS
```

DB verifier بعد از اجرای Migration:

```text
database/oracle/dps2/verification/0.11.0-phase11a-account-operations-schema-verifier.sql
```

Marker مورد انتظار:

```text
PHASE11A_DB_BASELINE_PASS
```

## Non-goals

Phase 11A عمداً این موارد را پیاده‌سازی نمی‌کند:

- API مربوط به Suspend / Dormant / Reactivate / Reopen
- Hold/Release business service
- Balance/Subledger posting
- Closure workflow
- Term/Profit/Transaction runtime
- Angular Account Operations UI جدید

این موارد از Phase 11B به بعد پیاده‌سازی می‌شوند.

## Exit Criteria

Phase 11A فقط زمانی CLOSED است که:

1. Static verifier PASS باشد.
2. Migration روی Oracle target بدون خطا اجرا شود.
3. DB verifier `PHASE11A_DB_BASELINE_PASS` بدهد.
4. Build regression قبلی همچنان PASS بماند.
