# CLT-R6M | T2 and negative T3 | UI/UAT evidence contract

Version: Core Banking Prototype 0.11.0, based on successfully installed R6L-HF1.
Status: source implementation complete, real Windows/Oracle browser acceptance pending.

## T2 - Reference group and type UI

- Change the row detail from end-of-page inline panel into a centered native modal dialog.
- Modal MUST support Escape, focus restoration to the triggering View button, and table scroll preservation.
- Separate technical database column metadata behind an expandable disclosure.
- Explicit Apply Search and Clear Search with a stable applied-filter state.
- Search group code with ASCII/Persian/Arabic digits and search approved group titles.
- Verify pagination with actual 28 Oracle active groups: first page 20, second page 8.
- Verify empty active type catalog: zero records is an allowed source state, not an HTTP error.
- Never present failed 401/403/503 requests as genuine empty data.

## T3 - Negative/empty-state boundaries

- A failed registry lookup is visually distinct from an authorized empty registry list.
- Introduction requires an approved real collateral type; lack of types cannot create a fake option.
- Invalid regulatory identifier is rejected; no positive registration UAT is claimed.
- No collateral finalization, capacity allocation or financial write is enabled.

## Evidence and acceptance

| Gate | Expected evidence | Status at packaging |
|---|---|---|
| T1 | R6L-HF1 Chrome E2E and analyst screenshot | PASSED |
| T2 source and pure tests | CLT_R6M_STATIC_PASS and CLT_R6J_REFERENCE_UI_SMOKE_PASS | TESTABLE |
| T2 real browser | CLT_R6M_T2_BROWSER_PASS: 28 / 20+8, code, title, modal/Escape, 0 types | PENDING_WINDOWS_UAT |
| T3 negative UI | Empty/error distinction and invalid-regulatory-number contract | SOURCE_QA; positive workflow gated |
| T4 DATA-01 | Bank-approved collateral types and full policy mapping | BLOCKED_BY_APPROVED_SOURCE |

No Oracle DDL/DML, no IAM change, no modification to R0-R7 phase boundaries.

## User-facing note (FA)

در نسخه جدید، جزئیات در Modal نمایش داده می‌شود؛ عملیات تنها خواندنی هست.
داده‌های ساختگی یا نوع وثیقه بدون مصوبه بانک ثبت نخواهند شد.
