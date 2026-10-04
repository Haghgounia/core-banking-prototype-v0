# DPS2 0.11.0 - Account Operations R10M-HF1

## هدف
هم‌راستاسازی Verifier تاریخی Phase 11N-B با قرارداد UX جدید R10M برای «نگهداری حساب سپرده».

## تصمیم کسب‌وکاری/UX
- «عنوان حساب» قابل ویرایش است.
- «واحد نگهدارنده» در فرم نگهداری فقط‌خواندنی است و به صورت «کد شعبه — عنوان شعبه» نمایش داده می‌شود.
- `OPENING_ORG_UNIT_CODE` همچنان Snapshot افتتاح و غیرقابل تغییر است.
- `ORG_UNIT_CODE` همچنان فیلد Canonical واحد متولی جاری در Backend است و قابلیت Backend حذف نشده است؛ فرم نگهداری فقط آن را تغییر نمی‌دهد.

## علت Hotfix
Verifier قدیمی Phase 11N-B انتظار داشت UI همان فرم نگهداری مستقیماً `orgUnitCode` را ویرایش کند. این انتظار با تصمیم جدید UI تعارض داشت و باعث توقف `build-production.cmd` می‌شد، با وجود صحیح بودن R10M.

## دامنه تغییر
فقط فایل زیر تغییر می‌کند:
- `tools/verify-dps2-phase11nb-steps01-02-canonical-closure.mjs`

هیچ UI، Backend، DDL یا Business DML تغییر نمی‌کند.

## Qualification
- R10M Maintenance Delta: 13/13 PASS
- Phase 11J: 78/78 PASS
- Phase 11N-B: 78/78 PASS
- Phase 11N-C: 72/72 PASS
- Phase 11N-D: 64/64 PASS
- Phase 11N Rapid Steps 11-17: 39/39 PASS
