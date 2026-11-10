# قرارداد تراکنشی ارزیابی و تجدید ارزیابی وثیقه — CLT R3

**وضعیت:** کاندید توسعه؛ کد Backend آماده فعال‌سازی مشروط، نه مصوبه بانکی یا اثبات Oracle Production. تاریخ 2026-11-09. **مراجع:** BUC-08، SUC-10، SVC-08، BR-07/BR-12 و اسناد مرجع پیوست `Collateral_DB_Specification_FA(2).html`، `Collateral_UI_Final_Offline_V1.42_FA(2).html`، `CLT-R3C-VALUATION-CONTRACT-FA.md`.

## مرز دامنه و قرارداد اتمیک

| گام | عملیات اتمیک | شرط لازم |
|---|---|---|
| دریافت | قفل `COLLATERAL_VALUATION_REQUEST` در شعبه کاربر واقعی و وضعیت REVIEWED/Version=3 | JWT موجود برنامه، Scope خواندن و بازبینی، Maker مستقل از Poster |
| شواهد | Audit سه رویداد، Fingerprint معتبر، نبود Reference تکراری و اشاره‌گر بدون تغییر | Revalidation تازه CLT/CIF در سرور؛ هر UNKNOWN مسدودکننده است |
| مصوبه | Release دارای مراجع رسمی G-07/G-18/G-09/G-10، Rule نوع وثیقه و Rule ارزیاب | APPROVED و در بازه معتبر؛ امضاکننده/تأییدکننده Rule غیر از Poster |
| تاریخ/ارزش | VALUATION_DATE و EFFECTIVE_FROM <= تاریخ مبنای مورد تصویب؛ EFFECTIVE_TO صریح و در بازه Rule؛ ارز برابر وثیقه | بدون FX فرضی و بدون محاسبه خودکار Haircut/Eligible Value |
| تاریخچه | INSERT رکورد `CLT.COLLATERAL_VALUATION_HISTORY` با وضعیت **FINAL**، نسخه ۱، توالی جدید | قید Unique واقعی `(COLLATERAL_ID,VALUATION_SEQUENCE_NO)` و شناسه Identity Oracle |
| تجدید | رکورد جاری قبلی با قفل و وضعیت FINAL به SUPERSEDED منتقل می‌شود؛ ارزش و تاریخچه آن حذف/بازنویسی نمی‌شود | `SUPERSEDES_VALUATION_HISTORY_ID` دقیقاً اشاره‌گر فعلی؛ بدون جانشین قبلی و با قید Unique فیزیکی |
| اشاره‌گر | `CLT.COLLATERAL.CURRENT_VALUATION_HISTORY_ID` در همان تراکنش به شناسه جدید تغییر می‌کند | `RECORD_VERSION` و old pointer کنترل CAS؛ FK مرکب معتبر |
| ثبت رسید | Ledger: PREPARED سپس COMMITTED با ID جدید، Actor، Reason، Hash، Ruleها و Revision | Unique Request و Branch/Idempotency؛ Replay با همان کلید، Payload، علت و Poster |
| Failure | هر خطا باعث Rollback همزمان History، Status قبلی، Pointer و Ledger می‌شود | در نبود DBA migration و تأیید Oracle، Feature Flag خاموش می‌ماند |

**تفاوت مهم:** وضعیت `FINAL` و `CURRENT` در R3 فقط شواهد ثبت عملیاتی ارزیابی هستند؛ کفایت برای تخصیص R4، نظارت پوشش R6، تأثیر بر تخصیص‌های تاریخی، و روزکاری مصوب بانک هنوز با همین عملیات اثبات نمی‌شوند. کد **هیچ DML** روی تخصیص‌ها و تاریخچه تخصیص اجرا نمی‌کند.

## قرارداد دوره اعتبار (G-18)

