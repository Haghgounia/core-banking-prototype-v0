# Core Banking Prototype

این پروژه یک Prototype بانکداری متمرکز با ساختار ماژولار و استقرار یکپارچه است.

## وضعیت این نسخه

در وضعیت فعلی سه دامنه اصلی فعال هستند:

```text
reference-data                    -> Schema GEO
deposit-product reference-data    -> Schema DPS
customer-information-file (CIF)   -> Schema CIF
```

در این نسخه ۱۲۳ فرم اطلاعات پایه فعال هستند: ۲۰ فرم GEO، ۵۰ فرم `DPS.REF_*` و ۵۳ فرم اطلاعات پایه Party/Customer در CIF. علاوه بر آن، ماژول «مدیریت مشتری / CIF» با فهرست Party و صفحه Customer 360 فعال است.

برای جداول عملیاتی `DEPOSIT_PRODUCT*` هنوز Package، API یا صفحه‌ای ایجاد نشده است. در دامنه CIF، CRUD فاز اول برای ۱۲ جدول اصلی فعال است؛ فاز ۲ جدول ارتباطی `CONTACT_POINT_ADDRESS` را وارد جریان عملیاتی کرد، فاز ۳ نیز `FINANCIAL_PROFILE`، `PARTY_EMPLOYMENT`، `PARTY_INCOME_SOURCE`، `PARTY_ASSET_LIABILITY` و `PARTY_LICENSE` را فعال کرد و فاز ۴ جریان مستقل شناسه‌های تکمیلی و مدارک را روی `PARTY_IDENTIFIER` و `PARTY_DOCUMENT` تکمیل کرده است. پوشش عملیاتی همچنان ۱۸ جدول CIF است، اما دو جدول هویتی/مدرکی اکنون Workflow اختصاصی دارند.

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

- ۱۲۳ فرم فعال اطلاعات پایه؛ شامل ۲۰ فرم GEO، ۵۰ فرم مرجع محصول سپرده در DPS و ۵۳ فرم Party/Customer در CIF
- فهرست Party و پرونده جامع Customer 360 در ماژول CIF
- CRUD عملیاتی CIF برای ۱۸ جدول: ۱۲ جدول پایه، `CONTACT_POINT_ADDRESS` در فاز ۲ و پنج جدول مالی/شغلی/مجوز در فاز ۳؛ فاز ۴ Workflow اختصاصی `PARTY_IDENTIFIER` و `PARTY_DOCUMENT` را تکمیل می‌کند
- Runtime عمومی Descriptor-driven
- جست‌وجو، مرتب‌سازی و صفحه‌بندی سمت سرور
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

فاز اول CIF بر اساس DDL واقعی `database/oracle/cif/ddl/CIF-050517.sql` پیاده‌سازی شده است. از نسخه 0.3.12 به بعد، مدل اجرایی Party با فایل مدل به‌روز `CIF-tables3.xlsx` همگام شده و Snapshot قدیمی DDL داخل Repository عمداً به‌عنوان مرجع تاریخی نگهداری شده است. راهنمای فاز اول در `docs/CIF-CUSTOMER-360-PHASE1-FA.md` و راهنمای فرم عملیاتی فاز ۲ در `docs/CIF-PARTY-OPERATIONS-PHASE2-FA.md` قرار دارد.


## ساختار ناوبری اطلاعات پایه از نسخه 0.3.11

منوی اصلی فقط یک گزینه «اطلاعات پایه» دارد. این گزینه به صفحه انتخاب دامنه هدایت می‌شود و سه دامنه مستقل «اطلاعات پایه عمومی»، «اطلاعات پایه مشتری / Party» و «اطلاعات پایه محصول سپرده» را نمایش می‌دهد. «درخت جغرافیایی» نیز به بخش «اطلاعات پایه عمومی / اطلاعات جغرافیایی» منتقل شده است و دیگر گزینه سطح اول Sidebar نیست.
