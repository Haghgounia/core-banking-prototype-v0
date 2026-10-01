# DPS2 0.11.0 — UAT Product Pack v1

## هدف
این Pack برای تست عمیق فرایند «چهار سپرده» ساخته شده است. داده‌ها از مسیر Product Builder API ایجاد می‌شوند و Wizard افتتاح، Policyهای Product Builder را در اولویت قرار می‌دهد. هیچ DML مستقیم روی PDL در Seeder وجود ندارد.

## کاتالوگ ۱۲ محصول
| خانواده | کد | سناریوی اصلی |
|---|---|---|
| قرض‌الحسنه پس‌انداز | `UAT-QS-STD-001` | عمومی، حقیقی/حقوقی، شعبه/موبایل/اینترنت |
| قرض‌الحسنه پس‌انداز | `UAT-QS-DIG-001` | دیجیتال، فقط حقیقی، موبایل/اینترنت |
| قرض‌الحسنه پس‌انداز | `UAT-QS-ORG-001` | حقوقی/سازمانی، شعبه/API |
| جاری | `UAT-CA-PER-001` | حقیقی، کارت و دسته‌چک |
| جاری | `UAT-CA-ORG-001` | حقوقی، دسته‌چک و Signatory سناریو |
| جاری | `UAT-CA-NOCHEQUE-001` | بدون CHEQUE برای تست منع دسته‌چک |
| کوتاه‌مدت | `UAT-ST-STD-001` | ۳/۶/۱۲ ماه، سود ماهانه |
| کوتاه‌مدت | `UAT-ST-DIG-001` | ۱/۳/۶ ماه، دیجیتال، فقط حقیقی |
| کوتاه‌مدت | `UAT-ST-HIGH-001` | مبلغ بالا، مقصد سود/سررسید حساب انتخابی |
| بلندمدت | `UAT-LT-STD-001` | ۱۲/۲۴/۳۶ ماه، بستن و تسویه در سررسید |
| بلندمدت | `UAT-LT-PAYOUT-001` | پرداخت سود به حساب انتخابی مشتری |
| بلندمدت | `UAT-LT-RENEW-001` | تمدید اصل و پرداخت سود در سررسید |

علاوه بر ۱۲ نسخه قابل افتتاح، چهار نسخه منفی برای تست Version Lifecycle ایجاد می‌شود: `EXPIRED`، `FUTURE` و `SUSPENDED/non-open`.

## Policy Coverage
Seeder حسب Descriptor واقعی Product Builder این جدول‌ها را پر می‌کند:

`PRODUCT`, `PRODUCT_VERSION`, `PRODUCT_VERSION_MODULE`, `PRODUCT_CHANNEL_RULE`, `PRODUCT_ELIGIBILITY_RULE`, `DEPOSIT_PRODUCT_OPENING_RULE`, `DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA`, `DEPOSIT_PRODUCT_TERM_RULE`, `DEPOSIT_PRODUCT_ALLOWED_TERM`, `PRODUCT_PRICING_RULE`, `PRODUCT_PRICING_COMPONENT`, `PRODUCT_RATE_TIER`, `DEPOSIT_PROFIT_PAYMENT_RULE`.

برای Compatibility، Wizard سیاست‌ها را PDL-first می‌خواند و فقط وقتی جدول/ستون مرتبط داده قابل استفاده ندارد، از fallback موجود Prototype استفاده می‌کند.

