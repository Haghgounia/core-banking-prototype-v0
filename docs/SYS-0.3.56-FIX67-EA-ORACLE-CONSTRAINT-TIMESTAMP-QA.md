# Core Banking Prototype 0.3.56 — FIX67
## تکمیل مقایسه EA/Oracle: TIMESTAMP، PK/FK/Check و Metadata Schema

## هدف
این اصلاح فرم «مقایسه مدل Enterprise Architect با Oracle» را برای مقایسه ساختاری مدل فیزیکی دقیق‌تر و عملیاتی‌تر می‌کند.

نیازمندی‌های این Fix:

1. اگر در EA نوع ستون `TIMESTAMP(0)` و در Oracle همان ستون `TIMESTAMP(6)` باشد، این اختلاف به‌تنهایی به‌عنوان تفاوت گزارش نشود.
2. Primary Key، Foreign Key و Check Constraint نیز در کنار Columnها در نتیجه مقایسه لحاظ شوند.
3. Schemaهای سیستمی/Oracle-maintained در فهرست Schemaهای قابل انتخاب نمایش داده نشوند.
4. عنوان فارسی جدول از EA Alias با Oracle Table Comment مقایسه شود و EA Documentation باعث False Positive نشود.

---

## 1. قرارداد مقایسه TIMESTAMP

قاعده سازگاری فقط برای زوج زیر اعمال شده است:

| EA | Oracle | نتیجه |
|---|---|---|
| `TIMESTAMP(0)` | `TIMESTAMP(6)` | همسان/سازگار |
| `TIMESTAMP(0)` | `TIMESTAMP(0)` | همسان |
| `TIMESTAMP(6)` | `TIMESTAMP(6)` | همسان |
| `TIMESTAMP(3)` | `TIMESTAMP(6)` | متفاوت |
| سایر Precisionهای متفاوت | متفاوت | متفاوت |

نکته مهم: مقدار خام Type در UI تغییر داده نمی‌شود؛ بنابراین کاربر همچنان `TIMESTAMP(0)` در EA و `TIMESTAMP(6)` در Oracle را مشاهده می‌کند، اما این زوج به‌تنهایی وضعیت Column/Table را «متفاوت» نمی‌کند.

---

## 2. Primary Key

Primary Key قبلاً در مدل نتیجه وجود داشت، اما Edge Case نبود PK در EA و وجود PK در Oracle می‌توانست وضعیت جدول را به‌درستی متفاوت نکند. این رفتار اصلاح شد.

مقایسه PK شامل:
- وجود/عدم وجود PK
- ترتیب ستون‌های PK
- مجموعه ستون‌های PK

قرارداد:
- هر دو بدون PK → همسان
- EA بدون PK و Oracle دارای PK → متفاوت
- EA دارای PK و Oracle بدون PK → متفاوت
- ستون/ترتیب متفاوت → متفاوت

---

## 3. Foreign Key

مقایسه FK به نتیجه جدول اضافه شد.

### منبع EA XMI
Parser از این ساختارها پشتیبانی می‌کند:
- `UML:Operation` با stereotype=`FK`
- `UML:Association` با stereotype=`FK`
- `UML:Constraint` مرتبط با FK

### منبع Oracle
- `ALL_CONSTRAINTS`
- `ALL_CONS_COLUMNS`
- رابطه `R_OWNER` / `R_CONSTRAINT_NAME` برای Resolve کردن Parent Constraint

### اقلام مقایسه‌شده
- نام Constraint
- ستون/ستون‌های Child با حفظ ترتیب
- جدول Parent
- ستون/ستون‌های Parent با حفظ ترتیب

برای هر FK یکی از وضعیت‌های زیر تولید می‌شود:
- MATCH
- DIFFERENT
- MISSING_IN_DATABASE
- EXTRA_IN_DATABASE

---

## 4. Check Constraint

مقایسه Check Constraint نیز به نتیجه جدول اضافه شد.

### منبع EA XMI
- `UML:Operation` با stereotype=`check`
- `UML:Constraint`
- شرط از Tagged Valueهای `code` یا `documentation` استخراج می‌شود.

### منبع Oracle
- `ALL_CONSTRAINTS`
- `CONSTRAINT_TYPE='C'`
- `SEARCH_CONDITION_VC`

### Normalization شرط Check
برای جلوگیری از False Positiveهای صرفاً متنی:
- پرانتز خارجی غیرضروری حذف می‌شود.
- Double Quote استاندارد اطراف Identifier نرمال می‌شود.
- فاصله و Case خارج از String Literal نادیده گرفته می‌شود.
- محتوای String Literal حفظ می‌شود.

