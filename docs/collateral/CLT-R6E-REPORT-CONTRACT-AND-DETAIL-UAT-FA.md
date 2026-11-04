# CLT R6E — کنترل قرارداد گزارش‌ها و شواهد خواندنی شناسنامه وثیقه

- نقشه راه ثابت: `CLT-ROADMAP-R1.0`، فاز **R6**.
- مبنای ارتقا: `Core Banking Prototype 0.11.0` با `R6D` نصب‌شده.
- محدوده تغییر: فقط Angular وثایق، اسناد CLT و ابزارهای CLT؛ **هیچ تغییر Backend، Oracle، IAM یا فایل مشترک**.
- ماهیت خروجی: اعتبارسنجی پاسخ API و کمک به تحلیلگر برای کشف تفاوت Runtime با قرارداد؛ نه تصدیق حقوقی، بانکی یا UAT خودکار.

## ۱. رفع نقص واقعی در صفحه گزارش‌ها

در R6D، `collateral-reports.component.ts` با `HttpClient.get<T>()` صرفاً نوع TypeScript را بر پاسخ تحمیل می‌کرد و قبل از اتصال به UI، پاسخ واقعی Runtime را اعتبارسنجی نمی‌کرد. در نتیجه پاسخ HTTP 200 با فیلد اشتباه، محدوده غیرشعبه‌ای یا مبلغ عددی ناسازگار می‌توانست به صفحه راه پیدا کند. R6E سه Guard زیر را پیش از تغییر State اجرا می‌کند:

| Guard | منبع Java | کنترل‌های مهم |
|---|---|---|
| `cltAssertOverview` | `CollateralReportingModels.Overview` | منبع `ORACLE_CLT_READ_ONLY`، Scope شعبه، `allocationAuthorization=false`، تطبیق مجموع گروه‌های وضعیت با تعداد کل، ارزهای مجزا، مبالغ رشته‌ای |
| `cltAssertEvents` | `CollateralReportingModels.StatusEvent` | حداکثر ۲۰ رویداد، شناسه‌های ۱۹رقمی **رشته‌ای**، وضعیت معتبر، شناسه یکتای رویداد |
| `cltAssertReferenceSummary` | `CollateralReportingModels.ReferenceSummary` | منبع و Scope مرجع CLT، شمارنده‌های امن و `hqAttestation=false` |

پاسخ ناسازگار به جای جدول نیمه‌درست، پیام «مغایرت قرارداد» نشان می‌دهد؛ بدنه پاسخ در لاگ ابزار یا JSON تشخیصی ضبط نمی‌شود. صفحه هر بار نتایج قبلی را قبل از درخواست تازه پاک می‌کند.

## ۲. اجرای ۱۱ کنترل خواندنی برای یک وثیقه

در `/collateral/uat`، زیر عیب‌یاب چهار Endpoint عمومی R6D، فرم **R6E — بررسی یک وثیقه** ایجاد شده است. تحلیلگر **شناسه داخلی** `COLLATERAL_ID` (نه شماره انتظامی) را وارد می‌کند؛ ارقام فارسی و عربی بدون تبدیل به IEEE754 به رشته ASCII تبدیل می‌شوند. UI ابتدا `GET /api/v1/collaterals/{id}` را اجرا می‌کند. تنها پس از دریافت ۲۰۰ با `COLLATERAL_ID` دقیق و `COLLATERAL_NUMBER` امن، ده GET دیگر فراخوانی می‌شوند:

1. `GET /{id}/sponsors?page=0&size=2`
2. `GET /{id}/insurance-policies?page=0&size=2`
3. `GET /{id}/valuations?page=0&size=2`
4. `GET /{id}/valuations/current-evidence`
5. `GET /{id}/capacity`
6. `GET /{id}/allocations?limit=2`
7. `GET /{id}/allocations/readiness`
8. `GET /{id}/final-release/readiness`
9. `GET /{id}/final-release/evidence`
10. `GET /{id}/final-release/status-history?limit=2`

تمام مسیرها زیر `/api/v1/collaterals` قرار دارند. مجموع **۱۱ GET** فقط پس از اقدام مستقیم تحلیلگر و به‌صورت **ترتیبی** اجرا می‌شود؛ هر یک حداکثر ۶٫۵ ثانیه فرصت دارد. ورود شناسه و اجرای کنترل، هیچ عملیات نوشتنی انجام نمی‌دهد. درخواست‌های بخش‌های بعدی هنگام رد شناسنامه اصلی اصلاً ارسال نمی‌شوند.

## ۳. تفکیک وضعیت‌ها

- `PASS`: HTTP 200 همراه با ساختار مورد انتظار و پرچم‌های Fail-Closed؛ **نه مجوز تخصیص، آزادسازی یا پذیرش UAT**.
- `BLOCKED_AUTH`: پاسخ 401 یا 403.
- `NOT_FOUND_OR_DENIED`: پاسخ 404؛ وجود رکورد یا مجوز شعبه افشا نمی‌شود.
- `READ_DISABLED`: پاسخ 503، یا 200 معتبر با قابلیت خواندن غیرفعال در Readiness.
- `CONTRACT_DRIFT`: پاسخ 200 با ساختار نامعتبر، شناسه نامطابق یا پرچم خطرناک.
- `TIMEOUT`، `UNAVAILABLE` و `SERVER_ERROR`: سایر خطاها.

خروجی JSON حاوی فقط `key`، وضعیت و کد HTTP هر آزمون است؛ شناسه وثیقه، شعبه، Party، بدنه API، URL و توکن هرگز در خروجی قرار نمی‌گیرند. در صورت رد کنترل اصلی، **همان نتیجه جزئی نیز قابل خروجی گرفتن است** تا مانع به‌درستی گزارش شود.

## ۴. نگاشت وظایف و مرزهای امنیتی

این مرحله گزارش‌ها و شواهد read-only را محکم می‌کند. هیچ Route مشترک جدید، اعتبارنامه جدید، Cookie/Token مصنوعی یا bypass اضافه نمی‌کند؛ کاربر با مجوزهای موجود فقط داده شعبه خودش را می‌بیند. هیچ مدل `CollateralAllocation`، بیمه، ارزیابی یا آزادسازی تغییر نمی‌کند.

## ۵. معیارها و بدهی‌های باز

- Policy Smoke **۲۶۵** کنترل روی سورس اصلی Pure TypeScript؛ اجرای TypeScript/Angular واقعی به Build پروژه Windows وابسته است.
- Build و JAR با `INSTALL-AND-BUILD.cmd` روی سورس محلی کاربر بررسی می‌شوند؛ تأیید Browser/Oracle نیازمند خروجی واقعی ابزار R6E در نشست مجاز است.
- ۱۰ سناریوی رسمی UAT همچنان `NOT_RUN` هستند مگر تحلیلگر با شواهد غیرمحرمانه نتیجه ثبت کند.
- R1 تا R5 هنوز عملیات بانکی قطعی تأیید نشده دارند؛ این مرحله هیچ‌یک را فعال نمی‌کند.