## نصب و Seed
ابتدا Patch را Extract/Replace و Build کنید:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-dps2-uat-product-pack-v1.mjs
build-production.cmd
bin\stop.cmd
bin\start.cmd
```

Dry Run بدون تغییر داده:

```bat
node tools\prepare-dps2-uat-product-pack-v1.mjs
```

اعمال Pack به Product Builder:

```bat
set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"
node tools\prepare-dps2-uat-product-pack-v1.mjs --apply
```

Runtime Verification:

```bat
node tools\runtime-dps2-uat-product-pack-v1.mjs
```

خروجی نهایی مورد انتظار:

```text
DPS2_UAT_PRODUCT_PACK_V1_APPLY_PASS
DPS2_UAT_PRODUCT_PACK_V1_RUNTIME_PRODUCTS=12
DPS2_UAT_PRODUCT_PACK_V1_RUNTIME_PASS
```

Seeder Idempotent طراحی شده است؛ اجرای مجدد محصول/نسخه/Policy موجود را دوباره ایجاد نمی‌کند.

## ماتریس UAT پیشنهادی
برای هر محصول حداقل ۵ سناریو اجرا شود: Happy Path، مرز حداقل مبلغ، کانال نامعتبر، نوع مشتری نامعتبر، و یک قاعده اختصاصی محصول. بنابراین Baseline حداقل **۶۰ سناریو** است. برای جاری، کوتاه‌مدت و بلندمدت سناریوهای اضافی Signatory/Cheque/Term/Maturity/Profit نیز اجرا شود.

## Boundary
این Pack فقط داده UAT/Prototype است. کدهای آن با Prefix `UAT-` قابل تشخیص‌اند و نباید به‌عنوان تعرفه، نرخ یا Policy مصوب Production تلقی شوند.


## Hotfix: Opening Rule constraint-safe resume
Seeder برای `DEPOSIT_PRODUCT_OPENING_RULE` از یک رکورد معتبر همان خانواده به‌عنوان Template استفاده می‌کند، مقادیر system-managed/PK را کپی نمی‌کند و فقط Version و مقادیر UAT را override می‌کند. حداقل/پیش‌فرض/حداکثر مبلغ نیز برای جلوگیری از نقض check constraintهای ترکیبی هم‌تراز می‌شوند. اجرای مجدد پس از شکست میانی idempotent است و Product/Versionهای ایجادشده قبلی را reuse می‌کند. در صورت conflict مجدد، payload دقیق با marker `UAT_PACK_OPENING_PAYLOAD` چاپ می‌شود.

## Hotfix 2 - Oracle numeric precision / optional opening flags

در اجرای واقعی UAT-QS-STD-001، `DEPOSIT_PRODUCT_OPENING_RULE` با `409 DATA_CONFLICT` متوقف شد. Payload تشخیصی نشان داد مقدار ثابت `MAX_OPENING_AMOUNT=999999999999` و Flagهای clone شده از template می‌توانند با precision/constraint واقعی Oracle ناسازگار باشند.

اصلاح:
- تمام سقف‌های عددی Seeder از `precision/scale` موجود در Product Builder descriptor محاسبه می‌شوند؛ مقدار ثابت 12 رقمی حذف شد.
- `MAX_OPENING_AMOUNT` تنها در محدوده قابل ذخیره ستون و حداقل بزرگ‌تر یا مساوی حداقل افتتاح ساخته می‌شود.
- برای UAT baseline، `IS_INTRODUCER_REQUIRED`, `IS_OVERDRAFT_ALLOWED`, `IS_STAMP_DUTY_APPLICABLE` در صورت وجود ستون برابر 0 قرار می‌گیرند؛ این قابلیت‌ها جزو هدف Product Pack v1 نیستند.
- Rate Tier نیز از همان numeric-cap helper استفاده می‌کند تا در مراحل بعد دوباره precision conflict ایجاد نشود.
- قبل از ایجاد Opening Rule، metadata ستون سقف با marker `UAT_PACK_OPENING_DESCRIPTOR` چاپ می‌شود.


## Hotfix — TERM rule reconciliation

Seeder برای `DEPOSIT_PRODUCT_TERM_RULE` از الگوی معتبر همان خانواده محصول استفاده می‌کند، سپس `PRODUCT_VERSION_ID`، `MATURITY_ACTION_CODE`، `IS_RENEWABLE`، `GRACE_PERIOD_DAYS=0` و وضعیت‌های canonical را override می‌کند. این رفتار با reconciliation اثبات‌شده Phase 11F هم‌راستا است و در صورت conflict، descriptor، payload و required columns را چاپ می‌کند. اجرای مجدد idempotent است و Product/Versionهای ساخته‌شده قبلی را reuse می‌کند.

## Term Rule metadata hotfix (2026-10-01)
Oracle metadata confirmed `CK_DPTR_RENEW`: when `IS_RENEWABLE=1`, `RENEWAL_INSTRUCTION_CODE` must be non-null and `MAX_RENEWAL_COUNT` must be null or greater than zero. The UAT seeder now sends `AUTO_RENEW` and a maximum renewal count of 12 for renewable products; non-renewable products explicitly send null/0. `CREATED_BY` is also populated explicitly for term-rule and allowed-term rows.

