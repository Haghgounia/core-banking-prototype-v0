# Collateral CLT-R1B — نام‌گذاری پایدار و گردش تغییرات اطلاعات پایه

**مرجع برنامه:** `CLT-ROADMAP-R1.0` (بدون تغییر فازهای R0..R7)  
**بسته:** `CLT-R1B` | **تاریخ:** 2026-11-03 | **Baseline:** Core Banking Prototype 0.11.0 + CLT-R1A  
**حدود:** Refactor نام‌گذاری + گردش پیشنهاد/تأیید مستقل برای ۶ جدول کسب‌وکار؛ ۱۱ جدول کدینگ فنی همچنان Read-only.

## ۱. تصمیمات قطعی و سازگاری

- ۹ کلاس اصلی Java و یک کلاس تستِ دارای نام `CltR1a*`، به کلاس‌هایی با نام پایدار و دامنه‌محور تبدیل می‌شوند. فایل‌های Java قدیمی پس از کنترل SHA-256 فقط در نصب کنترل‌شده حذف می‌شوند. هیچ فایل Java مشترک یا ماژول IAM دست‌کاری نمی‌شود.
- تنظیمات از `clt/r1a.properties` به `clt/reference.properties` منتقل می‌شود. **نام‌های قدیمی متغیر محیطی `CLT_R1A_*` صرفاً برای سازگاری خواندن حفظ شده‌اند.** پیشوند نام ویژگی‌های جدید `core-banking.collateral.reference.*` است.
- قراردادهای فعلی GET در `/api/v1/collateral/reference/**`، فایل JSON کاتالوگ ۱۷ جدولی و منوی فعلی بدون شکستن حفظ می‌شوند. Markerهای Integration با شناسه R1A سابقه نصب‌اند و نام کلاس به حساب نمی‌آیند.
- `CLT_REFERENCE_READS_ENABLED=false` و `CLT_REFERENCE_WRITES_ENABLED=false` مقادیر پیش‌فرض‌اند؛ هیچ اتصالی به Oracle یا IdP بدون Opt-in برقرار نمی‌شود.
- Installer نه Build اجرا می‌کند، نه DDL/DML، نه Grant، نه Seed؛ فقط فایل‌های سورس و اسناد را نصب می‌کند.

## ۲. مرز کسب‌وکار و اسکیمای فیزیکی

| نوع جدول | جدول‌ها | عملیات پشتیبانی‌شده |
|---|---|---|
| کسب‌وکار / ستاد | `COLLATERAL_GROUP`, `COLLATERAL_TYPE`, `COLLATERAL_ATTRIBUTE_DEFINITION`, `COLLATERAL_TYPE_ATTRIBUTE`, `COLLATERAL_INSURANCE_TYPE`, `INSURANCE_COMPANY` | پیشنهاد CREATE/UPDATE، تصمیم Checker، Audit؛ **فقط با Writer فعال و Migration مصوب** |
| کدینگ فنی | ۱۱ جدول `REF_*` از کاتالوگ R1A | خواندن و پیش‌نمایش Seed فنی؛ POST برای آن‌ها ممنوع |

**G-10 باز:** ضرایب و Policyهای نمونه HTML نسخه V1.42، مصوبه بانک نیستند؛ در Oracle Seed نشوند. **G-15 پیشنهادی:** دو جدول مجزای `CLT_REFERENCE_CHANGE_REQUEST` و `CLT_REFERENCE_CHANGE_AUDIT` به مدل فعلی ۲۶ جدولی اضافه *می‌شوند اگر Migration مستقلاً تصویب و اجرا شود*. اجرای کد به‌معنای تصویب DDL نیست.

## ۳. قرارداد API پیاده‌شده (در حالت پیش‌فرض DENY ALL)

| Method / Route | مجوز لازم | رفتار |
|---|---|---|
| GET `/api/v1/collateral/reference/catalog`, `/readiness`, `/{table}`, `/{table}/{id}` | `clt.reference.read` | سازگار با R1A؛ با Oracle Reader مستقل |
| GET `/api/v1/collateral/reference/governance` | `clt.reference.read` | نمایش Feature Gate بدون افشای Credential |
| GET `/api/v1/collateral/reference/changes?status=PENDING_APPROVAL` | `clt.reference.review` | صف حداکثر ۴۰ درخواست؛ فقط با Writer فعال |
| POST `/api/v1/collateral/reference/changes` | `clt.reference.propose` | ایجاد درخواست تغییر با `Idempotency-Key` و دلیل اجباری؛ بدون تغییر جدول اصلی |
| POST `/api/v1/collateral/reference/changes/{id}/decisions` | `clt.reference.approve` | `APPROVE` یا `REJECT`؛ بررسی `maker != checker`، نسخه مبنا و Apply + Audit اتمیک |

