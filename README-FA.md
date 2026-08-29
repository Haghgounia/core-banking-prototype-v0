# Core Banking Prototype

این پروژه یک Prototype بانکداری متمرکز با ساختار ماژولار و استقرار یکپارچه است.

## وضعیت این نسخه

در وضعیت فعلی پنج دامنه اطلاعات پایه/مرجع اصلی فعال هستند:

```text
reference-data                    -> Schema GEO
deposit-product reference-data    -> Schema DPS
customer-information-file (CIF)   -> Schema CIF
enterprise-calendar                -> Schema CAL
bian-400y-calendar                 -> Schema CAL2
```

در این نسخه ۲۰۳ فرم اطلاعات پایه/تقویم فعال هستند: ۲۰ فرم عمومی/GEO، ۵۰ فرم `DPS.REF_*`، ۱۰۱ فرم اطلاعات پایه Party/Customer در CIF (شامل دو فرم مستقل ISIC2)، ۱۶ فرم تقویم یک در CAL و ۱۶ فرم مستقل تقویم دو در CAL2. علاوه بر آن، ماژول «مدیریت مشتری / CIF» با فهرست Party، Workflow کامل شخص حقیقی/حقوقی و نمای نهایی Party / Customer 360 فعال است.

برای جداول عملیاتی `DEPOSIT_PRODUCT*` هنوز Package، API یا صفحه‌ای ایجاد نشده است. مسیر عملیاتی CIF از ایجاد Party تا اطلاعات Person/Organization، تماس و نشانی، مالی، شناسه و مدرک، طبقه‌بندی، روابط/UBO، Role/Customer، KYC/Risk/Screening، Consent/Preference، Lifecycle و Merge تکمیل شده است. در نسخه 0.3.22 تمام ۴۸ جدول عملیاتی موجود در `CIF-tables5.xlsx` در Backend پوشش داده می‌شوند: ۳۰ جدول در Workflowهای CIF استفاده/نگهداری می‌شوند و ۱۸ جدول تکمیلی بدون CRUD در CIF به‌صورت Read-only در Party / Customer 360 تجمیع می‌شوند؛ محصولات/تعاملات/شکایات و مشابه آن از سامانه‌های مبدأ می‌آیند و Registration/Audit صرفاً Trace خواندنی هستند.


### ISIC نسخه‌محور

از نسخه `0.3.64` مدل جدید ISIC کاملاً مستقل از `CIF.REF_ISIC_ACTIVITY` قدیمی است. دو فرم «نسخه‌های ISIC» و «فعالیت‌های ISIC نسخه‌محور» بر پایه `CIF.REF_ISIC_RELEASE` و `CIF.REF_ISIC_ACTIVITY2` فعال هستند و متن‌های توضیحی طولانی در جدول فرزند `CIF.REF_ISIC_ACTIVITY_NOTE` با نوع‌های `EXPLANATORY / INCLUDES / ALSO_INCLUDES / EXCLUDES` نگهداری می‌شوند. ساختار فعالیت‌ها با `PARENT_ACTIVITY_ID` و `LEVEL_NO` مدل شده است. Seed UNSD Rev.4 شامل 766 رکورد و 419 Class قابل انتخاب است و `NAME_FA` و `NAME_EN` برای همه رکوردها مقدار دارند؛ عنوان‌های فارسی با وضعیت `BANK_TRANSLATED` ثبت شده‌اند. اسکریپت‌های Oracle در `database/oracle/cif/isic2` قرار دارند و باید جدا از Build برنامه اجرا شوند.

## معماری فعلی

پروژه یک Modular Monolith سبک با یک فایل استقرار واحد است:

```text
core-banking-prototype/
├── backend/
├── frontend/
├── database/
├── config/
├── bin/
├── tools/
├── docs/
└── app/
```

### Packageهای Backend

```text
com.behsazan.corebanking
├── CoreBankingApplication
├── shared
│   ├── config
│   ├── error
│   └── model
├── referencedata
│   ├── catalog
│   ├── descriptor
│   ├── education
│   ├── employment
│   ├── general
│   ├── geography
│   └── management
├── deposit
│   └── productfactory
│       └── reference
├── calendar
│   ├── reference
│   │   ├── application
│   │   ├── domain
│   │   ├── oracle
│   │   └── web
│   └── datasetimport
│       ├── application
│       ├── domain
│       ├── oracle
│       └── web
├── calendar2
│   ├── reference
│   ├── datasetimport
│   ├── eventrecurrence
│   └── monthview
│       ├── application
│       ├── domain
│       ├── oracle
│       └── web
└── cif
    ├── application
    ├── domain
    ├── error
    ├── oracle
    ├── reference
    │   ├── application
    │   ├── domain
    │   ├── oracle
    │   └── web
    └── web
```

Package `deposit.productfactory.reference` فقط Metadata موردنیاز ۵۰ فرم مرجع DPS را نگهداری می‌کند؛ هیچ مدل عملیاتی محصول سپرده در آن ایجاد نشده است.

## Schemaهای Oracle

```yaml
core-banking:
  schemas:
    reference-data: GEO
    deposit-product-factory: DPS
    cif: CIF
    party-reference: CIF
    calendar: CAL
    calendar2: CAL2
```

