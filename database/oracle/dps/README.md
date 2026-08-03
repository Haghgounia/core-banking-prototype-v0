# DPS database scripts

این پوشه اسکریپت‌های استخراج‌شده از Oracle برای Schema `DPS` را نگهداری می‌کند.

- `ddl/01_sequences.sql` تا `ddl/07_column-comments.sql`: DDL فعال جدول‌های مرجع `REF_*`
- `ddl/08_add-created-by-to-reference-tables.sql`: افزودن ستون `CREATED_BY` و Comment آن به همه جدول‌های مرجع
- `data/`: محل داده‌های پایه تأییدشده
- `pending/`: اشیای مربوط به فازهای آینده که نباید همراه DDL فعال اجرا شوند

فایل `ddl/05_indexes.sql` فقط Indexهای جدول‌های `REF_*` را دارد. Indexهای `DEPOSIT_PRODUCT*` به `pending/product-factory-indexes.sql` منتقل شده‌اند.

خروجی زمان‌دار ابزار Export در `database/oracle/exports/` تولید می‌شود و به دلیل Generated بودن در Git ثبت نمی‌شود.
