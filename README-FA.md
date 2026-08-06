# Core Banking Prototype

این پروژه یک Prototype بانکداری متمرکز با ساختار ماژولار و استقرار یکپارچه است.

## وضعیت این نسخه

در وضعیت فعلی دو بخش اطلاعات پایه فعال هستند:

```text
reference-data                    -> Schema GEO
deposit-product reference-data    -> Schema DPS
```

مجموعاً ۷۰ فرم فعال وجود دارد: ۲۰ فرم عمومی و جغرافیایی قبلی و ۵۰ فرم `DPS.REF_*` زیر مجموعه «اطلاعات پایه محصول سپرده».

در این مرحله فقط جداول مرجع محصول‌ساز سپرده فعال شده‌اند. برای جداول عملیاتی `DEPOSIT_PRODUCT*` هنوز Package، API یا صفحه‌ای ایجاد نشده است.

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
└── deposit
    └── productfactory
        └── reference
```

Package `deposit.productfactory.reference` فقط Metadata موردنیاز ۵۰ فرم مرجع DPS را نگهداری می‌کند؛ هیچ مدل عملیاتی محصول سپرده در آن ایجاد نشده است.

## Schemaهای Oracle

```yaml
core-banking:
  schemas:
    reference-data: GEO
    deposit-product-factory: DPS
```

- `GEO`: مالک فیزیکی فعلی جداول اطلاعات پایه. جداسازی منطقی ماژول در کد انجام شده است، اما جداول فعلاً در همین Schema باقی می‌مانند.
- `DPS`: مالک جداول مرجع محصول‌ساز سپرده و اسکریپت‌های Oracle مربوط به آن‌ها.

## ساختار اسکریپت‌های پایگاه داده

```text
database/oracle/
├── geo/
│   ├── ddl/
│   ├── data/
│   ├── install-ddl.sql
│   └── install-data.sql
└── dps/
    ├── ddl/
    └── data/
```

DDL و Comment و Constraintهای دریافت‌شده از Oracle بدون بازطراحی در `database/oracle/dps/ddl` نگهداری می‌شوند. فایل افزایشی `08_add-created-by-to-reference-tables.sql` تغییر اعلام‌شده برای ستون `CREATED_BY` را ثبت می‌کند.

## قابلیت‌های ماژول اطلاعات پایه

- ۷۰ فرم فعال؛ شامل ۵۰ فرم مرجع محصول سپرده در DPS
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
