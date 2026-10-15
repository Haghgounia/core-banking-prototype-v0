# R30-HF2 — گردش کار تعیین نسخه مصوب به‌عنوان نسخه جاری

تاریخ: ۱۴۰۵/۰۷/۲۰ — دامنه: Product Builder، سپرده و تسهیلات. مبنا: نسخه کامل R30-HF1.

## دلیل اصلاح

محصول `PRODUCT_ID=31`، نسخه `PRODUCT_VERSION_ID=35` در Oracle:
`VERSION_STATUS_CODE=APPROVED`، `RECORD_STATUS_CODE=ACTIVE`، `IS_CURRENT=0`، `ORIGINATION_STATUS_CODE=OPEN`.
نسخه مصوب از مسیر ویرایش عمومی قابلیت تبدیل به نسخه جاری را ندارد؛ صدور کد مرحله ششم نیز به‌درستی نسخه جاری را الزامی می‌داند. این Hotfix عملیات مستقل و قابل ردیابی را بدون دورزدن کنترل مرحله ششم اضافه می‌کند.

## رفتار طراحی‌شده

1. مرحله دوم محصول‌ساز: اگر نسخه تصویب‌شده (`APPROVED`)، رکورد فعال (`ACTIVE`) و هنوز غیرجاری است، دکمه «تعیین نسخه جاری» نمایش داده می‌شود. کاربر دلیل تغییر را وارد و تأیید می‌کند.
2. Endpoint اختصاصی `POST /api/v1/product-builder/products/{productId}/versions/{versionId}/make-current` با بدنه `{ "reason": "..." }` اجرا می‌شود. API عمومی CRUD حق تبدیل `IS_CURRENT` از ۰ به ۱ را ندارد و ساخت نسخه جدید با پرچم جاری ممنوع است.
3. Backend در یک تراکنش، ابتدا محصول و سپس نسخه را `FOR UPDATE` قفل می‌کند، مالکیت نسخه، وضعیت محصول، تصویب، رکورد فعال، زمان و کاربر تصویب، تاریخ اعتبار مبتنی بر **روز پایگاه داده** و کنترل‌های `canApprove` را می‌سنجد.
4. اگر محصول از قبل **ACTIVE** و اعطای نسخه موردنظر **OPEN** است، `canOpen` نیز الزامی است؛ این مسیر نباید محصول عملیاتی را با دورزدن آمادگی Runtime جابه‌جا کند.
5. بعد از کنترل‌ها، نسخه جاری قبلی (در صورت وجود) با `IS_CURRENT=0` و نسخه انتخابی با `IS_CURRENT=1` ثبت می‌شوند؛ سپس سطر تاریخچه در `PDL.PRODUCT_VERSION_CURRENT_AUDIT` در **همان تراکنش** نوشته می‌شود. خطا در هر بخش کل تغییرات را برمی‌گرداند. تکرار فرمان برای نسخه جاری مجدداً تاریخچه نمی‌سازد.
6. وضعیت `PRODUCT`, `PRODUCT_CODE`, `VERSION_STATUS_CODE`, `ORIGINATION_STATUS_CODE` و قواعد نسخه از این عملیات تغییر نمی‌کنند؛ صدور کد همچنان فقط در مرحله ششم و با API مستقل R30 انجام می‌شود.

## Migration (پیش از نصب Runtime جدید)

`database/oracle/pdl/migrations/0.11.0-r30-hf2-current-version-governance.sql`

- نیازمند R30 Migration است؛ قید `CK_PDL_PRODUCT_CODE_DRAFT` باید `ENABLED/VALIDATED` باشد.
- اگر یک محصول بیش از یک نسخه جاری دارد، با خطا متوقف می‌شود؛ **هیچ داده‌ای را خودکار اصلاح نمی‌کند**.
- یک Unique Index تابعی روی نسخه جاری هر محصول اضافه می‌کند: `UX_PDL_PV_ONE_CURRENT` (چند نسخه غیرجاری مجاز است).
- جدول تاریخچه `PRODUCT_VERSION_CURRENT_AUDIT` را برای ثبت نسخه قبلی، نسخه جدید، دلیل، شناسه کاربر و زمان ایجاد می‌کند.
- قابل اجرای مجدد است؛ Oracle DDL مستقل Commit می‌شود، بنابراین از پیش DBA Backup و بازبینی اسکریپت الزامی است.

### کنترل‌های فقط‌خواندنی پس از Migration

```sql
SELECT INDEX_NAME, UNIQUENESS, STATUS
FROM ALL_INDEXES
WHERE OWNER='PDL' AND INDEX_NAME='UX_PDL_PV_ONE_CURRENT';

SELECT TABLE_NAME FROM ALL_TABLES
WHERE OWNER='PDL' AND TABLE_NAME='PRODUCT_VERSION_CURRENT_AUDIT';

SELECT PRODUCT_VERSION_ID, PRODUCT_ID, VERSION_NO, VERSION_STATUS_CODE,
       RECORD_STATUS_CODE, IS_CURRENT, APPROVED_AT, APPROVED_BY,
       VALID_FROM, VALID_TO, ORIGINATION_STATUS_CODE
FROM PDL.PRODUCT_VERSION
WHERE PRODUCT_ID=31 ORDER BY VERSION_NO;
```

