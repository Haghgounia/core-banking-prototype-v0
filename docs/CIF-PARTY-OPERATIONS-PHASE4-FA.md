# فاز ۴ فرم‌های عملیاتی Party — شناسه‌های تکمیلی و مدارک

نسخه پایه: `0.3.15-prototype` — تکمیل UX/فایل در `0.3.22-prototype-fix15`

## هدف

این فاز ادامه مستقیم فرایند ایجاد Party است و پس از اطلاعات مالی/شغلی اجرا می‌شود. تمرکز آن بر دو موجودیت `CIF.PARTY_IDENTIFIER` و `CIF.PARTY_DOCUMENT` است.

## قواعد اصلی

- شناسه اصلی در Phase 1 ایجاد می‌شود و در این فرم فقط نمایش داده می‌شود.
- شناسه‌های ثبت‌شده در Phase 4 همواره `IS_PRIMARY='N'` هستند.
- ترکیب `IDENTIFIER_TYPE_CODE + IDENTIFIER_VALUE + ISSUER_CODE + VALID_FROM` مطابق Constraint مدل تکراری نیست.
- تاریخ انقضا نمی‌تواند قبل از تاریخ صدور یا شروع اعتبار باشد.
- مدارک فقط Metadata، `CONTENT_HASH` و `STORAGE_REF` را در `PARTY_DOCUMENT` نگهداری می‌کنند؛ Binary فایل در جدول Oracle ذخیره نمی‌شود. در Fix15 خود فایل از طریق سرویس فایل خصوصی Backend نگهداری می‌شود.
- اتصال مدرک به `KYC_CASE` اختیاری است و فقط در صورت وجود پرونده متعلق به همان Party پذیرفته می‌شود.
- در Fix15 برای `CONTROL_STATUS_CODE` از Reference مشترک موجود `CIF.REF_WORKFLOW_STATUS` استفاده می‌شود؛ Reference مصنوعی جدید ایجاد نشده است.

## همگام‌سازی Schema

آخرین `CIF-tables3.xlsx` سه ستون زیر را برای `PARTY_DOCUMENT` دارد که در نسخه قبل Application پوشش نمی‌داد:

- `ISSUING_AUTHORITY_TEXT`
- `CONTROL_STATUS_CODE`
- `DESCRIPTION_TEXT`

این سه ستون در Domain Model، API Contract، Repository Oracle، UI و DDL snapshot پوشش داده شده‌اند. برای محیط‌های موجود Migration افزایشی `database/oracle/cif/migrations/0.3.15-party-document-alignment.sql` ارائه شده است.

## مسیر UI

```text
/cif/parties/{partyId}/onboarding/identifiers-documents
```

## APIهای استفاده‌شده

```text
POST   /api/v1/cif/parties/{partyId}/identifiers
PUT    /api/v1/cif/parties/{partyId}/identifiers/{id}
DELETE /api/v1/cif/parties/{partyId}/identifiers/{id}
POST   /api/v1/cif/parties/{partyId}/document-files       # بارگذاری فایل و تولید SHA-256 / STORAGE_REF
GET    /api/v1/cif/parties/{partyId}/documents/{id}/file  # فراخوانی فایل
POST   /api/v1/cif/parties/{partyId}/documents
PUT    /api/v1/cif/parties/{partyId}/documents/{id}
DELETE /api/v1/cif/parties/{partyId}/documents/{id}
```

## فاز بعدی

Phase 5: طبقه‌بندی Party و مدیریت `PARTY_CLASSIFICATION`.

## تکمیل Fix15 — فایل مدرک و رفتار پس از ذخیره

- `CONTENT_HASH` و `STORAGE_REF` دیگر ورودی کاربر نیستند و در UI فرم نمایش داده نمی‌شوند.
- کاربر فایل PDF/JPEG/PNG/TIFF یا فایل خروجی اسکنر را انتخاب می‌کند؛ Backend هش SHA-256 و مرجع opaque با Prefix `cif-doc:` را تولید می‌کند.
- حداکثر اندازه فایل ۲۰ مگابایت است و فایل زیر Static Resources قرار نمی‌گیرد.
- اتصال مستقیم Scanner از Browser عمداً شبیه‌سازی نشده است؛ برای TWAIN/WIA/SANE در محیط شعبه به Scanner Agent/Middleware مورد تأیید بانک نیاز است.
- پس از ثبت موفق شناسه تکمیلی یا مدرک، `FormGroupDirective` Reset و Focus فعال Blur می‌شود تا فیلد ثبت‌شده به‌اشتباه در وضعیت Error/Required باقی نماند.
- در Production، File Repository نمونه باید با DMS/Object Storage مورد تأیید بانک، رمزنگاری، Malware Scan، Retention/Legal Hold و Audit یکپارچه شود.
