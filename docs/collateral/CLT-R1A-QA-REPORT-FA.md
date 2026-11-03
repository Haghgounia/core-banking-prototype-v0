# گزارش کنترل کیفیت CLT-R1A — 2026-11-03

**مرجع:** CLT-ROADMAP-R1.0 | **تحویل:** R1A (Reference Tables Read-Only)

## شواهد اجراشده در محیط تهیه بسته

| کنترل | نتیجه | شاهد |
|---|---|---|
| صحت فایل Baseline | PASS | محتوای `VERSION`, `backend/pom.xml`, `app.routes.ts`, Shell HTML، Sidebar State و `application.yml` با ZIP آپلودی عیناً مقایسه شد |
| تطبیق داده‌ها با CLT Export | PASS | نام و ترتیب **۲۰۴ ستون در ۱۷ جدول** با `CLT-2026-11-1.txt` برابر بود |
| تطبیق Seed فنی | PASS | ۵۳ Insert در ۱۱ جدول مطابق فایل Seed؛ ۶ جدول کسب‌وکار فاقد Seed |
| نصب روی کپی Baseline | PASS | ۲۱ فایل محموله جدید + ۳ ادغام دقیق در Frontend؛ هیچ تغییر روی IAM/CIF/DPS2/FEE/PDL و ابزار اصلی |
| اجرای تکراری Installer | PASS | `PLANNED_FILES=0` (Idempotent) |
| برخورد با فایل Collateral تغییرکرده | PASS | نصب در Precheck با پیام `EXISTING_CLT_FILE_DIFFERS` متوقف می‌شود |
| کنترل Read-only | PASS (ایستا) | API فقط GET؛ خواندن از SELECT-only Credential مستقل؛ فاقد DDL/DML در Install و SQL Preflight؛ `reads-enabled=false` |
| کنترل فراداده | PASS | ۱۷ جدول، ۲۰۴ ستون، ۵۳ پیش‌نمایش با Verify.cjs |
| Syntax Java | PASS | ۱۰ فایل (۹ کلاس اصلی + ۱ JUnit) با JDK Compiler Parser بررسی شدند |
| Syntax TypeScript | PASS | ۳ فایل TS با TypeScript ۵.۹ Global Parser/Transpiler بدون خطای نحوی |
| Maven Compile و اجرای JUnit | NOT RUN | Maven در این محیط نصب نیست؛ Maven Wrapper به مخزن توزیع Maven دسترسی ندارد |
| Angular Template Typecheck و Build | NOT RUN | `frontend/node_modules` و Angular CLI محلی در محیط ارزیابی موجود نبودند |
| Oracle Readiness/Runtime | NOT RUN | اتصال Oracle کاربر و Credential SELECT-only در این محیط موجود نیست |
| UAT عملکردی مرورگر | NOT RUN | نیازمند نصب و اجرای نرم‌افزار در Workspace یکپارچه |

## تغییرات مشترک واقعی

1. `frontend/src/app/app.routes.ts`: افزودن دو Route مربوط به `/collateral/reference-data` و `/collateral/reference-data/:table`؛ بدون تغییر Routeهای قبلی.
2. `frontend/src/app/layout/app-shell.component.html`: افزودن گروه منوی «مدیریت وثایق».
3. `frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts`: افزودن مسیر `/collateral` به منطق Active Group برای بازشدن منوی مرتبط.

سایر فایل‌های موجود Baseline بدون تغییر باقی ماندند. جزئیات خط‌به‌خط در `SHARED-INTEGRATION-DIFF.patch` داخل بسته آمده است.

## محدودیت تحویل

این گزارش **تأیید Build کامل یا UAT بانکی نیست**. پس از نصب باید توسط Pipeline پروژه یک Maven Compile/Test و Angular Build، سپس با Oracle Reader واقعی تست GET و Readiness انجام شود. هیچ Test یا Oracle Write واقعی در این محیط اجرا نشده است.

**گام بعد:** R1B (Audit/change governance + CRUD ستادی بعد از حل G-10 و G-15).
