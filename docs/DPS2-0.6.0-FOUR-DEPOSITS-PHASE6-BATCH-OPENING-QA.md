# DPS2 0.6.0 — Phase 6 Batch / Bulk Deposit Opening QA

## مبنا
پیاده‌سازی این فاز مستقیماً از بخش «افتتاح گروهی حساب سپرده» فایل `Deposit_Account_Opening.html` و مدل/DDL ارسالی DPS2 استخراج شده است. مسیر کسب‌وکاری حفظ‌شده عبارت است از:

`Batch Header -> Batch Items -> Validate -> Error -> Opening Request مستقل -> Account PENDING_ACTIVATION -> Activate successful accounts`

## Backend
- Domain package اختصاصی `deposit.opening.batch` برای Header/Item/Error.
- APIهای create/get/validate/process/activate زیر `/api/v1/deposit-opening/batches`.
- اعتبارسنجی Party از Contract اسکیمای CIF و Product Version از Contract اسکیمای PDL؛ بدون FK فیزیکی Cross-Domain.
- هر Item معتبر در تراکنش `REQUIRES_NEW` مستقل پردازش می‌شود تا شکست یک ردیف موفقیت ردیف‌های قبل را Rollback نکند.
- هر Item یک `DEPOSIT_OPENING_REQUEST` با `REQUEST_TYPE_CODE=BULK`، مالک اصلی 100% و `BATCH_ITEM_ID` تولید می‌کند.
- ایجاد/فعال‌سازی حساب از `DepositAccountLifecycleService` موجود استفاده می‌کند و منطق Account دوباره‌نویسی نشده است.
- Account ایجادشده ابتدا `PENDING_ACTIVATION` است؛ Activate جداگانه آن را به `ACTIVE` و Opening را به `COMPLETED` منتقل می‌کند.

## Oracle
Migration: `database/oracle/dps2/migrations/0.6.0-phase6-batch-opening.sql`

جدول جدیدی ایجاد نمی‌شود. Migration فقط Reference Codeهای لازم را idempotent seed می‌کند:
- Batch: `DRAFT, VALIDATING, READY, PROCESSING, COMPLETED, PARTIAL, FAILED`
- Item: `PENDING, VALID, INVALID, PROCESSING, SUCCESS, FAILED`
- Source: `FILE, API, MANUAL`
- Error Stage: `VALIDATION, PROCESSING, POSTING`
- Request Type: `BULK`

`ROW_INVALID` از رفتار صریح HTML گرفته شده است. `PROCESSING_FAILED` و `ACTIVATION_FAILED` فقط کدهای فنی Prototype هستند و taxonomy نهایی Error Code همچنان تحت Data Governance بانک است.

## UI
Route اختصاصی: `/four-deposits/batch-opening`

UI همان ساختار HTML مرجع را حفظ می‌کند:
- مشخصات Batch، Source و Idempotency
- Channel و Org Unit پیش‌فرض
- بارگذاری Sample
- Validate
- Process & Create Account
- Activate Successful Accounts
- شمارنده Total / Valid / Success / Failed
- Grid شامل Party، Family، Product Version، Amount، Status، Error، Request No و Account No

Generic CRUD سه جدول Batch در Operations باقی می‌ماند اما مسیر کسب‌وکاری اصلی نیست.
