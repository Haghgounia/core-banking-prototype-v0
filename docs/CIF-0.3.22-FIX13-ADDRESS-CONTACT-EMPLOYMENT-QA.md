# CIF 0.3.22 Fix 13 QA

مرجع فیزیکی جاری: `CIF-tables5.xlsx`.

## اصلاح رفتار فرم‌ها

- شهر نشانی دیگر به بخش/منطقه وابسته نیست؛ استان -> شهرستان شهرهای همه بخش‌های شهرستان را بارگذاری می‌کند.
- انتخاب شهر قابل جست‌وجو است و `CITY_CODE` در `ADDRESS.CITY_CODE` ذخیره می‌شود؛ `DISTRICT_CODE` اختیاری باقی می‌ماند.
- پس از ثبت موفق نشانی، راه تماس و سابقه شغلی، وضعیت `submitted` فرم reset می‌شود تا فیلدهای خالی فرم جدید به اشتباه قرمز نمایش داده نشوند.
- `EMPLOYER_PARTY_ID` به‌صورت شناسه عددی نمایش داده نمی‌شود؛ کارفرمای موجود در CIF با جست‌وجوی Party انتخاب و شناسه پشت‌صحنه ذخیره می‌شود.
- برای کارفرمای خارج از CIF، نام و شناسه خارجی قابل ثبت است.
- گروه شغلی از `GEO.JOB_GROUPS` و شغل از `GEO.JOBS` با فیلتر گروه انتخاب می‌شوند.
- فعالیت اقتصادی و ارز درآمد با عنوان فارسی نمایش داده می‌شوند و کد مرجع ذخیره می‌شود.
- درآمد ماهانه با جداکننده هزارگان نمایش داده می‌شود.

## Reference جدید

`CIF.REF_CONTRACT_TYPE` طبق Vocabulary فرم عملیاتی:

- `PERMANENT` -> دائم
- `TEMPORARY` -> موقت
- `CONTRACTOR` -> پیمانکاری
- `SELF_EMPLOYED` -> خویش‌فرما

Migration: `database/oracle/cif/migrations/0.3.22-fix13-employment-reference-ui.sql`

Catalog پس از Fix13: 99 Reference در CIF / 169 Reference Form در کل سامانه.

## کنترل ساختاری

- Relative TypeScript imports: missing = 0
- Template/style references: missing = 0
- فرم‌های تغییرکرده: توازن `<form>` و `mat-form-field` تأیید شد.
- TypeScript parser diagnostics: 0 (خطاهای module-resolution ناشی از نبود `node_modules` در محیط QA نادیده گرفته شده‌اند).
- Java parser diagnostics: 0 (وابستگی‌های پروژه در classpath محیط QA موجود نیستند).
