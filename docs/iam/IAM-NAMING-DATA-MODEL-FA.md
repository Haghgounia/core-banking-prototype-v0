# قرارداد نام‌گذاری و مدل داده IAM — نسخه IAM-R0.1

**هدف:** پایه مدیریت کاربران در Enterprise Banking IAM؛ مستقل از مدل ۷ سال قبل و با قرارداد سبک Oracle پروژه.

## ۱. اصول نام‌گذاری

| موضوع | قرارداد | نمونه |
|---|---|---|
| Schema | نام کوتاه دامنه‌ای، حروف بزرگ | `IAM` |
| Table | اسم مفهومی مفرد، `UPPER_SNAKE_CASE` | `USER_ACCOUNT`, `IDENTITY_LINK` |
| Column | اسم دقیق، پسوند `*_ID`, `*_CODE`, `*_AT`, `*_FLAG` بر حسب معنا | `PRINCIPAL_ID`, `VALID_FROM` |
| PK | `NUMBER(19,0)`، Sequence مستقل و Default | `SEQ_USER_ACCOUNT` |
| Public UID | `RAW(16)`، مستقل از PK داخلی | `PRINCIPAL_UID` |
| Constraints | نام پایدار، حداکثر ۳۰ بایت، پیشوند PK/UQ/FK/CK | `FK_UACC_PRINCIPAL_ID` |
| Index | پیشوند `IX_`، بر اساس الگوی دسترسی | `IX_ILNK_PRINCIPAL` |
| Audit | `CREATED_AT/BY`, `UPDATED_AT/BY`, `RECORD_VERSION` | یکسان در همه جداول |
| Date/Time | `TIMESTAMP(6) WITH TIME ZONE` برای رویداد و اعتبار | `VALID_FROM` |
| Codes | کاتالوگ‌های `REF_*` با FK واقعی | `REF_ACCOUNT_STATUS` |
| Metadata | `COMMENT ON TABLE/COLUMN` فارسی و قرارداد Excel | `IAM-R0-DATA-DICTIONARY.csv` |
| Keys | از `CACHE 100 NOORDER NOCYCLE` و بدون سقف ۶ رقمی | قابل توسعه |

**توضیح:** در Oracle، Schema همان مالک فیزیکی جدول است. Keycloak باید مخزن مستقل خودش را داشته باشد؛ این مدل یک جایگزین برای جداول داخلی Keycloak نیست.

## ۲. مرزهای مدل

- `PRINCIPAL`: انسانی/سرویسی/عامل؛ `USER_ACCOUNT`: حساب دسترسی در یک Security Domain؛ `FEDERATED_IDENTITY`: پیوند `(IdP, sub)`؛ `IDENTITY_LINK`: اتصال به مرجع کسب‌وکاری CIF/HR.
- یک Party ممکن است چند User Account داشته باشد. ساختار پنج‌دامنه‌ای workforce/customer/partner/workload/privileged به صورت Reference Domain Type پشتیبانی می‌شود.
- Role باید در Domain دارای محدوده Scope باشد؛ `ROLE_ASSIGNMENT` فاقد Global Scope ضمنی است.
- جداول `ACCESS_REQUEST`, `ACCESS_APPROVAL`, `SOD_CONSTRAINT` برای گردش کار و ردیابی طراحی شده‌اند، اما تا مرحله اجرای Backend/Policy هیچ کنترل تجاری به طور خودکار فعال نیست.
- جدول `SECURED_RESOURCE` کاتالوگ نوع منابع است؛ برای هر حساب سپرده یا مشتری یک ردیف ایجاد نمی‌شود. اطلاعات نمونه واقعی را از منبع authoritative Service می‌خوانیم.
- `USERNAME_NORMALIZED` به‌صورت ستون مجازی محاسبه می‌شود. `SUBJECT_ID` بدون normalize ذخیره می‌شود چون مقدار token subject حساس به حروف است.

## ۳. جدول‌های پیشنهادی (در R0 فقط DDL)

