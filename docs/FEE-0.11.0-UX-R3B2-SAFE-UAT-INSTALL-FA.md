# FEE UX R3-B.2 — آزمون امن عملیات و بازگشت تراکنش

## دامنه و استقلال

این بسته **فقط دو فایل تست Java** را به `backend/src/test/.../fee/admin/application` اضافه می‌کند و ابزار/مستندات مستقل دارد. هیچ فایل تولیدی Frontend یا Backend، هیچ Route، هیچ داده/DDL/Seed در Oracle، و هیچ بخش Product Builder یا FEE2 اصلاح نمی‌شود. نصب‌کننده قبل از نوشتن، تعارض نام فایل و هش بسته را بررسی کرده و فایل موجودِ متفاوت را **هرگز بازنویسی نمی‌کند**. پیش‌نیاز: نصب R3-B و R3-B.1.

## ۱. آزمون ایستا و آزمون سرویس (بدون پایگاه داده)

پس از تهیه Backup، ZIP را در ریشه پروژه استخراج کنید. سپس در CMD ویندوز:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\install-fee-r3b2.mjs --check
node tools\install-fee-r3b2.mjs
node tools\verify-fee-r3b2.mjs
node tools\verify-fee-r3b1.mjs
node tools\verify-fee-r3b.mjs
node tools\verify-fee2-studio.mjs
node tools\verify-pdl-r29-finalization.mjs
cd backend
mvnw.cmd -q -Dtest=FeeChargeStageR3B2ServiceContractTest test
cd ..
```

آزمون سرویس (Mockito) بدون اتصال Oracle بررسی می‌کند: پذیرش مرحله و قاعده هم‌نسخه با مؤلفه اختیاری خالی، رد قاعده نامرتبط قبل از INSERT، رد مؤلفه نامرتبط قبل از INSERT، رد قاعده موجودنبودن، حفظ کلیدها در ویرایش جزئی، عدم انتقال والد وابسته به نسخه دیگر و وجود `@Transactional` برای عملیات نوشتن. **این مرحله اثبات‌کننده Rollback واقعی Oracle نیست.**

## ۲. آزمون فقط‌خواندنی API (هیچ تغییری ایجاد نمی‌کند)

با برنامه در حال اجرا:

```bat
cd /d D:\Projects\core-banking-prototype-v0
set CORE_BANKING_BASE_URL=http://127.0.0.1:8091
set FEE_R3B2_VERSION_ID=837
node tools\fee-r3b2\readonly-api.mjs
```

این ابزار فقط GET می‌فرستد: ساختار فیلد اختیاری مؤلفه، شناسه نسخه، موارد ثبت‌شده در همان نسخه، گزینه‌های قاعده/مؤلفه و مالکیت آن گزینه‌ها را بررسی می‌کند. هر ناسازگاری و خطای شبکه را به‌صورت `FAIL` گزارش می‌کند. نسخه ۸۳۷ فقط برای کنترل خواندنی به کار می‌رود؛ **هیچ درخواست نوشتن به تعرفه واقعی ارسال نمی‌شود**.

## ۳. آزمون واقعی Oracle (اختیاری و به‌شدت محافظت‌شده)

این مرحله فقط باید در **کپی جداگانه و غیرتولیدی از پایگاه داده FEE** با Backup تأییدشده اجرا شود؛ در اتصال مشترک عملیاتی اجرا نکنید. آزمون Java یک Context بسیار محدود شامل فقط DataSource، Repository و Service مربوط به FEE می‌سازد و هیچ سرویس یا راه‌انداز سایر ماژول‌ها را فعال نمی‌کند. آزمون به‌صورت پیش‌فرض DISABLED است و تا زمانی که اپراتور همه متغیرهای محافظ را تنظیم نکند، داده‌ای نمی‌نویسد. آزمون از Spring Transaction Test با `@Transactional` و `@Rollback` استفاده می‌کند و پس از پایان تراکنش، `@AfterTransaction` نبود رکوردهای آزمایشی را بررسی می‌کند. با وجود Rollback، **شماره‌های Sequence در Oracle بازگشت‌پذیر نیستند**؛ پس این آزمون روی بانک اصلی مجاز نیست.

ابتدا روی Oracle آزمایشیِ موردنظر، دو مقدار `DB_NAME` و `CON_NAME` را به‌صورت فقط‌خواندنی بگیرید:

```sql
SELECT SYS_CONTEXT('USERENV','DB_NAME') AS DB_NAME,
       SYS_CONTEXT('USERENV','CON_NAME') AS CON_NAME FROM DUAL;
