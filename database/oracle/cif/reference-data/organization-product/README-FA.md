# CIF Party Reference Data - Phase 4: Organization and Product

این فاز ۱۱ جدول مرجع جدید در Schema `CIF` ایجاد می‌کند و 44 رکورد Seed دارد.

دو جدول از مدل منبع عمداً در CIF تکرار نشده‌اند:

- `REF_CURRENCY` → استفاده از `GEO.CURRENCIES` (`CURRENCY_CODE` به کد الفبایی ISO نگاشت می‌شود).
- `REF_ORGANIZATIONAL_UNIT` → استفاده از `DPS.REF_ORG_UNIT_CODE` (`ORGANIZATIONAL_UNIT_CODE` به `CODE` نگاشت می‌شود).

ترتیب اجرا در SQL Developer با **Run Script / F5**:

```text
database\oracle\cif\reference-data\organization-product\install.sql
```

این فاز DDL جداول عملیاتی `CIF.ORGANIZATION`, `PARTY_EMPLOYMENT`, `PARTY_LICENSE`, `PARTY_PRODUCT_HOLDING` و ... را تغییر نمی‌دهد؛ فقط Reference Data مورد نیاز آن‌ها را تکمیل می‌کند.