## نصب Patch

۱) پس از Backup و بازبینی DBA، Migration را اجرا و خروجی Oracle را کنترل کنید.

۲) Patch R30-HF2 را **روی R30-HF1** در `D:\Projects\core-banking-prototype-v0` استخراج کنید؛ ساختار `backend/`, `frontend/`, `tools/`, `database/` باید حفظ شود.

۳) در CMD (خروجی لاگ را خارج ریشه Release نگه دارید):

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf2-current-version.mjs
build-production.cmd > "%TEMP%\build-r30-hf2.log" 2>&1
echo BUILD_EXIT_CODE=%ERRORLEVEL%
if exist app\BUILD-DIRTY (echo BUILD_NOT_COMPLETE) else (echo BUILD_CLEAN)
```

۴) سرویس را با JAR جدید restart و مرورگر را `Ctrl+F5` کنید. در محصول ۳۱، مرحله دوم، نسخه ۳۵ را انتخاب و دکمه «تعیین نسخه جاری» را باز کنید. دلیل تغییر (حداقل ۵ نویسه) را درج و تأیید کنید. اگر کنترل‌های حاکمیت عبور کنند، `IS_CURRENT` باید ۱ شود. سپس در مرحله ششم جداگانه صدور کد را اجرا کنید.

### کنترل بعد از عملیات واقعی

```sql
SELECT PRODUCT_VERSION_ID, PRODUCT_ID, VERSION_NO,
       VERSION_STATUS_CODE, RECORD_STATUS_CODE, IS_CURRENT,
       ORIGINATION_STATUS_CODE
FROM PDL.PRODUCT_VERSION
WHERE PRODUCT_ID=31 ORDER BY VERSION_NO;

SELECT AUDIT_ID, PRODUCT_ID, PREVIOUS_VERSION_ID, TARGET_VERSION_ID,
       CHANGE_REASON, ACTOR, CHANGED_AT
FROM PDL.PRODUCT_VERSION_CURRENT_AUDIT
WHERE PRODUCT_ID=31 ORDER BY AUDIT_ID DESC;

SELECT PRODUCT_ID, PRODUCT_CODE, PRODUCT_STATUS_CODE
FROM PDL.PRODUCT WHERE PRODUCT_ID=31;
```

## آزمون و محدودیت پذیرش

- `node tools/verify-pdl-r30-hf2-current-version.mjs`: آزمون Pure Java و Static برای وضعیت، اعتبار، شناسه، انحصار نسخه جاری، لاگ و مسیر API.
- رگرسیون‌های R19، R28، R29، R30 و R30-HF1 باید عبور کنند.
- آزمون‌ها و Verifierهای بدون نوشتن در محیط بسته‌سازی اجرا می‌شوند. **Build کامل Angular/Maven ویندوز و اجرای Oracle واقعی فقط پس از اجرای کاربر قابل تأیید است.**
- این نمونه پروژه هنوز احراز هویت و کنترل نقش سازمانی قابل اتکا برای endpointهای محصول‌ساز ندارد و نام کاربر از `X-User-Name` به‌عنوان فراداده تاریخچه گرفته می‌شود. این Header جایگزین احراز هویت، تفکیک وظایف یا تأیید دومرحله‌ای در محیط عملیاتی نیست؛ پیش از Production باید SSO/RBAC و maker-checker اضافه و تست شود.
- موجودبودن پرچم `OPEN` در نسخه **موجب بازشدن اعطای تسهیلات توسط این فرمان نمی‌شود**؛ آمادگی عملیاتی واقعی کنترل جداگانه دارد.

## معیار پذیرش مورد خاص

برای محصول ۳۱ و نسخه ۳۵:
- عملیات جاری‌سازی فقط با نسخه `APPROVED`, `ACTIVE`, معتبر در روز بانک و دارای تصویب ثبت‌شده موفق شود.
- قبل از اجرای عملیات، شمار نسخه‌های جاری ۰؛ پس از آن دقیقاً ۱ و شناسه نسخه جاری ۳۵ باشد.
- لاگ حاوی دلیل کاربر و هدف ۳۵ باشد؛ در اجرای مجدد رکورد لاگ اضافه نشود.
- `PRODUCT_CODE` همچنان `NULL` باقی بماند تا مرحله ششم به‌طور مستقل صادر کند.
- در صورت وجود blocker حاکمیتی، خطای شفاف نمایش داده شود و **هیچ وضعیت یا لاگی تغییر نکند**.
