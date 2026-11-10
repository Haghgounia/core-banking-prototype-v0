# BUC-15 — مدیریت استفاده بین‌واحدی از وثیقه | تحویل کد 2026-11-10

**مبنای کسب‌وکاری:** `Collateral_Business_Use_Cases_FA(2).html`: BUC-15 / BR-10؛ «نوع و معرفی هر دو اجازه استفاده بین واحدی داشته باشند؛ نبود یکی از مجوزها → منع». SUC-19 — کنترل مجوز بین‌واحدی. **وضعیت این تحویل:** کد Backend/Frontend و DDL Candidate توسعه یافته‌اند؛ تأیید Oracle عملیاتی و مجوزهای واقعی هنوز انجام نشده است.

## جریان یکپارچه پیاده‌سازی‌شده

1. **شعبه مالک:** در جزئیات همان وثیقه، بخش «مجوز بین‌واحدی»، کد شعبه مصرف‌کننده و مرجع نیاز را وارد و درخواست مرحله‌ای ثبت می‌کند. خود درخواست **مجوز نیست**. درخواست با UUID و شناسه actor/branch و نسخه وثیقه در Oracle ثبت می‌شود.
2. **تصمیم مستقل بانک:** یک actor با Scope `clt.crossunit.approve` و شعبه متناظر، غیر از ثبت‌کننده و خارج از Prototype محلی، می‌تواند «تأیید/رد» را ثبت کند. در زمان تأیید نوع وثیقه `USE_IN_OTHER_BRANCHES='Y'`، وضعیت فعال، شعبه مالک و `RECORD_VERSION` مجدداً بررسی می‌شوند. اعتبار انقضا UTC ثبت می‌شود. رخدادها Append-only هستند و امکان ابطال تأیید وجود دارد.
3. **شعبه مصرف‌کننده:** از صفحه مستقل «استفاده بین‌واحدی»، شماره انتظامی را دقیق استعلام می‌کند. در یک Query محدودشده به شعبه actor، **هر دو مجوز** و وضعیت/نسخه/انقضا بررسی می‌شوند. در صورت نبود مجوز، اطلاعات دارایی شعبه دیگر افشا نمی‌شوند. نتیجه موفق فقط فیلدهای مجاز را نشان می‌دهد؛ نام مشتری/حساب/مدرک نمایش داده نمی‌شود.
4. **درخواست استفاده:** با دامنه، پرونده، بدهکار اصلی و مشترکان و مبلغ/ارز، سرویس تحت قفل Grant و شناسنامه، مجوز را مجدداً اعتبارسنجی می‌کند، تعارض بدهکار با ضامن و ارز را کنترل می‌کند و در Journal مستقلِ Immutable ثبت می‌کند. پاسخ `RECORDED_NOT_ALLOCATED` است؛ نه تخصیص قطعی. مسیر مصرف‌کننده بانکی همچنین Client Binding مستقل را الزام می‌کند. Browser محلی صرفاً Stage است و هرگز به‌عنوان مصرف‌کننده احرازشده تلقی نمی‌شود.
5. **تکرار درخواست:** هویت UUID، Branch و Client و هش Payload را به‌صورت پایدار کنترل می‌کند؛ بازپخش همان Payload رسید قبلی را برمی‌گرداند و محتوای متفاوت با همان کلید رد می‌شود.

## قرارداد API