| گروه | جدول | شرح |
|---|---|---|
| Reference | `IAM.REF_DOMAIN_TYPE` | نوع دامنه امنیتی |
| Reference | `IAM.REF_PRINCIPAL_TYPE` | نوع موجودیت احراز هویت‌شونده |
| Reference | `IAM.REF_PRINCIPAL_STATUS` | وضعیت موجودیت |
| Reference | `IAM.REF_ACCOUNT_STATUS` | وضعیت حساب دسترسی |
| Reference | `IAM.REF_PROVIDER_TYPE` | نوع ارائه‌دهنده هویت |
| Reference | `IAM.REF_ROLE_TYPE` | نوع نقش دسترسی |
| Reference | `IAM.REF_SCOPE_TYPE` | نوع محدوده مجوز |
| Reference | `IAM.REF_ASSIGNMENT_STATUS` | وضعیت انتساب دسترسی |
| Reference | `IAM.REF_GROUP_TYPE` | نوع گروه |
| Reference | `IAM.REF_REQUEST_TYPE` | نوع اقدام درخواست دسترسی |
| Reference | `IAM.REF_REQUEST_STATUS` | وضعیت درخواست دسترسی |
| Reference | `IAM.REF_APPROVAL_STATUS` | نتیجه بررسی دسترسی |
| Reference | `IAM.REF_ACTION_TYPE` | نوع عملیات مجازسازی |
| Reference | `IAM.REF_RESOURCE_TYPE` | نوع منبع امنیتی |
| Foundation | `IAM.SECURITY_DOMAIN` | دامنه منطقی امنیتی با جداسازی سیاست و کاربر |
| Foundation | `IAM.IDENTITY_PROVIDER` | تعریف مرجع احراز هویت؛ بدون ذخیره Credential یا Secret |
| Foundation | `IAM.PRINCIPAL` | موجودیت انسانی، سرویس یا عامل مجاز به درخواست دسترسی |
| Foundation | `IAM.USER_ACCOUNT` | حساب دسترسی مستقل از شخص و مستقل از مخزن Credentialها |
| Foundation | `IAM.IDENTITY_LINK` | پیوند Principal به CIF/HR یا سایر مراجع؛ بدون تکثیر اطلاعات هویتی |
| Foundation | `IAM.FEDERATED_IDENTITY` | پیوند حساب با iss/sub بدون نگهداری اسرار یا Credential |
| Foundation | `IAM.APPLICATION` | سامانه یا قابلیت ثبت‌شده، مستقل از نقش‌های کاربری |
| Foundation | `IAM.SECURED_RESOURCE` | کاتالوگ نوع منابع محافظت‌شده؛ نه رکورد تمام حساب‌های بانکی |
| Foundation | `IAM.ACCESS_PERMISSION` | مجوز اتمیک بر عملیات در یک نوع منبع |
| Foundation | `IAM.ACCESS_ROLE` | نقش دسترسی در یک دامنه امنیتی؛ مجزا از نقش کسب‌وکاری CIF |
| Foundation | `IAM.ROLE_PERMISSION` | اتصال نقش به مجوز، با تاریخ اعتبار و ردگیری |
| Foundation | `IAM.ACCESS_SCOPE` | محدوده معنایی محدودکننده مجوز با ارجاع به مرجع معتبر |
| Foundation | `IAM.ROLE_ASSIGNMENT` | انتساب محدود و زمان‌دار نقش به حساب دسترسی |
| Foundation | `IAM.IDENTITY_GROUP` | گروه انتساب دسترسی؛ جدا از گروه کسب‌وکاری CIF |
| Foundation | `IAM.GROUP_MEMBERSHIP` | عضویت زمانی حساب در گروه دسترسی |
| Foundation | `IAM.GROUP_ROLE_ASSIGNMENT` | دسترسی گروه به نقش در یک محدوده تأییدشده |
| Governance | `IAM.ACCESS_REQUEST` | درخواست کنترل‌شده اعطا، لغو یا تغییر دسترسی |
| Governance | `IAM.ACCESS_REQUEST_ITEM` | ردیف درخواست دسترسی با نقش و محدوده مشخص |
| Governance | `IAM.ACCESS_APPROVAL` | گام تأیید درخواست توسط تصمیم‌گیر مجاز |
| Governance | `IAM.SOD_CONSTRAINT` | تعارض نقش‌های ناسازگار برای تفکیک وظایف |
| Governance | `IAM.ACCESS_AUDIT_EVENT` | ثبت رویداد حاکمیت دسترسی؛ نه محل ذخیره اطلاعات حساس |

