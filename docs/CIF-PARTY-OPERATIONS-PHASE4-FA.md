# فاز ۴ فرم‌های عملیاتی Party — شناسه‌های تکمیلی و مدارک

نسخه: `0.3.15-prototype`

## هدف

این فاز ادامه مستقیم فرایند ایجاد Party است و پس از اطلاعات مالی/شغلی اجرا می‌شود. تمرکز آن بر دو موجودیت `CIF.PARTY_IDENTIFIER` و `CIF.PARTY_DOCUMENT` است.

## قواعد اصلی

- شناسه اصلی در Phase 1 ایجاد می‌شود و در این فرم فقط نمایش داده می‌شود.
- شناسه‌های ثبت‌شده در Phase 4 همواره `IS_PRIMARY='N'` هستند.
- ترکیب `IDENTIFIER_TYPE_CODE + IDENTIFIER_VALUE + ISSUER_CODE + VALID_FROM` مطابق Constraint مدل تکراری نیست.
- تاریخ انقضا نمی‌تواند قبل از تاریخ صدور یا شروع اعتبار باشد.
- مدارک فقط Metadata و `STORAGE_REF` فایل امن را نگهداری می‌کنند؛ Binary فایل در `PARTY_DOCUMENT` ذخیره نمی‌شود.
- اتصال مدرک به `KYC_CASE` اختیاری است و فقط در صورت وجود پرونده متعلق به همان Party پذیرفته می‌شود.
- برای `CONTROL_STATUS_CODE` منبع Reference صریح در مدل تحویلی وجود ندارد؛ بنابراین Reference مصنوعی ایجاد نشده است.

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
POST   /api/v1/cif/parties/{partyId}/documents
PUT    /api/v1/cif/parties/{partyId}/documents/{id}
DELETE /api/v1/cif/parties/{partyId}/documents/{id}
```

## فاز بعدی

Phase 5: طبقه‌بندی Party و مدیریت `PARTY_CLASSIFICATION`.
