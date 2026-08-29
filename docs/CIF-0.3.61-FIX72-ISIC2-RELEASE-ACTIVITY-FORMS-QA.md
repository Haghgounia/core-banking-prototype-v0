# CIF 0.3.61 / FIX72 — ISIC2 Release-aware Reference Forms QA

## Scope

پیاده‌سازی مدل نسخه‌محور ISIC در کنار مدل Legacy بدون تغییر `CIF.REF_ISIC_ACTIVITY` موجود.

## Database objects

- `CIF.REF_ISIC_RELEASE`
- `CIF.REF_ISIC_ACTIVITY2`
- `CIF.V_REF_ISIC_ACTIVITY_LOOKUP2`

Installer: `database/oracle/cif/isic2/00-install-isic2.sql`

## Seed contract

Seed رسمی UNSD Rev.4 از فایل طراحی پیوست استخراج و برای جدول جدید `REF_ISIC_ACTIVITY2` تطبیق داده شده است:

- SECTION: 21
- DIVISION: 88
- GROUP: 238
- CLASS: 419
- TOTAL: 766
- Selectable leaf classes: 419
- Persian labels with `BANK_VERIFIED`: 4

Release `IR-SCI / Rev.4` فقط Registry می‌شود و `DRAFT`, `IS_ACTIVE=0`, `IS_CURRENT=0` باقی می‌ماند.

## UI

مسیر منو: `اطلاعات پایه > اطلاعات پایه مشتری / Party > طبقه‌بندی فعالیت اقتصادی ISIC`

فرم‌ها:

1. `/cif/reference-data/isic-releases` — CRUD روی `REF_ISIC_RELEASE`
2. `/cif/reference-data/isic-activities` — CRUD و ساختار سلسله‌مراتبی روی `REF_ISIC_ACTIVITY2`

فرم Activity شامل Release selector، Tree lazy، Grid، جستجو، فیلتر Level/Active/Selectable، Parent lookup در همان Release، عناوین فارسی/انگلیسی، CLOBهای Description/Inclusions/Exclusions، Translation Status و Persian Date Picker است.

## Backend API

Base: `/api/v1/cif/isic`

- `GET/POST /releases`
- `GET/PUT/DELETE /releases/{id}`
- `GET /releases/lookup`
- `GET/POST /activities`
- `GET/PUT/DELETE /activities/{id}`
- `GET /activities/lookup`

Validationهای Level/Code/Parent/Section/Selectable/Translation/Validity dates در Service تکرار شده‌اند و Oracle Constraint آخرین خط دفاع است. Update با `RECORD_VERSION` کنترل هم‌زمانی خوش‌بینانه دارد.

## Legacy isolation

در این Release عمداً موارد زیر تغییر نکرده‌اند:

- `CIF.REF_ISIC_ACTIVITY`
- Validation جاری `ORGANIZATION.ISIC_CODE`
- Validation جاری `PARTY_EMPLOYMENT.ISIC_CODE`
- Generic reference resource `ref-isic-activity`

بنابراین نصب ISIC2 هیچ Migration عملیاتی Party ایجاد نمی‌کند.

## Static verification

```text
node tools/verify-cif-isic2.mjs
CIF ISIC2 verification OK: 766 official UNSD rows, dedicated Release/Activity2 forms, legacy table untouched.
```

Java model records با JDK 21 کامپایل شدند. Service/Controller و Repository نیز با Stubهای امضای Spring/JdbcClient برای کنترل Syntax/Type کامپایل شدند.

Full Maven/Angular production build در محیط بسته قابل اجرا نبود: Maven Wrapper امکان دریافت Maven 3.9.16 از Maven Central را نداشت و `frontend/node_modules` در Workspace موجود نبود. Build نهایی باید با `build-production.cmd` در محیط پروژه اجرا شود.
