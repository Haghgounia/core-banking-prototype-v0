# عملیات Party — فاز ۱۱: Party / Customer 360 نهایی و End-to-End Hardening

نسخه: `0.3.22-prototype`

## هدف

این فاز Feature عملیاتی جدیدی ایجاد نمی‌کند. هدف، بستن جریان Party/Customer و تبدیل صفحه 360 به نمای تجمیعی نهایی بر اساس سه مرجع جاری است:

- XML/XMI تحویلی: مدل EA و مرزبندی دامنه‌ها
- HTML تحویلی: Workflow و رفتار عملیاتی
- `CIF-tables4.xlsx`: Metadata فیزیکی جاری Oracle

طبق فرم مرجع، Customer 360 محل ایجاد/ویرایش داده محصول، تعامل و شکایت نیست؛ داده‌های سامانه‌های مبدأ فقط خوانده و تجمیع می‌شوند. همچنین «ثبت نهایی» Workflow به‌معنای تغییر خودکار Lifecycle نیست؛ تغییر وضعیت فقط از عملیات مستقل Lifecycle انجام می‌شود.

## Party 360 Summary

کلاس مفهومی `PARTY_360_SUMMARY` در EA جدول فیزیکی Oracle نیست. در این نسخه مقادیر آن در زمان درخواست محاسبه می‌شوند:

- تعداد محصولات فعال از `PARTY_PRODUCT_HOLDING`
- تعداد شکایت‌های باز از `PARTY_COMPLAINT`
- آخرین تعامل از `PARTY_INTERACTION`
- امتیاز ارزش جاری از `PARTY_VALUE_SCORE`
- سگمنت جاری از `PARTY_SEGMENT_MEMBERSHIP`
- تعداد پیشنهادهای فعال از `PARTY_RECOMMENDATION`
- زمان Snapshot از Database

هیچ جدول ساختگی `PARTY_360_SUMMARY` ایجاد نشده است.

## Read-only 360 aggregation

۱۸ جدول عملیاتی که در این Prototype مالکیت و CRUD آن‌ها در فرم‌های CIF پیاده‌سازی نشده است، به‌صورت Read-only در نمای 360 فعال شده‌اند. بخش مهمی از این داده‌ها مانند محصول، تعامل، شکایت و Journey از سامانه/فرایند مبدأ می‌آیند؛ `PARTY_REGISTRATION_REQUEST` و `AUDIT_EVENT` نیز به‌عنوان Trace/Audit فقط خوانده می‌شوند:

`AUDIT_EVENT`, `ORGANIZATION_OFFICER`, `PARTY_ALERT_CASE`, `PARTY_COMPLAINT`, `PARTY_COMPLAINT_STATUS_HISTORY`, `PARTY_GROUP`, `PARTY_GROUP_MEMBER`, `PARTY_INTERACTION`, `PARTY_JOURNEY_EVENT`, `PARTY_METRIC_SNAPSHOT`, `PARTY_OPERATION_LIMIT`, `PARTY_PRODUCT_HOLDING`, `PARTY_PRODUCT_RESTRICTION`, `PARTY_RECOMMENDATION`, `PARTY_REGISTRATION_REQUEST`, `PARTY_SEGMENT_MEMBERSHIP`, `PARTY_VALUE_SCORE`, `SIGNATURE_SPECIMEN`.

برای جلوگیری از پاسخ نامحدود، هر مجموعه حداکثر ۱۰۰ رکورد جدیدتر را برمی‌گرداند. `SIGNATURE_IMAGE`، `AUDIT_EVENT.BEFORE_DATA` و `AFTER_DATA` عمداً در پاسخ 360 حمل نمی‌شوند؛ فقط وجود تصویر و Metadata ممیزی نمایش داده می‌شود.

## End-to-End Readiness

API جدید:

`GET /api/v1/cif/parties/{partyId}/readiness`

برای Party بدون Customer Role، هویت پایه معیار اجباری است. برای Party دارای Customer Role، علاوه بر هویت پایه موارد زیر کنترل می‌شوند:

1. نشانی و راه تماس
2. نمایه مالی
3. Customer Role جاری و `PARTY_CUSTOMER` متناظر با شماره مشتری
4. KYC دارای سطح ریسک نهایی و تصمیم
5. Consent اعطاشده و معتبر

Party ادغام‌شده قابل Finalize مجدد نیست. این API فقط Readiness را گزارش می‌کند و Lifecycle را تغییر نمی‌دهد.

## پوشش Metadata

تمام ۴۸ جدول عملیاتی موجود در Sheet `Column` فایل `CIF-tables4.xlsx` اکنون در Backend حضور دارند:

- ۳۰ جدول در Workflowهای CIF/Party نگهداری یا استفاده عملیاتی می‌شوند.
- ۱۸ جدول تکمیلی فوق فقط در Customer 360 خوانده می‌شوند و CRUD آن‌ها در CIF این Prototype انجام نمی‌شود.

فهرست کنترل در `docs/CIF-0.3.22-OPERATIONAL-TABLE-COVERAGE.csv` ذخیره شده است.

## Schema drift نهایی

`PARTY_REGISTRATION_REQUEST` در Metadata جاری و مدل EA وجود دارد، اما Snapshot تاریخی `database/oracle/cif/ddl/CIF-050517.sql` آن را ندارد. Migration زیر برای نصب‌هایی که از Snapshot تاریخی ارتقا می‌یابند اضافه شده است:

`database/oracle/cif/migrations/0.3.22-registration-request-alignment.sql`

Migration جدول ۱۵ ستونی، Sequence و Unique Index مشاهده‌شده در Metadata را به‌صورت idempotent ایجاد می‌کند. چون Sheetهای فیزیکی فعلی برای این جدول FK/Check قابل اتکایی گزارش نکرده‌اند، Constraint مرجع حدسی ایجاد نشده است.

## Hardening

- Customer number فقط وقتی Readiness را کامل می‌کند که `PARTY_CUSTOMER.PARTY_ROLE_ID` به یکی از Customer Roleهای فعال همان Party اشاره کند.
- این ۱۸ مجموعه Read-only هیچ API تغییر/حذف در CIF ندارند.
- BLOB نمونه امضا و JSON/CLOB قبل/بعد Audit از DTO 360 حذف شده‌اند.
- Importهای نسبی Angular و Template/Style referenceها در QA نهایی کنترل می‌شوند.
- تطبیق ستون‌های `PARTY_REGISTRATION_REQUEST` با Metadata باید 15/15 باشد.

## مرز ثبت اولیه

`PARTY_REGISTRATION_REQUEST` در مدل و Metadata وجود دارد و برای ارتقای Schema ایجاد می‌شود، اما فایل‌های مرجع واژگان معتبر `REQUEST_STATUS_CODE` و قرارداد API نوشتن این درخواست را مشخص نکرده‌اند. برای جلوگیری از اختراع State Machine، در 0.3.22 این جدول فقط در 360 خوانده می‌شود و جریان موجود ایجاد Party همچنان Atomic Onboarding باقی می‌ماند.