- `GEO`: مالک فیزیکی فعلی جداول اطلاعات پایه. جداسازی منطقی ماژول در کد انجام شده است، اما جداول فعلاً در همین Schema باقی می‌مانند.
- `DPS`: مالک جداول مرجع محصول‌ساز سپرده و اسکریپت‌های Oracle مربوط به آن‌ها.
- `CIF`: مالک جداول مدیریت Party، Person/Organization، KYC، نشانی، تماس، ریسک و غربالگری.
- `party-reference`: مالک منطقی Reference Data جدید Party/Customer و در این فاز برابر Schema `CIF` است.
- `CAL`: مالک مدل تقویم یک شامل تقویم سه‌گانه، روز کاری، مناسبت و اصلاح رسمی قمری است.
- `CAL2`: مالک مدل مستقل BIAN-aligned چهارصدساله شامل Calendar Variant، Source Authority، Dataset Version، Canonical Day، Event، Business Calendar و Validation Evidence است و هیچ جدول/FK مشترکی با `CAL` ندارد.

### Import Dataset تقویم بدون SQL*Loader

از مسیر `/calendar/reference-data/import` دو فایل `calendar_day.csv` و `calendar_date.csv` انتخاب می‌شوند. Backend با همان DataSource برنامه فایل‌ها را به‌صورت Streaming/JDBC Batch ثبت می‌کند. مطابق تصمیم اجرایی FIX50 هیچ کنترل معنایی/تعداد رکورد Dataset در مسیر Import انجام نمی‌شود؛ هر دو فایل در یک تراکنش درج و پس از پایان موفق Insertها Commit می‌شوند و خطای واقعی Insert/JDBC باعث Rollback می‌شود.

### Reference Data جدید Party / Customer

Reference Data جدید Party/Customer در دو فاز فعال شده است: ۳۲ فرم Identity/Party در فاز اول و ۲۱ فرم Compliance/Risk/KYC در فاز دوم. برای ارتقا از نسخه 0.3.2 فقط اسکریپت فاز دوم اجرا شود:

```sql
@database/oracle/cif/reference-data/compliance-risk/install.sql
```

برای نصب تازه، ابتدا `identity-party/install.sql` و سپس `compliance-risk/install.sql` اجرا می‌شود. جزئیات فاز دوم در `docs/CIF-PARTY-REFERENCE-PHASE2-COMPLIANCE-RISK-FA.md` ثبت شده است.

## ساختار اسکریپت‌های پایگاه داده

```text
database/oracle/
├── geo/
│   ├── ddl/
│   ├── data/
│   ├── install-ddl.sql
│   └── install-data.sql
├── dps/
│   ├── ddl/
│   └── data/
├── cal/
│   └── ...
├── cal2/
│   ├── 00-create-cal2-schema.sql
│   ├── 01-create-cal2-tables.sql
│   ├── 02-grant-cal2-to-application-user.sql
│   └── 99-verify-cal2-schema.sql
└── cif/
    ├── ddl/
    └── reference-data/
        ├── identity-party/
        └── compliance-risk/
```

DDL و Comment و Constraintهای دریافت‌شده از Oracle بدون بازطراحی در `database/oracle/dps/ddl` نگهداری می‌شوند. فایل افزایشی `08_add-created-by-to-reference-tables.sql` تغییر اعلام‌شده برای ستون `CREATED_BY` را ثبت می‌کند.

## قابلیت‌های ماژول اطلاعات پایه

- ۲۰۳ فرم فعال اطلاعات پایه/تقویم؛ شامل ۲۰ فرم عمومی/GEO، ۵۰ فرم مرجع محصول سپرده در DPS، ۱۰۱ فرم Party/Customer در CIF (با دو فرم مستقل ISIC2)، ۱۶ فرم تقویم یک در CAL و ۱۶ فرم تقویم دو/CAL2
- Import مستقیم Dataset تقویم یک/CAL از `calendar_day.csv` و `calendar_date.csv` با JDBC Batch و تراکنش واحد؛ بدون SQL*Loader/Oracle Client
- Import مستقیم بسته ZIP مدل BIAN شامل ۱۵ CSV به Schema مستقل `CAL2` با ترتیب FK، JDBC Batch و یک تراکنش
- نمای ماهانه CAL2 با پیش‌فرض هجری شمسی، جابه‌جایی ماه‌به‌ماه، سوئیچ شمسی/میلادی/قمری و نمایش مناسبت‌ها/تعطیلی‌های materialized از `EVENT_OCCURRENCE`
- فهرست Party و پرونده جامع Customer 360 در ماژول CIF
- پوشش کامل ۴۸ جدول عملیاتی CIF مطابق `CIF-tables5.xlsx`: ۳۰ جدول در Workflowهای ایجاد/نگهداری Party استفاده می‌شوند و ۱۸ جدول تکمیلی به‌صورت Read-only در Party / Customer 360 تجمیع می‌شوند
- Runtime عمومی Descriptor-driven
- جست‌وجو، مرتب‌سازی و صفحه‌بندی سمت سرور
- ComboBox جست‌وجویی reusable با debounce و جست‌وجوی سمت سرور برای Reference Data
- ثبت، ویرایش و حذف
- Lookupهای والد و روابط سلسله‌مراتبی
- درخت جغرافیایی
- ثبت ایجادکننده و کنترل هم‌زمانی رکوردهای DPS با `RECORD_VERSION`
- پاسخ خطای استاندارد `ProblemDetail`
- Theme روشن، تیره و هماهنگ با سیستم
- صفحه مشخصات فنی سیستم

