# DPS2 0.11.0 — Phase 11G Opening Pricing Trace Hotfix QA

## مسئله Runtime

پس از عبور کامل Migration و DB Verifier فاز 11G، Runtime E2E در ایجاد Opening جدید با خطای زیر متوقف شد:

`REQUIRED_DATABASE_VALUE_MISSING: PRICING_RULE_ID`

علت این بود که Bootstrap فاز 11G برای `DEPOSIT_OPENING_PROFIT_INSTRUCTION` نرخ و روش محاسبه را ارسال می‌کرد، اما شناسه‌های حاکمیتی PDL را `null` می‌گذاشت. در مدل فیزیکی Opening، `PRICING_RULE_ID` و `PROFIT_PAYMENT_RULE_ID` اجزای الزامی Trace هستند.

## تصمیم معماری

Profit Snapshot زمان Opening باید به Product Definition همان `PRODUCT_VERSION_ID` قابل Trace باشد. بنابراین Runtime مجاز نیست شناسه ساختگی تولید کند یا فقط `RATE_VALUE` را hard-code کند.

مسیر Resolution:

`PRODUCT_VERSION -> PRODUCT_PRICING_RULE -> PRODUCT_PRICING_COMPONENT -> PRODUCT_RATE_TIER`

و برای زمان‌بندی پرداخت:

`PRODUCT_VERSION -> DEPOSIT_PROFIT_PAYMENT_RULE`

`RATE_VALUE` ابتدا از Tier، سپس Component و در نهایت Rule Resolve می‌شود. Opening Snapshot شناسه‌های Rule/Component/Tier/Payment Rule و Rate را حفظ می‌کند؛ Operational Profit Contract در Activation از همین Snapshot ساخته می‌شود.

## PDL Reconciliation

ابزار جدید:

`tools/reconcile-pdl-profit-config-phase11g.mjs`

- پیش‌فرض Dry Run است.
- فقط با `--apply` داده PDL تغییر می‌کند.
- Descriptor واقعی Product Builder را می‌خواند و Payload را بر اساس Metadata/Constraintها می‌سازد.
- برای نسخه‌های Term Deposit باز، Configuration ناقص Pricing/Profit Payment را تکمیل می‌کند.
- زنجیره `Pricing Rule -> Component -> Rate Tier -> Profit Payment Rule` را Verify می‌کند.
- نرخ پیش‌فرض Qualification برابر 18 است و فقط برای Configurationهای Missing پروتوتایپ استفاده می‌شود؛ می‌توان با `PHASE11G_REPAIR_RATE` تغییر داد.

## Regression 11F

Runtime فاز 11F نیز بعد از فعال‌شدن شرط Profit Snapshot در 11G اصلاح شد و دیگر `PRICING_RULE_ID=null` / `PROFIT_PAYMENT_RULE_ID=null` ارسال نمی‌کند؛ بنابراین Re-run فاز 11F بعد از 11G نیز به Configuration Governed PDL متصل است.

## Qualification

- `node --check tools/reconcile-pdl-profit-config-phase11g.mjs` = PASS
- `node --check tools/runtime-dps2-phase11g-e2e.mjs` = PASS
- `node --check tools/runtime-dps2-phase11f-e2e.mjs` = PASS
- Mock سخت‌گیرانه PDL: Dry Run = PASS
- Mock سخت‌گیرانه PDL: Apply + Rule/Component/Tier/Payment Rule = PASS
- Static Phase 11G verifier = PASS
