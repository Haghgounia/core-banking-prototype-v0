# R29 — تولید کد محصول تسهیلات فقط پس از نهایی‌سازی مرحله ششم

## مبنای واقعی DDL دریافتی

در `PDL.PRODUCT` ستون `PRODUCT_CODE VARCHAR2(30 CHAR) NOT NULL` و قید
`UQ_PRODUCT_CODE UNIQUE(PRODUCT_CODE)` موجود است. نسخه R28 کد را در همان ثبت مرحله اول صادر می‌کرد.
این رفتار با تصمیم نهایی صاحب فرایند مغایرت داشت.

## قرارداد کسب‌وکاری R29

1. مرحله «هویت و طبقه‌بندی محصول»: کد محصول برای تسهیلات فقط‌خواندنی و **خالی** می‌ماند.
   کاربر فقط نام، دامنه، خانواده، ارز و شرح را ثبت می‌کند. وضعیت تسهیلات بدون کد فقط `DRAFT` است.
2. عقد در `LOAN_PRODUCT_PROFILE.LOAN_TYPE_ID` نسخه محصول ثبت می‌شود، نه در یک فیلد
   تکراری در هویت محصول. منبع واقعی کد عقد `PDL.LOAN_TYPE.LOAN_TYPE_CODE` است.
3. مراحل ۲ تا ۵ و سیاست قواعد باید تکمیل شده باشند. در مرحله ششم، نسخه جاری (`IS_CURRENT=1`)
   باید `APPROVED` و `RECORD_STATUS_CODE=ACTIVE` باشد و دقیقاً یک پروفایل تسهیلات معتبر داشته باشد.
   علاوه بر این، محصول نباید بیش از یک نسخه با `IS_CURRENT=1` داشته باشد.
4. دکمه «نهایی‌سازی و صدور کد محصول» درخواست مستقل `POST /api/v1/product-builder/products/{productId}/finalize-loan`
   با بدنه `{ "versionId": 123 }` را ارسال می‌کند.
5. Backend همه کنترل‌های در دسترس را دوباره ارزیابی می‌کند:
   هویت و دامنه، تطابق خانواده و عقد فعال، ارز، اعتبارسنجی پروفایل و دو دروازه
   `GovernanceReport.canApprove`. دروازه `canOpen` به فعال‌سازی عملیاتی تعلق دارد،
   نه به ایجاد شناسه محصول؛ بنابراین انتشار کد به‌تنهایی اجازه اعطای تسهیلات نیست.
   مهم: تا زمانی که `canOpen` به موتور عملیاتی اعطای تسهیلات متصل و آزمون نشده، گزارش این دروازه
   به‌تنهایی ضمانت جلوگیری از عملیات شعبه نیست. محصول پس از صدور کد `INACTIVE` است.
   اگر کنترل‌ها رد شوند، کدی ایجاد یا سریالی مصرف نمی‌شود.
6. فقط **پس از موفقیت** کد مطابق `LOAN_TYPE_CODE + WORK_YEAR + serial(001..999)` توسط Oracle
   در یک تراکنش صادر می‌شود؛ مثال `121405001`. سال کاری از جدول R28 می‌آید، نه ساعت سیستم.
7. محصول پس از صدور کد در وضعیت `INACTIVE` قرار می‌گیرد؛ **این مرحله اعطای تسهیلات در شعبه را فعال نمی‌کند**.
   `PRODUCT_CODE` و سابقه سریال با هم ثبت می‌شوند. در صورت خطا هر دو rollback می‌شوند.
8. تکرار درخواست برای محصول دارای کد، همان کد را بازمی‌گرداند و سریال جدید نمی‌گیرد.
   کدهای محصولات قدیمی R28 و قدیمی‌تر دست‌نخورده باقی می‌مانند.
9. محصول سپرده بدون هیچ تغییری همچنان هنگام ایجاد، کد محصول الزامی دارد.

## Migration الزامی

`database/oracle/pdl/migrations/0.11.0-r29-loan-product-finalization.sql`

فایل با بررسی وجود جدول رجیستری R28 آغاز می‌شود، **ابتدا قید محافظ را ایجاد می‌کند**
و فقط پس از آن `NOT NULL` ستون `PRODUCT_CODE` را حذف می‌کند.
قید جدید `CK_PDL_PRODUCT_CODE_ISSUED` فقط به LOAN+DRAFT اجازه کد NULL می‌دهد.
`UQ_PRODUCT_CODE` و کلید اصلی `PRODUCT_ID` تغییر نمی‌کنند. ساختار R28 رجیستری شماره سریال
و تنظیم سال کاری قبلی نیز دست‌نخورده می‌ماند.

