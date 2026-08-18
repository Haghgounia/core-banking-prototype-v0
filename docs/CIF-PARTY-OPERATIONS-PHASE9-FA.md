# CIF Party Operations — Phase 9

نسخه: `0.3.20-prototype`

## دامنه فاز

این فاز مرحله «رضایت‌ها و ترجیحات» جریان عملیاتی Party را پیاده‌سازی می‌کند. سه مرجع پروژه مبنا هستند:

1. `Party-Operation_Froms-1 (1).html` برای Workflow، فیلدهای رضایت، دامنه‌ها، مدارک و ترجیحات؛
2. `Party-Operation_Froms-1 (1).xml` برای مدل EA/XMI؛
3. `CIF-tables4.xlsx` برای ساختار فیزیکی Oracle.

موجودیت‌های عملیاتی این فاز:

- `CIF.PARTY_CONSENT`
- `CIF.COMMUNICATION_PREFERENCE`
- `CIF.PARTY_GENERAL_PREFERENCE`
- `CIF.PARTY_DOCUMENT` به‌صورت reuse از Phase 4

## PARTY_CONSENT

Metadata جاری ۲۱ ستون دارد. Snapshot تاریخی DDL فقط ۱۴ ستون داشت. هفت ستون زیر در DDL پایه و Migration افزایشی همگام شده‌اند:

- `CUSTOMER_DECISION_CODE`
- `CAPTURE_CHANNEL_CODE`
- `DECLARED_AT`
- `VALID_TO`
- `CONSENT_TEXT_VERSION_CODE`
- `SCOPE_TEXT`
- `SCOPE_LIMITATION_TEXT`

فرم عملیاتی نوع رضایت و هدف را از `REF_PARTY_CONSENT_TYPE` و `REF_PARTY_CONSENT_PURPOSE` می‌گیرد. منبع ثبت از `REF_SOURCE_SYSTEM` انتخاب می‌شود.

### تصمیم مشتری و Lifecycle

HTML دو تصمیم «اعطا» و «رد» را مدل می‌کند، ولی Reference Data فیزیکی `REF_PARTY_CONSENT_STATUS` فقط `GRANTED`, `REVOKED`, `EXPIRED`, `PENDING` دارد. برای جلوگیری از افزودن کد مرجع ساختگی:

- `CUSTOMER_DECISION_CODE=GRANT` تصمیم مثبت است؛
- `CUSTOMER_DECISION_CODE=REJECT` تصمیم منفی و مرجع نمایش «رد شده» است؛
- هیچ `REJECTED` جدیدی در Ref Seed ایجاد نشده است؛
- لغو یک رضایت اعطاشده، رکورد را حذف نمی‌کند و Status را به `REVOKED` تغییر می‌دهد.

برای Consentهای `MARKETING` و `THIRD_PARTY_SHARING` دامنه رضایت اجباری است. دامنه‌های HTML در `SCOPE_TEXT` با جداکننده `||` نگهداری می‌شوند. جدول مرجع فیزیکی مستقلی برای Consent Scope یا Consent Text Version در Metadata وجود ندارد؛ بنابراین جدول ساختگی ایجاد نشده است.

## COMMUNICATION_PREFERENCE

Metadata جاری ۱۶ ستون دارد. سه ستون زیر نسبت به DDL تاریخی افزوده شده‌اند:

- `ALLOWED_DAYS_CODE`
- `TIME_ZONE_CODE`
- `MARKETING_OPT_OUT_FLAG`

Channel و Purpose از `REF_CHANNEL` و `REF_COMMUNICATION_PURPOSE` به‌صورت Combo جست‌وجویی Server-side انتخاب می‌شوند. Unique Business Key فیزیکی `(PARTY_ID, CHANNEL_CODE, PURPOSE_CODE)` پیش از Oracle در Backend کنترل می‌شود.

قواعد:

- `ALLOWED_FLAG` و `MARKETING_OPT_OUT_FLAG` فقط Y/N؛
- اگر Opt-out بازاریابی Y باشد، Allowed به N Normalize می‌شود؛
- ساعت پایان باید بعد از ساعت شروع باشد؛
- `REF_LANGUAGE`, `REF_TIME_ZONE` و `REF_CONTACT_ALLOWED_DAYS` در Metadata فیزیکی وجود ندارند؛ بنابراین برای این فیلدها Reference Table مصنوعی ساخته نشده است.

## PARTY_GENERAL_PREFERENCE

ساختار فیزیکی ۱۲/۱۲ ستون از قبل در DDL پایه موجود بود. CRUD مستقل اضافه شده است. نوع ترجیح از `REF_PREFERENCE_TYPE` و منبع از `REF_SOURCE_SYSTEM` انتخاب می‌شود.

Reference Seed فعلی `REF_PREFERENCE_TYPE` شامل `LANGUAGE`, `CONTACT_TIME`, `STATEMENT_DELIVERY` است. گزینه‌های اضافی HTML که در Reference Data فیزیکی کد ندارند در این نسخه Seed نشده‌اند. Backend همپوشانی بازه اعتبار برای یک نوع ترجیح را رد می‌کند.

## مدارک

HTML رضایت، مدارک را در همان مرحله نشان می‌دهد؛ اما `PARTY_DOCUMENT` قبلاً در Phase 4 با CRUD کامل و ارتباط اختیاری KYC پیاده‌سازی شده است. برای جلوگیری از دو مسیر ناسازگار، Phase 9 فقط به همان فرم مدارک لینک می‌دهد.

## Party 360

سه Collection جدید به Party 360 اضافه شده‌اند:

- `consents`
- `communicationPreferences`
- `generalPreferences`

Tab «رضایت‌ها و ترجیحات» و لینک مستقیم فرم عملیاتی نیز اضافه شده است.

## Migration

برای دیتابیس موجود:

`database/oracle/cif/migrations/0.3.20-consent-preference-alignment.sql`

Migration فقط ستون‌های واقعی موجود در `CIF-tables4.xlsx` را در صورت نبودن اضافه می‌کند و idempotent است.

## پوشش Schema

- `PARTY_CONSENT`: 21/21
- `COMMUNICATION_PREFERENCE`: 16/16
- `PARTY_GENERAL_PREFERENCE`: 12/12
- `PARTY_DOCUMENT`: 21/21 (reuse Phase 4)

## مرحله بعد

Phase 10: عملیات کنترلی Party شامل Lifecycle/Status History و Merge Partyهای تکراری.
