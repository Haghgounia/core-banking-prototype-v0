# GEO scripts

این شاخه شامل DDL و Data Script جداول اطلاعات پایه است.

## DDL

```text
ddl/01_geo_hierarchy_tables.sql
ddl/02_reference_data_tables.sql
ddl/03_foreign_cities.sql
ddl/04_name_romanization_dictionary.sql
```

## Data

فایل‌های `data/*_data_*.sql` خروجی‌های داده موجود هستند. ترتیب پیشنهادی اجرای داده در `install-data.sql` ثبت شده است.

دو فایل مشاغل در منبع اولیه با Owner برابر `CIF` تولید شده بودند. چون جداول فعال فعلاً در `GEO` قرار دارند، فقط Qualifier دستورهای `INSERT` به `GEO` تغییر داده شده و این تغییر در ابتدای همان فایل‌ها مستند شده است.

داده سلسله‌مراتب داخلی ایران (استان، شهرستان، بخش، شهر، دهستان و روستا) در بسته فعلی وجود ندارد و باید جداگانه از پایگاه داده مرجع استخراج شود.


## Name Romanization Dictionary

دو فرم اطلاعات پایه عمومی برای جداول زیر در سامانه فعال شده‌اند:

- `GEO.NAME_ROMANIZATION_DICTIONARY` — واژه‌نامه رومن‌نویسی نام‌ها
- `GEO.NAME_AFFIX_DICTIONARY` — واژه‌نامه پیشوند و پسوند نام

فرم‌ها از Generic Reference CRUD استفاده می‌کنند و از مسیر «اطلاعات پایه عمومی» در دسترس هستند.
