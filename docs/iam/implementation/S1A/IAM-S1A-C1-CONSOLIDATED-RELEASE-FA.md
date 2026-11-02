# IAM-S1A-C1 — یادداشت انتشار تجمیعی

**تاریخ:** ۲۰۲۶-۱۱-۰۲  
**Baseline:** نسخه ۱۷:۵۱ `core-banking-prototype-v0-2026-11-02-1751.zip`  
**نوع:** Developer UI Mock + Release Integrity (بدون Backend Write و Oracle Write)

## علت خطای پیشین

در ZIP ساعت ۱۷:۵۱، مسیر `iam/user-management` و فایل Angular مربوط به آن وجود نداشت. Redirect پیشین به `dashboard` از fallback مسیر `**` رخ می‌داد. نصب‌نشدن Patchهای پیشین در Repository ارسالی اثبات شده است؛ **علت اینکه نصب محلی عملاً اعمال نشده، با شواهد موجود قابل تعیین نیست**.

## تغییرات واقعی

- route Lazy Loading داخل `frontend/src/app/app.routes.ts`، منوی مستقل IAM در App Shell، گروه فعال در Sidebar R22 و Breadcrumb.
- شبیه‌ساز مدیریت حساب با کنترل Username، تفکیک Domain/Context، UUIDv4، تاریخچه رخداد در نشست، و خطاهای منفی؛ اطلاعات واقعی وارد نشود.
- کنترل‌های `SOURCE`، `DIST`، `STATIC`، `JAR` و `HTTP` متصل به Build/Run.
- Artifact Proof با Hash سورس و Hash فایل‌های JS برای جلوگیری از پذیرش JAR قدیمی با Version مساوی.
- نسخه‌دار کردن نقشه راه Canonical و تفکیک شواهد R0.1/R1.5.1/R1.3؛ SQL Read-only فقط برای DBA.
- سازگاری verifier قدیمی R22 با مسیر موجود FEE3، بدون تغییر ناوبری FEE3.

## معیار پذیرش و شواهد

| Gate | شواهد قابل انتظار | وضعیت در محیط توسعه بسته |
|---|---|---|
| Source IAM | `IAM_UI_SOURCE_VISIBLE_PASS` | اجرا شده |
| Business Domain | `IAM_S1A_DEV_QA_PASS=17/17` | اجرا شده |
| R22 Navigation | `R22_SIDEBAR_VERIFY_PASS` | اجرا شده |
| Angular build | `IAM_UI_DIST_VISIBLE_PASS` | نیازمند نصب npm وابستگی‌ها؛ در محیط جاری غیرفعال |
| Static copy | `IAM_UI_STATIC_VISIBLE_PASS` | نیازمند Build ویندوز |
| Runtime JAR | `IAM_S1A_C1_JAR_PASS` | نیازمند Maven build |
| Running HTTP | `IAM_UI_HTTP_PASS` | نیازمند اجرای Java محلی |
| Browser UAT | مستقیم `#/iam/user-management` بدون Redirect | کاربر/تحلیلگر پس از نصب |

## قاعده ایمنی

فرم، دسترسی و لینک واقعی به اشخاص ایجاد نمی‌کند. هیچ Credential، داده واقعی CIF/ORG یا PII نباید در محیط نمایشی وارد شود. SQL R0.1 و DDL Export R1.5.1 اجرا نشده و به‌عنوان Migration نباید اجرا شوند.

## گام بعد

پس از Build و تأیید UI: طراحی Security ADR برای Backend و Actor Trust Boundary، Read-only Oracle As-Is و سپس طراحی Migration Delta با مجوز DBA/امنیت. فاز IAM-01 هنوز باز است.
