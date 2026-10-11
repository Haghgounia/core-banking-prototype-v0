# گزارش آزمون NAV-R22.1 (Visual Polish)

مبنا: `core-banking-prototype-v0-2026-10-11-2153-nav-r22.zip`، نسخه R22 فعال و تغییرات Product Builder R29 حفظ شده است.

## نتیجه آزمون‌های اجراشده

- `node tools/install-nav-r221.mjs` → `NAV_R221_INSTALL_PASS`
- اجرای دوباره نصب‌کننده → `NAV_R221_ALREADY_INSTALLED`
- `node tools/verify-nav-r221.mjs` → `NAV_R221_VERIFY_PASS`
- `node tools/verify-r22-sidebar.mjs` → `R22_SIDEBAR_VERIFY_PASS`
- `node tools/verify-fee2-studio-menu-cleanup.mjs` → `FEE2_STUDIO_MAIN_MENU_CLEANUP_PASS`
- `node tools/verify-fee2-forms.mjs` → `FEE2_FORMS_VERIFY_PASS`
- `node tools/verify-fee2-studio.mjs` → `FEE2_STUDIO_VERIFY_PASS`
- `node tools/verify-fee-ux-r1.mjs` → PASS
- `node tools/verify-fee-ux-r2.mjs` → `FEE_UX_R2_STATIC_PASS`
- `node tools/verify-fee-ux-r21.mjs` → `FEE_UX_R21_STATIC_PASS`
- `node tools/verify-pdl-r29-finalization.mjs` → `PDL_R29_STATIC_PASS=48 FAIL=0` و `PDL_R29_JAVA_GATE_PASS=19 FAIL=0`
- `node tools/verify-release-layout.mjs` → PASS
- افزودن لینک جدید فرضی Product Builder، بازگشت R22.1، نصب مجدد → PASS؛ لینک جدید محفوظ ماند و SCSS در بازگشت با نسخه اولیه byte-for-byte یکسان شد.
- تغییر عمدی و غیرمنتظره عنوان FEE2 قبل از نصب → توقف امن نصب‌کننده با `NAV_R221_INSTALL_FAIL`، بدون تغییر بایت‌های AppShell و SCSS.

## دامنه فایل‌ها

نصب‌کننده فقط فایل‌های زیر را به طور هدفمند و بدون جایگزینی فایل نسخه قدیمی اصلاح می‌کند:

1. `frontend/src/app/layout/app-shell.component.html` — متن/Tooltip دو لینک `/fee` و `/fee2`، **بدون تغییر href/route/آیکون یا لینک محصول**.
2. `frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.scss` — افزودن بلوک علامت‌دار R22.1 برای فاصله کمتر و نمایش چندخطی متن.

یک فایل بازگشت محلی در `docs/patch-backups/nav-r221/sidebar-visual-state.json` ایجاد می‌شود. ابزارهای جدید و مستندات نیز به ترتیب در `tools/` و `docs/` نصب می‌شوند.

## محدودیت آزمون

این نتایج صرفاً آزمون‌های استاتیک و Java مستقل هستند؛ **کامپایل کامل Angular/Java با `build-production.cmd` و کنترل واقعی در مرورگر روی Windows/8091 اجرا نشده است**. مشاهده تصویری منو برای اطمینان از شکست صحیح سطرها در عرض‌های مختلف ضروری است. نیازی به اجرای SQL وجود ندارد.
