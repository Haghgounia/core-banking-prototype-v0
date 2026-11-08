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


## CLT-R3C / Insurance Company catalog & analyst handoff (2026-11-07)

- Direct collateral menu and insurance panel links to the already supported, authenticated, read-only `CLT.INSURANCE_COMPANY` reference page.
- UI makes candidate/temporary `TMP-INS-1403-*` codes, inactive status and pending license/business approval explicit; candidate data is never shown as operationally eligible in insurance selector.
- Generic proposal UI disabled for insurance-company reference until governing license and approval model is designed; no data updates or auto-activation.
- R6M modal, search and paging maintained. Build, GET-only runtime verification and analyst UAT are distinct gates.
- Full source diff, manifest, independent QA and UAT cases in CLT-R3C release bundle; R0–R7 unchanged.

## CLT-RF1 — 17-reference read integration foundation (2026-11-07)

- Integrated R3C insurer catalog UI with the 2026-11-07 16:52 full prototype baseline.
- Added local-only `GET /api/v1/collateral/reference/uat-matrix` with two bounded SELECT checks (COUNT and allowlisted columns) per each of the 17 CLT reference tables.
- Introduced an automatic GET-only 17/17 real API contract gate and JSON report; no assumption that registry group 200 implies all reference tables 200.
- Reference landing page shows real connection states; 401/403/503 and empty table states remain separate; no-cache nonce protects against stale denials.
- Insurer candidates remain inactive, with temporary codes and no automatic eligibility, activation, or Oracle writes.
- Historical R0–R7 roadmap unchanged; runtime/JUnit and analyst UAT must be independently evidenced.

## CLT-R3D — Insurance analyst form preview in the real R3 component (2026-11-07)

- Enables business analysts to open the **same R3 insurance Angular form** from the existing `/collateral/uat` workspace even while `CLT.COLLATERAL` has zero real rows; does not fabricate a collateral entity.
- Fetches insurer candidates, insurance types, and active collateral-type counts exclusively through the existing authenticated GET reference APIs; statuses remain source-faithful and incomplete pages are not interpreted as zero active.
- Analyst preview forbids all per-collateral service calls, insurance validation POST and staging-request POST; unavailable references cannot be selected as authorized insurer options.
- Adds browser-local form-shape checks and eight R3-only analyst UAT evidence cases with local JSON export. Original ten R6 UAT cases are unchanged.
- No backend, Oracle, IAM, release version or operational writer feature-flag modification. G-04/G-06/G-09, official data approval and full R3 UAT remain open.
- Reference: `docs/collateral/CLT-R3D-INSURANCE-ANALYST-UAT-FA.md`.


### CLT-R3E — Consolidated analyst UAT UX correction (2026-11-07)
- Technical R6D/R6E diagnostics moved into a collapsed optional section to keep the analyst-facing form and eight R3 insurance test cases in focus.
- The production R3 insurance form is reused; analyst preview now distinguishes the current backend-mandated deposit number from an unapproved rule for non-deposit collateral.
- Analyst-only annotations document unresolved received insurance amount definition, beneficiary, insured subject, documents, insurance currency source and policy/coverage gates.
- No Oracle DDL/DML/COMMIT, no backend or IAM modification, no change to active-only insurer choice or real validation flow.
- R3E QA is source/static until Windows Angular build and analyst visual UAT.