**توجه:** DDL در Oracle به‌طور ضمنی COMMIT می‌کند؛ پیش از اجرا در محیط واقعی از ساختار و داده‌ها
نسخه پشتیبان تهیه کنید. قبل از اجرای Migration، Triggerها و سایر وابستگی‌های ستون را بررسی کنید.
اجرای Migration روی Oracle شما در این محیط انجام یا تأیید نشده است.

### کنترل پس از اجرای Migration (فقط خواندنی)

```sql
SELECT COLUMN_NAME, NULLABLE, DATA_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT' AND COLUMN_NAME='PRODUCT_CODE';
-- Expected: NULLABLE=Y, DATA_LENGTH according to database character semantics.

SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT'
   AND CONSTRAINT_NAME IN ('UQ_PRODUCT_CODE','CK_PDL_PRODUCT_CODE_ISSUED');
-- Both must be ENABLED and VALIDATED.

SELECT SETTING_ID, WORK_YEAR, UPDATED_BY
  FROM PDL.PRODUCT_LOAN_WORK_YEAR
 WHERE SETTING_ID=1;
```

## نصب روی آخرین سورس ۱۸:۵۳

بسته Patch `core-banking-pdl-r29-final-loan-code-patch.zip` را فقط روی پروژه‌ای اعمال کنید
که قبلاً R28 را دارد. بسته کامل R29 از آخرین سورس ارسالی `2026-10-11-1853` ساخته شده است
و اصلاحات مجزای Fee و Calendar2 موجود در همان سورس را حفظ می‌کند.

در CMD از ریشه پروژه:

```bat
node tools\verify-pdl-r28-loan-code-eligibility.mjs
node tools\verify-pdl-r29-finalization.mjs
build-production.cmd > "%TEMP%\build-r29.log" 2>&1
echo BUILD_EXIT_CODE=%ERRORLEVEL%
if exist app\BUILD-DIRTY (echo BUILD_NOT_COMPLETE) else (echo BUILD_CLEAN)
```

فایل لاگ را در ریشه پروژه نسازید؛ کنترل Release Layout آن را غیرمجاز می‌شمارد.

## سناریوهای UAT الزامی

- ثبت محصول تسهیلات جدید بدون کد: ذخیره موفق، `PRODUCT_CODE` در Oracle مقدار NULL.
- نهایی‌سازی بدون نسخه مصوب/جاری/فعال: رد با توضیح و بدون ایجاد رکورد رجیستری.
- نهایی‌سازی با پروفایل صفر یا چندگانه: رد؛ بدون صدور کد.
- نهایی‌سازی با عقد نامعتبر، غیرفعال یا ناسازگار با خانواده: رد.
- نهایی‌سازی با نقص حاکمیت پیکربندی (`canApprove=false`): رد؛ سریال رزرو نشود.
- اگر `canApprove=true` ولی `canOpen=false` باشد، صدور کدِ محصول غیرفعال مجاز است؛ اعطای عملیاتی مجاز نیست.
- نهایی‌سازی با همه شرایط: ثبت `PRODUCT_CODE`، ایجاد رکورد رجیستری و وضعیت `INACTIVE`.
- دو درخواست هم‌زمان برای یک محصول: فقط یک کد؛ درخواست دوم همان کد اول را بازگرداند.
- دو محصول یک عقد و یک سال: کدهای دارای سریال متفاوت `001` و `002`.
- ثبت محصول سپرده بدون کد: رد؛ سپرده با کد معتبر: قبول.
- تلاش تغییر کد محصول تسهیلات با API عمومی: رد.
- محصولات تسهیلاتی کددار موجود در R28: کد و رجیستری سابق حفظ شود.

## محدوده اثبات

آزمون‌های ایستای سورس، parsing فایل‌های TypeScript و آزمون خالص Java در
`tools/verify-pdl-r29-finalization.mjs` انجام شده‌اند. Build کامل Angular/Maven،
Oracle DDL روی پایگاه داده کاربر و آزمون E2E شعبه در این محیط انجام نشده است.

نکته سیاستی: Rule Governance Lite در پروژه هنوز احراز هویت واقعی Maker/Checker را اثبات نمی‌کند.
صدور کد در محیط تولید به دسترسی مجاز و گردش کار تأیید هویت‌یافته نیاز دارد.
همچنین صدور کد به‌تنهایی تضمین اجرای قواعد تسهیلات در Runtime نیست.
