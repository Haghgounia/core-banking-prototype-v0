# Core Banking Prototype

این پروژه یک Prototype بانکداری متمرکز با ساختار ماژولار و استقرار یکپارچه است.

## وضعیت این نسخه

در وضعیت فعلی سه دامنه اصلی فعال هستند:

```text
reference-data                    -> Schema GEO
deposit-product reference-data    -> Schema DPS
customer-information-file (CIF)   -> Schema CIF
```

در این نسخه ۱۷۳ فرم اطلاعات پایه فعال هستند: ۲۰ فرم عمومی/GEO، ۵۰ فرم `DPS.REF_*` و ۱۰۳ فرم اطلاعات پایه Party/Customer در CIF. علاوه بر آن، ماژول «مدیریت مشتری / CIF» با فهرست Party، Workflow کامل شخص حقیقی/حقوقی و نمای نهایی Party / Customer 360 فعال است.

برای جداول عملیاتی `DEPOSIT_PRODUCT*` هنوز Package، API یا صفحه‌ای ایجاد نشده است. مسیر عملیاتی CIF از ایجاد Party تا اطلاعات Person/Organization، تماس و نشانی، مالی، شناسه و مدرک، طبقه‌بندی، روابط/UBO، Role/Customer، KYC/Risk/Screening، Consent/Preference، Lifecycle و Merge تکمیل شده است. در نسخه 0.3.22 تمام ۴۸ جدول عملیاتی موجود در `CIF-tables5.xlsx` در Backend پوشش داده می‌شوند: ۳۰ جدول در Workflowهای CIF استفاده/نگهداری می‌شوند و ۱۸ جدول تکمیلی بدون CRUD در CIF به‌صورت Read-only در Party / Customer 360 تجمیع می‌شوند؛ محصولات/تعاملات/شکایات و مشابه آن از سامانه‌های مبدأ می‌آیند و Registration/Audit صرفاً Trace خواندنی هستند.

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
```

- `GEO`: مالک فیزیکی فعلی جداول اطلاعات پایه. جداسازی منطقی ماژول در کد انجام شده است، اما جداول فعلاً در همین Schema باقی می‌مانند.
- `DPS`: مالک جداول مرجع محصول‌ساز سپرده و اسکریپت‌های Oracle مربوط به آن‌ها.
- `CIF`: مالک جداول مدیریت Party، Person/Organization، KYC، نشانی، تماس، ریسک و غربالگری.
- `party-reference`: مالک منطقی Reference Data جدید Party/Customer و در این فاز برابر Schema `CIF` است.

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
└── cif/
    ├── ddl/
    └── reference-data/
        ├── identity-party/
        └── compliance-risk/
```

DDL و Comment و Constraintهای دریافت‌شده از Oracle بدون بازطراحی در `database/oracle/dps/ddl` نگهداری می‌شوند. فایل افزایشی `08_add-created-by-to-reference-tables.sql` تغییر اعلام‌شده برای ستون `CREATED_BY` را ثبت می‌کند.

## قابلیت‌های ماژول اطلاعات پایه

- ۱۶۸ فرم فعال اطلاعات پایه؛ شامل ۲۰ فرم عمومی/GEO، ۵۰ فرم مرجع محصول سپرده در DPS و ۹۸ فرم Party/Customer در CIF
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

منوی اصلی فقط یک گزینه «اطلاعات پایه» دارد. این گزینه به صفحه انتخاب دامنه هدایت می‌شود و سه دامنه مستقل «اطلاعات پایه عمومی»، «اطلاعات پایه مشتری / Party» و «اطلاعات پایه محصول سپرده» را نمایش می‌دهد. «درخت جغرافیایی» نیز به بخش «اطلاعات پایه عمومی / اطلاعات جغرافیایی» منتقل شده است و دیگر گزینه سطح اول Sidebar نیست.


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
