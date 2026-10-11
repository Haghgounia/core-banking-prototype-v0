# FEE UX R2 — کاتالوگ و طراح قواعد مستقل

تاریخ: ۱۹ مهر ۱۴۰۵ / 2026-10-11  
مبنای Patch: نسخه کامل `core-banking-prototype-v0-2026-10-11-1311-fee-ux-r1.zip`  
اصل معماری: FEE و FEE2 **کاملاً مستقل**. این توسعه فقط از Schema و API موجود FEE استفاده می‌کند.

## ۱. هدف و محدوده

در R1، ۴۷ فرم در هشت محیط کاری دسته‌بندی و دو فرم مرجع به یک Master–Detail تبدیل شدند. R2 فقط دو محیط از هشت محیط را ارتقا می‌دهد: **کاتالوگ و نسخه‌ها (چهار جدول)** و **شرایط اعمال و محاسبه (هفت جدول)**. هیچ‌یک از ۴۷ جدول Oracle حذف یا ادغام نمی‌شود. مسیرهای قبلی `/fee/tables/:table`، `/fee/references`، `/fee/simulator` و `/fee/workspaces/:workspace` برقرارند. هیچ تغییر داده یا DDL لازم نیست.

### مسیرهای جدید

- `/fee/design/catalog` — انتخاب تعریف، انتخاب نسخه و چهار تب «تعریف کارمزد»، «نسخه‌ها»، «ویژگی‌های پایه»، «ویژگی کارمزدی محصول».
- `/fee/design/rules` — انتخاب همان تعریف و نسخه، سپس هفت تب «قواعد شمول»، «شروط شمول»، «قواعد محاسبه»، «ورودی‌ها»، «پله‌ها»، «اجزای قاعده ترکیبی»، «مؤلفه‌های مبلغ».

کارت‌های «کاتالوگ و نسخه‌ها» و «شرایط و محاسبه» در صفحه اصلی `/fee` به این دو مسیر جدید متصل می‌شوند. در Workspaceهای قدیمی نیز لینک طراح جدید وجود دارد.

## ۲. رفتار کسب‌وکاری و مدل زمینه (Context)

انتخاب Definition → انتخاب Definition Version → انتخاب Applicability Rule / Calculation Rule. هر تب فقط رکوردهای قابل اعمال به زمینه انتخابی را فهرست می‌کند؛ داده‌های فهرست مستقیماً از API موجود `/api/v1/fees/admin/tables/{table}/rows` با `filterColumn` و `filterValue` خوانده می‌شوند. شناسه والد هنگام ایجاد/ویرایش رکورد وابسته از Context انتخاب‌شده گرفته می‌شود و در فرم تعبیه‌شده قابل تغییر نیست. بنابراین ریسک اشتباه کاربر در ورود شناسه والد کاهش می‌یابد؛ Foreign Keyها همچنان در Oracle اعمال می‌شوند.

| موجودیت | وابستگی مورد استفاده در طراح |
|---|---|
| `FEE_DEFINITION` | سراسری؛ تعیین Feature اصلی از طریق `FEE_FEATURE_ID` |
| `FEE_DEFINITION_VERSION` | `FEE_DEFINITION_ID` |
| `FEE_FEATURE` | سراسری / مستقل از نسخه |
| `FEE_PRODUCT_FEATURE` | `FEE_DEFINITION_VERSION_ID` |
| `FEE_APPLICABILITY_RULE` | `FEE_DEFINITION_VERSION_ID` |
| `FEE_APPLICABILITY_CONDITION` | `APPLICABILITY_RULE_ID` |
| `FEE_CALCULATION_RULE` | `FEE_DEFINITION_VERSION_ID` |
| `FEE_INPUT_DEFINITION` | `CALCULATION_RULE_ID` |
| `FEE_CALCULATION_TIER` | `CALCULATION_RULE_ID` |
| `FEE_RULE_COMPONENT` | `CALCULATION_RULE_ID` |
| `FEE_CHARGE_COMPONENT` | `FEE_DEFINITION_VERSION_ID` |

**نکته:** جدول `FEE_CALCULATION_RULE` علاوه بر نسخه، می‌تواند به `APPLICABILITY_RULE_ID` نیز وابسته باشد. فیلتر اصلی طراحی این تب بر نسخه است؛ نسبت میان دو قاعده با FK و منطق موجود فرم کنترل می‌شود. همچنین تعریف نسخه مستلزم `POLICY_VERSION_ID` است؛ سیاست موردنیاز باید قبلاً در بخش سیاست‌های FEE وجود داشته باشد. برای اتصال `FEE_PRODUCT_FEATURE` نیز داده DEMO_PRODUCT_FEATURE مرتبط در محیط مستقل داده‌های آزمایشی لازم است.

## ۳. تجربه کاربری

