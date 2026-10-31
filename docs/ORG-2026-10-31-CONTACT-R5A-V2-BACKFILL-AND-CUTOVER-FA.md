# ORG R5-A — Contact Points V2 / Backfill and Read-Only Cutover

تاریخ مبنا: 2026-10-31  |  ماژول ORG  |  بدون تغییر خودکار پایگاه داده

## 1. تصمیم معماری

`ORG.CONTACT_POINTS_V2` مرجع **Canonical برای راه‌های تماس واحدهای سازمانی** است. جدول شش‌ستونی `ORG.CONTACT_POINTS` صرفاً Master قدیمی راه تماس‌هاست و نباید به جای V2 به قرارداد تک‌جدولی جدید تغییر شکل داده شود. `ORG.ORGANIZATION_UNIT_CONTACT_POINTS` انتساب Legacy محسوب می‌شود. `ORG.SERVICE_POINT_CONTACT_POINTS` ساختاری مجزا دارد و بازنشستگی آن به طراحی صریح راه‌های تماس نقاط خدمت وابسته است.

در تاریخ ممیزی پیش از R5: 6930 انتساب Legacy، 6844 رکورد V2، 86 انتساب بدون V2، صفر مغایرت تجاری در رکوردهای منتقل‌شده و صفر رکورد تماس نقاط خدمت گزارش شده بود. این اعداد **Baseline تاریخی** هستند و اسکریپت مجدداً وضعیت فعلی Oracle را اندازه‌گیری می‌کند.

## 2. تفاوت شناسه‌ها

- `V2.CONTACT_POINT_ID`: کلید اصلی مستقل رکورد Canonical، برای رکوردهای جدید از `SEQ_CONTACT_POINTS_V2`.
- `V2.LEGACY_ASSOCIATION_ID`: شناسه قدیمی انتساب واحد، صرفاً برای lineage؛ در رکوردهای Native مقدار NULL دارد.
- `V2.LEGACY_CONTACT_POINT_ID`: شناسه Master قدیمی که ممکن است بین چند واحد مشترک باشد.
- برای 6844 رکورد قدیمی شناسه Canonical و انتساب قبلاً برابر گزارش شده بود. **R5-A برای رکوردهای تکمیلی از Sequence استفاده می‌کند؛ این دو شناسه لزوماً برابر نخواهند بود.**
- در API عمومی وقتی `storage-mode=canonical` است، شناسه سازگار `organizationUnitContactPointId` عمداً برابر با `contactPointId` جدید V2 برگردانده می‌شود. شناسه انتساب قدیمی در ستون lineage حفظ می‌شود، اما شناسه عملیاتی بعد از Cutover نیست.

## 3. آنچه در بسته سورس تغییر کرده

1. سرویس `CanonicalOrganizationContactService` اکنون تمام عملیات SELECT، INSERT، UPDATE و Readiness را روی `ORG.CONTACT_POINTS_V2` انجام می‌دهد؛ دیگر به Master شش‌ستونی در حالت Canonical مراجعه نمی‌کند.
2. Readiness علاوه بر قرارداد 15 ستونی و FKهای V2، پوشش همه انتساب‌های Legacy، نبود لینک یتیم V2 و خالی‌بودن تماس نقاط خدمت را کنترل می‌کند.
3. سرویس شمارشی جدید `GET /api/v1/organization/unit-contacts/migration/readiness` در هر دو حالت Legacy و Canonical کار می‌کند و هیچ مقدار ایمیل/تلفن برنمی‌گرداند.
4. `ORG_CONTACT_STORAGE_MODE=legacy` و `ORG_CONTACT_CANONICAL_WRITES_ENABLED=false` به عنوان پیش‌فرض باقی مانده‌اند؛ نصب Source هیچ Cutover خودکاری انجام نمی‌دهد.
5. تست‌های Java و Verifier جدید همراه با Build تولیدی اجرا می‌شوند.

## 4. ترتیب اجرای SQL و بسته

**الف. نصب Source:** بعد از Stop برنامه، از پوشه Patch `INSTALL.cmd` را اجرا کنید. این کار فایل‌های Modified را با Guard بایت به بایت بررسی، Backup و جایگزین می‌کند و Build کامل را روی ویندوز اجرا می‌کند. هیچ DDL/DML Oracle اجرا نمی‌شود.

**ب. Preview:** در SQL Developer فایل زیر را با F5 اجرا کنید؛ همه پرچم‌ها NO هستند:

`database/oracle/org/contact-cutover-r5/01-ORG-R5-V2-UNIT-CONTACT-BACKFILL-OPT-IN.sql`

