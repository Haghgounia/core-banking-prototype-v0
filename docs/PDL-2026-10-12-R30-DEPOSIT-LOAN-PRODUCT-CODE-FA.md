# PDL R30 — کد سیستمی محصول سپرده و تسهیلات، فقط پس از نهایی‌سازی مرحله ششم

## تصمیم کسب‌وکاری تأییدشده

در فرم «هویت و طبقه‌بندی محصول»، فیلد ورود «کد محصول» **برای هر دو دامنه سپرده و تسهیلات حذف شد**. هیچ محصول جدیدی در زمان ثبت پیش‌نویس کد نمی‌گیرد. در پایان طراحی، پس از تصویب نسخه جاری و تأیید حاکمیت پیکربندی، Backend کد را به‌صورت تراکنشی صادر می‌کند؛ مقدار صادرشده صرفاً در مرحله ششم و نمای فهرست محصولات قابل مشاهده است.

- تسهیلات: `LOAN_TYPE.LOAN_TYPE_CODE` (از پروفایل نسخه مصوب) + سال کاری + `001..999`؛ بدون تغییر نسبت به R29.
- سپرده: `PRODUCT.PRODUCT_FAMILY_CODE` (خانواده انتخاب‌شده در مرحله اول) + سال کاری + `001..999`. مانند `QARD_SAVINGS1405001`. این الگو برخلاف تسهیلات به یک فیلد تکراری «نوع سپرده» نیازی ندارد؛ خانواده، منبع پایدار کد است.
- سال کاری: **تنظیم مجاز بانک در جدول موجود `PDL.PRODUCT_LOAN_WORK_YEAR`** برای هر دو دامنه مشترک است؛ برای حفظ تنظیم ۱۴۰۵، در R30 جدول سال کاری دیگری ایجاد نشده است. نام این جدول تاریخی است و در صورت بازطراحی بعدی می‌تواند با Migration مجزا عمومی شود.
- سریال هر خانواده سپرده در هر سال مستقل است؛ کدهای هر عقد تسهیلات همچنان سریال مستقل دارند.
- صدور کد به معنی فعال‌شدن محصول در شعبه نیست؛ پس از صدور، وضعیت `INACTIVE` است و مجوز عملیات، وابسته به کنترل‌های دیگری است که باید جداگانه UAT شوند.
- محصولات دارای کد قبلی در سپرده یا تسهیلات دست‌نخورده‌اند؛ تغییر دستی یا صدور دوباره برای آن‌ها مجاز نیست. درخواست دوباره، کد قبلی را بازمی‌گرداند.

## فایل‌های اصلی R30

- `frontend/src/app/features/product-builder/product-workspace.component.ts/.html`: حذف ورود کد، پیش‌نویس بدون کد، دکمه نهایی‌سازی مشترک.
- `frontend/src/app/features/product-builder/product-list.component.html`: نمایش «در انتظار صدور» برای پیش‌نویس‌های بدون کد.
- `frontend/src/app/features/product-builder/product-builder.service.ts`: API صدور مستقل سپرده.
- `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/DepositProductCodePolicy.java`: کنترل نوع خانواده، سال کاری، سریال و طول کد.
- `backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java`: منع کد دستی در هر دو دامنه؛ کنترل پیش‌نویس و عملیات نهایی‌سازی سپرده.
- `backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java`: قفل سال کاری، تخصیص و ثبت رجیستری سپرده.
- `backend/src/main/java/com/behsazan/corebanking/productbuilder/web/ProductBuilderController.java`: مسیر `POST /api/v1/product-builder/products/{productId}/finalize-deposit` با درخواست `{ "versionId": 123 }`.
- `database/oracle/pdl/migrations/0.11.0-r30-deposit-final-product-code.sql`: ایجاد رجیستری سپرده و جایگزینی قید DRAFT مشترک.
- `tools/verify-pdl-r30-deposit-finalization.mjs`: آزمون‌های اختصاصی. Verifierهای R28/R29 فقط در نقاطی که رفتار تجاری سپرده در R30 تغییر کرده، به‌صورت سازگار با نسخه قبلی تطبیق داده شده‌اند.

## پیش‌نیاز و نصب

1. از آخرین R29 همراه با فایل تنظیمات محلی، Database Schema و JAR خروجی نسخه پشتیبان بگیرید.
2. پیش‌نیاز: Migrationهای R28 و R29 و سال کاری دارای تأیید راهبر اجرا شده باشند.
3. Migration R30 را با حساب DBA مجاز در **محیط UAT مستقل** و پس از تأیید برنامه بازگشت و بررسی Triggerها اجرا کنید. DDL در Oracle `COMMIT` ضمنی دارد؛ اجرای آن برگشت تراکنشی سراسری ندارد. **لازم است ترتیب اجرای Migration قبل از استقرار Backend R30 باشد.**
4. Patch R30 را **فقط روی سورس R29 نهایی** استخراج کنید، نه R28. گزینه امن دیگر ZIP کامل R30 است. هیچ فایل Fee یا Fee2 در Patch تغییر نکرده است.
5. در CMD ویندوز از ریشه پروژه:

