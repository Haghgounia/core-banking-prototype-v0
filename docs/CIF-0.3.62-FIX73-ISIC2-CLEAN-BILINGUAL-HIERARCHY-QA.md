# QA — CIF 0.3.62 / FIX73 — ISIC2 Clean Bilingual Hierarchy

## هدف

بازطراحی مستقل ISIC بر پایه Release + Activity Tree، حذف ستون‌های ساختاری تکراری، اجباری کردن عنوان فارسی/انگلیسی و تکمیل Seed فارسی برای UNSD ISIC Rev.4.

## مدل Oracle

### `CIF.REF_ISIC_RELEASE`

- PK: `ISIC_RELEASE_ID`
- Natural key: `CLASSIFICATION_CODE + REVISION_CODE + VARIANT_CODE + COUNTRY_CODE`
- `NAME_FA` و `NAME_EN`: اجباری
- Dataset status: `DRAFT/PARTIAL/COMPLETE/RETIRED`
- Current/Active و بازه اعتبار نگهداری می‌شود.

### `CIF.REF_ISIC_ACTIVITY2`

- PK: `ISIC_ACTIVITY_ID`
- FK به Release: `ISIC_RELEASE_ID`
- Self hierarchy: `PARENT_ACTIVITY_ID`
- Unique business code: `(ISIC_RELEASE_ID, ISIC_CODE)`
- Same-release parent FK: `(ISIC_RELEASE_ID, PARENT_ACTIVITY_ID)` -> `(ISIC_RELEASE_ID, ISIC_ACTIVITY_ID)`
- `LEVEL_CODE` + `LEVEL_NO` با نگاشت ثابت 1 تا 5
- `NAME_FA NOT NULL`, `NAME_EN NOT NULL`
- CLOBهای Description/Inclusions/Exclusions اختیاری‌اند و بدون منبع معتبر Seed نشده‌اند.

## Seed UNSD Rev.4

نتیجه بررسی فایل `isic_rev4_unsd_bilingual.csv`:

| سطح | تعداد |
|---|---:|
| SECTION | 21 |
| DIVISION | 88 |
| GROUP | 238 |
| CLASS | 419 |
| **جمع** | **766** |

کنترل‌های Seed:

- `NAME_FA`: 766/766 پر
- `NAME_EN`: 766/766 پر
- `TRANSLATION_STATUS_CODE=BANK_TRANSLATED`: 766/766
- `IS_SELECTABLE=1`: فقط 419 Class
- Duplicate `ISIC_CODE` در یک Release: صفر
- Parent مفقود یا Parent پس از Child در ترتیب Import: صفر
- متن لاتین سه‌حرفی یا بیشتر در `NAME_FA`: صفر

عنوان انگلیسی از ساختار مرجع ورودی Rev.4 گرفته شده است. عنوان فارسی ترجمه پروژه بانکی است و با وضعیت `BANK_TRANSLATED` از ترجمه رسمی تفکیک می‌شود.

## فرم‌ها

### نسخه‌های ISIC

- CRUD مستقل روی `REF_ISIC_RELEASE`
- نام فارسی و انگلیسی اجباری
- Revision/Variant/Country/Authority/Status/Current/Active/Validity

### فعالیت‌های ISIC

- Release selector در سطح صفحه
- Tree سلسله‌مراتبی با Parent ID
- Search روی کد، عنوان فارسی و انگلیسی
- Filter سطح/فعال/قابل انتخاب
- Detail Editor شامل Level/Level No/Parent/Code/دو عنوان/وضعیت ترجمه/شرح‌ها/اعتبار
- انتخاب Parent فقط از سطوح مجاز همان Release
- عنوان فارسی اجباری است.

## اسکریپت‌ها

- `00-reset-isic2-redesign.sql`
- `01-create-isic2-tables.sql`
- `02-import-isic-rev4-unsd.sql`
- `03-register-ir-sci-release.sql`
- `04-verify-isic2.sql`
- `00-install-isic2.sql` برای SQL*Plus/SQLcl با include
- `CIF_ISIC2_FULL_INSTALL.sql` نسخه Standalone

**هشدار:** Reset/Full Install دو جدول ISIC2 را Drop/Recreate می‌کند و برای Prototype در نظر گرفته شده است.

## Validation اجراشده در محیط ساخت

- `tools/verify-cif-isic2.mjs`: PASS
- TypeScript syntax preflight برای فایل‌های ISIC با TypeScript 5.8.3: PASS
- CSV structural validation: PASS
- Maven compile: اجرا نشد؛ Maven Wrapper در محیط ساخت برای دریافت Maven 3.9.16 به Maven Central نیاز داشت و شبکه در دسترس نبود.
- Angular production build: اجرا نشد؛ `frontend/node_modules` در بسته Source محیط ساخت موجود نبود. Build نهایی روی سیستم مقصد با `build-production.cmd` انجام شود.
