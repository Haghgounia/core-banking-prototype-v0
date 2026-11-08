# استاندارد مشترک فرم‌های Core Banking Prototype — نسخه UI-STANDARD-V1

**وضعیت:** IMPLEMENTED — SHARED PRESENTATION BASELINE / SPECIALIZED WORKFLOW PARITY NOT YET VERIFIED  
**مبنای سورس:** `core-banking-prototype-v0-2026-11-08-1219.zip`، نسخه محصول `0.11.0`  
**SHA-256 بسته مبنا:** `bd436f33342f77791931bc50f916ab16aa4371e5d02ead2b228da1be25d8432b`  
**مرجع بصری:** `/#/reference-data/currencies` و `ReferencePageComponent`، با حفظ تجربه کاربری تصویب‌شده پنل‌های تخصصی.  
**دامنه کنترل:** ۸۶ Template در `frontend/src/app/features/**`، ۲۳ حوزه، به‌اضافه یک Template مرکزی AppShell؛ در مجموع ۸۷ Template Angular.

## استاندارد لازم‌الاجرا

| شناسه | استاندارد | مسیر اجرا / کنترل |
|---|---|---|
| UX-01 | نمایش فارسی، RTL، عنوان و برچسب کسب‌وکاری؛ شناسه فنی فقط در جای مناسب | `ui-standard-v1`، بازنویسی متن عمومی ۷ نما |
| UX-02 | یک Design Token مشترک در تم روشن/تیره | `styles.scss`، Date Picker مشترک، دو Workspace سپرده |
| UX-03 | نمایش متناسب با موبایل، عدم سرریز فرم، قابلیت پیمایش جداول عریض | Global scoped responsive CSS |
| UX-04 | دسترس‌پذیری پایه: focus-visible و نام دکمه آیکونی | ۸۶ دکمه آیکونی نام‌گذاری شدند؛ اسکن تکمیلی ضروری است |
| UX-05 | ایجاد/ویرایش/حذف و تأیید حذف فقط در فرم‌های دارای قرارداد CRUD | `ReferencePageComponent` و فرم‌های موجود؛ ایجاد اجباری دکمه در Workflows ممنوع |
| UX-06 | جست‌وجو/فیلتر/مرتب‌سازی/صفحه‌بندی واقعی در فرم‌های فهرستی | الگوی مرجع؛ سایر ۲۶ نامزد نیازمند بازبینی قرارداد Pagination هستند |
| UX-07 | انتخاب فیلد نوع‌دار (وضعیت، ارجاع، عدد، پرچم، تاریخ) | تاریخ‌های فرم‌های عمومی به `app-persian-date-input` متصل شدند |
| UX-08 | تاریخ شمسی در UI و مقدار قراردادی Gregorian/ISO در API | `PersianDateInputComponent` موجود؛ منطق تبدیل و API تغییر نکرده |
| UX-09 | واحد پول، رقم و درصد مطابق نوع داده و قواعد دامنه | نیازمند UAT تخصصی برای هر فرم؛ داده و محاسبه تغییر نکرد |
| UX-10 | حالات Loading/Empty/Error/Success، پیام فارسی و بدون ادعای ثبت ناموفق | قراردادهای فعلی حفظ شدند؛ ممیزی رفتاری مرورگر باز است |
| UX-11 | حذف اصطلاحات فنی غیرضروری از عملیات روزمره کاربر | ۷ پیام عمومی تغییر کرد؛ تشخیص و ابزار فنی استثناء |
| UX-12 | عدم تغییر منطق کسب‌وکار، IAM، Schema، DataSource، Endpoints با Patch UI | هیچ فایل Backend/Database/Runtime یا منطق و مجوز IAM تغییر نکرده است؛ تنها قالب IAM نیز مانند سایر فرم‌ها Scope استایل دریافت کرده است |

## اجرای واقعی در این بسته

