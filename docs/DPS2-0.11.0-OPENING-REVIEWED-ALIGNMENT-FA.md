# هم‌ترازی فرم افتتاح حساب با نسخه Reviewed 2026-09-20

## مبنا
این تغییر براساس فایل `Deposit_Account_Opening_Operational_Final_Reviewed_2026-09-20_ReadOnly_Color_Aligned.html` انجام شده است.

## وضعیت
- UI مراحل 1 تا 7 با نسخه Reviewed هم‌تراز شده است.
- Step 2: پیشنهاد سیستمی صاحبان امضا و ثبت استثناء.
- Step 3: Withdrawal Media، دستور سررسید، سود Product-derived، خدمات، ابزار پرداخت، Pricing/Tax/Reward.
- Step 4: تخصیص Funding به Obligation واقعی و تولید canonical Fund Allocation.
- Step 5: جدول کنترل‌ها، Document Checklist و Terms Acceptance با Evidence فنی.
- Step 6: وضعیت تصمیم سیستمی و تصمیم‌های APPROVE/REJECT/REFER/RETURN.
- Step 7: Create -> PENDING_ACTIVATION -> Settlement -> Readiness -> ACTIVE.
- `DOCUMENT_REFERENCE` در Checklist مدرک اجباری نیست و به عنوان Reference اختیاری DMS/مرجع خارجی باقی می‌ماند.
- شناسه‌های Term/Tier/Pricing/Payment Rule مطابق داده نمونه فایل Reviewed در Payload استفاده می‌شوند.

## موارد نیازمند Oracle Metadata قبل از DDL/Backend Change
فایل Reviewed این ستون‌ها را استفاده می‌کند، ولی مدل Java/Repository/Operational Descriptor فعلی پروژه آن‌ها را ندارد:

1. `DPS2.DEPOSIT_OPENING_PAYMENT_INSTRUMENT.INSTRUMENT_HOLDER_ROLE_CODE`
2. `DPS2.DEPOSIT_OPENING_PAYMENT_INSTRUMENT.CHEQUEBOOK_LEAF_COUNT`
3. `DPS2.DEPOSIT_OPENING_MATURITY_INSTRUCTION.SETTLEMENT_ACCOUNT_ID`
4. `DPS2.DEPOSIT_OPENING_PROFIT_INSTRUCTION.DESTINATION_ACCOUNT_ID`

تا دریافت Metadata واقعی Oracle، برای این چهار ستون Migration حدسی تولید نشده است. Referenceهای موجود (`SETTLEMENT_ACCOUNT_REFERENCE` و `DESTINATION_ACCOUNT_REFERENCE`) همچنان کامل پشتیبانی می‌شوند.

## کنترل‌های انجام‌شده
- `DPS2_OPENING_REVIEWED_ALIGNMENT_STATIC_PASS` — 78/78
- `DPS2_OPENING_REVIEWED_TS_SYNTAX_PASS`
- `DPS2_OPENING_REVIEWED_FORM_CONTROL_BINDING_PASS` — 94 control / 0 missing

Build/Runtime/Regression کامل در این بسته اجرا نشده و طبق تصمیم پروژه به مرحله بعد موکول است.