## ۴. روابط کلیدی

`PRINCIPAL 1—N USER_ACCOUNT`؛ `SECURITY_DOMAIN 1—N USER_ACCOUNT`؛ `USER_ACCOUNT 1—N FEDERATED_IDENTITY`؛ `ACCESS_ROLE N—M ACCESS_PERMISSION` با `ROLE_PERMISSION`؛ `USER_ACCOUNT N—M ACCESS_ROLE` با `ROLE_ASSIGNMENT` و `ACCESS_SCOPE`؛ `IDENTITY_GROUP` و `GROUP_MEMBERSHIP` برای عضویت گروهی.

برای جلوگیری از عبور اشتباه از مرز امنیتی، در `ROLE_ASSIGNMENT`, `GROUP_MEMBERSHIP`, `GROUP_ROLE_ASSIGNMENT` و `FEDERATED_IDENTITY` از کلید خارجی ترکیبی همراه با `SECURITY_DOMAIN_ID` استفاده شده است.

## ۵. مرزبندی با نمونه‌های قدیمی

| الگوی تاریخی | تصمیم جدید |
|---|---|
| جدول `USR.USERS` با `PASSWORD` و مشخصات کامل فرد | `IAM.USER_ACCOUNT` بدون رمز/PII، لینک به CIF/HR و IdP |
| `USER_ID NUMBER(6)` و Sequence با MAXVALUE محدود | `NUMBER(19)` با Sequence مستقل و CACHE |
| دسترسی مبتنی بر `BANK_UNIT_ID NUMBER(4)` | Scope Typed Reference به ORG با `ORG_SUBTREE` و ارزیابی Policy |
| `USER_ROLE_STATUS` عددی سخت‌کدشده | Reference Table و FK معنایی |
| نگهداری نشست/توکن در اپلیکیشن | IdP مدیریت Credential/Session؛ IAM فقط Governance |
| ارتباطات خارجی ناقص و `TODO` | مرجع خارجی Typed Reference تا تأیید Data Contract؛ FK واقعی فقط میان جداول IAM |

## ۶. الزام‌های خارج از ظرفیت DDL

- ثبت هویت در IdP از طریق ابزار استاندارد؛ این اسکیما Credential Store نیست و داده‌های `USER_ACCOUNT` تصویر Governance قابل همگام‌سازی هستند، نه مخزن دوم رمز/نشست.
- ناسازگاری Role/Scope و استثنای Global باید در Service/Policy با Deny-by-default کنترل شوند. این جداول فقط Metadata اختیارات بانکی را مالک‌اند، نه نقش‌های داخلی IdP.
- زمان‌ها بر مبنای Instant و TZ ذخیره می‌شوند؛ تاریخ کسب‌وکاری/جلالی از CAL2 استخراج می‌شود.
- پیشگیری از هم‌پوشانی انتساب‌های فعال، کنترل optimistic locking بر اساس `RECORD_VERSION` و SoD بین درخواست‌کننده و تأییدکننده در Backend/Policy انجام می‌شود.
- Event audit باید به مخزن immutable / SIEM با کنترل append-only منتقل شود.
- مدل HR/ORG و CIF قبل از تولید FK بین‌اسکیمایی به‌طور دقیق راستی‌آزمایی شود.
- برای داده‌های دسترسی فعلی باید Migration Mapping و Rollback Plan مستقل تهیه شود؛ هیچ انتقال خودکاری در R0 مجاز نیست.

## ۷. دستورالعمل توسعه فرم‌ها

- برای هر جدول عملیاتی فرم اختصاصی تعریف شود؛ Dynamic CRUD عمومی برای IAM مجاز نیست.
- مقادیر Seed در این بسته **پیشنهادی** هستند و به‌عنوان کدهای رسمی BIAN معرفی نشده‌اند.

## ۸. رفرنس‌ها

- BIAN Service Landscape 14: https://bian.org/deliverables/service-landscape/
- NIST SP 800-63-4: https://csrc.nist.gov/pubs/sp/800/63/4/final
- Enterprise Identity Governance: https://learn.microsoft.com/en-us/entra/id-governance/identity-governance-overview
- OpenID Foundation FAPI 2: https://openid.net/specs/fapi-security-profile-2_0-final.html
