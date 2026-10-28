# FEE3 UI R8+R2 Integrity Bridge B1 — Source-only

**هدف:** رفع تعارض ماهوی بین آزمون Historical R8 و وضعیت قانونی پس از نصب `FEE3-BE-R2` بدون تغییر R8 Manifest، سه فایل UI، Backend، Oracle یا اسکریپت Build.

**مبنای شواهد:** بسته `core-banking-prototype-v0-2026-10-27-1721.zip`، پروژه 0.11.0. هر ۱۹ فایل Manifest نصب R2 دقیقاً با SHA256 نسخه `after` برابر است. سه فایل `fee3-workspace.component.{html,ts,scss}` هم در Manifest R8 با مقدار `before` در R2 منطبق‌اند و هم نسخه جاری آنان با مقدار `after` R2 منطبق است. نسخه‌های قبل از R2 در پوشه Backup داخلی ZIP همین تطابق را نشان می‌دهند.

## نصب ایمن، Windows CMD

فایل `FEE3-UI-R8-R2-B1-PATCH.zip` را **ابتدا** در ریشه پروژه استخراج کنید؛ خود ZIP را به پوشه والد منتقل نمایید، نه در Root پروژه. سپس:

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-ui\releases\r8-r2-b1\install.py --preview
python tools\fee3-ui\releases\r8-r2-b1\selftest.py
python tools\fee3-ui\releases\r8-r2-b1\install.py --apply
python tools\fee3-ui\verify-r8-r2-integration.py
```

**خروجی معتبر:** `FEE3_R8_R2_INTEGRATION_PASS=12 R2_FILES=19 FAIL=0`.

- اسکریپت تاریخی `verify-r8-source-compatibility.py` **تغییر داده نمی‌شود**. در نسخه تلفیقی R2، خطای آن انتظار می‌رود؛ آن اسکریپت فقط برای R8 خالص معتبر است.
- ابزار جدید در صورت عدم انطباق Manifest تاریخی R8، Manifest R2، فایل تاریخی verifier، ۹ فایل بدون تغییر R8 یا حتی یک فایل از ۱۹ فایل R2، Fail-closed است.
- اگر بخشی از R2 در آینده به‌صورت رسمی تغییر کند، باید Baseline تازه با شواهد ارائه شود؛ **هیچ Hash را صرفاً برای سبز شدن آزمون عوض نکنید.**
- اجرای این ابزار **فقط کنترل Source Integrity** است. Build واقعی Angular AOT، Maven، مرورگر، Oracle، UAT، امنیت Configuration و مجوز مالی را تأیید نمی‌کند.
- عدم انطباق یا نتیجه `CONFLICT` به معنای توقف نصب است. هیچ گزینه `--force` وجود ندارد.

## کنترل‌های تکمیلی پس از Gate جدید

```bat
python tools\fee3-ui\verify-reference-r8.py
python tools\fee3-ui\verify-r8-hf1-aot-date-types.py
node tools\fee3-backend\verify-r2.mjs
node tools\fee3-backend\verify-r2-mapping.cjs
bin\stop.cmd
build-production.cmd
```

پس از Build موفق و بررسی تنظیمات امن می‌توان `bin\start.cmd` را طبق روال UAT اجرا کرد. **هیچ Write/Workflow/Posting نباید به واسطه این Patch فعال شود.**

## Security gate مستقل

در سورس دریافت‌شده، هر دو مسیر `backend/src/main/resources/application.yml` و `config/application.yml` حاوی تنظیمات پیش‌فرض دارای حساب ممتاز و رمز صریح هستند. این Patch برای جلوگیری از قطع اتصال توسعه‌ای آن‌ها را خودکار تغییر نمی‌دهد. قبل از اجرای محیط عملیاتی باید DBA و تیم امنیت، Externalization اعتبارنامه‌ها، حساب حداقل دسترسی، Rotation و جلوگیری از نگهداری Secret در Release را تأیید کنند. رمز یا Config محرمانه را در گزارش‌های تست ارسال نکنید.
