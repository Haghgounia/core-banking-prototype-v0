# FEE3-BE-R4C3 — نصب و آزمون کارمزد قرض‌الحسنه (Preview فقط‌خواندنی)

**تاریخ مرجع:** 2026-10-27  
**پیش‌نیاز:** Core Banking Prototype 0.11.0، پذیرش واقعی R4-C2 و نصب کامل Patch R4-C2.  
**دامنه:** فقط Backend FEE3؛ بدون تغییر Oracle، UI، FEE، FEE2 یا Product Builder.

## ۱. محدودیت و هدف

R4-C3 محاسبه و *تطبیق احتمالی زمان مطالبه* چهار قلم قرض‌الحسنه را به‌عنوان شبیه‌سازی غیرماندگار ارائه می‌کند. این سرویس **نه مطالبه ایجاد می‌کند، نه تاریخ قطعی مطالبه را از سامانه تسهیلات می‌خواند، نه سوابق مطالبه/وصول را بررسی می‌کند، نه بدهی ثبت می‌کند و نه مجوز وصول وجه است**. همه نرخ‌های ورودی آزمون فرضی و فاقد تأیید هستند.

API تخصصی:

`POST http://127.0.0.1:8091/api/v1/fee3/r4/qard/preview`

فقط عملیات Oracle `LOAN/QARD_FEE` و ماهیت `QARD_FEE` و عقد `QARD_HASANA` مجازند. فرمول ثابت allowlist شده `TPL_E624746F262F6E` در Oracle باید فعال باشد؛ متن فرمول اجرا یا تفسیر نمی‌شود.

## ۲. رویدادها و مبنای روزشمار

| مدل API `loanModelProfile` | `qardChargeTiming` | `yearStartBasis` | کنترل |
| --- | --- | --- | --- |
| `ON_ANNIVERSARY` | `ON_ANNIVERSARY` | `UNDECIDED` | همان ماه و روز جلالی مبدأ عقد در سال بعد، با اطلاعات CAL2 |
| `FIRST_INSTALLMENT_OF_PERIOD` | `FIRST_INSTALLMENT_OF_PERIOD` | `UNDECIDED` | `firstInstallmentDate` برابر روز رویداد؛ تأیید واقعی جدول اقساط انجام نمی‌شود |
| `JALALI_YEAR_START` | `START_OF_YEAR` | `JALALI_CALENDAR_YEAR` | روز اول فروردین طبق CAL2 |
| `CONTRACT_YEAR_START` | `START_OF_YEAR` | `LOAN_CONTRACT_YEAR` | ماه و روز آغاز قرارداد در سال جاری طبق CAL2 |

فرمول شبیه‌سازی **مبلغ × نرخ قراردادی / ۱۰۰ × کسر سال جلالی** است. مخرج سال از پوشش ردیف‌های `CAL2` با ۳۶۵ یا ۳۶۶ روز استخراج می‌شود، نه از ورودی `yearFraction` یا سال میلادی. تاریخ انتهای دوره غیرشامل است. از `BigDecimal` و گردکردن صریح نسخه استفاده می‌شود. سایر روش‌های محاسبه، نرخ ثابت تعرفه، مالیات تأییدنشده، معافیت، بخشودگی، محاسبه خسارت تأخیر و سود قرارداد همگی خارج از محدوده‌اند.

## ۳. واقعیت مهم درباره عدم مطالبه تکراری

مقدار `candidateAssessmentKey` یک اثرانگشت *پیشنهادیِ دوره و نوع رویداد* است. این مقدار بین درخواست‌های تکراری و حتی در صورت تغییر مبلغ/نرخ، تا زمانی که شناسه قرارداد، مدل و تاریخ‌ها ثابت باشند، تکرار می‌شود. اما چون هیچ سرویس تاریخچه قطعی مطالبه، گردش‌کار یا وصول متصل نیست، **این کلید نه تضمین یکتایی بانکی است و نه نشان می‌دهد دوره قبلاً مطالبه نشده**. خروجی صریحاً:

- `duplicateCheckStatus = NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY`
- `PREVIOUS_CHARGE_HISTORY_NOT_CHECKED`
- `NO_DUPLICATE_CERTIFICATION`
- `NOT_A_CHARGE_AUTHORIZATION`

است. برای فعال‌سازی عملیاتی، اتصال خواندنی معتبر به دفتر مطالبات/چرخه عمر تسهیلات و کنترل یکتایی اتمیک در لایه ایجاد مطالبه ضروری است؛ در R4-C3 انجام نشده است.