```

سپس در CMD ویژه UAT (با مشخصات پایگاه آزمایشی شما و نه مقادیر نمونه)، اتصال را با متغیرهای Spring در صورت نیاز به آن پایگاه آزمایشی هدایت کنید؛ نام پایگاه و PDB را دقیقاً تعیین کنید. از اتصال پیش‌فرض احتمالی `application.yml` استفاده نکنید مگر ثابت شده باشد پایگاه کاملاً آزمایشی است:

```bat
cd /d D:\Projects\core-banking-prototype-v0\backend
set "SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//YOUR_UAT_HOST:1521/YOUR_UAT_PDB"
set "SPRING_DATASOURCE_USERNAME=YOUR_UAT_USER"
set "SPRING_DATASOURCE_PASSWORD=YOUR_UAT_PASSWORD"
set "FEE_R3B2_EXPECTED_DB_NAME=YOUR_UAT_DB_NAME"
set "FEE_R3B2_EXPECTED_CON_NAME=YOUR_UAT_PDB_NAME"
set "FEE_R3B2_ORACLE_UAT=YES_ROLLBACK_ONLY"
mvnw.cmd -q -Dtest=FeeChargeStageR3B2OracleUatTest test
```

*قبل از فعال‌کردن آخرین متغیر، اتصال و تطابق محیط آزمایشی را دو بار کنترل کنید.* آزمون قبل از هر `INSERT` و حتی قبل از مصرف Sequence، تطابق پایگاه را کنترل می‌کند. برای ساخت داده اختصاصی، از پیشوند تصادفی `R3B2_UAT_` استفاده کرده، یک تعریف و دو نسخه آزمایشی با قواعد و یک مؤلفه متعلق به نسخه دیگر می‌سازد؛ سپس ثبت معتبر، رد قاعده بیگانه، رد مؤلفه بیگانه، جلوگیری از انتقال قاعده وابسته و رد ویرایش ناسازگار را آزمایش می‌کند. پس از پایان، تمام ردیف‌ها Rollback می‌شوند و وجود رکورد باقی‌مانده با همان پیشوند باعث شکست آزمون می‌شود.

هیچ HTTP POST برای UAT خودکار استفاده نشده است؛ چون HTTP روی تراکنش مستقل سرور می‌تواند Commit کند و با Rollback اجرای تست خارج از سرور بازنمی‌گردد. اگر سرویس از Trigger یا فرآیند غیرتراکنشی دیگری استفاده کند، این آزمون جایگزین ممیزی DBA نیست.

## ۴. تفسیر نتیجه و بازگشت

- `FEE_R3B2_STATIC_PASS`: قرارداد ابزارها و سورس FEE درست است؛ نه آزمون Oracle.
- `FEE_R3B2_READONLY_API_PASS`: API فقط‌خواندنی و روابط نسخه درست‌اند؛ نه آزمون ثبت.
- موفقیت JUnit `ServiceContractTest`: منطق سرویس و رد قبل از ذخیره درست است؛ نه آزمون DB.
- `FEE_R3B2_ORACLE_ROLLBACK_VERIFIED`: آزمون تراکنشی Oracle آزمایشی انجام شده و ردیف باقی نمانده است.

در صورت نیاز به حذف فایل‌های تست نصب‌شده:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\uninstall-fee-r3b2.mjs --check
node tools\uninstall-fee-r3b2.mjs
```

این بازگشت هیچ داده‌ای را تغییر نمی‌دهد و کنترل R3-B/R3-B.1 در سورس تولیدی باقی می‌ماند. اگر فایل تست را فرد دیگری تغییر داده باشد، حذف متوقف می‌شود.

## محدودیت

در محیط بسته‌بندی، بدون اتصال قابل اعتماد به Oracle UAT و بدون Maven cache کامل، نمی‌توان نتیجه «ذخیره و Rollback واقعی» را PASS نامید؛ آن وضعیت فقط پس از اجرای گام سوم توسط اپراتور تأیید خواهد شد.
