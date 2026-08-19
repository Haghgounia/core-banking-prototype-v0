# CIF Party Reference Data - Phase 6: Analytics and Recommendation

این فاز آخرین حوزه Reference Data مدل Party/Customer است.

- **7** جدول جدید در Schema `CIF` ایجاد می‌شود.
- **24** رکورد Seed برای این 7 جدول درج/به‌روزرسانی می‌شود.
- `REF_CUSTOMER_SEGMENT` عمداً در CIF ایجاد نمی‌شود؛ منبع موجود `DPS.REF_CUSTOMER_SEGMENT_CODE` حفظ می‌شود.

## جداول جدید

- `REF_METRIC` — شاخص تحلیلی مشتری
- `REF_METRIC_UNIT` — واحد اندازه‌گیری شاخص
- `REF_MODEL` — مدل تحلیلی
- `REF_RECOMMENDATION_STATUS` — وضعیت پیشنهاد
- `REF_RECOMMENDATION_TYPE` — نوع پیشنهاد
- `REF_SCORE_BAND` — طبقه امتیاز
- `REF_SCORE_TYPE` — نوع امتیاز

## اجرا

در SQL Developer با **Run Script / F5**:

```text
database\oracle\cif\reference-data\analytics-recommendation\install.sql
```

پس از این فاز هر 104 تعریف منبع اصلی تعیین تکلیف شده است: 96 فرم CIF و 8 مورد Reuse از GEO/DPS. جدول `REF_TENURE_TYPE` بعداً به‌عنوان Extension محلی برای وضعیت تصرف نشانی افزوده شده و در شمارش Runtime، Catalog را به 97 Reference می‌رساند.
