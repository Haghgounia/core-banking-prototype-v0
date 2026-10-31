# ORG — استقلال جدول تجمیعی مکان و نشانی (گام R3، ۳۱ اکتبر ۲۰۲۶)

**مرجع تصمیم:** `ORG.ORGANIZATION_UNIT_LOCATIONS` تنها مرجع عملیاتی مکان و نشانی واحدهای سازمانی است؛ `LOCATION_ID` تنها شناسه تاریخی اختیاری است و نباید برای ثبت مکان جدید یا مشاهده فهرست الزامی باشد.

**مبنای سورس:** `core-banking-prototype-v0-2026-10-31-1151.zip` و DDL ارسالی `ORG-2026-10-31.txt`. **وضعیت:** بسته Source + SQL آماده، بدون اجرای DDL/Oracle در محیط تدوین. این سند تأیید نصب یا UAT نیست.

## تغییرات واقعی سورس

- API عملیاتی جدید `GET /api/v1/organization/unit-locations/browse` با جست‌وجو، صفحه‌بندی و خواندن مستقیم `ORG.ORGANIZATION_UNIT_LOCATIONS` و فقط Lookup نام/کد واحد از `ORG.ORGANIZATION_UNITS`. هیچ Join با `LOCATIONS` یا `POSTAL_ADDRESSES` در مسیر عملیاتی وجود ندارد.
- صفحه Angular «نشانی و موقعیت شعب و واحدها» این API را مصرف می‌کند. مرور تاریخی `GET /api/v1/organization/unit-location-canonical-review` و مسیر R1 برای ممیزی باقی می‌مانند؛ **نمی‌توانند معیار جاری بودن منبع Legacy در فرم عملیاتی باشند**.
- `OrganizationUnitLocationService.readiness()` تا زمانی که هیچ FK از `ORGANIZATION_UNIT_LOCATIONS` به `LOCATIONS` باقی نمانده باشد و ستون‌ها و Audit حاضر نباشند، `ready=false` برمی‌گرداند. بلافاصله پس از اعمال DDL، ثبت/ویرایش در صورت احراز همه پیش‌نیازها فعال می‌شود.
- ثبت (`INSERT`) از قبل `LOCATION_ID=NULL` قرار می‌داد، تاریخچه و نسخه رکورد را نگه می‌داشت و با همان تراکنش در `OU_LOCATION_CHANGE_AUDIT` ممیزی می‌کرد؛ این قرارداد تغییر نکرده است.
- فرم‌های عمومی قدیمی از قبل در `ReferenceService.requireNotLegacyGenericWrite()` در برابر نوشتن محافظت شده‌اند؛ محافظ برداشته نشده است.
- آزمون جدید `OrganizationUnitLocationBrowseServiceTest` و کنترل ایستا در Build Windows/Linux افزوده شد.

## مراحل اجرای کنترل‌شده

**۱. Source:** ابتدا برنامه را متوقف و بسته را با `INSTALL.cmd` در مسیر اصلی پروژه نصب کنید؛ نصب‌کننده از فایل‌های موجود نسخه پشتیبان می‌گیرد، تطبیق بایت انجام می‌دهد و `build-production.cmd` را اجرا می‌کند.

**۲. Oracle Preview:** در SQL Developer با F5، فایل زیر را با مقدار پیش‌فرض `APPLY_CHANGES=NO` اجرا کنید:

`database/oracle/org/canonical-location-independence/01-ORG-UNIT-LOCATION-DETACH-OPT-IN.sql`

خروجی `PRECHECK | BLOCKERS=0` لازم است؛ بررسی داده‌های ناموجود/انتقال‌نیافته و ارتباطات نقطه خدمت نیز چاپ می‌شود. این پیش‌نمایش هیچ DDL یا DML ندارد. نسخه پشتیبان مستقل Oracle و تأیید DBA پیش از گام بعد الزامی است.

**۳. Oracle Apply:** پس از توقف مصرف‌کنندگان و تأیید DBA، فقط در همان فایل مقدار `DEFINE APPLY_CHANGES = YES` را قرار داده و با F5 اجرا کنید. DDL ضمنی Commit می‌کند؛ Rollback تراکنشی ندارد. اسکریپت وابستگی FK واحدها را حذف می‌کند، `LOCATION_ID` را nullable می‌نماید، سه ستون R2B و سه CHECK و ساختار Audit را در صورت فقدان ایجاد می‌کند. **هیچ سطر موجودی تغییر نمی‌کند.** اگر DDL نیمه‌کاره متوقف شد، اسکریپت پیش از تکرار باید با خروجی Verify بازبینی شود.

**۴. Verify:** با F5 اسکریپت

`database/oracle/org/canonical-location-independence/02-ORG-UNIT-LOCATION-INDEPENDENCE-VERIFY-READONLY.sql`

را اجرا کنید. انتظار: همه کنترل‌های ساختاری مقدار ۱، FK از `ORGANIZATION_UNIT_LOCATIONS` به `LOCATIONS` صفر، تعداد رکوردهای تجمیعی قبل/بعد برابر، و Audit رکوردهای Managed پوشش‌دار. **وجود FK برای `SERVICE_POINTS` و `POSTAL_ADDRESSES` فعلاً طبیعی است.**

**۵. Runtime/UAT:** پس از `bin\start.cmd` آدرس `http://localhost:8091/#/organization/unit-locations-canonical` را باز کنید. API `/api/v1/organization/unit-locations/readiness` باید `ready=true` و `legacyLocationFkDetached=true` باشد. موارد پذیرش: فهرست ۱۳۴۰ استقرار قبلی با نشانی و مختصات، رکوردهای مشترک `LOCATION_ID=1327` بدون حذف/تفکیک، درج یک نشانی جدید بدون مقدار `LOCATION_ID` و ثبت Audit، ویرایش کنترل‌شده `ROW_VERSION`، اختیاری بودن کدپستی و مختصات. در تست ثبت، مرجع مستند تصمیم الزامی است. داده رسمی شعب از منبع رسمی بانک ملاک است؛ GEO فقط مرجع شناسه فنی است.

## محدودیت مرز دامنه

این تغییر **استقلال مکان واحد سازمانی** را هدف می‌گیرد؛ حذف `ORG.LOCATIONS` یا `ORG.POSTAL_ADDRESSES` هنوز مجاز نیست. `ORG.SERVICE_POINTS.LOCATION_ID` و `ORG.POSTAL_ADDRESSES.LOCATION_ID` هنوز به جدول قدیمی ارجاع دارند. نقاط خدمت مستقل نباید بدون اثبات نگاشت یک‌به‌یک یا تصمیم مدل داده به مکان یک واحد منتسب شوند. اسکریپت حاضر روی این جداول، سطرهای تاریخی یا Sequenceهای Legacy DDL/DML اجرا نمی‌کند.

### وضعیت و شواهد

- انجام‌شده در Source: مسیر خواندن Canonical، قرارداد Readiness، آزمون و کنترل Build، اسکریپت Opt-in و Verify.
- در انتظار اجرای کارفرما/DBA: Preview، Oracle Apply، Grantهای احتمالی برای کاربر برنامه، آزمون اتصال Oracle، UAT فرم‌ها و تصمیم مستقل نقاط خدمت.
- Commit Git: در Snapshot ارسالی داده `.git` وجود ندارد؛ در این بسته **Commit یا اجرای Git Diff روی Repository واقعی ادعا نمی‌شود**. Manifest دارای SHA-256 و فهرست فایل‌های جدید/تغییریافته است.
- تغییر نقشه راه FEE3: ندارد. Oracle Write کارمزد و Posting: تغییری ندارد.
