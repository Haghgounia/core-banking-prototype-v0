# DPS2 Document / Oracle / Code Delta Audit — 2026-09-24

**Scope:** Four Deposits / Deposit Account Operations

این سند برای سرعت اجرای تغییرات، اختلاف و پوشش بین مستندات مرجع، Oracle واقعی و آخرین Source پروژه را به‌صورت یک نقطه کنترل ثبت می‌کند.

## 1. نتیجه فوری

- سه سند XML / Trace Guide / Operational HTML که در 2026-09-24 دوباره ارسال شدند، از نظر SHA-256 با نسخه‌های ذخیره‌شده در Repository **کاملاً یکسان‌اند**؛ بنابراین Requirement document جدیدی نسبت به baseline داخل ZIP وجود ندارد.
- Full Oracle DDL فعلی شامل **214 جدول DPS2** است.
- اتحاد جدول‌های مورد استفاده در Action Trace گام‌های 00..17 برابر **107 جدول یکتا** است و **همه 107 جدول در Oracle DDL فعلی وجود دارند**.
- نتیجه: گلوگاه اصلی بعدی «نبود جدول» نیست؛ گلوگاه غالب، API/Service/UI/Runtime coverage و orchestration بین دامنه‌هاست.

## 2. هویت اسناد مرجع

| سند | نسبت به نسخه Repository | SHA-256 |
|---|---|---|
| XML | IDENTICAL | `61d42f5a7dfe57a5e48d2849afe1310450476442cb5d53b7ba800e58dfc296ce` |
| Trace Guide | IDENTICAL | `976c5e568724d62ff38efae668abd9069d381b6aa24aeebb281e9cc7ad45bf14` |
| Operational HTML | IDENTICAL | `5ead9d6b058eddfe25342c6b70655320a5f5a365a9a7e5a03b6d697c24976f8b` |

## 3. Oracle Baseline ثبت‌شده

- `docs/dps2/reference/DPS-2026-09-24.txt` — Full DPS2 table DDL export
- `docs/dps2/reference/DPS-2026-09-24-2.txt` — Profit-related column metadata snapshot
- `docs/dps2/reference/DPS-2026-09-24-3.txt` — Profit-related constraint metadata snapshot

## 4. Coverage Matrix 00..17

> ستون «Repository reference» فقط یک **heuristic سریع** است: وجود نام جدول در Java/Angular/Migration/Verifier/Runtime را نشان می‌دهد و به‌تنهایی اثبات DONE بودن Step نیست. DONE فقط با Static + DB + Runtime و Action-level trace تعیین می‌شود.

