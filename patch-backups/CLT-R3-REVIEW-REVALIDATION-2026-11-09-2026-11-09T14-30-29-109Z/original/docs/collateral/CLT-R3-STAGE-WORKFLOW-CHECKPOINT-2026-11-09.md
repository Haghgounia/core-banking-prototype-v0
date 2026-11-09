# CLT R3 — پیاده‌سازی گردش درخواست‌های مرحله‌ای ضامن، بیمه و ارزیابی

**تاریخ:** ۲۰۲۶-۱۱-۰۹  
**مبنای سورس واقعی:** `core-banking-prototype-v0-2026-11-09-1618.zip` (آخرین نسخه کامل تحویلی کاربر)  
**آخرین نصب موفق تأییدشده توسط کاربر:** `CLT-R3-SHARED-DATASOURCE-INTAKE-HF2-2026-11-09`  
**شناسه تحویل:** `CLT-R3-STAGE-WORKFLOW-2026-11-09`  
**وضعیت:** SOURCE IMPLEMENTED / DATABASE MIGRATION NOT APPROVED / LIVE ORACLE NOT VERIFIED / BUSINESS UAT DEFERRED

## دامنه پیاده‌سازی

- حفظ سه سرویس موجود R3 و DataSource مشترک در حالت Loopback، بدون حساب یا Starter جدید.
- تکمیل اجازه محلی ثبت اولیه **فقط در جدول‌های Stage** ضامن، بیمه و ارزیابی که قبلاً در Security Chain اجازه POST نداشتند. کنترل دسترسی actor، branch و اعتبارسنجی داخل سرویس‌ها برقرار است.
- ایجاد گردش مشترک درخواست‌های Stage: `DRAFT → SUBMITTED → REVIEWED / REJECTED`، و انصراف `DRAFT / SUBMITTED → WITHDRAWN`.
- ثبت توضیح اجباری هر انتقال در `DECISION_REASON` و درج رویداد به‌صورت Append-only با همان `SNAPSHOT_HASH`، `EVENT_ACTOR`، `CORRELATION_ID` و `EVENT_AT`.
- کنترل دوباره Hash داده ذخیره‌شده با بازسازی Candidate از Oracle، کنترل رویداد ایجاد، شماره نسخه و اجرای انتقال + Audit در یک تراکنش Oracle.
- در زمان `SUBMIT`، اجرای دوباره اعتبارسنجی فعلی BUC/SUC از سرویس اصلی همان دامنه با CIF و مراجع بانک (Fail-closed).
- کارتابل یکپارچه Maker/Reviewer در کامپوننت مشترک R3، بدون تغییر HTML سفارشی سه فرم.
- بازبین مستقل باید اختیار `SCOPE_clt.collateral.review` داشته باشد و **نباید همان ثبت‌کننده باشد**. پروفایل محلی تک‌کاربره فعلی به‌عمد این اختیار را برای تأیید مستقل صادر نمی‌کند. هیچ IAM جدیدی ساخته نشده است.

## مرز بسیار مهم

`REVIEWED` فقط «ثبت بازبینی داخلی در Stage» است؛ **ثبت قطعی، تأیید بانکی، تمدید مؤثر بیمه، اعتبار ضامن برای تخصیص و تعیین ارزش قابل تخصیص را فعال نمی‌کند**. این تحویل هیچ `INSERT/UPDATE/DELETE` در `CLT.COLLATERAL_SPONSOR`، `CLT.COLLATERAL_INSURANCE`، `CLT.COLLATERAL_VALUATION_HISTORY` یا جدول اصلی وثیقه ندارد.

## Migration پیشنهادی ـ اجرانشدنی

مسیر: `database/oracle/clt/r3workflow/01_R3_STAGE_TRANSITIONS_CANDIDATE_NOT_APPROVED.sql`. فایل با `RAISE_APPLICATION_ERROR` شروع می‌شود و دستورهای نمونه داخل بلوک Comment هستند. **نه نصاب، نه Build، نه Starter هیچ DDL/DML این فایل را اجرا نمی‌کنند.** جداول اصلی Stage هنوز خارج از ۲۶ جدول مدل EA مصوب و نیازمند Change Control و تصویب DBA هستند. به‌روزرسانی Check Constraint وضعیت/رویداد و اضافه‌شدن `DECISION_REASON` لازم است؛ Backend هنگام نبود این پیش‌نیازها HTTP 503 و UI «آماده نیست» نشان می‌دهد.

## APIهای جدید (فقط Local Prototype)

| Method / Path | کارکرد | وضعیت |
|---|---|---|
| GET `/api/v1/collaterals/r3-workflow/{kind}/readiness` | بررسی آمادگی Oracle Stage Workflow | کد پیاده شد |
| GET `/api/v1/collaterals/r3-workflow/{kind}/pending` | صف انتظار بازبین مستقل در شعبه | کد پیاده شد |
| POST `/api/v1/collaterals/r3-workflow/{kind}/{requestId}/transition` | SUBMIT / WITHDRAW / REVIEW / REJECT با `expectedVersion` و `reason` | کد پیاده شد |

`kind` محدود به `sponsor`، `insurance` و `valuation` است؛ شناسه‌های SQL هرگز از ورودی HTTP ساخته نمی‌شوند. هیچ مسیر Finalize در API افزوده نشده است.

## شکاف‌ها و معیارهای اعلام اتمام

| Gap / فاز | وضعیت بعد از توسعه | گیت باقی‌مانده |
|---|---|---|
| R2 / G-01,G-03,G-12,G-14,G-17 | تغییری در ثبت قطعی ایجاد نشد | Migration و تصمیم هویت/شماره باز |
| R3 / G-05,G-11 | گردش Stage نامزد ضامن | تعداد حداقل ضامن، چرخه عملیاتی لغو/تأیید نهایی باز |
| R3 / G-04,G-06 | گردش Stage درخواست بیمه | پوشش اجباری، تجدید، لغو و بیمه‌گر مصوب باز |
| R3 / G-07,G-18 | گردش Stage ارزیابی | `FINAL` canonical و دوره تناوب/اعتبار ارزیابی باز |
| R3 / G-09 | مسیر مشترک Oracle در Loopback | یکپارچه‌سازی واقعی Party/DMS/Consumer و امنیت Enterprise باز |
| R6/R7 | تعریف فاز بدون تغییر | UAT جامع و آمادگی عملیاتی در پایان توسعه |

## شواهد و محدودیت تحویل

- نسخه بسته بر اساس SHA-256 دقیق ZIP کاربر تهیه می‌شود؛ هر فایل ناسازگار پیش از کپی رد می‌شود.
- آزمون‌های Java Policy، کنترل Syntax TypeScript، سازگاری API و نصب/بازگردانی باید همراه گزارش QA تحویل ثبت شوند.
- **Build کامل Windows و اتصال زنده Oracle در محیط تولید بسته اجرا نشده‌اند.** موفقیت نصب سورس معادل UAT، تصویب Migration یا ثبت قطعی وثیقه نیست.
- عنوان و ترتیب فازهای **R0 تا R7** و مستند مرجع `CLT-ROADMAP-R1.0-FA.md` حفظ شده‌اند.
