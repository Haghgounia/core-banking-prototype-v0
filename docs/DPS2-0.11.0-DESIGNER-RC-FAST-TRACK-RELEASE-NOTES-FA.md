# DPS2 0.11.0 — Designer / Business RC — Fast-Track Release Notes

**تاریخ:** 2026-09-27  
**وضعیت:** `FUNCTIONAL_IMPLEMENTATION_COMPLETE / FINAL_DELTA_TEST_DEFERRED`  
**Scope:** فقط سه سند مرجع Deposit Account Operations موجود در Repository  

## 1. هدف این RC

این بسته برای شروع سریع تست طراح/کسب‌وکار Consolidate شده است. Feature جدید خارج از فایل‌های مرجع اضافه نشده است.

## 2. Baseline تأییدشده قبل از Deltaهای نهایی

- Canonical Steps 00–17 قبلاً با Marker زیر Final Qualification شده‌اند:
  - `DPS2_PHASE11NK_STEPS00_17_FINAL_QUALIFICATION_PASS`
- Step 16 پس از Business Gap Closure با هر دو شاخه NOSTRO و VOSTRO Runtime Qualified شده است:
  - `DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_RUNTIME_PASS`
  - `DPS2_DESIGNER_STEP16_QUALIFICATION_PASS`

## 3. Deltaهای Functional بعد از Baseline

### Steps 09–13
- Notification Event و Delegation در UI به Backend واقعی متصل شدند.
- Reserve Position و Regulatory Report/Item Action واقعی شدند.
- Fee Rule/Tier authoring، Fee Assessment و Profitability Snapshot تکمیل شدند.
- Tax Adjustment execution، Liability، Payment و Reconciliation تکمیل شدند.

### Step 14
- Reconciliation Run/Item/Discrepancy از Suspense جدا شد.
- `DEPOSIT_SUSPENSE_OPEN_ITEM` فقط با Action مستقل «ثبت قلم باز» ایجاد می‌شود، مطابق RC.

### Step 15
- چهار نوع مستند `REVERSAL`, `CORRECTION`, `BACKDATED_CORRECTION`, `DUPLICATE_CANCEL` پشتیبانی می‌شوند.
- REVERSAL و DUPLICATE_CANCEL از Step 05 عبور می‌کنند؛ دو نوع دیگر Correction Entry ممیزی‌شده ثبت می‌کنند و Posting مالی جعل نمی‌کنند.

### Step 16
- Account Master مستقل NOSTRO/VOSTRO + Extension + Profile + Reconciliation.
- Product/Profile qualification fixture فقط از Product Builder API استفاده می‌کند.
- Run uniqueness به‌صورت account-scoped اصلاح شد.
- NOSTRO/USD و VOSTRO/EUR هر دو Runtime PASS شده‌اند.

### Step 17
- Reward Program + Eligibility Rule.
- Enrollment و eligibility شامل minimum balance، minimum active days و dormant exclusion.
- «تثبیت و قرعه‌کشی» یک Action کسب‌وکاری واحد است: Entry freeze -> weighted draw -> Winner.
- Prize Payment فقط از Step 05 transaction/subledger انجام می‌شود.

## 4. Test Debt عمداً Deferred

طبق تصمیم پروژه، بعد از آخرین Deltaهای Steps 14/15/17 تست سنگین اجرا نشده است. موارد زیر قبل از Production Final Baseline لازم‌اند:

1. Delta Regression برای Steps 11–15 و 17.
2. Full Regression مجدد Steps 00–17.
3. Designer / Business UAT.
4. Bug-fix-only cycle بر اساس UAT.
5. Final Release Baseline/Freeze.

این Deferred Test Debt به معنی Feature ناقص شناخته‌شده نیست؛ به معنی «پیاده‌سازی کامل ولی Qualification نهایی پس از Delta هنوز انجام نشده» است.

## 5. Scope Freeze

- Feature خارج از Reference Files ممنوع است.
- Phase 12/Step 18+ در اسناد فعلی تعریف نشده است.
- تا دریافت Requirement جدید، فقط Consolidation، Deferred Testing و Bug Fix مجاز است.

## 6. Designer / Business UAT Handoff Pack

برای تحویل رسمی UAT از مسیر زیر شروع کنید:

```text
docs/uat/UAT-START-HERE-FA.md
```

بسته شامل Runbook اجرا، Preconditions/Test Data، Acceptance Checklist برای Steps 00–17، Known/Deferred Items و Defect Template است. ابزار `tools\uat-preflight.cmd` فقط Preflight غیرمخرب انجام می‌دهد و Business Write/Qualification اجرا نمی‌کند.