1. **همه ۸۶ نما** یک Scope استاندارد با نام `ui-standard-v1` در ریشه Template دارند. Shell مرکزی حفظ شد.
2. یک لایه سراسری CSS در `frontend/src/styles.scss`، بدون اضافه‌کردن CSS اختصاصی به ۸۶ کامپوننت، قواعد Responsive/RTL/Focus/Shared Theme را اعمال می‌کند.
3. **۸۶ دکمه فقط آیکونی** در ۲۸ Template دارای `aria-label` فارسی شدند.
4. `PersianDateInputComponent` مشترک از Tokenهای تم استفاده می‌کند؛ ارزش قراردادی فیلد همچنان ISO است.
5. در ۷ Template اصلی (`ReferencePage`, `PartyReferencePage`, `Calendar2ReferencePage`, `FeeManagement`, `Fee2StudioCreateDialog`, `RulePolicyLite`, `DepositAccountOperations`) فیلدهای تاریخ Gregorian بومی با Date Picker شمسی موجود جایگزین شدند؛ در نتیجه هیچ `type=date/time/datetime-local` بومی در ۸۶ قالب Feature باقی نمانده است. آزمون زمانی Runtime همچنان لازم است.
6. دو Workspace بزرگ سپرده (`OpeningWizard`, `DepositAccountOperations`) متغیرهای سطح پوسته خود را از Tokenهای سیستم دریافت می‌کنند؛ قرارداد فرآیند و صفحات خاص آن‌ها تغییر نکرده است.
7. هفت پیام عمومی که نام موتور پایگاه داده را بی‌دلیل در UI نشان می‌دادند، به زبان کاربر بانکی بازنویسی شدند.

## خروجی ممیزی جامع و مرز پذیرش

- فایل `UI-STANDARD-V1-SYSTEM-INVENTORY-2026-11-08.csv` شامل **هر ۸۶ نما، گروه، نوع تجربه، Material Table، جدول بومی، Paginator، تعداد کنترل‌های تاریخ بومی، رخدادهای اصطلاحات فنی، تعداد رنگ‌های ثابت و ستون `additional_review`** است؛ Shell یک پوسته مسیریابی مشترک است، نه فرم CRUD.
- علامت `PAGINATION_CONTRACT_REVIEW` به معنی نقص قطعی نیست. برخی جداول، تکرارشونده و محدود به یک Aggregate هستند و صفحه‌بندی برای آن‌ها مفید یا مجاز نیست. این موارد باید با قرارداد کاربردی بررسی شوند.
- علامت `HARD_CODED_THEME_REVIEW` به معنی الزام تغییر رنگ معنایی/هویت بصری نیست؛ فقط نقاطی که در تم تاریک مشکل خوانایی دارند باید هدف قرار گیرند.
- `TECHNICAL_COPY_REVIEW` در صفحات تشخیص، معماری، DBA و UAT فنی می‌تواند با دلیل مستند معاف شود. نام موتور پایگاه داده در دکمه CRUD عادی مجاز نیست.
- هرگز فقط با وجود `ui-standard-v1` وضعیت **FULL CRUD PARITY** ادعا نمی‌شود؛ چون API، سرور، دسترسی و workflowهای هر ماژول مستقل‌اند.
- کنترل ایستای این بسته: `node tools/ui-standard/verify-system-ui.cjs`. تولید مجدد ممیزی: `node tools/ui-standard/audit-system-ui.mjs`.
- **انجام‌نشده در محیط تحویل:** ساخت JAR در ویندوز بانک، اجرای واقعی Angular Template Type Check، DOM/UX UAT هر ۸۶ نما، Oracle Write / Financial E2E.

## تصمیم معماری و موارد باز

**تصمیم:** UI-STANDARD-V1 لایه مشترک تجربه کاربری است، نه یک Bounded Context جدید و نه توسعه دامنه‌های دیگر. تغییرهای عمومی فقط در UI و Componentهای Shared قرار گرفته‌اند. قراردادهای BIAN و تفکیک دامنه‌های سپرده، Party، محصول و وثایق حفظ شده‌اند.

**موارد نیازمند کار تخصصی بعدی:** فیلتر/صفحه‌بندی Server-Side برای فهرست‌هایی که واقعاً Dataset بزرگ دارند، جایگزینی فیلدهای بومی با کنترل مرجع به شرط وجود API Lookup، شناسایی فرم‌های فاقد Dialog استاندارد حذف، کنترل تم تیره در سبک‌های قدیمی (به‌ویژه CIF و سپرده)، و مرورگر UAT در اندازه‌های ۱۳۶۶px و موبایل. هر اقدام تغییر Workflow یا Backend مستلزم Change Request دامنه است و با Patch عمومی UX به‌طور پنهانی انجام نمی‌شود.

## نقشه راه وثایق

مرجع معتبر و ثابت **`docs/collateral/CLT-ROADMAP-R1.0-FA.md`** با فازهای **R0 تا R7** است. این بسته فاز جدیدی برای CLT تعریف نکرده و جایگزین سند آن نشده است. وضعیت به‌روز در `docs/collateral/CLT-ROADMAP-R1.0-STATUS-2026-11-08-SYSTEM-UI.md` ثبت شده است.
