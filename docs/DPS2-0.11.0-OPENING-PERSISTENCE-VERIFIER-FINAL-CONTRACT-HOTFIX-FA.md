# DPS2 0.11.0 - Opening Persistence Verifier Final Contract Hotfix

تاریخ: 2026-09-29

## علت
پس از هم‌ترازی UI با `11(1).html`، سه شرط قدیمی در `verify-dps2-deposit-opening-persistence.mjs` با قرارداد نهایی Opening ناسازگار مانده بود:

- Gate ثبت اتمیک هنوز رشته نمایشی قدیمی `OPENING_REQUEST_ID=` را الزام می‌کرد، در حالی که Angular فعلی از `persistAggregate()`، `createAggregate(this.payload())` و `persisted()?.openingRequestId` استفاده می‌کند.
- فهرست `CHECK_CODE` قدیمی، `EXPECTED_ACTIVITY` را به عنوان Check الزام می‌کرد؛ در قرارداد نهایی، Expected Activity یک Reference روی Request است و در فهرست CHECKS نهایی وجود ندارد.
- Gate Funding/Terms هنوز Mapping قدیمی inline برای Acceptance Source را می‌خواست، در حالی که قرارداد نهایی Source را از Channel با `syncAcceptanceSource()` استخراج و در `ACCEPTANCE_SOURCE_CODE` ذخیره می‌کند.

## تغییرات
- Verifier به 22 کد Check نهایی Opening هم‌راستا شد.
- Gate ثبت اتمیک به اتصال واقعی Angular -> API متکی شد، نه متن نمایشی قدیمی.
- Gate Funding/Terms وجود چهار Funding Method نهایی و Mapping سیستمی Acceptance Source را کنترل می‌کند.
- مقدار پیش‌فرض `fundingMethod` در Angular از `TRANSFER` به `INTERNAL` تغییر کرد تا با ترتیب/پیش‌فرض HTML نهایی هم‌راستا باشد.

## نتیجه Static QA

```text
DPS2 Deposit Opening persistence verification OK: 13 transactional table contracts, 22 source-aligned checks, idempotent create endpoint.
DPS2_OPENING_UI_REFERENCE_PARITY_FAIL=0
DPS2_OPENING_FINAL_UI_11_FAIL=0
DPS2_OPENING_REVIEWED_ALIGNMENT_FAIL=0
DPS2 Four Deposits verification OK: 59 reference forms, 25 operational forms, 4 families, 7-step wizard.
```

Build کامل Angular/Maven در محیط بسته‌بندی اجرا نشده است؛ اجرای `build-production.cmd` روی Windows پروژه لازم است.
