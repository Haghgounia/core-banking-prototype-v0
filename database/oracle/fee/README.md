# FEE schema — standalone prototype

این پوشه مدل فیزیکی اولین increment ماژول مستقل کارمزد را نگهداری می‌کند.

## اصول
- هیچ FK فیزیکی به CIF/DPS/Account/Accounting وجود ندارد.
- Product/Party/Account در Prototype فقط reference نمایشی هستند.
- Configuration از Runtime جداست.
- Fee Transaction و Calculation Snapshot به‌عنوان شواهد مالی/محاسباتی immutable مدل می‌شوند.
- BIAN در سطح semantic alignment استفاده شده و این جداول «BIAN tables» محسوب نمی‌شوند.

## ترتیب اجرا
1. ایجاد schema/user `FEE` توسط DBA یا محیط Prototype
2. `ddl/01_sequences.sql`
3. `ddl/02_tables.sql`
4. `ddl/03_indexes.sql`
