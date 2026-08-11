# CIF Party Reference - Phase 6: Analytics, Scoring & Recommendation

## نتیجه

آخرین حوزه Reference Data مدل Party/Customer تکمیل شده است. 7 جدول جدید CIF فعال شده و `REF_CUSTOMER_SEGMENT` به `DPS.REF_CUSTOMER_SEGMENT_CODE` نگاشت شده است تا منبع داده تکراری ایجاد نشود.

## فرم‌های جدید

- شاخص تحلیلی مشتری (`REF_METRIC`)
- واحد اندازه‌گیری شاخص (`REF_METRIC_UNIT`)
- مدل تحلیلی (`REF_MODEL`)
- وضعیت پیشنهاد (`REF_RECOMMENDATION_STATUS`)
- نوع پیشنهاد (`REF_RECOMMENDATION_TYPE`)
- طبقه امتیاز (`REF_SCORE_BAND`)
- نوع امتیاز (`REF_SCORE_TYPE`)

تعداد Seed این فاز: **24**.

## نگاشت Customer Segment

`CIF.PARTY_SEGMENT_MEMBERSHIP.SEGMENT_CODE` در فاز عملیاتی بعدی باید Lookup خود را از `DPS.REF_CUSTOMER_SEGMENT_CODE.CODE` بگیرد. در CIF جدول `REF_CUSTOMER_SEGMENT` ساخته نشده است.

## مصرف عملیاتی بعدی

این Referenceها برای جدول‌های عملیاتی زیر آماده شده‌اند:

- `PARTY_METRIC_SNAPSHOT`
- `PARTY_SEGMENT_MEMBERSHIP`
- `PARTY_VALUE_SCORE`
- `PARTY_RECOMMENDATION`

این نسخه فقط Reference Data را تکمیل می‌کند؛ CRUD عملیاتی این چهار بخش در فاز بعدی Customer 360 اضافه می‌شود.
