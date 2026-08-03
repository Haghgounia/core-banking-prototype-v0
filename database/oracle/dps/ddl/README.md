# DPS active DDL

ترتیب فایل‌ها برای نگهداری و بررسی Metadata جدول‌های مرجع `DPS.REF_*` است:

1. `01_sequences.sql`
2. `02_tables.sql`
3. `03_check-and-not-null-constraints.sql`
4. `04_primary-and-unique-constraints.sql`
5. `05_indexes.sql`
6. `06_table-comments.sql`
7. `07_column-comments.sql`
8. `08_add-created-by-to-reference-tables.sql`

فایل‌های `03`، `04` و `05` خروجی‌های مستقل Oracle هستند و ممکن است Indexهای پشتیبان Constraintها را تکرار کنند. پیش از تبدیل این مجموعه به Install Script یکپارچه، تکرار Indexها در محیط مقصد کنترل شود.

Indexهای جدول‌های Product Factory در `../pending/` نگهداری می‌شوند و بخشی از DDL فعال این مرحله نیستند.
