# SYS 0.3.55 / FIX66 — Oracle Metadata-driven Schema Discovery

## هدف
حذف فهرست Hard-code/Configuration-driven اسکیماها از فرم مقایسه EA با Oracle و دریافت پویا از متادیتای Oracle.

## رفتار جدید
- API موجود `/api/v1/system/database-model-comparison/configuration` در هر درخواست فهرست Schemaها را از `ALL_TABLES` می‌خواند.
- Query مبنا:
  `SELECT OWNER, COUNT(*) AS TABLE_COUNT FROM ALL_TABLES GROUP BY OWNER ORDER BY OWNER`
- در نتیجه فقط Schema/Ownerهایی نمایش داده می‌شوند که برای User اتصال Oracle حداقل یک Table قابل مشاهده دارند.
- `core-banking.schemas.*` فقط برای Friendly Label و Preferred Default استفاده می‌شوند.
- اگر CIF تنظیم‌شده در Metadata موجود نباشد، User جاری Oracle و سپس اولین Schema قابل مشاهده به‌عنوان Default انتخاب می‌شود.
- Validation مقایسه و Export نیز بر همان فهرست Metadata-driven اعمال می‌شود و Schema خارج از محدوده قابل مشاهده پذیرفته نمی‌شود.

## UI
- متن راهنما و Hint فرم مقایسه EA/Oracle اصلاح شد و صراحتاً Oracle Data Dictionary را Source of Truth معرفی می‌کند.
- Hint فرم Oracle→EA XMI نیز با همین رفتار همگام شد.
- تعداد Tableهای قابل مشاهده در Label هر Schema نمایش داده می‌شود.

## امنیت و کنترل دسترسی
استفاده از `ALL_TABLES` عمداً به‌جای `DBA_TABLES` انجام شده تا Dropdown فقط Objectهایی را نشان دهد که اتصال جاری Oracle حق مشاهده آنها را دارد و نیاز به DBA Privilege ایجاد نشود.

## Migration
نیازی به DDL یا Migration دیتابیس نیست.
