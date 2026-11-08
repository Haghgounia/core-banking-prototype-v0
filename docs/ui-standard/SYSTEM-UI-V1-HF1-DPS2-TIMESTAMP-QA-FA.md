# System UI V1 — HF1: قرارداد TIMESTAMP در DPS2

**تاریخ:** 2026-11-08  
**مبنای نصب:** `core-banking-prototype-v0-2026-11-08-1219.zip` + `SYSTEM-UI-STANDARD-V1-2026-11-08-1219.zip`  
**نوع تغییر:** اصلاح کنترل رگرسیون ناسازگار با UI جدید؛ بدون تغییر قرارداد API، Backend، داده یا دامنه کسب‌وکاری.

## علت
کنترل قدیمی `tools/verify-dps2-four-deposits.mjs` صرفاً وجود `type="datetime-local"` را به عنوان شاهد ویرایش `TIMESTAMP` مطالبه می‌کرد. این ورودی میلادی به‌درستی در استاندارد سراسری UI به انتخابگر شمسی یکپارچه دارای ساعت و ثانیه تبدیل شده بود؛ در نتیجه Build پیش از کامپایل متوقف می‌شد.

## اصلاح
تنها کنترل مربوط به `TIMESTAMP` در آزمون DPS2 تغییر کرد و اکنون وجود `@case ('TIMESTAMP')`، انتخابگر مشترک `app-persian-date-input`، اتصال Reactive Form (`formControlName`)، دو ویژگی `withTime` و `withSeconds`، و قرارداد `ControlValueAccessor`/بازنویسی مقدار ISO را کنترل می‌کند. حضور ورودی میلادی در همان شاخه به‌صورت عمدی مردود است.

## شواهد کنترل محلی
- `node tools/verify-dps2-four-deposits.mjs`: **PASS** (59 فرم مرجع، 25 فرم عملیاتی، 4 خانواده سپرده، Wizard هفت‌مرحله‌ای).
- `node tools/ui-standard/verify-system-ui.cjs`: **PASS** (21 کنترل).
- `node tools/clt/ui-standard/verify-ui-standard.cjs`: **PASS** (20 کنترل).
- 72 دستور Node بعد از آزمون DPS2 در `build-production.cmd`: **72 PASS / 0 FAIL** روی نسخه آزمایشی استخراج‌شده از سورس V1.
- تزریق سه نقص آزمایشی به ورودی TIMESTAMP و کنترل مشترک، هر سه به‌درستی باعث **رد شدن** آزمون شدند.
- **انجام‌نشده:** Build کامل Windows/Maven/Angular، راه‌اندازی JAR، آزمون مرورگر، CRUD واقعی Oracle.

## قلمرو و نقشه راه وثایق
- هیچ تغییر Backend یا UI جدید در این HF اعمال نمی‌شود؛ خود انتخابگر تاریخ قبلاً در V1 وجود داشت.
- نقشه راه ثابت `docs/collateral/CLT-ROADMAP-R1.0-FA.md` و سند وضعیت `docs/collateral/CLT-ROADMAP-R1.0-STATUS-2026-11-08-SYSTEM-UI.md` بدون بازنویسی باقی می‌مانند.
- نتیجه این آزمون، اثبات پذیرش عملکردی R3/R4/R5 وثایق یا تأیید Oracle Write نیست.
