# DPS database scripts

این پوشه اسکریپت‌های استخراج‌شده از Oracle برای Schema `DPS` را نگهداری می‌کند.

- `ddl/01_sequences.sql` تا `ddl/07_column-comments.sql`: خروجی‌های دریافت‌شده از پایگاه داده
- `ddl/08_add-created-by-to-reference-tables.sql`: تغییر افزایشی اعلام‌شده برای افزودن `CREATED_BY` به همه جدول‌های `REF_*`
- `data/`: محل اسکریپت داده‌های پایه و نمایشی؛ در این مرحله داده‌ای دریافت نشده است

اسکریپت `05_indexes.sql` علاوه بر Indexهای جداول مرجع، Indexهای جدول‌های Product Factory را نیز دارد؛ در این مرحله فقط جدول‌های `REF_*` در برنامه فعال شده‌اند.
