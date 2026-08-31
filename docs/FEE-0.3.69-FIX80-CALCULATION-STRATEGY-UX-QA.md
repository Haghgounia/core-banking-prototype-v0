# FEE 0.3.69 / FIX80 — Calculation Strategy UX QA

## مسئله
در Baseline 1.0، نوع محاسبه از ابتدا در جدول `FEE.FEE_CALCULATION_RULE` و ستون `CALCULATION_STRATEGY_CODE` وجود داشت، اما فرم Metadata-driven آن را در میان تعداد زیادی فیلد نمایش می‌داد و برای کاربر به‌عنوان «فرم نوع محاسبه» قابل تشخیص نبود.

## مبنای داده
دامنه `CALCULATION_STRATEGY` در Seed شامل 11 مقدار است: FIXED، PER_UNIT، PERCENTAGE، PERCENTAGE_WITH_FLOOR، PERCENTAGE_WITH_CAP، PERCENTAGE_FLOOR_CAP، FIXED_PLUS_PERCENTAGE، TIERED، MARGINAL_TIERED، ANNUALIZED_PERCENTAGE و COMPOSITE.

## تغییر UI
- عنوان فرم: «نوع و قاعده محاسبه کارمزد»
- کارت انتخاب سریع برای هر 11 Strategy
- پیش‌مقداردهی `CALCULATION_STRATEGY_CODE` با کلیک روی کارت
- نمایش شرطی فیلدها بر اساس Strategy
- لینک مستقیم از Quick Access و Flowchart صفحه `/fee`
- لینک‌های تکمیلی به `FEE_CALCULATION_TIER`، `FEE_RULE_COMPONENT` و `FEE_INPUT_DEFINITION`

## قواعد نمایش شرطی
- FIXED: `FIXED_AMOUNT`
- PERCENTAGE: `RATE_VALUE`
- PERCENTAGE_WITH_FLOOR: `RATE_VALUE + MIN_FEE_AMOUNT`
- PERCENTAGE_WITH_CAP: `RATE_VALUE + MAX_FEE_AMOUNT`
- PERCENTAGE_FLOOR_CAP: `RATE_VALUE + MIN_FEE_AMOUNT + MAX_FEE_AMOUNT`
- FIXED_PLUS_PERCENTAGE: `FIXED_AMOUNT + RATE_VALUE + MIN/MAX`
- ANNUALIZED_PERCENTAGE: نرخ + Rate Period + Day Count + Proration + حداقل روز
- TIERED/MARGINAL_TIERED: جزئیات پله در `FEE_CALCULATION_TIER`
- COMPOSITE: اجزا در `FEE_RULE_COMPONENT`

## نکته نرخ
`RATE_VALUE` طبق Seed به‌صورت ضریب اعشاری ذخیره می‌شود؛ برای نمونه `0.002` معادل `0.2%` است. Input عددی UI نیز `step=any` دارد تا مقادیر اعشاری معتبر باشند.

## Backend Metadata
- `CALCULATION_STRATEGY_CODE -> CALCULATION_STRATEGY`
- `BASIS_TYPE_CODE -> CALCULATION_BASIS`

## Database
هیچ DDL/Seed جدیدی لازم نیست. تغییر فقط Backend metadata mapping و Frontend UX است.
