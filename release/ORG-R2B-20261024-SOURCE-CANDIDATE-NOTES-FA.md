# ORG R2-B — Source Candidate for UAT (2026-10-24)

**وضعیت:** `SOURCE_CANDIDATE / DB_R2B_NOT_APPLIED / FULL_BUILD_NOT_VERIFIED / UAT_PENDING`

## علت تحویل

- فرم اطلاعات تماس پس از انتخاب شعبه خالی است، زیرا جداول ORG مربوط به انتساب تماس در آخرین baseline، صفر رکورد داشتند و Master تماس جدید نیز پیش از شروع فاقد رکورد بود. این وضعیت اثبات پاک شدن شماره تلفن‌های تاریخی نیست.
- شماره‌های احتمالی شعب در سایر اسناد/جداول/فایل‌های رسمی تا زمان تعیین قطعی منبع و تطبیق `ORGANIZATION_UNIT_ID` به شکل خودکار وارد نمی‌شوند.
- فرم مکان و نشانی R2-A صرفاً برای تطبیق اطلاعات پس از COMMIT بوده و قابلیت درج/ویرایش نداشته است.

## اصلاحات سورس

1. عنوان‌های UI: **اطلاعات تماس واحدهای سازمانی** و **مکان و نشانی استقرار واحدهای سازمانی**.
2. توضیح قابل مشاهده در گرید خالی تماس برای جلوگیری از برداشت اشتباه درباره حذف داده.
3. اسکریپت فقط خواندنی برای شناسایی جدول/ستون‌های محتمل تلفن شعب: `database/oracle/org/contacts-r2/01-ORG-LEGACY-BRANCH-PHONES-READONLY.sql`.
4. فرم ثبت استقرار جدید و اصلاحیه نشانی با جست‌وجوی واحد، تاریخ اعتبار، مختصات، کدپستی اختیاری، مرجع مستند و نمایش کنترل نسخه.
5. API اختصاصی مکان و نشانی با ثبت تغییر و Snapshot قبل/بعد در تراکنش واحد؛ جلوگیری از هم‌پوشانی PRIMARY برای یک واحد و کنترل تغییرات هم‌زمان.
6. SQL اختیاری/غیرخودکار R2-B؛ از پیش `NO` و بدون حذف یا UPDATE رکوردهای قدیمی. برای ردیف‌های جدید `LOCATION_ID` می‌تواند NULL باشد؛ FK قدیمی برای رکوردهای دارای شناسه پابرجاست.
7. منع CRUD از مسیر فرم عمومی قدیمی مکان/نشانی پس از فعال شدن رابط یکپارچه، با حفظ قابلیت مشاهده و جداول قدیمی.
8. سازگاری Backend با **Jackson 3 / tools.jackson.databind.json.JsonMapper** مطابق دیگر سرویس‌های همین پروژه.

## نتیجه آزمون‌های انجام‌شده

- R2-B static verifier: `11/11 PASS`.
- Java validation: `14/14 PASS`.
- Java syntax/contract test: کامپایل کلاس‌های مرتبط با Stub های آزمایشی موفق؛ **معادل Maven Build نیست**.
- TypeScript syntax/transpile: ۴ فایل مرتبط بدون خطای نحوی؛ **معادل Angular template build نیست**.
- Verifierهای R1/R2-A، ORG contact، منو، ORG UX و ORG/CAL2: PASS.
- **اجرا نشده:** Oracle R2-B DDL در محیط کاربر، تست CRUD واقعی، Maven/Spring Boot و Build کامل Angular، تست مرورگر و UAT کسب‌وکار.

## ترتیب نصب ایمن

1. از سورس و دیتابیس نسخه پشتیبان تهیه کنید. این بسته **فقط سورس** است، نه JAR آماده اجرا.
2. `tools\\VERIFY-ORG-R2B-LOCATION-CONTACTS.cmd` و `build-production.cmd` را در محیط ویندوز پروژه اجرا کنید.
3. `database/oracle/org/contacts-r2/01-ORG-LEGACY-BRANCH-PHONES-READONLY.sql` را صرفاً برای کشف محل احتمالی داده‌های قبلی با `F5` اجرا کنید؛ **هیچ شماره‌ای را پاک یا منتقل نمی‌کند**.
4. `database/oracle/org/locations-r2b/01-ORG-UNIT-LOCATIONS-R2B-OPT-IN.sql` را ابتدا با `APPLY_CHANGES = NO` اجرا و خروجی را بررسی کنید. تغییر به `YES` فقط در **پایگاه داده آزمایشی با تأیید DBA**.
5. پس از APPLY، `02-ORG-UNIT-LOCATIONS-R2B-VERIFY-READONLY.sql` را در نشست جدید اجرا کنید، مجوز اتصال برنامه را بازبینی و Build/Restart کنید.
6. فرم‌ها: `http://localhost:8091/#/organization/unit-contacts` و `http://localhost:8091/#/organization/unit-locations-canonical`.

**حفاظت داده:** هیچ عملیات SQL روی Oracle شما توسط تولید این بسته اجرا نشده است. ۱۳۴۰ استقرار R1، چهار انتساب مشترک LOCATION_ID=1327 و اطلاعات رسمی بانک ملت در اسکریپت‌های تحویل‌شده عمداً بازنویسی نمی‌شوند. حذف فیزیکی `ORG.LOCATIONS` یا `ORG.POSTAL_ADDRESSES` مجاز نیست.
