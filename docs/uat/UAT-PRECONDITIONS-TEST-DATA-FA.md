# DPS2 0.11.0 — Preconditions و Test Data برای UAT

## قواعد عمومی

- از داده Production واقعی برای تست مخرب استفاده نشود.
- Accountهای تست با Prefix/شناسه قابل تشخیص UAT نگهداری شوند.
- Maker و Checker باید Actorهای متفاوت باشند هرجا Approval لازم است.
- Idempotency Key در تکرار عمدی یک Action ثابت و در Action جدید متفاوت باشد.
- Configurationهای Product/Fee/Tax/Regulatory از Product Builder یا جداول Governed موجود استفاده کنند؛ داده مالی جعلی برای عبور از Gate ساخته نشود.

## ماتریس حداقل داده

| Step | داده/پیش‌نیاز حداقل |
|---:|---|
| 00 | حداقل یک Account قابل مشاهده؛ ترجیحاً Active با Balance و History |
| 01 | Account فعال با Party/Contact؛ برای Product Version Change حداقل دو Version از همان Product |
| 02 | Account مناسب Lifecycle؛ Hold test amount؛ برای Activation یک Opening با Readiness واقعی |
| 03 | Term Account فعال با Term Contract و Maturity Instruction |
| 04 | Profit-bearing Account با Profit Profile/Period |
| 05 | Active Account با Available Balance کافی؛ برای Internal Transfer دو Account سازگار |
| 06 | Account دارای Subledger transaction برای Statement |
| 07 | Active Account برای Limit/Restriction |
| 08 | Term Account نزدیک/رسیده به Maturity یا Account واجد Closure |
| 09 | Account با Contact معتبر برای Notification و Service actions |
| 10 | Active Account Party و Partyهای مجاز برای Delegation/Authorized User/Instrument |
| 11 | Active Regulatory Rule/Requirement در داده Governed |
| 12 | Fee/Pricing configuration؛ Rule/Tier و Account مناسب Assessment |
| 13 | Governed Tax Rule/Rate و Taxable Event قابل Trace |
| 14 | Account با Reconciliation Run و حداقل یک mismatch برای Discrepancy/Suspense |
| 15 | Exception Case و برای REVERSAL/DUPLICATE_CANCEL یک Step05 Transaction معتبر |
| 16 | Product Version با `CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE` برای NOSTRO/VOSTRO؛ Party/External Ref/BIC تست |
| 17 | Reward Program + Eligibility Rule؛ Active eligible Account با Balance و Active Days کافی |

## Step 16 — Correspondent Product Configuration

UAT واقعی نباید به QA Fixture وابسته باشد. حداقل یکی از مسیرهای Governed زیر در Product Builder آماده باشد:

```text
NOSTRO_ACCOUNT + settlement currency
VOSTRO_ACCOUNT + settlement currency
```

Profile باید شامل Type/Currency و تنظیمات تطبیق مورد نیاز محصول باشد.

## Step 17 — Reward

برای تست مثبت:

- Program فعال
- Eligibility Rule فعال
- Account فعال و غیر Dormant
- Minimum Average Balance برقرار
- Minimum Active Days برقرار
- Consent/Enrollment معتبر

برای تست منفی حداقل یکی از شروط بالا عمداً نقض شود تا Eligibility رد شود.

## داده‌های ممنوع برای Fabrication

- Compliance evidence خارجی که واقعاً وجود ندارد
- Taxable Event بدون Trace
- Fee/Tax posting مرجع ساختگی
- Product Version یا Profile صرفاً برای عبور از Business Gate خارج از Product Builder
- Transaction مالی بدون Step 05/Posting Trace در مسیرهایی که سند آن را الزام کرده است
