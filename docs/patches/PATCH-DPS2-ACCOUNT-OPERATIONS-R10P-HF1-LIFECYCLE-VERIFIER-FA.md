# DPS2 Account Operations R10P-HF1 — Lifecycle R8 Verifier Compatibility

Baseline: `core-banking-prototype-v0-2026-10-04-1409`

این Hotfix رفتار کسب‌وکاری R10P را تغییر نمی‌دهد. در R10P جستجوی حساب‌های قابل فعال‌سازی همچنان صریحاً فقط با وضعیت `PENDING_ACTIVATION` انجام می‌شود، اما برای حفظ Scope مشتری، `partyId` نیز به Query اضافه شده است.

Verifier تاریخی R8 فقط رشته‌ی دقیق زیر را قبول می‌کرد:

`this.service.search({status:'PENDING_ACTIVATION',limit:100})`

و به همین دلیل Query صحیح R10P که شامل `partyId` بود را FAIL می‌کرد. در HF1، Guard به‌جای Exact String، بدنه‌ی `loadActivationCandidates()` را بررسی می‌کند و وجود همزمان این سه قرارداد را الزام می‌کند:

- فراخوانی `this.service.search(...)`
- وضعیت `PENDING_ACTIVATION`
- `limit:100`

همچنین Installer تجمعی HF1 خود Verifier R8 را هم اجرا می‌کند تا این ناسازگاری دیگر قبل از `build-production.cmd` مخفی نماند.

DDL/Migration: ندارد.
