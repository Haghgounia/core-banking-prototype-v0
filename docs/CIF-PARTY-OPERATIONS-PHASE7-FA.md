# فرم‌های عملیاتی Party — فاز ۷: نقش‌ها و رابطه بانکی

نسخه: `0.3.18-prototype`

این فاز مستقیماً بر مدل EA/XMI و فرم عملیاتی Party منطبق است و دو موجودیت `CIF.PARTY_ROLE` و `CIF.PARTY_CUSTOMER` را عملیاتی می‌کند.

## اصل معماری

- Person یا Organization ابتدا `PARTY` است.
- `CUSTOMER` فقط یکی از Roleهای Party است.
- سایر نقش‌ها بدون ایجاد شماره مشتری با `PARTY_ID` مدیریت می‌شوند.
- با ایجاد Role نوع `CUSTOMER`، یک رکورد `PARTY_CUSTOMER` و شماره مشتری سیستمی ایجاد می‌شود.
- شماره مشتری در طول عمر رابطه ثابت می‌ماند؛ پایان رابطه با تغییر Status/Valid-To انجام می‌شود و Customer Role حذف فیزیکی نمی‌شود.

## کنترل منبع مدل

- فایل EA/XMI پیوست برای `PARTY_ROLE` تعداد ۲۱ ویژگی کسب‌وکاری تعریف می‌کند.
- `CIF-tables3.xlsx` همان مدل را با سه ستون Audit سازگاری (`CREATED_DATE`, `LAST_MODIFIED_BY`, `LAST_MODIFIED_DATE`) در مجموع به ۲۴ ستون می‌رساند. این سه ستون حذف نشده‌اند تا DDL تاریخی و داده موجود شکسته نشود.
- فایل EA/XMI برای `PARTY_CUSTOMER` تعداد ۱۳ ویژگی تعریف می‌کند و Excel نیز همان ۱۳ ستون را دارد.
- در XMI پیوست، بخش Constraintهای زیر کلاس `PARTY_CUSTOMER` ظاهراً متن‌هایی از `PARTY_CLASSIFICATION` را تکرار می‌کند، در حالی که Association موجود به Party و معنای `PARTY_ROLE_ID` رابطه Customer Role را نشان می‌دهد. این ناسازگاری منبع به‌عنوان Model-QA ثبت شده و در Prototype از ساختار فیلدها و معنای فرم عملیاتی استفاده شده است.

## مدل PARTY_ROLE

نسخه جاری فایل `CIF-tables3.xlsx` برای `PARTY_ROLE` ۲۴ ستون دارد. هشت ستون جدید نسبت به DDL تاریخی عبارت‌اند از: `PRINCIPAL_PARTY_ID`, `RELATIONSHIP_TYPE_CODE`, `AUTHORITY_BASIS_CODE`, `AUTHORITY_DOCUMENT_NO`, `AUTHORITY_ISSUER`, `AUTHORITY_SCOPE_TEXT`, `ASSIGNMENT_REASON_TEXT`, `DESCRIPTION_TEXT`.

## مدل PARTY_CUSTOMER

`PARTY_CUSTOMER` ۱۳ ستون دارد و رابطه نقش CUSTOMER با `CUSTOMER_NO`, وضعیت، بازه اعتبار و `IS_CURRENT` را نگهداری می‌کند.

## قواعد

- نوع Role، Context و Status باید از Reference Data فعال CIF باشند.
- Context Type و Context ID باید همزمان پر یا خالی باشند.
- Roleهای مبتنی بر شخص/سازمان اصیل نیازمند `PRINCIPAL_PARTY_ID` هستند و آن Party باید در CIF وجود داشته باشد.
- برای Roleهای مبتنی بر اختیار، شماره/نوع سند اختیار الزامی است.
- برای هر Party فقط یک رابطه مشتری جاری قابل ایجاد است.
- `RECORD_VERSION` برای ویرایش Optimistic Lock استفاده می‌شود.

## شماره مشتری

مدل EA/XMI فیلد `CUSTOMER_NO` را تعریف می‌کند اما الگوریتم شماره‌گذاری را تعیین نکرده است. در Prototype از `CIF.SEQ_CUSTOMER_NO` مستقل استفاده می‌شود؛ این سیاست عمداً از `PARTY_CUSTOMER_ID` جداست تا در پیاده‌سازی بانکی با سیاست واقعی شماره مشتری جایگزین شود.

## Migration

پیش از اجرای Backend این نسخه روی دیتابیس موجود اجرا شود:

`database/oracle/cif/migrations/0.3.18-party-role-customer.sql`

Migration idempotent است، ستون‌های جدید `PARTY_ROLE` را اضافه می‌کند، `PARTY_CUSTOMER` و Sequenceهای لازم را در صورت نبود ایجاد می‌کند و Roleهای عملیاتی موجود در فرم مرجع را به `REF_ROLE_TYPE` اضافه می‌کند.
