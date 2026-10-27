# FEE3-R3-QA2 — رفع مانع آزمون Runtime فقط‌خواندنی

**مبنای این مرحله:** در اجرای قبلی، `curl.exe /api/v1/fee3/readiness` موفق بود و ۲۹۷ قلم و ۳۰۰ نسخه Oracle را گزارش کرد، اما اسکریپت Python پیش از اولین کنترل با `RemoteDisconnected` شکست خورد (`PASS=0 FAIL=1`). این رفتار **می‌تواند** ناشی از Proxy سیستم/محیط Python باشد، اما علت در سیستم مقصد هنوز قطعی نشده است.

## دامنه این بسته

- بسته **فقط ابزار آزمون جدید** اضافه می‌کند. هیچ فایل `backend/src`، `frontend/src`، `pom.xml`، تنظیم اتصال یا Schema/Seed تغییر نمی‌کند.
- نسخه R3-QA1 دست‌نخورده باقی می‌ماند؛ اجرا از مسیر جدید `r3-qa2` است.
- ارتباط مستقیم با `127.0.0.1` بدون استفاده از Proxy محیط/سیستم (`ProxyHandler({})`).
- Redirect HTTP به هر مقصدی (حتی محلی) مسدود است؛ هیچ Secret/Header احراز هویت ارسال نمی‌شود.
- مسیر و فعل هر فراخوانی در کنسول چاپ می‌شود (`HTTP_STEP_START`, `HTTP_STEP_PASS`, `HTTP_STEP_FAIL`). خطاهای HTTP شامل Status و `errorCode` مجاز بدون افشای بدنه کامل پاسخ هستند.
- تنها POSTهای مجاز: `validation` و `preview`، هر دو بدون ثبت در پایگاه داده. هیچ API مربوط به `drafts`، `workflow submit/decision`، انتشار یا Posting فراخوانی نمی‌شود.
- در صورت فعال بودن مجوز نوشتن یا گردش‌کار، **قبل از ارسال هر POST** آزمون متوقف می‌شود.
- مقایسه تعداد رکوردها اصلاح شد: فیلدهای واقعی Readiness عبارت‌اند از `calcItemCount` و `tariffVersionCount`، نه `itemCount` و `versionCount`. نبود فیلد شمارش نیز اکنون موجب رد آزمون می‌شود.

## نصب در Windows CMD

ابتدا فایل ZIP این نسخه را در ریشه پروژه استخراج کنید. هیچ فایل هم‌نامی از R3-QA1 نباید جایگزین شود.

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-backend\qualification\r3-qa2\verify-kit.py
python tools\fee3-backend\qualification\r3-qa2\selftest.py
```

کنترل اول یکپارچگی فایل‌های QA2 و کنترل دوم ۱۱ سناریوی مستقل با HTTP Mock محلی را آزمایش می‌کند. **قبولی Mock به معنای قبولی API واقعی نیست.**

اگر سرویس روی پورت 8091 روشن است:

```bat
curl.exe --noproxy "*" -i http://127.0.0.1:8091/api/v1/fee3/readiness
call tools\fee3-backend\qualification\r3-qa2\run-runtime-readonly.cmd
```

گزارش جدید در `docs\install\FEE3-R3-QA2-RUNTIME-RESULT.json` ذخیره می‌شود. نتیجه قبلی QA1 حفظ می‌گردد.

## تفسیر نتیجه

- `HTTP_STEP_FAIL GET /readiness`: مشکل در اولین اتصال مستقیم Python یا سرویس وب. اگر `curl --noproxy` موفق است، برای تعیین تفاوت محیط Python و curl به لاگ سرور و جزئیات خطا نیاز داریم؛ **نباید** خطا را خودکار به Proxy نسبت داد.
- `HTTP_STEP_FAIL GET /items...` یا `GET /references...`: مسیر و کد HTTP را با API و لاگ Spring Boot تطبیق دهید.
- `HTTP_STEP_FAIL POST /validation` یا `/preview`: خطای این API مشخص شده؛ از فعال‌سازی دسترسی نوشتن برای دورزدن مشکل خودداری کنید.
- `FAIL | Real Oracle item/version counts unchanged...`: شمارش اولیه و نهایی در گزارش ثبت می‌شود؛ تغییر هم‌زمان توسط کاربران دیگر نیز باید بررسی شود.
- `FEE3_R3_QUALIFICATION_PASS=N FAIL=0`: آزمون Runtime فقط‌خواندنی **در همان اجرا** موفق بوده است. برای پذیرش نهایی، Oracle SQL پیش و پس از آزمون نیز باید بدون تفاوت باشد.

## حفظ محدودیت‌های بانکی

- `FEE3_R3_WORKFLOW_ENABLED=false`
- `FEE3_R3_CHECKER_DECISIONS_ENABLED=false`
- `FEE3_R3_UAT_APPROVAL_ENABLED=false`
- `FEE3_DRAFT_WRITES_ENABLED=false` (یا تنظیم معادل در نصب فعلی)

این بسته موتور محاسباتی و قواعد تعرفه را تغییر نمی‌دهد؛ همچنین هیچ Maker/Checker، انتشار، وصول یا ثبت حسابداری را فعال نمی‌کند.

**شواهد موردنیاز در صورت خطا:** خروجی کامل کنسول QA2 و فایل `FEE3-R3-QA2-RUNTIME-RESULT.json`، به‌علاوه بخش مربوط از لاگ Spring Boot. رمز، Token، اطلاعات مشتری و رشته اتصال Oracle را پیش از ارسال حذف کنید.