## ۴. نصب در Windows CMD

ابتدا `FEE3-R4C3-PATCH.zip` را در ریشه `D:\Projects\core-banking-prototype-v0` قرار دهید. سپس:

```bat
cd /d D:\Projects\core-banking-prototype-v0
tar -xf FEE3-R4C3-PATCH.zip
python tools\fee3-backend\releases\r4c3\install.py --preview
```

در صورت `FEE3_R4C3_PREVIEW_PASS_NO_CHANGES` و نبود `CONFLICT`:

```bat
python tools\fee3-backend\releases\r4c3\install.py --apply
python tools\fee3-backend\r4c3\verify-r4c3.py
python tools\fee3-backend\r4c3\verify-java.py
python tools\fee3-backend\r4c3\run-pure-java.py
python tools\fee3-backend\r4c3\selftest.py
```

**حتماً ZIP را پیش از Build از ریشه پروژه خارج کنید**؛ در غیر این صورت Release Layout Verification متوقف می‌شود:

```bat
move FEE3-R4C3-PATCH.zip ..\FEE3-R4C3-ARCHIVE.zip
bin\stop.cmd
build-production.cmd
bin\start.cmd
```

`bin\start.cmd` را فقط پس از موفقیت `build-production.cmd` اجرا کنید. هیچ حذف یا جایگزینی فایل قدیمی لازم نیست.

## ۵. آزمون واقعی فقط‌خواندنی

در یک CMD جدید پس از راه‌اندازی Backend:

```bat
cd /d D:\Projects\core-banking-prototype-v0
curl.exe --noproxy "*" http://127.0.0.1:8091/api/v1/fee3/readiness
call tools\fee3-backend\r4c3\run-runtime-readonly.cmd
```

قبل از نخستین POST آزمون باید `schema=FEE3`, `readsEnabled=true`, `missingTables=[]`, `draftWritesEnabled=false` و تمام کلیدهای گردش‌کار/ثبت مالی برابر `false` باشند؛ در غیر این صورت ابزار پیش از POST متوقف می‌شود.

برنامه Runtime دارای **۳۵ کنترل** با دو نمونه موفق مدت‌دار، آزمون رد سالگرد همان‌سال و رد روز عادی برای اول فروردین، آزمون‌های منفی امنیتی، رد تقلب در نرخ/مدارک، کنترل بازپخش SHA و عدم تغییر شمارش ۲۹۷/۳۰۰ است. در صورت موفقیت واقعی انتظار می‌رود:

```text
FEE3_R4C3_RUNTIME_PASS=35 FAIL=0
REPORT=D:\Projects\core-banking-prototype-v0\docs\install\FEE3-R4C3-RUNTIME-RESULT.json
```

۳۵/۳۵ در محیط Mock تنها آزمون ابزار است، نه اثبات Oracle. موفقیت Runtime واقعی همچنان به اجرای شما وابسته است. کنترل `CAL2` در ۱۰ روز فعلی به معنی اثبات همه دوره‌ها و سال‌های جلالی نیست.

## ۶. SQL کشف داده — فقط SELECT

فایل `docs\install\FEE3-R4C3-ORACLE-READONLY-VERIFY.sql` را در SQL Developer با `F5` اجرا کنید. خروجی A تا E را برای ارزیابی چهار ردیف Oracle، سیاست زمانی، وضعیت منبع و روز فعلی CAL2 نگه دارید. DML/DDL/COMMIT در این بسته وجود ندارد.

**نکته انطباق:** چهار نسخه Seed فعلی `taxMode=UNREVIEWED` و در JSON مقدارهای پروفایل `loanModelProfile=null` دارند. مسیر تخصصی R4-C3 فقط با وضعیت مالیات `OUT_OF_SCOPE` و پروفایل صریح UAT شبیه‌سازی می‌کند. لذا نمی‌تواند نسخه Seed را بی‌مداخله به یک نسخه قابل مطالبه تبدیل کند؛ تبدیل این وضعیت به تعرفه واقعی نیازمند تصمیم مستقل کسب‌وکار و کنترل سازگاری Oracle است.

## ۷. محدودیت پذیرش

پذیرش Read-only Runtime، **تأیید نرخ قرض‌الحسنه، صحت برنامه واقعی اقساط، مجاز بودن مطالبه، عدم مطالبه قبلی، تاریخ قانونی مطالبه، محاسبه مالیات یا مجوز حسابداری نیست**. هیچ عملیات نوشتنی یا برداشت پول در R4-C3 فعال نمی‌شود.
