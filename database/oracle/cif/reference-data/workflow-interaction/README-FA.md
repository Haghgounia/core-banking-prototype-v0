# CIF Party Reference Data - Phase 5: Workflow and Interaction

این فاز **17** جدول مرجع جدید در Schema `CIF` ایجاد می‌کند و **71** رکورد Seed دارد.

سه زنجیره مهم این فاز عبارت‌اند از:

- `REF_JOURNEY` → `REF_JOURNEY_STAGE` → `REF_JOURNEY_EVENT_TYPE`
- `REF_COMPLAINT_TYPE / REF_COMPLAINT_STATUS / REF_COMPLAINT_STATUS_REASON / REF_COMPLAINT_RESOLUTION` برای شکایت مشتری
- `REF_WORKFLOW_STATUS` برای وضعیت‌های عملیاتی مشترک؛ از جمله `KYC_CASE.STATUS_CODE`

ترتیب اجرا در SQL Developer با **Run Script / F5**:

```text
database\oracle\cif\reference-data\workflow-interaction\install.sql
```

این فاز DDL جداول عملیاتی `AUDIT_EVENT`, `PARTY_COMPLAINT`, `PARTY_INTERACTION`, `PARTY_JOURNEY_EVENT` و `PARTY_GENERAL_PREFERENCE` را تغییر نمی‌دهد؛ فقط Reference Data مورد نیاز آن‌ها را اضافه می‌کند.
