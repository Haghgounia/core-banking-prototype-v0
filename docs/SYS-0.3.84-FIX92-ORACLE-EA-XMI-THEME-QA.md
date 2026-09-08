# SYS 0.3.84 — FIX92: سازگاری صفحه Oracle → EA XMI با Theme

## مسئله مشاهده‌شده
در Route زیر، هنگام فعال بودن Dark Theme، بخش اصلی صفحه همچنان با Palette ثابت روشن Render می‌شد:

`#/system/oracle-ea-xmi-export`

نشانه‌های Runtime شامل Card سفید، Connection Strip روشن، Headingهای تیره روی زمینه Dark، Badgeها و پیام‌های Semantic با رنگ ثابت و کاهش کنتراست Labelهای Angular Material بود.

## علت ریشه‌ای
SCSS این صفحه پیش از FIX92 به‌صورت مستقل از Theme Contract پروژه نوشته شده بود و برای Surface/Text/Border/Success/Error/Warning/Code از مقادیر Hex و RGB ثابت استفاده می‌کرد. در نتیجه Angular Material در Dark Theme رنگ متن و Controlها را از Theme تیره می‌گرفت، اما Containerهای خود Component همچنان روشن باقی می‌ماندند.

## اصلاح
فایل زیر به Theme Tokenهای سراسری پروژه منتقل شد:

`frontend/src/app/features/oracle-ea-xmi-export/oracle-ea-xmi-export.component.scss`

Tokenهای استفاده‌شده:
- `--app-surface`
- `--app-surface-muted`
- `--app-border`
- `--app-divider`
- `--app-text`
- `--app-muted`
- `--app-primary`
- `--app-primary-strong`
- `--app-primary-soft`
- `--app-success` / `--app-success-soft`
- `--app-danger` / `--app-danger-soft`
- `--app-warning-bg` / `--app-warning-border` / `--app-warning-text`
- `--app-code-bg` / `--app-code-text`
- `--app-shadow`

هیچ Color Literal از نوع Hex/RGB در Stylesheet این صفحه باقی نمانده است.

## رفتار مورد انتظار
### Dark Theme
- Cardها از `--app-surface` استفاده می‌کنند.
- Connection Strip و Guidance از `--app-surface-muted` استفاده می‌کنند.
- Title/Value از `--app-text` و توضیحات از `--app-muted` استفاده می‌کنند.
- Badge فرمت، Metric Iconها و Highlightها از Primary Theme Tokenها استفاده می‌کنند.
- Error/Success/Warning با Semantic Tokenهای Theme نمایش داده می‌شوند.
- Code/Table-nameها از Code Surface سراسری استفاده می‌کنند.

### Light Theme
همان Component بدون Branch یا Style جداگانه، از Tokenهای Light Theme استفاده می‌کند و ظاهر روشن فعلی را حفظ می‌کند.

## Guard
`tools/verify-oracle-ea-xmi-export.mjs` از FIX92 علاوه بر Wiring فنی Export، موارد Theme زیر را نیز کنترل می‌کند:
1. وجود Stylesheet صفحه.
2. استفاده از Surface/Text/Border/Semantic/Code tokenها.
3. نبودن Hex/RGB hard-coded در Stylesheet.

این Verifier به `build-production.cmd` و `build-production.sh` نیز اضافه شده است تا Regression در Build متوقف شود.

## Regression Scope
- Backend API بدون تغییر است.
- Oracle Data Dictionary queryها بدون تغییر هستند.
- XMI Writer و Stable GUID generation بدون تغییر هستند.
- Schema selection، Preview و Export workflow بدون تغییر هستند.
- هیچ DDL/Migration/Seed جدیدی وجود ندارد.

## QA Runtime
1. Theme را روی Dark قرار دهید.
2. Route `#/system/oracle-ea-xmi-export` را باز کنید.
3. سفید بودن Card اصلی یا Guidance نباید مشاهده شود.
4. Labelهای Schema/Pattern، Checkboxها، JDBC، Connection User و توضیحات باید خوانا باشند.
5. Preview Metadata را اجرا کنید و Metric/Warning/Table Codeها را کنترل کنید.
6. Theme را به Light تغییر دهید و همان صفحه را بدون Reload اجباری کنترل کنید.
7. Export XMI را اجرا کنید تا عدم Regression عملکردی تأیید شود.