## فناوری‌ها

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring JDBC / JdbcClient
- Oracle JDBC 23.26.2.0.0
- Angular 21
- TypeScript 5.9
- Angular Material 21
- RxJS 7.8
- Sass / SCSS
- Executable JAR

## Build

Windows:

```cmd
build-production.cmd
```

Linux/macOS:

```bash
./build-production.sh
```

خروجی:

```text
app/core-banking-prototype.jar
```

نسخه Build در فایل `app/BUILD-VERSION` ثبت می‌شود. نام فایل اجرایی در همه نسخه‌ها ثابت است و `start` فقط وقتی اجرا می‌شود که مقدار `BUILD-VERSION` با فایل `VERSION` یکسان باشد.

اجرا در Windows:

```cmd
bin\start.cmd
```

توقف:

```cmd
bin\stop.cmd
```

استخراج DDL و داده Oracle:

```cmd
bin\export-database.cmd
```

این Batch پیش از شروع تأیید می‌گیرد و به‌صورت پیش‌فرض جدول‌های `DPS.REF_*` را در `database/oracle/exports` استخراج می‌کند. راهنمای کامل در `docs/DATABASE-EXPORT-FA.md` قرار دارد.

آدرس برنامه:

```text
http://localhost:8091
```

## اتصال Oracle

این پروژه در محیط تستی و ایزوله اجرا می‌شود؛ بنابراین مشخصات اتصال مستقیماً در هر دو فایل `application.yml` ثبت شده است:

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/FREEPDB1
    username: SYSTEM
    password: Oracle123
