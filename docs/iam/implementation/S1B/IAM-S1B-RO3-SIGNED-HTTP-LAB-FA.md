# IAM-01 / S1B-RO3 — آزمون HTTP امضاشده در آزمایشگاه ایزوله

**Release:** `IAM-R1.9 / S1B-RO3` — ۲۰۲۶-۱۱-۰۳  
**Baseline:** سورس تجمیعی IAM-R1.8 / S1B-RO2؛ استقرار R1.8-REPAIR1 در سیستم کاربر هنوز با لاگ قابل تأیید نشده است.  
**محدوده:** آزمون رمزنگاری و کنترل دسترسی روی HTTP واقعی با `SecurityFilterChain` خود برنامه و Oracle Mock.  
**Oracle DDL/DML/COMMIT:** صفر. **IAM Write API:** صفر. **Frontend:** بدون تغییر. **IdP واقعی:** استفاده نشده.

## ۱. هدف و تمایز دو Gate

- **LAB GATE:** تولید کلید RSA 2048بیتی موقت در حافظه، امضای JWT با `SHA256withRSA`، راستی‌آزمایی امضا توسط `NimbusJwtDecoder` در Spring Security و آزمون HTTP محلی روی پورت تصادفی. مخزن Oracle به‌طور کامل Mock است؛ هیچ اتصال به Oracle رخ نمی‌دهد.
- **BANK INTEGRATION GATE:** اعتبارسنجی HTTPS/JWKS و Access Token صادرشده توسط IdP مصوب، بررسی Trust/Issuer/Audience/Claims و پاسخ `401/403/200/503` با حساب واقعی SELECT-only و Oracle DEV. این Gate تنها پس از مصوبات SEC-IAM-01 و DATA-IAM-01 قابل اجراست و با موفقیت LAB GATE بسته نمی‌شود.

## ۲. تغییر معماری محدود در Production

`IamS1bSecurityConfiguration` اکنون Decoder را تحت نام مستقل `iamS1bJwtDecoder` تعریف می‌کند. FilterChain IAM فقط Decoder دارای همین نام را دریافت می‌کند؛ `JwtDecoder` عمومیِ یک Module دیگر نمی‌تواند ناخواسته مرز اعتماد IAM شود. رفتار Production همچنان چنین است:

- `IAM_S1B_READS_ENABLED=false` ⇒ همه `/api/v1/iam/**` بسته و بدون Oracle I/O.
- `true` ⇒ `https` Issuer و JWKS با RS256، Audience محدود، Scope `iam.users.read`، Domain/Context معتبر؛ سایر متدها رد می‌شوند.
- هیچ کاربر ساختگی، Bearer Token ثابت، endpoint صدور Token آزمایشی یا Private Key در Production تعبیه نشده است.
- تغییر فقط در نحوه DI Decoder است و نباید مرز امنیت سایر endpointهای Core Banking را تغییر دهد.

## ۳. ماتریس آزمون در تست JUnit با HTTP واقعی

| وضعیت | انتظار | منبع اعتبار | وضعیت |
|---|---|---|---|
| بدون Bearer / JWT خراب | 401 | Spring Security Resource Server | تست JUnit نوشته شده؛ اجرای Maven باز |
| تغییر امضا، Issuer یا Audience نامعتبر، Token منقضی | 401 | Nimbus RS256 و Validator واقعی | تست JUnit نوشته شده؛ اجرای Maven باز |
| Token معتبر بدون Scope یا Domain مجاز | 403، بدون مراجعه به Repository | FilterChain و Domain Policy | تست JUnit نوشته شده؛ اجرای Maven باز |
| درخواست POST با Token معتبر | 403 | Deny by default | تست JUnit نوشته شده؛ اجرای Maven باز |
| Readiness و Accounts با Token معتبر | 200 | Mock `IamS1bReadRepository` و سرویس واقعی | تست JUnit نوشته شده؛ **نه Oracle 200** |
| Metadata ناموجود | 503 | سرویس و Exception Handler واقعی، Repository Mock | تست JUnit نوشته شده؛ **نه Oracle 503** |

**نکته:** JUnit با `@SpringBootTest(RANDOM_PORT)` و HTTP Client JDK کار می‌کند. فقط Decoder **با نام دقیق IAM** و Repository در ApplicationContext آزمون جایگزین می‌شوند. Decoder آزمایشگاهی نیز امضای RSA را با Nimbus کنترل می‌کند و صرفاً token را از قبل معتبر فرض نمی‌کند.

## ۴. شواهد واقعی قابل ادعا

- ابزار مستقل `IamS1bRs256Fixture.main` با JDK21 در این Release اجرا و `12/12` تأیید شد؛ کلیدها موقت‌اند، private key ذخیره یا چاپ نمی‌شود.
- کنترل‌های ساختاری `verify-s1b-ro3-contract.mjs` و کنترل‌های پیشین S1B/RO2 اجرایی و موفق‌اند.
- اجرای تست JUnit بر بستر Maven در محیط تحویل **در این Session انجام نشده است**؛ نتیجه `200/503` از سرور Spring را پیش از اجرای Maven ادعا نکنید.
- وجود تست‌ها، Decoder مشخص و Hash سورس در JAR از طریق `verify-s1b-ro3-artifact.cjs` بررسی می‌شود.

## ۵. اجرای توسعه‌دهنده و معیار پذیرش

در Build عادی `build-production.cmd`، Verifier به‌صورت خودکار و سپس Maven `clean package` اجرا می‌شود؛ آزمایش JUnit روی پورت تصادفی و بدون Oracle انجام می‌شود. بعد از Build، JAR باید `IAM_S1B_RO3_JAR_PASS` بدهد. Installer جدید در حالت پیش‌فرض غیرفعال، پنج آزمون HTTP منفی R1.8 را نیز حفظ می‌کند.

در صورتی که Maven/JUnit روی محیط ویندوز شکست خورد، **Release ناموفق است**؛ خروجی لاگ را مبنا قرار دهید و سراغ تغییر تنظیمات امنیتی یا دسترسی Oracle نروید.

## ۶. گام بعد و تصمیم‌های باز

- تأیید استقرار R1.8 یا نصب تجمیعی R1.9 در ویندوز و اخذ گزارش Maven/JUnit.
- امضای `SEC-IAM-01`: Issuer/JWKS/Audience/Scope Mapping و CORS/Threat Model.
- امضای `DATA-IAM-01`: مالکیت داده و Oracle Reader با چهار SELECT/READ Grant مستقیم، بدون Role.
- اجرای Live Acceptance با IdP و Oracle DEV و مستندسازی واقعی 401/403/200/503.
- فقط پس از عبور از Gateها، برنامه‌ریزی S1C (Migration و Audit ماندگار) و کنترل‌های عملیات نوشتنی.

**بیانیه وضعیت:** این تحویل آزمایش پذیرش S1B را قابل تکرار می‌کند؛ IAM-01، IAM-02 و Production Security را تکمیل‌شده اعلام نمی‌کند.
