# عملیات Party — فاز ۱۰: چرخه عمر، تغییر وضعیت و ادغام

نسخه: `0.3.21-prototype`

## دامنه

این فاز دو عملیات کنترلی مستقل فرم مرجع Party را عملیاتی می‌کند:

- تغییر وضعیت زمان‌مند Party با ثبت `PARTY_STATUS_HISTORY`
- ادغام Party تکراری با ثبت `PARTY_MERGE_HISTORY` و علامت‌گذاری Party مبدأ به‌عنوان `MERGED`

## تغییر وضعیت

تغییر مستقیم تاریخچه مجاز نیست. هر درخواست تغییر وضعیت:

1. `RECORD_VERSION` رکورد PARTY را کنترل می‌کند.
2. وضعیت و دلیل را در Reference Data واقعی کنترل می‌کند.
3. رکورد باز تاریخچه قبلی را با تاریخ اثر می‌بندد.
4. رکورد جدید تاریخچه را ایجاد می‌کند.
5. `PARTY.LIFECYCLE_STATUS_CODE`، `STATUS_REASON_CODE` و `STATUS_CHANGED_AT` را به‌روزرسانی می‌کند.

وضعیت `MERGED` فقط از عملیات اختصاصی Merge قابل ثبت است. تغییر وضعیت با تاریخ اثر آینده در این نسخه پشتیبانی نمی‌شود.

## ادغام Party

قواعد اصلی:

- مبدأ و مقصد باید متفاوت باشند.
- هر دو Party باید از یک `PARTY_TYPE_CODE` باشند.
- مقصد نباید خودش `MERGED` باشد.
- Party مبدأ پس از عملیات به `MERGED` تغییر می‌کند و `MERGED_INTO_PARTY_ID` به مقصد اشاره می‌کند.
- علت وضعیت سیستمی ادغام از کد واقعی `DUPLICATE_MERGED` استفاده می‌کند.
- Merge History append-only است.

در Metadata فیزیکی برای `MERGE_REASON_CODE` و `CONFLICT_RESOLUTION_CODE` جدول مرجع مستقلی وجود ندارد. بنابراین گزینه‌های عملیاتی فرم مبنا عیناً ذخیره می‌شوند و Reference ساختگی Seed نشده است.

مطابق فرم عملیاتی مبنا، در Merge سه گروه داده دارای اعتبار جاری به Party مقصد منتقل می‌شوند: `PARTY_NAME`، `PARTY_IDENTIFIER` و `PARTY_CLASSIFICATION`. سوابق تاریخی/منقضی روی Party مبدأ باقی می‌مانند تا Audit مخدوش نشود. اگر مقصد از قبل Primary Name/Identifier داشته باشد، Primary منتقل‌شده به Secondary تنزل می‌یابد؛ Classification معادل که از قبل با همان کلید یکتای دوره در مقصد وجود دارد دوباره منتقل نمی‌شود. سایر Child Domainها در این فاز جابه‌جا نمی‌شوند چون فرم مبنا انتقال آن‌ها را تصریح نکرده است.

## پوشش Metadata

- `PARTY_STATUS_HISTORY`: 12/12 ستون
- `PARTY_MERGE_HISTORY`: 13/13 ستون فیزیکی؛ `CREATED_DATE` ستون Audit فیزیکی است که در Manifest HTML/XML کسب‌وکاری وجود ندارد ولی برای سازگاری Oracle حفظ شده است.

## Migration

`database/oracle/cif/migrations/0.3.21-party-lifecycle-merge.sql`

Migration وجود Sequence/Table را کنترل می‌کند و روی دیتابیس فعلی که این اشیا را دارد، فقط Alignment لازم را انجام می‌دهد.
