# CIF 0.3.29 — FIX40: Persian lifecycle status, unified onboarding stepper, and employment occupation FK alignment

## Scope
- Main Party grid
- Party onboarding navigation
- Person employment save flow (`CIF.PARTY_EMPLOYMENT`)

## Root Cause Analysis
1. **Grid lifecycle status**
   - `party-list.component.html` rendered `row.lifecycleStatusCode` directly.
   - Result: lifecycle values such as `ACTIVE` appeared in Latin while other columns had already been localized.

2. **Confusing onboarding navigation**
   - Several onboarding pages used page-local hard-coded progress strips.
   - Some contexts effectively exposed 9 high-level items while other contexts exposed 10+ atomic items, with no governing hierarchy.
   - Result: operator could not clearly distinguish macro phase from detailed step.

3. **Employment reference-FK error**
   - UI occupation search was bound to GEO employment catalogs (`job-groups` / `jobs`).
   - Persistence target `CIF.PARTY_EMPLOYMENT.OCCUPATION_CODE` is governed by `CIF.REF_OCCUPATION`, not the GEO job catalog.
   - Result: a value selected from GEO (for example a detailed job title like a blacksmith/trades job) could be valid for lookup display but invalid for Oracle FK at save time, producing the localized message: «یکی از کدهای مرجع ارسالی در جدول مرجع متناظر وجود ندارد.»

## Implemented Fix
- Added Persian resolver for lifecycle status in the main Party grid.
- Introduced one shared standalone component `PartyOnboardingStepperComponent` and applied it across the onboarding/operations journey.
- Step model reorganized into **6 phases / 10 substeps**:
  1. هویت پایه → 1 substep
  2. اطلاعات ارتباطی → 1 substep
  3. نمایه اقتصادی → 1 substep
  4. هویت تکمیلی و ساختار → 4 substeps
  5. کنترل‌های انطباق → 2 substeps
  6. کنترل نهایی → 1 substep
- Employment occupation control now uses `cif/reference/ref-occupation` instead of GEO jobs for persisted occupation selection.
- Reference seed extended with `HOMEMAKER` and `UNEMPLOYED`.

## Regression Checklist
- [ ] Main grid shows Persian lifecycle, verification and data-quality labels.
- [ ] Each onboarding page shows the same nested stepper model.
- [ ] Current page highlights the current substep and enclosing phase.
- [ ] Saving a PERSON employment record with valid occupation/status no longer raises Oracle FK/reference error.
- [ ] Existing edit flow still reloads and updates employment records correctly.