- چون مدل فیزیکی `COLLATERAL_TYPE` فاقد `IS_APPRAISABLE_FLAG` و `REVALUATION_INTERVAL_UNIT` است، Rule تأییدشده مستقل `CLT.R3_VAL_POSTING_TYPE_RULE` پیشنهاد شده است: `APPRAISABLE_FLAG`، `INTERVAL_VALUE`، `INTERVAL_UNIT` از `DAY | MONTH | YEAR`، `ANCHOR_CODE` از `VALUATION_DATE | EFFECTIVE_FROM` و `POLICY_REVISION`.
- فقط `BOUNDARY_MODE=EXCLUSIVE_NEXT_START` قابل پردازش است: آخرین روز مجاز = `anchor + interval − 1 day`. این فرمول **پیشنهاد اجرای شرطی** است، نه قاعده مصوب بانک؛ تنها با Release دارای سند تصمیم معتبر استفاده می‌شود.
- `EFFECTIVE_TO` مستقیماً از Stage ثبت می‌شود و نباید بعد از سقف Rule باشد. Null و انقضا نسبت به تاریخ مبنای جاری، ثبت قطعی را مسدود می‌کنند؛ تاریخ امروز از Oracle در `Asia/Tehran` خوانده می‌شود و **روزکاری رسمی بانک محسوب نمی‌شود** تا مرجع آن توسط بانک تصویب و در Release ثبت شود.
- دریافت Receipt از Ledger پس از تجدید دیگر، `currentAtRead=false` را برمی‌گرداند؛ همان رسید همچنان سابقه معتبر یک ثبت پیشین است، نه ارزیابی جاری.

## مصوبات و شکاف‌های باز

| شناسه | گام فنی پیش‌بینی‌شده | موانع خروج |
|---|---|---|
| G-07 | ایجاد `FINAL` و تغییر وضعیت قبلی به `SUPERSEDED` با Maker/Reviewer/Poster مستقل | مصوبه نگاشت `APPROVED` به `FINAL` و نقش‌های رسمی |
| G-18 | Rule جداگانه تناوب/شمول، Anchor و Boundary | مصوبه نوع‌های قابل ارزیابی، مرز انتهای دوره، Business Date و تغییر مدل فیزیکی |
| G-09 | Rule صلاحیت ارزیاب بر حسب Party/نوع وثیقه/روش | تایید مرجع صلاحیت و FX/سرویس‌های خارجی |
| G-10 | ارزش‌های واردشده در Stage با محدودیت عدم تجاوز از ارزش ارزیابی | قواعد رسمی Haircut و Eligible Value برای R4؛ هیچ فرمولی حدس زده نمی‌شود |
| D-06 (فنی) | Check دیکشنری Oracle برای PK Identity، Unique sequence/supersedes و FK اشاره‌گر | Recon داده زنده، بررسی نسخ فیزیکی و Migration/Recovery Plan مصوب DBA |

## API و نحوه استقرار

- `GET /api/v1/collaterals/r3-workflow/valuation/posting-activation` — وضعیت Flag/Schema/Bank Release.
- `GET /api/v1/collaterals/r3-workflow/valuation/{requestId}/receipt` — رسید تراکنش ثبت‌شده، حتی بعد از خاموش‌شدن Flag.
- `POST /api/v1/collaterals/r3-workflow/valuation/{requestId}/commit` با JSON `{"expectedVersion":"3","idempotencyKey":"valuation.20261109.01","reason":"Documented bank valuation reason"}` — فقط با Scopeهای موجود `SCOPE_clt.collateral.read/review` و Actor معتبر سیستم.
- Feature Flag: `core-banking.collateral.r3-posting.valuation-enabled=false`، بدون تغییر فایل پیکربندی. هیچ حساب DB یا Connection جدیدی تعریف نشده است.
- `database/oracle/clt/r3valuationpost/00_VALUATION_POSTING_READONLY_DICTIONARY.sql` فقط خواندنی است؛ `01_VALUATION_POSTING_SCHEMA_CANDIDATE_NOT_APPROVED.sql` در ابتدا `RAISE_APPLICATION_ERROR(-20954)` می‌دهد و بدنه DDL آن **کامنت غیرقابل‌اجرا** است. نصب‌کننده SQL اجرا نمی‌کند.

## کنترل QA و پذیرش

Unit/Policy مستقل Java 21، کنترل قرارداد سورس، Synthetic Spring Typecheck و نصب/rollback محافظت‌شده در بسته گزارش می‌شوند. **این کنترل‌ها جایگزین Build کامل Maven/Angular، تراکنش زنده Oracle، آزمون همزمان ۱۰ درخواست و UAT تحلیلگر نیستند.** فعال‌سازی Flag تنها پس از تصویب، Migration و آزمون واقعی قابل بررسی است.
