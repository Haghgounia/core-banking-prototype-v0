# فاز اول مدیریت مشتری / CIF

مبنای این فاز فایل واقعی Oracle با نام `CIF-050517.sql` است. جداول در محیط Oracle از قبل ایجاد شده‌اند و پروژه صرفاً API و UI را بر اساس همان ساختار فعال می‌کند.

## دامنه فعال

فاز اول روی این جداول متمرکز است:

```text
CIF.PARTY
CIF.PERSON
CIF.ORGANIZATION
CIF.PARTY_NAME
CIF.PARTY_IDENTIFIER
CIF.ADDRESS
CIF.PARTY_ADDRESS
CIF.CONTACT_POINT
CIF.KYC_CASE
CIF.PARTY_DOCUMENT
CIF.PARTY_RISK_ASSESSMENT
CIF.SCREENING_RESULT
```

جدول `PARTY` موجودیت محوری است. صفحه اصلی CIF فهرست Partyها را نمایش می‌دهد و صفحه Customer 360 اطلاعات وابسته را در Tabهای زیر تجمیع می‌کند:

1. مشخصات پایه Party و Person/Organization
2. نام‌ها و شناسه‌های هویتی
3. نشانی و راه‌های تماس
4. KYC/KYB و مدارک
5. ارزیابی ریسک و غربالگری

## مسیرهای UI

```text
/cif/parties
/cif/parties/{partyId}
```

## API اصلی

```text
GET    /api/v1/cif/parties
POST   /api/v1/cif/parties
GET    /api/v1/cif/parties/{partyId}
PUT    /api/v1/cif/parties/{partyId}
PUT    /api/v1/cif/parties/{partyId}/person
PUT    /api/v1/cif/parties/{partyId}/organization
```

برای جداول فرزند، مسیرهای `names`, `identifiers`, `addresses`, `contacts`, `kyc-cases`, `documents`, `risk-assessments` و `screenings` عملیات ایجاد/ویرایش/حذف را ارائه می‌کنند.

## قواعد مهم پیاده‌سازی

- `PARTY_TYPE_CODE` هنگام ایجاد فقط `PERSON` یا `ORGANIZATION` است و پس از ایجاد از UI تغییر نمی‌کند.
- `RECORD_VERSION` برای ویرایش Optimistic Lock استفاده می‌شود.
- فیلدهای Audit مانند `CREATED_AT`, `CREATED_BY`, `UPDATED_AT`, `UPDATED_BY` در Backend مقداردهی می‌شوند و در فرم ویرایش کاربر قرار نمی‌گیرند.
- برای نام، شناسه، نشانی و راه تماس، در صورت انتخاب رکورد اصلی (`IS_PRIMARY='Y'`) رکورد اصلی قبلی همان Party به `N` تغییر می‌کند.
- قواعد تاریخی و Flagهای Y/N قبل از ارسال SQL اعتبارسنجی می‌شوند و Constraintهای Oracle نیز لایه نهایی کنترل هستند.
- `PARTY_DOCUMENT` در این فاز فایل را داخل DB بارگذاری نمی‌کند؛ فرم، `STORAGE_REF`, `CONTENT_HASH` و `MIME_TYPE` را مطابق DDL ثبت می‌کند.
- جداول دیگر موجود در DDL مانند Beneficial Ownership، Complaint، Consent، Group، Product Holding و Audit Event در فازهای بعدی به Customer 360 افزوده می‌شوند.
