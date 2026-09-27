# DPS2 0.11.0 — Known Issues / Deferred Items برای Fast-Track RC

## 1. نوع بسته

این تحویل یک **Source RC** است و `app/core-banking-prototype.jar` داخل ZIP قرار نگرفته است. در اولین استقرار UAT باید `build-production.cmd` اجرا شود. این وضعیت Packaging است، نه Functional Gap.

## 2. Test Debt عمدی

این RC به دلیل Fast-Track با وضعیت زیر تحویل می‌شود:

```text
FUNCTIONAL_IMPLEMENTATION_COMPLETE
LATEST_DELTA_TEST_DEFERRED
```

مواردی که بعداً باید اجرا شوند:

1. Delta Regression برای آخرین تغییرات Steps 11–15 و 17.
2. Full Regression مجدد Steps 00–17.
3. Bug-fix-only Regression بعد از UAT.
4. Final Release Baseline/Freeze.

## 3. Baselineهای قبلاً Qualified

- Canonical Steps 00–17 قبل از Deltaهای نهایی Final Qualification شده‌اند.
- Step 16 NOSTRO/VOSTRO بعد از Delta نیز مستقل Runtime Qualified شده است.

## 4. مواردی که Known Functional Gap محسوب نمی‌شوند

- نبود Phase 12: در Reference Documents فعلی Scope بعد از Step 17 تعریف نشده است.
- نبود Seed کسب‌وکاری برای Product/Fee/Tax/Regulatory: Configuration باید Governed باشد و نبود آن ممکن است UAT را `BLOCKED_BY_ENV_OR_DATA` کند، نه اینکه الزاماً Bug باشد.
- Qualification Fixtureهای Step 16 ابزار QA هستند و جزو UAT Business Data نیستند.

## 5. Dependency Hygiene

آخرین Build ثبت‌شده قبل از Fast-Track Deltaها گزارش npm audit زیر را داشته است:

```text
15 vulnerabilities (11 moderate, 4 high)
```

این مورد در Qualification قبلی Blocker دامنه Deposit تلقی نشده، ولی قبل از Production Release باید در چرخه Dependency/Security جداگانه ارزیابی شود.

## 6. Legacy Documentation

`docs/install/INSTALL-0.11.0-FA.txt` یک Runbook تاریخی Phase 11C است و برای RC فعلی جامع نیست. برای تحویل UAT این سند مرجع است:

```text
docs/uat/UAT-DEPLOYMENT-RUNBOOK-FA.md
```

## 7. Bug Fix Policy در Fast-Track

بعد از این Handoff:

- Feature جدید: ممنوع
- Refactor غیرضروری: ممنوع
- Schema redesign بدون Bug قطعی: ممنوع
- Fix مجاز: فقط Defect مستند UAT/Regression یا blocker محیطی اثبات‌شده