- ناحیه انتخاب کارمزد و نسخه دارای جستجو و صفحه‌بندی مستقل است. در صورت وجود شناسه‌ها در Query String، انتخاب اولیه با API جزئیات رکورد بازیابی می‌شود.
- تعویض کارمزد یا نسخه موجب پاک‌شدن انتخاب قواعد وابسته می‌شود؛ نمایش رکوردهای نامرتبط مجاز نیست.
- برای «شروط شمول»، ابتدا از فهرست قواعد شمول رکورد هدف را با دکمه «انتخاب» مشخص کنید.
- برای «ورودی‌ها»، «پله‌ها» و «اجزای قاعده ترکیبی»، ابتدا از فهرست قواعد محاسبه یک Rule را انتخاب کنید.
- فرم‌های ایجاد و ویرایش *درون* محیط طراحی باز می‌شوند و از همان Descriptor، Lookup، تاریخ شمسی و API فعلی `FeeAdminTableComponent` استفاده می‌کنند.
- تب محاسبه، انتخاب سریع `CALCULATION_STRATEGY_CODE` و نمایش شرطی اقلام نرخ/مبلغ را از منطق موجود FEE به ارث می‌برد.
- حذف فیزیکی در نسخه تعبیه‌شده ارائه نمی‌شود. API حذف و فرم‌های فنی قبلی حفظ شده‌اند؛ این سیاست UI **جایگزین کنترل دسترسی Backend نیست**.
- هنگام تغییر تب/تعریف/نسخه یا خروج از صفحه، برای ویرایش فرمِ دارای تغییرات ذخیره‌نشده تأیید گرفته می‌شود.

## ۴. محدوده تغییر در سورس

**فایل‌های جدید**: `fee-design-studio.component.ts/html/scss`، `tools/verify-fee-ux-r2.mjs` و این Runbook.  
**فایل‌های اصلاح‌شده**: `fee-admin-table.component.ts/html/scss` (حالت Embedded با فیلتر و FK قفل‌شده)، `fee-admin.service.ts` (خواندن یک رکورد برای Deep Link)، `fee-admin-home.component.ts/html`، `fee-admin-workspace.component.html` و `app.routes.ts`.

هیچ فایل اختصاصی `fee2`، هیچ Java Backend، هیچ SQL و هیچ تنظیمات Schema تغییر داده نشده است.

## ۵. نصب در ویندوز

۱. از پوشه `D:\Projects\core-banking-prototype-v0` و تنظیمات خود نسخه پشتیبان بگیرید و برنامه را متوقف کنید.  
۲. تنها **Patch R2** را در ریشه پروژه استخراج کنید. Patch R1 باید قبلاً اعمال شده باشد. از هم‌پوشانی نصب نسخه کامل و Patch در یک مرحله خودداری کنید.  
۳. در CMD:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-fee-admin-baseline.mjs
node tools\verify-fee-ux-r1.mjs
node tools\verify-fee-ux-r2.mjs
node tools\verify-fee2-forms.mjs
node tools\verify-fee2-studio.mjs
build-production.cmd
```

۴. فقط در صورت Build موفق، با `bin\start.cmd` Runtime را راه‌اندازی کنید و صفحات `/fee/design/catalog` و `/fee/design/rules` را باز کنید. نیازی به اجرای هیچ SQL جدیدی نیست.

## ۶. سناریوهای آزمون پذیرش (UAT)

| شناسه | سناریو | خروجی مورد انتظار |
|---|---|---|
| UAT-01 | ورود از کارت کاتالوگ صفحه `/fee` | ورود به `/fee/design/catalog` |
| UAT-02 | انتخاب یک تعریف با چند نسخه | فقط نسخه‌های همان تعریف در پنل نسخه‌ها ظاهر شوند |
| UAT-03 | انتخاب نسخه و رفتن به طراح قواعد | Definition/Version در صفحه جدید حفظ شود |
| UAT-04 | انتخاب Rule شمول و ورود به شروط | فقط Conditions همان Rule دیده شوند |
| UAT-05 | ثبت شرط آزمایشی در محیط تست | `APPLICABILITY_RULE_ID` با Context تکمیل شود؛ رکورد در Oracle مربوط به همان Rule باشد |
| UAT-06 | ثبت محاسبه FIXED یا TIERED | `FEE_DEFINITION_VERSION_ID` خودکار باشد؛ برای Tier/Inputs ابتدا Rule انتخاب شود |
| UAT-07 | تغییر تب با اطلاعات ذخیره‌نشده | دیالوگ حفظ/صرف‌نظر نمایش داده شود و لغو تغییر، داده را حفظ کند |
| UAT-08 | دسترسی به `/fee/tables/FEE_DEFINITION` | مسیر قدیمی همچنان باز شود |
| UAT-09 | ورود به `/fee/references` | R1 بدون رگرسیون فعال باشد |
| UAT-10 | بازکردن `/fee2/studio` و `/fee2/tables/...` | هیچ تغییر رفتار یا خطای جدید رخ ندهد |

سناریوهای ایجاد/ویرایش را روی دیتابیس آزمایشی و داده مجاز انجام دهید؛ برخی تنظیمات کارمزد ماهیت مقرراتی دارند.

## ۷. محدودیت‌های آگاهانه

- R2 یک **Contextual CRUD Designer** است؛ نه موتور جدید قیمت‌گذاری، نه Workflow تصویب و نه عملیات اتمیک روی Aggregate چندجدولی.
- اعتبارسنجی مرتبط با مقررات، تطبیق بین قواعد شمول و محاسبه، تضاد پله‌ها و Maker–Checker هنوز نیازمند فاز بعدی است.
- سرورِ محلی کاربر و Oracle در این محیط قابل دسترسی نیستند؛ فقط آزمون‌های استاتیک و Syntax از سورس اجرا شده‌اند. Build کامل Angular انجام نشده زیرا وابستگی‌های لازم در npm cache موجود نیستند و دسترسی به registry از این محیط برقرار نشد. پذیرش نهایی R2 منوط به Build و UAT روی ویندوز است.
