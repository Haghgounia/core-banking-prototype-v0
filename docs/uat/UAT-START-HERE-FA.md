# DPS2 0.11.0 — شروع سریع UAT طراح / کسب‌وکار

**نسخه:** 0.11.0  
**تاریخ بسته:** 2026-09-27  
**وضعیت:** `DESIGNER_BUSINESS_RC / FUNCTIONAL_IMPLEMENTATION_COMPLETE / LATEST_DELTA_TEST_DEFERRED`

## هدف

این بسته نقطه شروع رسمی UAT برای دامنه **Deposit Account Operations Steps 00–17** است. Scope کسب‌وکاری بر مبنای اسناد مرجع Deposit Account Operations Freeze شده و Feature جدید خارج از آن وارد این RC نشده است.

## قبل از شروع

1. `docs/uat/UAT-DEPLOYMENT-RUNBOOK-FA.md` را اجرا کنید.
2. `tools\uat-preflight.cmd` باید بدون تغییر Business Data عبور کند.
3. Preconditions هر سناریو را از `UAT-PRECONDITIONS-TEST-DATA-FA.md` آماده کنید.
4. سناریوها را به ترتیب `UAT-ACCEPTANCE-CHECKLIST-FA.md` اجرا کنید.
5. هر Failure را در `UAT-DEFECT-LOG-TEMPLATE-FA.md` ثبت کنید.

## Baseline و Delta

- Baseline Canonical Steps 00–17 قبلاً Final Qualification شده است.
- Step 16 NOSTRO/VOSTRO پس از Delta نیز Runtime Qualification مستقل گرفته است.
- Deltaهای نهایی Steps 11–15 و 17 از نظر پیاده‌سازی کامل‌اند، اما Heavy Regression آن‌ها طبق تصمیم پروژه به بعد از تحویل Fast-Track موکول شده است.

## قانون Scope Freeze

در UAT فقط این سه نوع خروجی مجاز است:

- `PASS`
- `FAIL / BUG`
- `BLOCKED_BY_ENV_OR_DATA`

درخواست Feature جدید در این RC وارد توسعه نمی‌شود و باید جداگانه به Backlog/Requirement بعدی منتقل شود.

## ترتیب پیشنهادی UAT

1. Smoke: Step 00، انتخاب حساب، Account 360
2. Account/Lifecycle: Steps 01–02
3. Term/Profit/Transactions: Steps 03–05
4. Statement/Limits/Maturity: Steps 06–08
5. Services/Party Access: Steps 09–10
6. Regulation/Pricing/Tax: Steps 11–13
7. Reconciliation/Exceptions: Steps 14–15
8. NOSTRO/VOSTRO: Step 16
9. Rewards/Lottery: Step 17

## اسناد این بسته

- `UAT-DEPLOYMENT-RUNBOOK-FA.md`
- `UAT-ACCEPTANCE-CHECKLIST-FA.md`
- `UAT-PRECONDITIONS-TEST-DATA-FA.md`
- `UAT-KNOWN-ISSUES-DEFERRED-FA.md`
- `UAT-DEFECT-LOG-TEMPLATE-FA.md`
- `UAT-HANDOFF-MANIFEST.txt`
