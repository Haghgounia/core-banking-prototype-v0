# IAM-01 / S1B-RO.1 — پایه سرویس فقط‌خواندنی امن

**تاریخ:** ۲۰۲۶-۱۱-۰۲  
**مبنا:** ZIP ساعت ۱۷:۵۱ + S1A-C1/REPAIR1 (تأیید نمایش فرم در تصویر کاربر)  
**تغییر Oracle:** صفر؛ **تغییر API نوشتنی:** صفر؛ **تغییر رابط S1A:** صفر.  
**وضعیت:** پیاده‌سازی سورس / پذیرش نهایی امنیت، DBA و Build ویندوز باز.

## محدوده تحویل و Boundaries

- `IAM-00` و `IAM-01` بدون تغییر نام/تعریف ادامه دارند. `IAM-02` در این بسته پیاده‌سازی نشده؛ فقط Adapter استاندارد OAuth2 Resource Server برای اعتبارسنجی توکن صادرشده توسط IdP مورد تصویب آماده شده است.
- Spring Security روی **فقط** `/api/v1/iam/**` اعمال می‌شود تا رفتار پیشین سایر Prototype APIها تغییر نکند. این امر به معنی ایمن‌شدن کل برنامه نیست.
- `IAM_S1B_READS_ENABLED=false` پیش‌فرض است؛ در این وضعیت دسترسی IAM رد می‌شود و هیچ Bean مخزن/Controller فعال نیست. فعال‌سازی بدون HTTPS Issuer، JWKS، Audience، JDBC و حساب خواننده اختصاصی با خطا متوقف می‌شود.
- هیچ `X-User-Id`، Demo Actor، Password، IdP Secret، Credential، Refresh Token، Private Key یا Cookie Session برای اثبات هویت قابل قبول نیست. JWT امضاشده توسط Nimbus/Spring Security با Issuer، Audience، exp/nbf و Scope اعتبارسنجی می‌شود.
- JWT باید `scope` دارای `iam.users.read` و آرایه‌های امضاشده `iam_domains` و `iam_contexts` داشته باشد. Policy پس از خواندن Domain واقعی از Oracle آن را با هر دو Claim مقایسه می‌کند. `*` یا ادعای آزاد Context مجوز نیست. `DENY` پیش‌فرض.
- پایگاه خواننده یک Hikari pool خصوصی و مجزا است، بدون تعریف DataSource/JdbcTemplate سراسری. Driver/JDBC از Core SYSTEM به ارث نمی‌رسد. DB User `SYS`/`SYSTEM`/`IAM`/`DBA` رد می‌شود. DBA باید حساب **SELECT-only** اختصاصی با تنها مجوزهای لازم ایجاد کند.
- audit اولیه فقط Event Log حاوی Hash Actor/Domain/Action/Correlation ID است؛ **Audit ماندگار، کامل و بانک‌پذیر نیست** و IAM-01 را نمی‌بندد. لاگ به‌صورت عمدی Username، External Entity ID یا Token را منتشر نمی‌کند.
- معادل‌سازی `IAM.ACCESS_ROLE` با `CIF.PARTY_ROLE` ممنوع است. اتصال Workforce/ORG و Entitlement واقعی مربوط به مراحل بعدی است.

## APIهای تنها GET

| متد/مسیر | قرارداد |
|---|---|
| `GET /api/v1/iam/readiness?domainCode=...` | کنترل زنده ستون‌های اصلی R1.5.1 با ALL_TAB_COLUMNS؛ فقط پس از AuthZ |
| `GET /api/v1/iam/accounts?domainCode=...&search=...&offset=0&limit=25` | جست‌وجوی Username نرمال و صفحه‌بندی محدود؛ `limit<=100`، `offset<=10000` |
| `GET /api/v1/iam/accounts/{id}?domainCode=...` | جزئیات Principal و Account با تطبیق هویت و Domain |
| `GET /api/v1/iam/accounts/{id}/identity-links?domainCode=...` | پیوندهای Principal، فقط اگر آن Principal دقیقاً به یک Account مرتبط است؛ در غیر این صورت 409 تا تصویب ADR-IAM-04 |

