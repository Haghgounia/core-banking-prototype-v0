# Release Notes — 0.11.0 (Development Baseline)

## Phase 11A — Account Operations Schema Reconciliation

- Started the post-0.10.0 Account Operations implementation line.
- Preserved 0.10.0 Opening/Activation runtime baseline unchanged.
- Reconciled the legacy three-state `DEPOSIT_ACCOUNT` status guard with the canonical five-state contract.
- Expanded lifecycle event type contract to support `SUSPEND`, `REACTIVATE`, `MARK_DORMANT` and `REOPEN` in addition to existing events.
- Corrected `DEPOSIT_ACTIVATION_RUN.RUN_STATUS_CODE` default to `STARTED`.
- Corrected `DEPOSIT_ACCOUNT_EXT_REGISTRY.REGISTRATION_STATUS_CODE` default to `NOT_SENT`.
- Added a static Phase 11A verifier and a read-only Oracle DB verifier.
- Added Phase 11A verification to Windows/Unix production build gates and source packaging.

No Account Operations business API is enabled by 11A; those behaviors begin in Phase 11B.

## Phase 11B — Account Servicing Core + Account Party
- Promotes Party relationships from opening-only data to persistent account-level servicing data.
- Adds account basic-info, account-party and account-contact APIs.
- Adds Servicing History read/write integration.
- Updates Account 360 UI for Party, Contact and servicing changes.
- Keeps Hold/Lifecycle state transitions out of scope until Phase 11C.

## Phase 11C — Lifecycle + Hold/Block
Account Operations now supports controlled suspension, dormancy and reactivation plus persistent Hold/Block controls with idempotency and history. Balance/Subledger effects are intentionally deferred to Phase 11D; Controlled Closure/Reopening remains Phase 11E.

### Phase 11C Angular compile hotfix
- Fixed Deposit Opening funding-account ownership verification to use active Account Party rows (`details.parties`) instead of the removed legacy `details.owners` contract.
- Added fail-fast detection for a locked runtime JAR on Windows production builds.
- No database change is required.


## CLT-R6L / T1 — آزمون مرورگر واقعی و ثبت سند پوشش (2026-11-07)

- رفع نارسایی تشخیص HTTP در فرم اطلاعات پایه، جلوگیری از بازاستفاده پاسخ خطای کش‌شده، و افزودن نشانگر تشخیصی امن در فیلتر محلی GET-only.
- ارائه آزمون خودکار Chrome Headless برای نمایش ۲۸ رکورد واقعی در Grid Angular (نه صرفاً HTTP 200). تا اجرای واقعی در Windows/Oracle، نتیجه Browser E2E تأیید نشده است.
- ثبت `docs/collateral/CLT-STEP01-FEATURE-COVERAGE-DATA-GATES-FA.md` به‌عنوان ماتریس پوشش ۱۶ BUC / ۲۰ SUC / ۱۵ UI و گیت‌های داده، با ارجاع از نقشه راه.
- IAM، سیاست‌های مرجع، داده‌های Oracle و عملیات مالی تغییری نکرده‌اند. `COLLATERAL_TYPE` و `COLLATERAL` همچنان نیازمند داده مصوب هستند.


## CLT-R6M / T2 + negative T3 — Read-only UI and browser gate (2026-11-07)
- Native accessible modal for collateral reference details, Escape close and focus restoration; technical columns moved to disclosure.
- Explicit search action, clear action and applied filter separated from draft text; preserves 20/8 page behavior.
- Type empty state vs authorization error and registry empty/error differentiation.
- No new database/API write, no IAM modifications. Browser gate records its own receipt; no automatic PASS from a successful Maven build.
- Detailed coverage and status: docs/collateral/CLT-R6M-T2-T3-UI-UAT-FA.md
