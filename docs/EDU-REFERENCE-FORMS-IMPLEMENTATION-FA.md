# پیاده‌سازی فرم‌های مدل جدید EDU

در این تغییر، فرم‌های قدیمی آموزش و تحصیلات در Schema قبلی دست‌نخورده باقی مانده‌اند و یک دسته‌بندی مستقل برای مدل جدید `EDU` به منوی «اطلاعات پایه عمومی» اضافه شده است.

## دسته‌بندی جدید منو

**اطلاعات پایه تحصیلات - مدل جدید EDU**

فرم‌های جدید:

1. `EDU.EDUCATION_LEVELS` — مقاطع و مدارک تحصیلی
2. `EDU.EDUCATION_FIELDS` — رشته‌ها و گرایش‌های تحصیلی
3. `EDU.EDUCATION_FIELD_LEVELS` — مقاطع معتبر هر رشته
4. `EDU.EDUCATION_INSTITUTIONS` — دانشگاه‌ها و مؤسسات آموزشی
5. `EDU.EDUCATION_SOURCES` — منابع داده آموزشی
6. `EDU.EDUCATION_SOURCE_MAPPINGS` — نگاشت منابع به داده Canonical

## مدل قبلی

دسته «آموزش و تحصیلات - مدل قبلی» همچنان فرم‌های زیر را نمایش می‌دهد:

- EDUCATION_GROUPS
- EDUCATION_SUBGROUPS
- EDUCATION_DEGREES
- EDUCATION_FIELDS
- EDUCATION_UNIVERSITIES

## فایل‌های تغییرکرده

- `backend/src/main/java/com/behsazan/corebanking/referencedata/education/descriptor/EduCanonicalDescriptorProvider.java`
- `frontend/src/app/features/reference-menu/reference-menu.component.ts`

فرم‌ها از موتور عمومی Reference Data موجود استفاده می‌کنند و CRUD، Grid، جست‌وجو، Lookup و Selectهای کنترل‌شده را در همان UI استاندارد پروژه دریافت می‌کنند.