**هیچ POST/PUT/PATCH/DELETE وجود ندارد.** همه مقادیر SQL از Prepared Statement bind می‌شوند؛ نام Schema و جدول‌ها ثابت‌اند. خروجی 401/403/404/409/503 تمایز دارد؛ جزئیات Oracle/JDBC در خطای عمومی افشا نمی‌شود.

## دلتا و کیفیت داده Oracle

- R1.5.1 برای `USER_ACCOUNTS.USERNAME_NORMALIZED` مقدار NULL را مجاز می‌داند؛ **این نقص داده هنوز اصلاح نشده**. این سرویس نه داده را تغییر می‌دهد، نه یکتایی کامل را تضمین می‌کند؛ جست‌وجوی نرمال فقط روی مقادیر ثبت‌شده اعمال می‌شود.
- در R1.5.1، `IDENTITY_LINKS` مالک `PRINCIPAL_ID` است، نه `SECURITY_DOMAIN_ID`. برای جلوگیری از نمایش Cross-Domain در Principal مشترک، سرویس با شمارش سراسری حساب‌های آن Principal در صورت چندگانگی خطای 409 برمی‌گرداند.
- `RAW(16)` Principal UID صرفاً به UUID رشته‌ای تبدیل می‌شود؛ محتوای Token/IdP به‌عنوان Principal UID ساخته یا جایگزین نمی‌شود.
- Readiness صرفاً وجود چهار جدول و ستون‌های لازم را اثبات می‌کند؛ سلامت کامل ۳۷ جدول، Constraints/FKs، کیفیت داده یا مهاجرت R1.3 را اثبات نمی‌کند.

## فعال‌سازی فقط پس از مجوز و تنظیمات عملیاتی

مقادیر زیر باید توسط تیم امنیت/DBA در Secret Manager یا محیط کنترل‌شده تعریف شوند. **مثال صرفاً نام متغیر است؛ مقدار معتبر ارائه نشده است.**

```text
IAM_S1B_READS_ENABLED=true
IAM_S1B_ISSUER=<HTTPS trusted issuer>
IAM_S1B_JWKS_URI=<HTTPS approved JWKS endpoint>
IAM_S1B_AUDIENCE=<approved resource identifier>
IAM_S1B_JDBC_URL=<Oracle JDBC URL of approved IAM DEV>
IAM_S1B_JDBC_USERNAME=<dedicated SELECT-only account>
IAM_S1B_JDBC_PASSWORD=<secret via approved vault>
```

معیارهای امنیت: 401 بدون Bearer معتبر، 403 توکن فاقد `iam.users.read` یا domain/context، 200 تنها برای دامنه فعال و مجاز، 503 هنگام نقص Schema/اتصال، و صفر Write. **این آزمون‌های Runtime تا تأمین IdP و IAM DEV اجرا/تأیید نشده‌اند.**

## شواهد قابل بازتولید

`node tools/iam/s1b/verify-s1b-security.mjs` شامل کنترل ساختاری Release و آزمون‌های Java مستقل است. تست‌های JUnit `IamS1bPolicyTest` نیز در مسیر Backend ثبت شده‌اند و در Maven اصلی اجرا می‌شوند.

پذیرش **G1** معماری، تایید Trust Issuer/Token Claims و قرارداد حساب SELECT-only با تیم امنیت و DBA باز است. IAM-01 تا فعال‌سازی Oracle DEV، Audit ماندگار و UAT بسته نمی‌شود.

## منابع استاندارد

- Spring Security 7 — OAuth2 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- RFC 9700 — OAuth 2.0 Security BCP: https://datatracker.ietf.org/doc/rfc9700/
- NIST SP 800-63-4: https://csrc.nist.gov/pubs/sp/800/63/4/final
- BIAN Service Landscape: https://bian.org/deliverables/service-landscape/