- همه مسیرها Bearer JWT معتبر با RS256، Issuer، Audience و عمر توکن محدود دارند؛ هویت Maker/Checker فقط از JWT اخذ می‌شود، نه از Body یا Header دلخواه.
- شماره و شناسهٔ ردیف فیزیکی به‌صورت رشته حمل می‌شود؛ `RECORD_VERSION` در UPDATE الزامی است. هیچ DELETE فیزیکی یا آپدیت Audit پیاده نشده.
- نام جدول و ستون فقط از allowlist کاتالوگ فیزیکی انتخاب می‌شود؛ ورودی خام کاربر در SQL Identifier قرار نمی‌گیرد. خطاهای دیتابیس در API مخفی می‌شوند.
- عمل APPROVE در یک Transaction شامل `SELECT ... FOR UPDATE`, کنترل `RECORD_VERSION`, به‌روزرسانی/ایجاد و Before/After Audit انجام می‌شود؛ در خطا rollback. درخواست ردشده هیچ داده کسب‌وکاری تغییر نمی‌دهد.

## ۴. Oracle — پیش‌نیازهای مستقل و غیرفعال

۱. DBA باید `00_REFERENCE_GOVERNANCE_READONLY_PRECHECK.sql` را اجرا و با Dictionary واقعی و وضعیت Sequenceهای CLT تطبیق کند.  
۲. `01_REFERENCE_GOVERNANCE_DDL_OPT_IN.sql` فقط پیشنهاد تکمیل **G-15** است، نه Migration تصویب‌شده؛ با Change Ticket و Backup و کنترل نسخه Oracle اجرا شود.  
۳. حساب Reader فقط SELECT روی جداول مرجع؛ حساب Writer مستقل، بدون DBA/DDL، فقط SELECT و DML ضروری شش جدول کسب‌وکار + خواندن/نوشتن درخواست تغییر + INSERT تنها روی Audit + SELECT بر Sequenceهای مشخص. مجوز Oracle توسط DBA تعیین می‌شود.  
۴. پس از آمادگی IdP و DBA، فقط در محیط مجاز: `CLT_REFERENCE_READS_ENABLED=true` و `CLT_REFERENCE_WRITES_ENABLED=true` با تنظیمات `CLT_REFERENCE_*` و دو حساب جدا. استفاده از SYSTEM، SYS، DBA یا حساب مالک CLT ممنوع است.  
۵. **برای محیط عملیاتی بانک فعال نشود** تا G-10 (Policy مصوب)، G-15 (Audit)، IAM/SoD و UAT رسمی تأیید شوند.

## ۵. صفحات Angular

- کاتالوگ ۱۷ جدول و پیش‌نمایش Seed سابق باقی است.
- شش جدول کسب‌وکار «پیشنهاد ایجاد/اصلاح»، «پیش‌نمایش درخواست» و «صف تأیید Maker/Checker» دارند.
- وقتی Writer غیرفعال یا JWT وجود ندارد، پیش‌نویس فقط در مرورگر نمایش داده می‌شود؛ هیچ داده‌ای به‌عنوان ذخیره‌شده اعلام نمی‌شود.
- برای ارسال/تصمیم واقعی، Frontend باید Bearer Token را از IAM/Session معتبر دریافت کند؛ پیاده‌سازی Token Handling سراسری، به عمد به‌جای جعل هویت در این بسته انجام نشده است.

## ۶. آزمون و محدودیت

- آزمون‌های ایستای Installer، اجرای دوم بدون تغییر، کنترل SHA و محدوده فایل، Data Catalog، Java Parser و Pure-Java Policy انجام شوند.
- Build واقعی Maven/Angular، JWT با دو Principal جداگانه، Oracle Migration/Transaction/Rollback، کنترل Privilege و UAT یکپارچه **تا وجود runtime و تأیید DBA** باز هستند؛ نتیجه ایستا هرگز جایگزین UAT بانکی نیست.
- انتشار جداگانه R1B هیچ نسخه محصول یا Runtime JAR نصب‌شده را به‌طور خودکار به‌روزرسانی نمی‌کند. برای اجرا باید طبق فرآیند Master Project Build از سورس جدید انجام شود.

## ۷. وضعیت نقشه راه

**R1A:** سورس خواندنی مرجع نصب شده و ساختار آن حفظ می‌شود.  
**R1B:** نام‌گذاری پایدار + گردش درخواست تغییر و Audit با Feature Flag؛ گیت‌ها و Oracle واقعی باز.  
**R1 Exit Gate:** تا بسته‌شدن G-10/G-15 و شواهد Build/Oracle/IAM/UAT، *ناتمام* است.  
**گام بعد:** تأیید و استقرار گیت‌های R1 و سپس ورود به R2 — شناسنامه و معرفی وثیقه.