```sql
DEFINE APPLY_CHANGES = NO
DEFINE CONFIRM_BACKUP = NO
DEFINE CONFIRM_SOURCE_QUIESCED = NO
```

**ج. در صورت تأیید داده‌ها و DBA:** از Backup مطمئن شوید. تمام برنامه‌ها و writerهای Legacy (شامل Job/مصرف‌کننده بیرونی) متوقف شوند. در یک Connection اختصاصی، پرچم‌ها را YES کنید و با F5 اجرا کنید. اسکریپت قفل موقت جدول‌ها می‌گیرد، فقط رکوردهای منتقل‌نشده را با MERGE درج می‌کند، از Sequence موجود V2 استفاده می‌کند و **COMMIT نمی‌کند**. خروجی موفق `ORG_R5A_PENDING_SUCCESS` خواهد بود. فقط با رؤیت این پیام و بررسی تعداد رکوردها، در **همان Session** `COMMIT;` را جداگانه اجرا کنید. در صورت خطا، وضعیت تراکنش را بررسی و در همان Session از `ROLLBACK;` استفاده کنید. هیچ اسکریپت دیگری را میان DML و Commit اجرا نکنید.

**د. Verify از Connection دیگر:** پس از Commit، فایل `02-ORG-R5-V2-CONTACT-COVERAGE-VERIFY-READONLY.sql` را با F5 اجرا کنید. انتظار می‌رود `MISSING_LINKS=0`، `ORPHANS=0` و `SOURCE_BUSINESS_DRIFT=0` باشد، مادامی که هنوز در V2 نوشتن عملیاتی آغاز نشده است.

**هـ. تأیید Runtime:** برنامه را در وضعیت پیش‌فرض Legacy راه‌اندازی کنید. API جدید باید تعداد و پوشش واقعی را گزارش کند. اگر DBA اجازه دهد، صرفاً برای UAT خواندنی، متغیر `ORG_CONTACT_STORAGE_MODE=canonical` و `ORG_CONTACT_CANONICAL_WRITES_ENABLED=false` را هنگام راه‌اندازی مجدد تنظیم کنید؛ سپس `GET /api/v1/organization/unit-contacts/readiness` باید `ready=true`, `storageMode=CANONICAL`, `writesEnabled=false` باشد. صحت تعداد رکوردها، تماس‌های شعب و فقدان ریزش داده را در UI بررسی کنید.

**و. فعال‌سازی نوشتن:** این بسته مجوز خودکار نوشتن در Canonical نیست. فعال‌سازی `ORG_CONTACT_CANONICAL_WRITES_ENABLED=true` تنها بعد از UAT، بررسی IAM/Actor و تأیید DBA مجاز است. Audit Actor فعلی `PROTOTYPE` یک Placeholder است و برای محیط واقعی بانکی قابل اتکا نیست.

## 5. موارد خارج از R5-A

- هیچ جدول Legacy از جمله `CONTACT_POINTS`, `ORGANIZATION_UNIT_CONTACT_POINTS`, `SERVICE_POINT_CONTACT_POINTS` تغییرنام یا حذف نمی‌شود.
- یکپارچه‌سازی راه تماس نقاط خدمت مستقل به مدل Canonical واحدها در این مرحله انجام نمی‌شود؛ صفر بودن رکورد کنونی دلیل حذف دائمی این قابلیت نیست.
- اطلاعات تماس اشخاص در CIF در دامنه نیست.
- پس از آغاز نوشتن Canonical، اختلاف با snapshot Legacy طبیعی است و نباید صرفاً به عنوان drift مهاجرت تعبیر شود.
- **بازگشت به legacy پس از ثبت داده جدید در V2 بدون برنامه انتقال معکوس، ایمن نیست.**
- آزمون کامل Maven/Angular و اتصال Oracle در محیط توسعه‌دهنده محلی ویندوز، نه این محیط آفلاین، اجرا می‌شوند.

## 6. چک‌لیست پذیرش

| شرط | معیار |
|---|---|
| Installer | `ORG_R5_INSTALL_EXIT=0` و `SOURCE_VERIFIED` کامل |
| Preview | `BLOCKERS=0` و `MISSING` مطابق شمارش جاری |
| Apply | `ORG_R5A_PENDING_SUCCESS` سپس `COMMIT` دستی همان Session |
| Verify | پوشش 100%، هیچ لینک یتیم، تعداد تماس نقاط خدمت 0 |
| Readiness | `sourceCoverageComplete=true` و بعد از تغییر موقت حالت `ready=true` |
| UI | فرم تماس در canonical read-only خوانا و بدون خطای Schema |
| Governance | امضای Cutover، بررسی مصرف‌کنندگان بیرونی، حفظ Backup و روش بازیابی |