| Endpoint | نقش و شرط | اثر |
|---|---|---|
| `GET /api/v1/collaterals/cross-unit/readiness` | actor با Scope خواندن وثایق | وضعیت ساختار و Flag |
| `GET /api/v1/collaterals/cross-unit/owner/{id}/grants` | actor شعبه مالک | فهرست وضعیت مجوزها |
| `POST /api/v1/collaterals/cross-unit/owner/{id}/grants` | actor شعبه مالک؛ Stage-enabled | درخواست مجوز، بدون تأیید |
| `POST /api/v1/collaterals/cross-unit/owner/{id}/grants/{grant}/decision` | checker بانکی مستقل؛ تصمیم Flag-enabled | تأیید یا رد و ثبت Event |
| `POST /api/v1/collaterals/cross-unit/owner/{id}/grants/{grant}/revoke` | checker بانکی مستقل | ابطال مجوز و ثبت Event |
| `GET /api/v1/collaterals/cross-unit/by-registration/{number}` | actor شعبه مصرف‌کننده | استعلام محرمانه با دو مجوز معتبر |
| `POST /api/v1/collaterals/cross-unit/intents/analyst` | فقط Prototype محلی؛ Stage-enabled | ثبت درخواست آزمایشی قابل پیگیری |
| `POST /api/v1/collaterals/cross-unit/intents/consumer` | Client معتبر دامین صاحب تعهد | ثبت درخواست غیرمالی از مصرف‌کننده |
| `GET /api/v1/collaterals/cross-unit/intents` | actor همان شعبه و User | مشاهده رسیدهای خود |

## مالکیت داده / ساختار Oracle

نام چهار جدول **Candidate** (هیچ‌یک توسط نصاب ایجاد نمی‌شوند):

- `CLT.CLT_CROSS_UNIT_GRANT` — درخواست و تأیید سطح شناسنامه+شعبه مصرف‌کننده؛ شاخص Unique برای وضعیت‌های فعال و نسخه ردیف Parent.
- `CLT.CLT_CROSS_UNIT_GRANT_EVENT` — رویداد Append-only برای درخواست، تأیید، رد و ابطال.
- `CLT.CLT_CROSS_UNIT_INTENT` — Journal غیرمالی و Idempotency درخواست استفاده.
- `CLT.CLT_CROSS_UNIT_INTENT_DEBTOR` — بدهکاران اصلی و مشترک درخواست.

**بسته نصب فقط Source/Config/UI را تغییر می‌دهد، هیچ DDL/DML اجرا نمی‌کند.** اجرای کاندید `database/oracle/clt/crossunit/01_BUC15_CROSS_UNIT_SCHEMA_CANDIDATE_NOT_APPROVED.sql` عمداً خطای `-20945` می‌دهد. ابتدا لازم است طراحی توسط بانک و DBA تصویب شود. `00_BUC15_READONLY_DISCOVERY.sql` فقط Metadata را می‌خواند.

## پرچم‌ها و مرزبندی

| تنظیم | پیش‌فرض | معنی |
|---|---:|---|
| `core-banking.collateral.cross-unit.requests-enabled` | `false` | امکان ثبت Stage درخواست مجوز و استفاده در مدل مصوب |
| `core-banking.collateral.cross-unit.bank-decisions-enabled` | `false` | مجوز صدور/ابطال بانکی؛ در Prototype محلی حتی با true ممنوع |

مهم: Stage ممکن است پس از نصب Oracle Candidate و روشن‌شدن Flag قابل آزمون باشد، ولی **تخصیص واقعی BUC-09، Ledger بین‌دامنه‌ای، تسویه تعهد، محاسبه پوشش، FX و بازگردانی ظرفیت هنوز با این تحویل عملیاتی نشده‌اند**. G-02/G-09/G-13/G-16 و مجوز معتبر `ORG`، شیوه تأیید و نقش‌های رسمی همچنان نیازمند تصمیم بانک‌اند. مجوز فقط به کد شعبه در Claim معتبر مقید است؛ وجود/اختیارات شعبه در ORG باید قبل از فعال‌سازی نهایی ثابت شود.

**شواهد این تحویل:** Java21 pure policy smoke, Java syntax, synthetic Spring/JDBC typecheck, frontend TS syntax + strict shim typecheck, regression, guarded installer tests. بدون ادعای Oracle Live، Full Maven/Angular Build یا UAT حقیقی.
