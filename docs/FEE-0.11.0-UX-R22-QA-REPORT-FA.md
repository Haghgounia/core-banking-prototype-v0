# FEE UX R2.2 — گزارش آزمون مستقل

**مبنای تطبیق:** سورس ۲۱:۵۳ مورخ ۱۴۰۵/۰۷/۱۹، شامل FEE R2.1، Product Builder R29 و اصلاح منوی NAV-R22. آزمون سازگاری با NAV-R22.1 نیز انجام شد.

## کنترل‌های تأییدشده در محیط بررسی

| آزمون | نتیجه |
|---|---|
| `verify-fee-admin-baseline.mjs` (۴۷ جدول/فرم، ۵۷۴ رکورد Seed) | PASS |
| `verify-fee-ux-r1.mjs` | PASS |
| `verify-fee-ux-r2.mjs` | PASS |
| `verify-fee-ux-r21.mjs` | PASS |
| `verify-fee-ux-r22.mjs` | PASS |
| `verify-fee2-forms.mjs` | PASS |
| `verify-fee2-studio.mjs` | PASS |
| `verify-pdl-r29-finalization.mjs` (شامل ۱۹ کنترل Java) | PASS |
| `verify-r22-sidebar.mjs` و `verify-nav-r221.mjs` | PASS |
| `verify-release-layout.mjs` | PASS |
| Syntax Transpile دو فایل TypeScript اصلاح‌شده | PASS |
| نصب بار اول و نصب تکراری بدون تغییر اضافی | PASS |
| بازگشت و نصب دوباره | PASS |
| شناسایی تغییر غیرمنتظره FEE و توقف پیش از بازنویسی | PASS |
| مقایسه باینری با نسخه ۲۱:۵۳: فقط شش فایل FEE تغییر کرده‌اند | PASS |

**حدود اطمینان:** این کنترل‌ها اثبات Build کامل Angular و Spring، ارتباط Oracle، صحت همه عملیات CRUD در Runtime، یا نمایش بصری در مرورگر نیستند. آن مراحل باید روی ماشین Windows و بانک اطلاعاتی مربوطه اجرا شوند.

## محدوده اصلاح

فقط شش فایل زیر در `frontend/src/app/features/fee-admin/` پس از اجرای نصب‌کننده تغییر می‌کنند:

- `fee-design-studio.component.ts`
- `fee-design-studio.component.html`
- `fee-design-studio.component.scss`
- `fee-admin-table.component.ts`
- `fee-admin-table.component.html`
- `fee-admin-table.component.scss`

محتوای دیگر پروژه، شامل FEE2، Product Builder، AppShell، جداول Oracle، API و اسکریپت‌های Build دست‌نخورده می‌ماند.
