# CIF Party Operations — Phase 8

نسخه: `0.3.19-prototype`

## دامنه فاز

این فاز مرحله «شناخت مشتری و ریسک» در جریان عملیاتی Party را به یک Workflow مستقل تبدیل می‌کند. مبنای توسعه سه منبع تحویلی پروژه است:

1. `Party-Operation_Froms-1 (1).html` برای ترتیب فرم، فیلدهای عملیاتی و رفتار کاربر؛
2. `Party-Operation_Froms-1 (1).xml` برای مدل EA/XMI، موجودیت‌ها و روابط منطقی؛
3. `CIF-tables4.xlsx` برای ساختار فیزیکی جاری Oracle شامل Column/FK/Index/Count/Constraint.

این فاز مدل جدید KYC اختراع نمی‌کند و بر چهار جدول موجود بنا شده است:

- `CIF.KYC_CASE`
- `CIF.PARTY_RISK_ASSESSMENT`
- `CIF.SCREENING_RESULT`
- `CIF.EXTERNAL_INQUIRY_RESULT`

## KYC_CASE

Metadata جاری ۲۷ ستون دارد. نه ستون تکمیلی که در Snapshot تاریخی DDL نبودند در Backend، UI و DDL همگام شده‌اند:

- `RELATION_PURPOSE_CODE`
- `EXPECTED_ACTIVITY_LEVEL_CODE`
- `GEOGRAPHIC_SCOPE_CODE`
- `ACTIVITY_COUNTRIES_TEXT`
- `REQUESTED_PRODUCTS_TEXT`
- `PREFERRED_SERVICE_CHANNEL_CODE`
- `PEP_STATUS_CODE`
- `HIGH_RISK_COUNTRY_FLAG`
- `EDD_REQUIRED_FLAG`

برای ارتقای دیتابیس موجود Migration افزایشی و idempotent زیر اضافه شده است:

`database/oracle/cif/migrations/0.3.19-kyc-case-alignment.sql`

در Metadata جاری برای `HIGH_RISK_COUNTRY_FLAG` و `EDD_REQUIRED_FLAG` Check Constraint فیزیکی Y/N وجود ندارد. به‌منظور عدم تغییر خودسرانه مدل فیزیکی، Migration Constraint جدیدی ایجاد نمی‌کند؛ Validation Y/N در Backend اعمال می‌شود.

## فرم عملیاتی KYC

فرم مستقل Phase 8 شامل این سه دسته است:

### شناخت مشتری

- نوع KYC/KYB
- سطح بررسی SDD/CDD/EDD
- وضعیت Workflow
- هدف افتتاح رابطه
- حجم عملیات مورد انتظار
- محدوده جغرافیایی فعالیت
- کشورهای محل فعالیت
- محصولات مورد تقاضا
- کانال ترجیحی خدمت

### کنترل‌های تطبیق

- وضعیت PEP
- Flag کشور پرریسک
- نیاز به EDD
- تاریخ بازبینی بعدی

### تصمیم پذیرش

- سطح ریسک نهایی
- تصمیم
- مرجع/تأییدکننده
- توضیح تصمیم

برای ستون‌های KYC که در Metadata FK/REF فیزیکی صریح ندارند، Reference Table ساختگی ایجاد نشده است. گزینه‌های محلی فرم فقط همان مقادیر عملیاتی نمونه HTML را نمایش می‌دهند.

## ارزیابی ریسک

`PARTY_RISK_ASSESSMENT` با ۲۰ ستون فیزیکی جاری حفظ شده است. ۱۷ فیلد مدل کسب‌وکاری به همراه سه ستون Audit تاریخی (`CREATED_DATE`, `LAST_MODIFIED_BY`, `LAST_MODIFIED_DATE`) پوشش داده می‌شوند. هنگام Insert/Update ستون‌های Audit اجباری نیز مقداردهی می‌شوند.

فیلدهای مرجع شامل نوع ریسک، سطح ریسک، تصمیم و مدل ریسک از Reference Data موجود خوانده می‌شوند.

## Screening

`SCREENING_RESULT` با ۲۱ ستون فیزیکی پوشش داده می‌شود. فرم عملیاتی از Reference Data موجود برای نوع Screening، فهرست منبع، ارائه‌دهنده و تصمیم استفاده می‌کند و قواعد Metadata زیر در Backend نیز کنترل می‌شوند:

- `FALSE_POSITIVE_FLAG` فقط Y/N؛
- Match Score بین ۰ تا ۱۰۰؛
- زمان و کاربر بازبینی باید همزمان ثبت یا خالی باشند؛
- `RECORD_VERSION >= 1`.

## استعلام‌های بیرونی

`EXTERNAL_INQUIRY_RESULT` برای نخستین بار وارد CRUD و Party 360 می‌شود و پوشش عملیاتی CIF را از ۲۴ به ۲۵ جدول می‌رساند.

قواعد اجرایی:

- زمان پاسخ و نتیجه باید با هم ثبت شوند؛
- پاسخ نمی‌تواند قبل از درخواست باشد؛
- تاریخ انقضا فقط پس از پاسخ مجاز است؛
- `PAYLOAD_REF` و `PAYLOAD_HASH` باید با هم ثبت یا هر دو خالی باشند؛
- Payload کامل در جدول ذخیره نمی‌شود و فقط مرجع امن و Hash نگهداری می‌شود.

## حفاظت از وابستگی KYC

حذف `KYC_CASE` در صورت وجود رکورد وابسته در یکی از جداول زیر مسدود می‌شود:

- `PARTY_RISK_ASSESSMENT`
- `SCREENING_RESULT`
- `PARTY_DOCUMENT`

این کنترل در Application انجام می‌شود تا حتی در محیطی که FK Metadata ناقص استخراج شده باشد، رکورد یتیم تولید نشود.

## Searchable Combo

Comboهای Reference در این فاز از کامپوننت reusable جست‌وجوی سمت سرور استفاده می‌کنند. در QA این فاز ناسازگاری Resource Name بین UIهای اخیر (`REF_*`) و API (`ref-*-*`) شناسایی و در `CifService.partyReferenceLookup` به‌صورت مرکزی Normalize شد؛ بنابراین Phaseهای 5 تا 8 از یک قرارداد واحد استفاده می‌کنند.

## QA دیتابیس

بررسی `CIF-tables4.xlsx` نشان داد برای این فاز ساختار Column/Index/Constraint لازم موجود است. Sheet `FK` در کل Metadata نسبت به `IXFK_*`ها ناقص به نظر می‌رسد، اما این موضوع برای Phase 8 بازدارنده نیست؛ Validationهای Application و FKهای واقعاً موجود برای Party/KYC همچنان رعایت می‌شوند.

پوشش فیزیکی این فاز:

- `KYC_CASE`: 27/27
- `PARTY_RISK_ASSESSMENT`: 20/20
- `SCREENING_RESULT`: 21/21
- `EXTERNAL_INQUIRY_RESULT`: 16/16

## مرحله بعد

طبق جریان عملیاتی فایل HTML، مرحله بعدی «رضایت‌ها و ترجیحات» است.
