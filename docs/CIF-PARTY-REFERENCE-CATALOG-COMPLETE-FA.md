# وضعیت نهایی Reference Data دامنه Party / Customer

مدل منبع شامل 104 تعریف Reference Data است. پس از Phase 6 تمام آن‌ها تعیین تکلیف شده‌اند:

- 98 جدول/فرم Reference Data فعال در Schema `CIF` (96 مورد از مدل مرجع اصلی + `REF_TENURE_TYPE` برای وضعیت تصرف نشانی + `REF_ADDRESS_SOURCE` برای منبع نشانی)
- 6 مورد استفاده مجدد از `GEO`
- 2 مورد استفاده مجدد از `DPS`
- 0 مورد Deferred

موارد DPS عبارت‌اند از `REF_ORGANIZATIONAL_UNIT` و `REF_CUSTOMER_SEGMENT`.

اصل معماری حفظ شده است: داده‌ای که قبلاً Source of Truth معتبر در GEO یا DPS دارد، در CIF دوباره ساخته نشده است.