Checkهای سیستمی مرتبط با `NOT NULL` از بخش Check حذف می‌شوند، زیرا Nullable/Not Null قبلاً در سطح Column به‌طور مستقل مقایسه می‌شود.

---

## 5. اثر Constraintها بر وضعیت جدول

وضعیت نهایی Table اکنون وابسته به همه موارد زیر است:
- Column structure
- Nullable
- PK
- FK
- Check Constraint
- Persian metadata

بنابراین اختلاف FK یا Check حتی در صورت یکسان بودن همه Columnها، جدول را «دارای اختلاف» می‌کند.

در Grid اصلی برای FK و Check ستون مستقل با تعداد EA/Oracle و Badge وضعیت اضافه شده و در Detail هر جدول، ردیف‌های Constraint به‌صورت مجزا قابل مشاهده هستند.

خروجی CSV نیز وضعیت و Countهای FK/Check را صادر می‌کند.

---

## 6. اصلاح Metadata فارسی

قرارداد مقایسه عنوان فارسی جدول اصلاح شد:

- EA `Alias` ↔ Oracle `TABLE COMMENT`
- EA `Documentation` اطلاعات توصیفی مستقل است و با همان Oracle Table Comment مقایسه نمی‌شود.

این تغییر False Positiveهایی مانند حالتی را رفع می‌کند که Alias و Oracle COMMENT یکسان‌اند ولی Documentation توضیح فنی طولانی‌تری دارد.

---

## 7. حذف Schemaهای سیستمی Oracle

فهرست Schema همچنان Metadata-driven است، اما Schemaهای Oracle-maintained حذف می‌شوند.

Query مبنا:

```sql
SELECT T.OWNER, COUNT(*) AS TABLE_COUNT
  FROM ALL_TABLES T
  JOIN ALL_USERS U ON U.USERNAME = T.OWNER
 WHERE COALESCE(U.ORACLE_MAINTAINED, 'N') = 'N'
 GROUP BY T.OWNER
 ORDER BY T.OWNER
```

بنابراین Schemaهای کاربردی قابل مشاهده برای User اتصال نمایش داده می‌شوند و Schemaهای سیستمی Oracle در Dropdown ظاهر نمی‌شوند.

---

## 8. Validation انجام‌شده

### Static Verification
موارد زیر PASS شدند:
- CIF persisted grids verifier
- EA/Oracle comparison verifier
- CAL reference verifier
- CAL dataset import verifier
- CAL2 reference verifier
- CAL2 month-view verifier
- Calendar display-label verifier
- Runtime artifact contract verifier

### Java compile/syntax harness
کلاس‌های تغییرکرده با JDK 21 و Stubهای حداقلی Spring/JDBC بررسی شدند:
- `EaXmiModelParser`
- `OracleSchemaInspector`
- `EaOracleComparisonService`
- `ConfiguredDatabaseSchemas`

### تست Parser روی Final_4.xml
Parser جدید روی XMI واقعی `Final_4.xml` اجرا شد و این ساختار را استخراج کرد:
- 50 Table
- 100 Foreign Key
- 131 Check Constraint
- 102 ستون TIMESTAMP با Precision/Length مؤثر 0 در مدل Parsed

نمونه `SUB_OPERATION`:
- PK: `SUB_OPERATION_ID`
- Check: `CK_SO_REC_STATUS`

### Behavior Harness مقایسه
سناریوهای زیر به‌صورت اجرایی بررسی شدند:
- EA `TIMESTAMP(0)` / Oracle `TIMESTAMP(6)` → MATCH و بدون Difference
- FK یکسان → MATCH
- Check یکسان → MATCH
- Constraint mismatch → در وضعیت Table منعکس می‌شود

### محدودیت محیط Build
Full Maven/Angular production build در محیط تولید این بسته قابل اجرا نبود، زیرا Maven Wrapper برای Maven 3.9.16 نیاز به دانلود از Maven Central داشت و Runtime اینترنت نداشت؛ همچنین `frontend/node_modules` در Source Package نگهداری نمی‌شود.

کنترل نهایی Runtime روی محیط نصب باید با دستور زیر انجام شود:

```bat
build-production.cmd
```

---

## 9. Database impact

این Fix فقط Read/Comparison Logic و UI را تغییر می‌دهد.

- DDL جدید: ندارد
- Migration: ندارد
- Seed data: ندارد
- تغییر Schema: ندارد

## نسخه
`0.3.56-prototype-fee-p1`
