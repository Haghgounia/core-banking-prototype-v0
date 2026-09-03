# Party / Customer Reference Data - Phase 1

این پوشه از مدل HTML ارسالی تولید شده است.

- تعداد جدول‌های فعال این فاز: **36**
- حوزه اصلی: `Identity and Party` (35 جدول)
- وابستگی مستقیم اضافه‌شده: `REF_LEGAL_CAPACITY`
- تعداد Seed Row مرجع حوزه Identity and Party: **184** (به‌علاوه وابستگی `REF_LEGAL_CAPACITY`)
- Schema: `CIF`
- Tablespace داده: `TS_CIF`
- Tablespace ایندکس PK: `ITS_CIF`

قبل از اجرا، Quota کاربر CIF روی هر دو Tablespace باید کافی باشد.

ترتیب اجرا:

```sql
@@01-ddl.sql
@@02-seed.sql
@@03-verify.sql
```

نکته: متن فارسی Seedها عین مدل ارسالی نگهداری شده و در این فاز بازنویسی زبانی نشده است.

افزوده‌شده در 0.3.79: `REF_RELIGION` و `REF_RELIGIOUS_DENOMINATION` برای دین و مذهب/شاخه دینی.