```bat
node tools\verify-pdl-r28-loan-code-eligibility.mjs
node tools\verify-pdl-r29-finalization.mjs
node tools\verify-pdl-r30-deposit-finalization.mjs
build-production.cmd > "%TEMP%\build-r30.log" 2>&1
echo BUILD_EXIT_CODE=%ERRORLEVEL%
if exist app\BUILD-DIRTY (echo BUILD_NOT_COMPLETE) else (echo BUILD_CLEAN)
```

نکته: فایل Build Log را در ریشه پروژه ایجاد نکنید؛ کنترل انتشار فایل‌های اضافی را رد می‌کند.

## کنترل Oracle (فقط خواندنی)

```sql
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT'
   AND CONSTRAINT_NAME IN ('UQ_PRODUCT_CODE','CK_PDL_PRODUCT_CODE_DRAFT','CK_PDL_PRODUCT_CODE_ISSUED')
 ORDER BY CONSTRAINT_NAME;
-- انتظار: UQ_PRODUCT_CODE و CK_PDL_PRODUCT_CODE_DRAFT هر دو ENABLED/VALIDATED
-- CK_PDL_PRODUCT_CODE_ISSUED نسخه قدیم R29 باید حذف شده باشد.

SELECT TABLE_NAME FROM ALL_TABLES
 WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT_DEPOSIT_CODE_REGISTRY';

SELECT SETTING_ID, WORK_YEAR, UPDATED_BY FROM PDL.PRODUCT_LOAN_WORK_YEAR WHERE SETTING_ID=1;

SELECT PRODUCT_CLASS_CODE, PRODUCT_STATUS_CODE,
       COUNT(*) AS ROW_COUNT,
       SUM(CASE WHEN PRODUCT_CODE IS NULL THEN 1 ELSE 0 END) AS WITHOUT_CODE
  FROM PDL.PRODUCT
 GROUP BY PRODUCT_CLASS_CODE, PRODUCT_STATUS_CODE
 ORDER BY PRODUCT_CLASS_CODE, PRODUCT_STATUS_CODE;
```

## سناریوهای UAT

1. سپرده جدید: «کد محصول» در مرحله اول وجود ندارد؛ `PRODUCT_CODE=NULL` و `DRAFT` در Oracle. برای تسهیلات نیز همین.
2. تلاش ارسال کد دلخواه با API عمومی هنگام ایجاد هر دامنه: خطای کنترل اعتبار و بدون درج رکورد. تلاش تغییر کد قدیمی: رد.
3. تلاش تغییر وضعیت سپرده یا تسهیلات بدون کد به `ACTIVE/INACTIVE/RETIRED` از API عمومی: رد.
4. نهایی‌سازی سپرده با نسخه DRAFT، نسخه غیرجاری، پروفایل مفقود/چندگانه، یا ناسازگاری حاکمیت: رد و **بدون مصرف شماره سریال**.
5. سپرده عادی: دقیقاً یک پروفایل `DEPOSIT_PRODUCT_PROFILE`؛ نوسترو/وسترو: دقیقاً یک پروفایل `CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE`. قواعد اختصاصی هر خانواده تابع ماتریس حاکمیت است.
6. پس از تکمیل و تصویب نسخه، دریافت کد نظیر `QARD_SAVINGS1405001` به همراه پیام موفقیت؛ `PRODUCT_STATUS_CODE=INACTIVE`؛ ثبت هم‌زمان `PDL.PRODUCT` و `PDL.PRODUCT_DEPOSIT_CODE_REGISTRY`.
7. دو محصول هم‌خانواده در یک سال: `001` و `002`. خانواده‌های متفاوت هر یک سریال مستقل دارند؛ در سال کاری بعد `001` مجدداً آغاز می‌شود. اگر کدی قبلاً در محصولات تاریخی وجود داشته باشد، شماره آن در صورت تعارض کنار گذاشته می‌شود.
8. دو درخواست هم‌زمان برای همان محصول: فقط یک کد و یک رکورد رجیستری؛ درخواست دوم همان کد قبلی را برگرداند.
9. رگرسیون تسهیلات: همان عقد + همان سال + سریال، بدون تغییر نسبت به R29؛ کدهای موجود در هر دو دامنه همچنان بدون تغییر.
10. آزمون بلوکه بودن انتخاب/افتتاح واقعی محصول غیرفعال در موتور عملیات شعبه **مستقل از این مرحله است** و با آزمون پیکربندی ادعای اثبات آن نمی‌شود.

## حدود آزمون‌های انجام‌شده

کنترل‌های Static/Java و رگرسیون Source در محیط ایزوله اجرا شده‌اند؛ **Build کامل Angular/Maven و اجرای Migration/صدور تراکنشی روی Oracle فعلی بانک انجام نشده‌اند**. لذا R30 «آماده نصب آزمایشی/UAT» است، نه «تأیید نهایی عملیاتی».
