# Core Banking Prototype 0.3.32 — FIX43 Oracle → Enterprise Architect XMI Export

## هدف
افزودن یک فرم عملیاتی در بخش «مدیریت و سیستم» که Metadata مدل فیزیکی Oracle را مستقیماً از Data Dictionary خوانده و به فایل XML/XMI قابل Import در Enterprise Architect تبدیل کند.

## مسیر UI
`/system/oracle-ea-xmi-export`

منوی جدید:
`مدیریت و سیستم → استخراج Oracle به EA XML`

## اتصال Oracle
فرم Credential دریافت نمی‌کند. اتصال از همان `DataSource` پیکربندی‌شده Backend استفاده می‌شود و فقط Schemaهای موجود در `core-banking.schemas.*` قابل انتخاب‌اند. در بالای فرم Product/Version/User/JDBC URL فعلی به‌صورت فقط‌خواندنی نمایش داده می‌شود.

## فرمت خروجی
- XMI 1.1
- UML 1.3 namespace: `omg.org/UML1.3`
- Exporter metadata: `Enterprise Architect / 2.5`
- مدل فیزیکی Oracle با `gentype=Oracle`

## پوشش Metadata
### همیشه استخراج می‌شود
- Table
- Column
- Oracle datatype
- Length / CHAR-BYTE semantics
- Precision / Scale
- Nullable / Not Null
- Column position
- Default value در صورت قابل خواندن بودن `DATA_DEFAULT`
- Identity / AutoNum
- Primary Key
- Unique Constraint
- Owner
- Tablespace

### قابل انتخاب در فرم
- Foreign Key operation
- Foreign Key association
- Index / Unique index مستقل از constraint
- Check Constraint
- Table/Column comments
- Stub جدول مرجع خارج از Pattern یا Schema

## Oracle Data Dictionary Sources
- `ALL_USERS`
- `ALL_TABLES`
- `ALL_TAB_COLUMNS`
- `ALL_TAB_COMMENTS`
- `ALL_COL_COMMENTS`
- `ALL_CONSTRAINTS`
- `ALL_CONS_COLUMNS`
- `ALL_INDEXES`
- `ALL_IND_COLUMNS`

## نحوه مدل‌سازی در EA
- Table → `UML:Class` با stereotype=`table`
- Column → `UML:Attribute` با stereotype=`column`
- PK → `UML:Operation` با stereotype=`PK`
- Unique → `UML:Operation` با stereotype=`unique`
- Index → `UML:Operation` با stereotype=`index`
- FK → هم `UML:Operation` با stereotype=`FK` و هم `UML:Association` با stereotype=`FK`
- Check / PK / UK / FK → `UML:Constraint` نیز در Class ثبت می‌شوند.

## Reference Stub
اگر جدول انتخاب‌شده به جدولی خارج از Pattern یا Schema دیگری FK داشته باشد و گزینه Reference Stub فعال باشد، Exporter جدول مقصد را به‌صورت حداقلی به XMI اضافه می‌کند. فقط ستون‌های مورد نیاز کلید مرجع و Key Constraint مورد نیاز برای برقرار ماندن Association استخراج می‌شوند.

این قابلیت برای روابط Cross-Schema مانند CIF → GEO مهم است.

## Stable EA IDs
GUID/XMI IDs با `UUID.nameUUIDFromBytes` و بر اساس کلیدهای ثابت زیر تولید می‌شوند:
- schema/package
- owner.table
- owner.table.column
- constraint/index name

هدف این است که Exportهای بعدی برای همان Object شناسه تصادفی جدید نسازند.

## Validation قبل از Download
پس از تولید فایل، همان XMI توسط `EaXmiModelParser` موجود در پروژه دوباره Parse می‌شود. تعداد Tableهای Parseشده باید با تعداد Tableهای تولیدشده برابر باشد؛ در غیر این صورت فایل برای دانلود تحویل داده نمی‌شود.

## API
### Configuration
`GET /api/v1/system/oracle-ea-xmi-export/configuration`

### Preview
`GET /api/v1/system/oracle-ea-xmi-export/preview`

Query parameters:
- schema
- tablePattern
- includeForeignKeys
- includeIndexes
- includeChecks
- includeComments
- includeExternalReferences

### Export
`GET /api/v1/system/oracle-ea-xmi-export/export`

Response:
- `Content-Type: application/xml`
- `Content-Disposition: attachment`
- `X-EA-XMI-Table-Count`
- `X-EA-XMI-FK-Count`

## QA اجراشده
`node tools/verify-oracle-ea-xmi-export.mjs`

نتیجه: **20/20 PASS**

همچنین:
- `node tools/verify-ea-oracle-comparison.mjs` → PASS
- `node tools/verify-cif-persisted-grids.mjs` → PASS

Writer اصلی به‌صورت مستقل با `javac` و مدل ساختگی Table/PK/Index/Check/FK کامپایل و اجرا شد؛ فایل XMI تولیدشده XML-valid بود و Association/Operationهای مورد انتظار در آن وجود داشتند.

## محدودیت آگاهانه نسخه اول
برای Function-based Index، Data Dictionary ممکن است نام ستون داخلی Oracle را برگرداند. در این نسخه Index در XMI حفظ می‌شود و Warning نمایش داده می‌شود، اما عبارت Function از `ALL_IND_EXPRESSIONS` بازسازی نمی‌شود. این مورد می‌تواند در فاز بعدی افزوده شود.