```

فایل Runtime اصلی که توسط `bin\start.cmd` و `bin\export-database.cmd` خوانده می‌شود:

```text
config/application.yml
```

نسخه دوم برای اجرای مستقیم Backend از IDE یا Maven نگهداری می‌شود:

```text
backend/src/main/resources/application.yml
```

## API فعلی

برای جلوگیری از شکست Contract نسخه قبلی، مسیرهای REST اطلاعات پایه فعلاً حفظ شده‌اند:

```text
/api/v1/catalog
/api/v1/reference/{resource}
/api/v1/dashboard/counts
/api/v1/cif/parties
/api/v1/cif/parties/{partyId}
/api/v1/cif/dashboard/summary
```

مسیر رابط کاربری فرم‌ها به شکل زیر است:

```text
/#/reference-data/{resource}
```

## صفحه مشخصات فنی

```text
/#/system-specification
```

شماره نسخه و نسخه فناوری‌ها با اسکریپت زیر از فایل‌های Build همگام می‌شوند:

```text
tools/sync-system-specification.mjs
```


## مدیریت مشتری / CIF

فاز اول CIF بر اساس DDL واقعی `database/oracle/cif/ddl/CIF-050517.sql` پیاده‌سازی شده است. از نسخه 0.3.12 به بعد، مدل اجرایی Party با Metadata تحویلی همگام شده است؛ از فاز ۸ به بعد `CIF-tables5.xlsx` مرجع فیزیکی جاری Oracle است و XML/HTML تحویلی به‌ترتیب مرجع مدل EA و Workflow عملیاتی هستند. Snapshot قدیمی DDL داخل Repository فقط برای نصب پایه نگهداری و با Migrationهای هر فاز همگام می‌شود. راهنمای فاز اول در `docs/CIF-CUSTOMER-360-PHASE1-FA.md` و راهنمای فرم عملیاتی فاز ۲ در `docs/CIF-PARTY-OPERATIONS-PHASE2-FA.md` قرار دارد.


## ساختار ناوبری اطلاعات پایه از نسخه 0.3.11

منوی اصلی فقط یک گزینه «اطلاعات پایه» دارد. این گزینه به صفحه انتخاب دامنه هدایت می‌شود و پنج دامنه مستقل «اطلاعات پایه عمومی»، «اطلاعات پایه مشتری / Party»، «اطلاعات پایه محصول سپرده»، «تقویم یک / CAL» و «تقویم دو / CAL2» را نمایش می‌دهد. «درخت جغرافیایی» نیز به بخش «اطلاعات پایه عمومی / اطلاعات جغرافیایی» منتقل شده است و دیگر گزینه سطح اول Sidebar نیست.


راهنمای فاز ۶ روابط و ذی‌نفعان: `docs/CIF-PARTY-OPERATIONS-PHASE6-FA.md`

راهنمای فاز ۷ نقش‌ها و رابطه بانکی: `docs/CIF-PARTY-OPERATIONS-PHASE7-FA.md`

راهنمای فاز ۸ KYC و ریسک: `docs/CIF-PARTY-OPERATIONS-PHASE8-FA.md`

راهنمای فاز ۹ رضایت‌ها و ترجیحات: `docs/CIF-PARTY-OPERATIONS-PHASE9-FA.md`

راهنمای فاز ۱۰ چرخه عمر و ادغام: `docs/CIF-PARTY-OPERATIONS-PHASE10-FA.md`

راهنمای فاز ۱۱ Party / Customer 360 نهایی و Hardening: `docs/CIF-PARTY-OPERATIONS-PHASE11-FA.md`

### اصلاح نشانی Party در 0.3.22-fix2

در این Fix، `PARTY_ADDRESS.SOURCE_CODE` از `CIF.REF_DATA_SOURCE` و `TENURE_TYPE_CODE` از جدول مرجع جدید `CIF.REF_TENURE_TYPE` خوانده می‌شود. برای دیتابیس موجود قبل از اجرای Backend، اسکریپت زیر اجرا شود:

```text
database/oracle/cif/migrations/0.3.22-fix2-address-reference-alignment.sql
```

`VALID_FROM/VALID_TO` در فرم نشانی بازه اعتبار نشانی برای Party هستند؛ زمان تأیید مستقل در مدل `PARTY_ADDRESS` وجود ندارد. فیلد مستقل «سطر تکمیلی نشانی» نیز از UI حذف شده و `ADDRESS_DETAIL` برای شرح تکمیلی استفاده می‌شود.

از `0.3.22-fix9`، منبع نشانی از جدول اختصاصی `CIF.REF_ADDRESS_SOURCE` خوانده می‌شود تا Vocabulary فرم عملیاتی («اظهار مشتری»، «سامانه پست»، «مدرک سکونت») با `REF_DATA_SOURCE` عمومی مخلوط نشود. برای دیتابیس موجود Migration زیر نیز اجرا شود:

```text
database/oracle/cif/migrations/0.3.22-fix9-contact-date-address-source.sql
```

### اصلاح Fix 11 — تطبیق با CIF-tables5 و نمایش فارسی فرم‌ها

از `0.3.22-prototype-fix11` فایل `CIF-tables5.xlsx` مبنای Schema اجرایی CIF است. SQLهای ارزیابی ریسک و غربالگری با ستون‌های جاری همگام شده‌اند. در فرم‌های عملیاتی CIF نیز کدهای لاتین از عنوان/گزینه/Placeholder/Combo حذف شده‌اند؛ مقدار کد همچنان برای API و Database حفظ می‌شود.


### اصلاح Fix 12
- در فرم شناسه، کاربر فقط «مرجع صادرکننده» را انتخاب می‌کند و کد فنی `ISSUER_CODE` در پشت‌صحنه از همان انتخاب تولید می‌شود.
- پیام‌های اعتبارسنجی CIF از `ProblemDetail.fieldErrors` به‌صورت مستقیم در فرم نمایش داده می‌شوند.

### اصلاح Fix 15 — Lookupهای عملیاتی و مدیریت فایل مدارک

در `0.3.22-prototype-fix15` نمایش و ورود کدهای فنی در فرم‌های مالی/شغلی و مدارک بازبینی شده است. فعالیت اقتصادی و انواع/وضعیت‌های عملیاتی با عنوان فارسی نمایش داده می‌شوند، کارفرمای خارج از سامانه مسیر ورود مستقل دارد، Grid منابع درآمد اطلاعات کامل رکورد را نشان می‌دهد و وضعیت‌های منابع درآمد/دارایی از `CIF.REF_WORKFLOW_STATUS` استفاده می‌کنند.

برای دیتابیس موجود Migration زیر اجرا شود:

```text
database/oracle/cif/migrations/0.3.22-fix15-operational-lookup-alignment.sql
```

در فرم مدارک، `CONTENT_HASH` و `STORAGE_REF` دیگر ورودی کاربر نیستند. فایل PDF/JPEG/PNG/TIFF از UI بارگذاری می‌شود، SHA-256 و مرجع `cif-doc:` توسط Backend تولید می‌شوند و فایل در Repository خصوصی تنظیم‌شده با `DOCUMENT_STORAGE_ROOT` نگهداری می‌شود. اتصال مستقیم سخت‌افزاری Scanner در نسخه وب نیازمند Agent/Middleware مورد تأیید بانک است؛ فایل خروجی Scanner از همین Upload Flow پشتیبانی می‌شود.

### اصلاح Fix 17 — ثبت مدارک، طبقه‌بندی و کنترل تاریخ

در `0.3.22-prototype-fix17` جریان ثبت `PARTY_DOCUMENT` بازبینی شده است. فایل مدرک/خروجی اسکنر به‌صورت صریح الزامی است و نتیجه Upload و Save در همان بخش مدارک نمایش داده می‌شود. همچنین Constraint قدیمی `CK_DOC_VERIFY_DATE` حذف شده است؛ `VERIFIED_AT` زمان رخداد کسب‌وکاری است و الزام منطقی ندارد که حتماً بعد از `CREATED_AT` رکورد دیتابیس باشد.

در فرم طبقه‌بندی، «نوع طبقه‌بندی»، «مقدار طبقه‌بندی» و «علت تخصیص» همگی از Reference Data خوانده می‌شوند. مقدار طبقه‌بندی وابسته به نوع است (`REF_CLASSIFICATION_TYPE` -> `REF_CLASSIFICATION_VALUE`). برای گروه صنعت، این طبقه‌بندی فقط سطح پرتفوی است و فعالیت اقتصادی تفصیلی همچنان باید از ISIC نگهداری شود. دکمه ثبت در حالت ناقص قفل نمی‌شود و پس از کلیک، فهرست فیلدهای تکمیل‌نشده به کاربر نمایش داده می‌شود.

کنترل مشترک تاریخ شمسی نیز اصلاح شده است؛ کلیک روی «امروز» همان لحظه مقدار را در FormControl ثبت و در فیلد نمایش می‌دهد.

برای دیتابیس موجود Migration زیر پیش از تست Fix17 اجرا شود:

```text
database/oracle/cif/migrations/0.3.22-fix17-party-document-classification-date.sql
```


### Fix19 — Party Operational Grid & Role Context

مبنای فیزیکی این Fix، آخرین Metadata تحویلی `CIF-tables-2026-08-22-1200.xlsx` است.

در `0.3.22-prototype-fix19` تمام Gridهای اصلی عملیات Party از منظر حداقل Business Context بازبینی شده‌اند. راه‌های تماس، پروفایل مالی، منابع درآمد، دارایی/تعهد، شناسه‌ها، طبقه‌بندی‌ها، نقش‌ها، KYC/Risk و سایر تاریخچه‌های عملیاتی علاوه بر مقدار اصلی، نوع/وضعیت/تاریخ یا Context لازم برای تشخیص رکورد را نمایش می‌دهند. `PARTY_ROLE.CONTEXT_TYPE_CODE/CONTEXT_ID` نیز در فرم نقش به‌صورت صریح مدیریت می‌شود.

در `0.3.22-prototype-fix20` ارزیابی ریسک به داده مرجع مدل متصل شده است. `REF_RISK_MODEL` علاوه بر کد/عنوان، نسخه مدل و دامنه مجاز امتیاز (`MODEL_VERSION`, `MIN_SCORE`, `MAX_SCORE`) را نگهداری می‌کند. با انتخاب مدل در فرم Risk Assessment، نسخه و حداقل/حداکثر خودکار واکشی می‌شوند و Backend نیز قبل از ذخیره همان دامنه و تطبیق نسخه را کنترل می‌کند. دکمه ثبت به‌دلیل Invalid بودن خاموش نمی‌ماند و در صورت نقص، پیام دقیق فیلدهای لازم نمایش داده می‌شود.

## 0.3.22-prototype-fix21 - Persian Reference Column Comments

در Fix21 برای تمام ستون‌های ۹۹ جدول مرجع Party/CIF، Comment فارسی Oracle اضافه شده است. فهرست گزارش‌شده از دیتابیس شامل ۹۹۱ ستون بود؛ سه ستون افزوده‌شده در Fix20 برای مدل ریسک نیز پوشش داده شده‌اند و در مجموع ۹۹۴ ستون دارای Comment فارسی governed هستند. برای دیتابیس موجود، migration `database/oracle/cif/migrations/0.3.22-fix21-reference-column-comments-fa.sql` اجرا شود.
## 0.3.22-prototype-fix22 - Legal Entity Economic Profile

در Fix22 فقط مسیر شخص حقوقی بازبینی شده است. `ORGANIZATION.ISIC_CODE` با عنوان فارسی و جست‌وجوی ISIC انتخاب می‌شود و کد در دیتابیس باقی می‌ماند. سه دامنه مستقل `CIF.REF_ORGANIZATION_ACTIVITY_STATUS`، `CIF.REF_ENTERPRISE_SIZE` و `CIF.REF_OWNERSHIP_TYPE` برای وضعیت فعالیت اقتصادی، اندازه بنگاه و نوع مالکیت اضافه شده‌اند. `EMPLOYEE_COUNT` مقدار عددی است و فقط در هویت شخص حقوقی اخذ می‌شود؛ ورود تکراری آن از بخش ۳.۱ حذف شده است. فرم‌های هویت شخص حقیقی و نگاشت `PERSON` در این Fix تغییر نکرده‌اند. برای دیتابیس موجود migration `database/oracle/cif/migrations/0.3.22-fix22-organization-economic-profile.sql` اجرا شود.




## 0.3.22-prototype-fix24 - Party Authority Reference & Currency UX

- مرجع سند اختیار در بخش C از `CIF.REF_AUTHORITY_DOCUMENT_TYPE` انتخاب می‌شود و کد آن در `PARTY_AUTHORITY.DOCUMENT_REF` ذخیره می‌گردد.
- حداکثر مبلغ با جداکننده هزارگان نمایش داده می‌شود و مقدار عددی بدون قالب‌بندی ذخیره می‌شود.
- ارز حد اختیار به‌صورت پیش‌فرض «ریال ایران / IRR» است؛ اگر سقف مبلغ خالی باشد ارز در رکورد ذخیره نمی‌شود تا قید موجود دیتابیس حفظ شود.
- Backend نوع اختیار، دامنه اختیار، مرجع سند و ارز را قبل از Persistence با داده‌های مرجع معتبر می‌کند.

## 0.3.22-prototype-fix23 - Build Correction

- رفع خطای Angular مربوط به `workflowStatuses()` در `Party360Component`.
- بارگذاری `REF_WORKFLOW_STATUS` برای وضعیت پرونده KYC در نمای Party 360.
- این نسخه تغییر دیتابیسی جدیدی نسبت به Fix22 ندارد.


## 0.3.22-prototype-fix26 - Registration Geography & KYC Readiness

- در هویت شخص حقوقی، ابتدا کشور ثبت انتخاب می‌شود و سپس شهر محل ثبت فقط از زیرمجموعه همان کشور قابل انتخاب است.
- برای ایران `GEO.CITIES` و برای سایر کشورها `GEO.FOREIGN_CITIES` مبناست؛ Backend نیز تطابق کشور/شهر را کنترل می‌کند.
- آمادگی End-to-End به‌جای اعلام کلی «KYC ناقص»، فیلد دقیق ناقص را نمایش می‌دهد.

## 0.3.22-prototype-fix27 - Contact/Address Grid & Role Context UX

- پس از ثبت موفق «ارتباط راه تماس با نشانی»، وضعیت Submitted فرم نیز Reset می‌شود و فیلدهای اجباری خالی دیگر به اشتباه قرمز نمایش داده نمی‌شوند.
- رکوردهای ذخیره‌شده نشانی، راه تماس و ارتباط راه تماس/نشانی از نمایش خطی به Grid ستونی تفکیک‌شده تبدیل شدند.
- قبل از Insert/Update ارتباط تماس و نشانی، Duplicate در سرویس کنترل می‌شود و به‌جای ORA-00001 پیام فارسی قابل فهم نمایش داده می‌شود.
- در فرم نقش Party توضیح روشن اضافه شد که «نوع زمینه» دامنه اعتبار نقش است؛ برای نقش عمومی CUSTOMER معمولاً خالی است و برای نقش محدود به حساب/محصول/شعبه/قرارداد/پرونده استفاده می‌شود.
- Fix27 تغییر Schema جدید ندارد.

## 0.3.22-prototype-fix25 - UX / Risk / Preference

- نمایش راهنمای کم‌رنگ در ورودی‌های عملیاتی، بازچینی ارزیابی ریسک، Combo ارائه‌دهنده استعلام، روشن‌سازی مقدار ترجیح و جلوگیری از انتخاب مجدد نوع شناسه/طبقه‌بندی اعمال شده است.

## 0.3.22-prototype-fix28 - Persisted Data Grids & Dockable Sidebar

- تمام مجموعه‌های چندرکوردی ذخیره‌شده در فرم‌های عملیاتی CIF و نمای Party/Customer 360 به جدول ستونی واقعی تبدیل شده‌اند؛ نمایش Stream/Card برای رکوردهای دیتابیس باقی نمانده است.
- Gridهای مشترک در Desktop داخل عرض فرم جا می‌گیرند، متن سلول‌ها Wrap می‌شود و Scroll افقی اجباری حذف شده است؛ فقط در نمایشگرهای کوچک‌تر از 820px Scroll افقی به‌عنوان fallback واکنش‌گرا مجاز است.
- Sidebar سمت چپ در محیط RTL قابل Dock/Collapse است: حالت کامل 290px و حالت جمع‌شده 76px. وضعیت انتخاب کاربر در `localStorage` نگهداری می‌شود و در اولین اجرا برای بیشترین فضای فرم، حالت جمع‌شده پیش‌فرض است.
- Fix28 تغییر دیتابیسی ندارد و تمام اصلاحات Fix19 تا Fix27 را حفظ می‌کند.



## 0.3.22-prototype-fix29 - Windows Build Verifier Path Fix

- اصلاح مسیر‌یابی `tools/verify-cif-persisted-grids.mjs` در Windows با `fileURLToPath(import.meta.url)`.
- جلوگیری از ساخته‌شدن مسیر نادرست `D:\\D:\\...` هنگام اجرای `build-production.cmd`.
- تمامی Gridها و Dockable Sidebar نسخه Fix28 بدون تغییر حفظ شده‌اند.
- Migration جدیدی لازم نیست.


## 0.3.22-prototype-fix33 - مقایسه عنوان و Comment فارسی EA / Oracle

- مقایسه EA/Oracle علاوه بر ساختار فیزیکی، متادیتای فارسی را نیز کنترل می‌کند.
- در XMI خروجی EA، `alias` به‌عنوان عنوان فارسی جدول، `documentation` به‌عنوان توضیح جدول و `description` هر Attribute به‌عنوان شرح/Comment فارسی ستون خوانده می‌شود.
- در Oracle، `ALL_TAB_COMMENTS.COMMENTS` برای Comment جدول و `ALL_COL_COMMENTS.COMMENTS` برای Comment ستون خوانده می‌شود.
- اختلاف Comment ستون باعث `DIFFERENT` شدن همان ستون و جدول می‌شود؛ اختلاف عنوان/Documentation جدول نیز وضعیت جدول را `DIFFERENT` می‌کند و در «جزئیات اختلاف» نمایش داده می‌شود.
- Oracle فیلد Alias مستقل برای جدول ندارد؛ بنابراین Alias فارسی EA با متن Comment جدول Oracle از نظر وجود عنوان مقایسه می‌شود و Documentation EA نیز به‌صورت متن نرمال‌شده با Comment جدول Oracle تطبیق داده می‌شود.
- برای جلوگیری از اختلاف کاذب، تفاوت‌های `ي/ی`، `ك/ک`، فاصله/نیم‌فاصله، علائم جهت متن و نشانه‌گذاری هنگام مقایسه نرمال می‌شوند.
- در فایل نمونه EA، هر ۴۸ جدول یکتا Alias و Documentation دارند و هر ۸۱۶ ستون استخراج‌شده دارای Description است.
- Migration دیتابیس لازم نیست.

## 0.3.22-prototype-fix32 - اصلاح Length Semantics در مقایسه EA / Oracle

- اگر EA فقط `VARCHAR2(30)` را تعریف کرده و `LengthType` را مشخص نکرده باشد، Oracle `VARCHAR2(30 CHAR)` صرفاً به‌خاطر پسوند `CHAR` اختلاف محسوب یا به‌صورت اختلاف ظاهری نمایش داده نمی‌شود؛ هر دو در نمای مقایسه به `VARCHAR2(30)` نرمال می‌شوند.
- اگر EA صریحاً `LengthType=CHAR` یا `LengthType=BYTE` داشته باشد، Length Semantics همچنان دقیق مقایسه می‌شود و اختلاف واقعی `BYTE` در برابر `CHAR` گزارش می‌شود.
- در Tableهای تکراری XMI، Metadata ناقص ستون از تعریف‌های هم‌نام غنی‌تر تکمیل می‌شود؛ بنابراین `LengthType=CHAR` موجود در یکی از تعریف‌های EA دیگر از دست نمی‌رود.
- Migration دیتابیس لازم نیست.

## 0.3.22-prototype-fix31 - لینک جزئیات اختلاف EA / Oracle

در Grid نتیجه مقایسه EA/Oracle، برای هر جدول با وضعیت «دارای اختلاف» لینک «جزئیات اختلاف» نمایش داده می‌شود. انتخاب لینک همان جدول را فعال کرده و کاربر را مستقیماً به Grid اختلاف ستون‌های EA و Oracle هدایت می‌کند. جدول‌های همسان این لینک را نمایش نمی‌دهند. Backend و موتور مقایسه Fix30 بدون تغییر باقی مانده‌اند.

## 0.3.22-prototype-fix30 - مقایسه مدل EA با Oracle

در Fix30 یک فرم مدیریتی جدید در مسیر `مدیریت و سیستم > مقایسه مدل EA / Oracle` اضافه شده است. از FIX66 به بعد، Schema مقصد مستقیماً از Oracle Data Dictionary دریافت می‌شود و از FIX67 Schemaهای `ORACLE_MAINTAINED` سیستمی در فهرست UI نمایش داده نمی‌شوند؛ Configuration فقط برای Friendly Label و Default ترجیحی استفاده می‌شود.

گزارش شامل وجود/عدم وجود جدول، تعداد ستون‌های EA و Oracle، نوع داده، طول، Precision/Scale، Nullable، Primary Key، Foreign Key، Check Constraint، ستون‌های مفقود/اضافی/متفاوت و `COUNT(*)` دقیق رکوردهای جدول‌های مقایسه‌شده است. Checkهای سیستمی `NOT NULL` از مقایسه Check Constraint حذف می‌شوند چون همان معنا در Nullable به‌صورت مستقل کنترل می‌شود. بنا بر قرارداد سازگاری پروژه، اختلاف صرفاً `TIMESTAMP(0)` در EA در برابر `TIMESTAMP(6)` در Oracle به‌تنهایی اختلاف محسوب نمی‌شود؛ سایر اختلاف‌های Precision زمانی همچنان گزارش می‌شوند. تعریف‌های تکراری یک Table در Packageهای مختلف EA با نام جدول ادغام می‌شوند و هشدار مربوطه در گزارش نشان داده می‌شود. خروجی CSV سطح جدول نیز قابل دریافت است.

Parser XML در برابر DTD/External Entity غیرفعال و سخت‌سازی شده است. این قابلیت Schema دیتابیس جدیدی ایجاد نمی‌کند و Migration لازم ندارد؛ مقایسه در Runtime با همان DataSource Oracle برنامه انجام می‌شود.

## ماژول مستقل مدیریت کارمزد (FEE) — Baseline 1.0 / FIX77

از نسخه `0.3.67` صفحه `/fee` بر مبنای مدل Oracle و Seed Data Baseline 1.0 پیوست بازطراحی شده است. مدل جاری 47 جدول دارد و Seed پایه 574 رکورد را پوشش می‌دهد. در همین نسخه، یک دیاگرام تعاملی نیز به صفحه اصلی کارمزد اضافه شده است که ترتیب پیشنهادی تعریف اطلاعات را نمایش می‌دهد و هر گام را مستقیماً به فرم مربوط متصل می‌کند.

- UI اصلی: `/fee` — کاتالوگ 47 فرم در شش گروه کسب‌وکاری
- شبیه‌ساز قبلی: `/fee/simulator`
- API مدیریت Metadata-driven: `/api/v1/fees/admin`
- Schema: `FEE`
- DDL جاری: `database/oracle/fee/install-baseline-1.0-ddl.sql`
- Seed جاری: `database/oracle/fee/install-baseline-1.0-seed.sql`
- Verification: `database/oracle/fee/verify-baseline-1.0.sql`

فرم‌ها از Metadata واقعی Oracle ساخته می‌شوند؛ عنوان فارسی ستون‌ها از `COMMENT ON COLUMN`، Lookupهای FK از روابط Oracle و مقادیر کدی از `FEE_REF_DOMAIN/FEE_REF_VALUE` دریافت می‌شوند. 35 جدول تنظیمات/مرجع/Arrangement قابل نگهداری و 12 جدول Runtime/Audit در فرم عمومی فقط خواندنی هستند. شناسه‌های Seed منفی‌اند و درج‌های جدید از Sequenceهای مثبت همان جدول استفاده می‌کنند.

## استخراج Oracle به Enterprise Architect — FIX43

از نسخه `0.3.32-prototype-fee-p1` مسیر زیر در بخش مدیریت و سیستم در دسترس است:

`/system/oracle-ea-xmi-export`

این فرم از اتصال Oracle موجود در Backend استفاده می‌کند و Metadata فیزیکی Schema انتخاب‌شده را به XMI 1.1 / UML 1.3 سازگار با Enterprise Architect تبدیل می‌کند. جداول، ستون‌ها، PK/UK، روابط FK، Index، Check Constraint، Comment، Default، Owner و Tablespace پوشش داده می‌شوند. برای FKهای خارج از محدوده Export نیز امکان افزودن Reference Stub وجود دارد.


## تقویم یک / CAL — FIX46

از نسخه `0.3.35-prototype-fee-p1` دامنه مستقل «تقویم یک» در صفحه اطلاعات پایه اضافه شده است. این دامنه مستقیماً از Schema `CAL` استفاده می‌کند و ۱۶ جدول مدل Enterprise Calendar را در چهار گروه «ساختار و داده تقویم»، «تقویم کاری و بانکی»، «مناسبت‌ها و رویدادها» و «اصلاحات رسمی تقویم قمری» پوشش می‌دهد.

دو جدول `CALENDAR_DAY` و `CALENDAR_DATE` به دلیل ماهیت Dataset تقویم فقط‌خواندنی هستند. سایر جداول متناسب با Contract فیزیکی Oracle قابلیت ایجاد/ویرایش/حذف دارند. برای فیلدهای مبتنی بر `DAY_ID` Lookup جست‌وجویی سه‌تقویمی ارائه شده است تا کاربر به‌جای وارد کردن شناسه فنی، روز را با تاریخ میلادی/شمسی/قمری جست‌وجو کند.


## تقویم دو / CAL2 — FIX54

از نسخه `0.3.43-prototype-fee-p1` مدل دوم تقویم به‌صورت کاملاً مستقل در Schema `CAL2` اضافه شده است. این مدل از بسته `BIAN_Calendar_400Y_Oracle_Import` استخراج شده و با جدول قواعد مناسبت در FIX56 اکنون ۱۶ جدول دارد. بازه Dataset مبنا `1826-01-01` تا `2225-12-31`، شامل ۱۴۶٬۰۹۷ روز Canonical و ۴۳۸٬۲۹۱ نگاشت تاریخ برای سه Variant است.

منوی مستقل «تقویم دو» زیر «اطلاعات پایه» قرار دارد و فرم‌ها در شش گروه تعاریف تقویم، منبع/نسخه Dataset، Dataset تقویم، مناسبت‌ها، تقویم کاری و کنترل/ممیزی ارائه می‌شوند. `CANONICAL_DAY`، `CALENDAR_DATE`، `DATASET_VERSION`، `VALIDATION_RUN` و `VALIDATION_RESULT` در UI فقط‌خواندنی هستند؛ فرم‌های Event و Business Calendar قابل نگهداری‌اند.

برای نصب Schema جدید ابتدا به‌ترتیب `database/oracle/cal2/00-create-cal2-schema.sql` و `01-create-cal2-tables.sql` اجرا شود. اگر برنامه با User دیگری به Oracle متصل می‌شود، `02-grant-cal2-to-application-user.sql` نیز اجرا شود. پس از ایجاد ساختار، فایل ZIP اصلی ۱۵-CSV از مسیر `/calendar2/reference-data/import` با JDBC Batch و یک تراکنش Import می‌شود؛ SQL*Loader لازم نیست. این مدل هیچ FK یا وابستگی فیزیکی به Schema `CAL` ندارد.

### قواعد مناسبت تکرارشونده — FIX56

از نسخه `0.3.45-prototype-fee-p1` جدول `CAL2.EVENT_RECURRENCE_RULE` و عملیات Materialize مناسبت‌ها اضافه شده است. کاربر یک مناسبت را در `EVENT` و قاعده آن را یک‌بار در فرم «مناسبت‌های تقویم» ثبت می‌کند. قواعد `ANNUAL_FIXED_DATE` با ماه/روز و Calendar Variant مبنا برای تمام سال‌های موجود در `CALENDAR_DATE` رخداد تولید می‌کنند؛ قواعد `ONE_TIME_DATE` فقط یک سال مشخص را Materialize می‌کنند. ذخیره یا ویرایش قاعده در همان Transaction رخدادهای `GENERATED` آن قاعده را حذف و بازسازی می‌کند؛ رخدادهای `MANUAL` و `OFFICIAL` بازنویسی نمی‌شوند. از نسخه `0.3.46-prototype-fee-p1` Grid این فرم به نمایش کسب‌وکاری تغییر کرده و به‌جای شناسه‌های خام، عنوان مناسبت، نام تقویم، تاریخ خوانا، بازه و تعداد رخدادهای تولیدشده را نمایش می‌دهد.

از نسخه `0.3.47-prototype-fee-p1` فرم «رخدادهای مناسبت‌ها» نیز از نمایش مستقیم `EVENT_OCCURRENCE` خارج شده است. صفحه به‌صورت پیش‌فرض سال شمسی جاری را نشان می‌دهد و در هر ردیف سه تاریخ شمسی/میلادی/قمری، عنوان مناسبت، منشأ رخداد، وضعیت و منبع قابل مشاهده است. رخدادهای `GENERATED` فقط‌خواندنی هستند، رخدادهای `MANUAL` قابل ویرایش/حذف‌اند و رخدادهای `OFFICIAL` قابل ویرایش کنترل‌شده ولی غیرقابل حذف هستند.

از نسخه `0.3.48-prototype-fee-p1` فرم `CAL2.CANONICAL_DAY` با عنوان فارسی «روزهای مرجع تقویم» نمایش داده می‌شود. این صفحه به‌صورت پیش‌فرض سال شمسی جاری را از Default Persian Variant انتخاب می‌کند، فیلتر قرن و سال شمسی دارد و در Grid علاوه بر تاریخ مرجع/ISO، نام روز هفته و نام ماه شمسی را از جداول مرجع CAL2 نمایش می‌دهد. این تغییر فقط در لایه Query/UI است و نیاز به تغییر DDL یا Dataset ندارد.

برای Schemaهای CAL2 موجود، Migration زیر اجرا شود:

`database/oracle/cal2/migrations/0.3.45-fix56-event-recurrence-rule.sql`
