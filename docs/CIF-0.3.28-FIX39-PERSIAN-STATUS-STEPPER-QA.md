# CIF 0.3.28 - FIX39 - Persian Grid Status + Party Create Stepper QA

## Scope

Presentation-only improvement for CIF Party search and Party creation UX. No Oracle DDL/DML contract changes.

## Party search grid

- `احراز` displays the Persian title resolved from `CIF.REF_VERIFICATION_STATUS` through `ref-verification-status`.
- `کیفیت داده` displays the Persian title resolved from `CIF.REF_DATA_QUALITY_STATUS` through `ref-data-quality-status`.
- Technical codes are retained in the element `title` attribute for support/debugging.
- Fallback mappings match governed seeds: `UNVERIFIED=تأییدنشده`, `PENDING=در انتظار`, `VERIFIED=تأییدشده`, `REJECTED=ردشده`; `GOOD=مطلوب`, `REVIEW_REQUIRED=نیازمند بازبینی`, `INCOMPLETE=ناقص`, `INVALID=نامعتبر`.

## Party creation progress UX

The old header marked steps 1-3 as active simultaneously. It is replaced by a live state tracker:

1. اطلاعات پایه Party
2. هویت شخص حقیقی / حقوقی
3. شناسه اصلی
4. ثبت و ادامه پرونده به نشانی و اطلاعات تماس

State rules:

- Step 1 is complete when `partyForm.valid`.
- Step 2 is complete when the selected identity form is valid; Person additionally requires the primary name form.
- Step 3 is complete when `identifierForm.valid`.
- Step 4 becomes current only after the first three steps are complete.
- Header shows current step, completed count and progress percentage in 25% increments.

## Compatibility

- No API schema change.
- No Oracle schema/data change.
- Existing onboarding POST payload remains unchanged.
- Responsive layout collapses from 4 columns to 2 and then 1.