| Step | عنوان | Source Tables | Oracle DDL | Repository reference | Canonical Status | Table references absent from runtime/source areas |
|---:|---|---:|---:|---:|---|---|
| 00 | نمای عملیاتی سپرده | 4 | 4/4 | 4/4 | PARTIAL | — |
| 01 | نگهداری حساب سپرده | 10 | 10/10 | 10/10 | PARTIAL | — |
| 02 | چرخه عمر، راکدی و انسداد | 11 | 11/11 | 11/11 | PARTIAL | — |
| 03 | عملیات سپرده مدت‌دار | 14 | 14/14 | 14/14 | PARTIAL | — |
| 04 | مدیریت سود سپرده | 7 | 7/7 | 7/7 | PARTIAL | — |
| 05 | تراکنش‌های سپرده | 15 | 15/15 | 15/15 | DONE | — |
| 06 | صورتحساب سپرده | 7 | 7/7 | 7/7 | PARTIAL | — |
| 07 | سقف و محدودیت حساب | 3 | 3/3 | 3/3 | PARTIAL | — |
| 08 | بستن حساب و پردازش سررسید | 15 | 15/15 | 15/15 | PARTIAL | — |
| 09 | خدمات حساب | 5 | 5/5 | 0/5 | NOT_STARTED | `DEPOSIT_ACCOUNT_API_ACCESS`<br>`DEPOSIT_ACCOUNT_CONFIRMATION`<br>`DEPOSIT_ACCOUNT_INQUIRY`<br>`DEPOSIT_ACCOUNT_NOTIFICATION_EVENT`<br>`DEPOSIT_ACCOUNT_NOTIFICATION_PREFERENCE` |
| 10 | طرف، دسترسی و ابزار | 5 | 5/5 | 0/5 | PARTIAL | `DEPOSIT_ACCOUNT_AUTHORIZED_USER`<br>`DEPOSIT_ACCOUNT_BENEFICIARY`<br>`DEPOSIT_ACCOUNT_DELEGATION`<br>`DEPOSIT_ACCOUNT_PAYMENT_INSTRUMENT`<br>`DEPOSIT_ACCOUNT_SIGNATURE_RULE` |
| 11 | مقررات و انطباق | 8 | 8/8 | 2/8 | NOT_STARTED | `DEPOSIT_COMPLIANCE_EVALUATION`<br>`DEPOSIT_REGULATORY_REPORT`<br>`DEPOSIT_REGULATORY_REPORT_ITEM`<br>`DEPOSIT_REGULATORY_RULE`<br>`DEPOSIT_RESERVE_POSITION`<br>`DEPOSIT_RESERVE_REQUIREMENT` |
| 12 | قیمت‌گذاری و کارمزد | 9 | 9/9 | 1/9 | PARTIAL | `DEPOSIT_ACCOUNT_PRICING_OVERRIDE`<br>`DEPOSIT_CUSTOMER_PACKAGE_ENROLLMENT`<br>`DEPOSIT_FEE_ASSESSMENT`<br>`DEPOSIT_FEE_RULE`<br>`DEPOSIT_FEE_TIER`<br>`DEPOSIT_PRICING_PACKAGE`<br>`DEPOSIT_PRICING_PACKAGE_BENEFIT`<br>`DEPOSIT_PROFITABILITY_SNAPSHOT` |
| 13 | مالیات و کسورات | 8 | 8/8 | 0/8 | NOT_STARTED | `DEPOSIT_TAX_ADJUSTMENT`<br>`DEPOSIT_TAX_CALCULATION`<br>`DEPOSIT_TAX_CERTIFICATE`<br>`DEPOSIT_TAX_EXEMPTION`<br>`DEPOSIT_TAX_LIABILITY`<br>`DEPOSIT_TAX_PAYMENT`<br>`DEPOSIT_TAX_RECONCILIATION`<br>`DEPOSIT_TAX_RULE` |
| 14 | تطبیق و مغایرت | 5 | 5/5 | 0/5 | NOT_STARTED | `DEPOSIT_DISCREPANCY`<br>`DEPOSIT_EXCEPTION_CASE`<br>`DEPOSIT_RECONCILIATION_ITEM`<br>`DEPOSIT_RECONCILIATION_RUN`<br>`DEPOSIT_SUSPENSE_OPEN_ITEM` |
| 15 | استثنا و اصلاح | 9 | 9/9 | 3/9 | NOT_STARTED | `DEPOSIT_CORRECTION_AUTHORIZATION`<br>`DEPOSIT_CORRECTION_ENTRY`<br>`DEPOSIT_CORRECTION_REQUEST`<br>`DEPOSIT_EXCEPTION_ASSIGNMENT`<br>`DEPOSIT_EXCEPTION_CASE`<br>`DEPOSIT_ROOT_CAUSE_ANALYSIS` |
| 16 | نوسترو / وسترو | 5 | 5/5 | 1/5 | NOT_STARTED | `CORRESPONDENT_ACCOUNT_EXTENSION`<br>`CORRESPONDENT_RECONCILIATION_PROFILE`<br>`DEPOSIT_RECONCILIATION_ITEM`<br>`DEPOSIT_RECONCILIATION_RUN` |
| 17 | جوایز و قرعه‌کشی | 7 | 7/7 | 1/7 | NOT_STARTED | `DEPOSIT_LOTTERY_DRAW`<br>`DEPOSIT_LOTTERY_ENTRY`<br>`DEPOSIT_LOTTERY_WINNER`<br>`DEPOSIT_REWARD_ELIGIBILITY_RULE`<br>`DEPOSIT_REWARD_ENROLLMENT`<br>`DEPOSIT_REWARD_PROGRAM` |

## 5. برداشت اجرایی برای Waveها

### Wave A — Stabilize 00..05
- Step 03/04/05 از نظر جدول‌های سند در Oracle کامل‌اند و نام همه جدول‌های Trace در Source/Runtime areas دیده می‌شود؛ تمرکز باید روی Runtime qualification و behavior contract باشد، نه ساخت Schema جدید.
- Step 01 و 02 در Phase 11J از نظر نام همه جدول‌های Trace به 10/10 و 11/11 Source reference رسیده‌اند و Technical Phase 11J با DB/Build/Runtime PASS بسته شده است؛ وضعیت Document Coverage همچنان تا Action-by-Action audit `PARTIAL` می‌ماند.

### Wave B — 06..08
- Phase 11K Source تمام Table referenceهای Step 06 را به 7/7، Step 07 را به 3/3 و Step 08 را به 15/15 رسانده است.
- Source Gate 11K برابر 88/88 PASS است؛ Oracle 34/34 + Production Build + Runtime E2E هنوز qualification باز Wave B هستند.
- Document Step 08 همچنان `PARTIAL` می‌ماند چون financial execution سررسید باید از workflowهای Step 03/05/11E عبور کند و صرف materialization رویداد batch معادل completion سند نیست.

### Wave C — 09..13
- Schema آماده است، ولی runtime/service coverage برای Services, Party/Access, Compliance, Pricing/Fee و Tax عمدتاً ایجاد نشده است.

### Wave D — 14..17
- همه جدول‌های Trace در Oracle وجود دارند؛ بیشترین کار باقی‌مانده در Service/API/UI/E2E است.

## 6. قاعده ادامه

1. DDL جدید Oracle دیگر به‌صورت فایل بیرونی رها نمی‌شود؛ همین سه snapshot جزو reference baseline پروژه‌اند.
2. هر Wave قبل از کدنویسی Action-level matrix می‌گیرد، اما اجرای آن به چندین micro-hotfix خرد نمی‌شود.
3. Migration جدید فقط وقتی ایجاد می‌شود که اختلاف واقعی Oracle با Requirement/Runtime ثابت شود.
4. Canonical Roadmap همچنان مرجع وضعیت است؛ این Audit سند evidence آن Roadmap است.
