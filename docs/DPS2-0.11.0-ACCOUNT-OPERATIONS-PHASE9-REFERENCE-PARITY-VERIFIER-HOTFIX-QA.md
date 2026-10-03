# DPS2 0.11.0 — Account Operations Phase 9 Reference-Parity Verifier Hotfix

## Problem
After Account Operations was aligned with the approved Persian operational HTML, the production build stopped in the legacy Phase 9 static guard with `controlled Servicing UX is incomplete` even though the new UI retained controlled closure/reopening actions.

## Root cause
The Phase 9 verifier accepted only the old literal `ACTIVE → CLOSED` plus `بستن حساب`, or the obsolete English marker `Controlled Closure / Reopening`. The reference-parity UI now uses the approved Persian labels `بستن و بازگشایی کنترل‌شده حساب` and `ثبت درخواست بستن حساب`, and renders the CLOSED branch explicitly.

## Fix
The verifier now accepts three equivalent controlled-closure presentations:
1. legacy `ACTIVE → CLOSED` contract;
2. legacy English `Controlled Closure / Reopening` marker;
3. current Persian reference-parity contract requiring the controlled-closure heading, closure-request action, and explicit CLOSED state branch.

No runtime API, business rule, Oracle DDL, or Account Operations component logic is changed by this hotfix.

## Phase 11E compatibility
The Phase 11E closure/reopening verifier had the same obsolete English-only UI marker. It is updated to accept the current Persian reference-parity closure/reopening contract while retaining the existing API, state-transition, Hold/Reservation, approval, idempotency, database and runtime checks.

## Phase 11B compatibility
The 11B servicing verifier also used the obsolete English-only `Servicing History` UI marker. It now accepts the approved Persian reference heading `تاریخچه نگهداری حساب`; the underlying `detail.servicingHistory` rendering and servicing APIs remain unchanged.

## Phase 11C compatibility
The 11C lifecycle/hold verifier used old English UI labels (`Hold / Block`, `Status History`). It now accepts their approved Persian reference equivalents (`مسدودی دستی/کنترلی غیر وثیقه‌ای`, `تاریخچه وضعیت حساب`) while retaining the Phase 11C/11E boundary requirement.

## Phase 11D compatibility
The Package 17 UI guard used the old English labels `Balance / Package 17`, `Subledger`, and `Balance Reservations`. It now accepts the current reference-parity headings `مانده و وضعیت قابل برداشت`, `دفتر معین سپرده`, and `رزروهای مانده`, while the existing ledger/availability boundary check remains unchanged.

## Phase 11F compatibility
The term-operations verifier now accepts the Persian reference title `عملیات سپرده مدت‌دار` and the current explicit Step 03/05/11E closure handoff marker, while still requiring the Step 03/04/05 ownership text and Phase 11F / Package 13 marker.

## Phase 11I compatibility
The Step 05 UI guard now accepts the localized `Legs/دفتر معین سپرده` label in addition to the legacy `Legs/Subledger` wording; the Phase 11I marker and the transaction action wiring remain mandatory.

## Phase 11K compatibility
The Step 08 maturity-action guard now accepts the Persian reference wording for batch maturity processing in addition to the legacy `Maturity Batch` label. The `runMaturityBatch()` wiring and Phase 11K marker remain required.
