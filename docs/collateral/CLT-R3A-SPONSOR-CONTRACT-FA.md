# CLT-R3A — مدیریت ضامنین وثیقه

نسخه قرارداد: R3A-1.0 | محصول مبنا: 0.11.0 | تاریخ: 2026-11-03  
نقشه راه مرجع **CLT-ROADMAP-R1.0** بدون بازتعریف R0–R7 ثابت می‌ماند. R3A یک برش درون **R3 — ضامن، بیمه و ارزیابی** است.

## ۱. مراجع و قواعد

- UI آفلاین Collateral V1.42: فرم «بیمه و ضامنین» و جست‌وجوی مستقیم شماره انتظامی.
- `BUC-06` و `SUC-07`: معرفی مستقل Party ضامن بر وثیقه، نه پرونده مصرف‌کننده.
- `SUC-08`: تعارض ضامن با بدهکار اصلی/مشترک **هنگام تخصیص** با اطلاعات معتبر Consumer بررسی می‌شود؛ بررسی این مرحله به معنی مجوز تخصیص نیست.
- `UI-05` حداقل ضامنین **معتبر** را در زمان تخصیص می‌سنجد؛ هیچ سقف عمومی تعداد ضامنین وجود ندارد.
- `G-05`: `CLT.COLLATERAL_SPONSOR` فاقد وضعیت، تأیید/لغو و تاریخچه تغییرات کامل است.
- `G-11`: UI مرجع فیلد `SPONSOR_REQUIRED_COUNT` را می‌شناسد، اما DDL ارسالی `CLT-2026-11-1.txt` برای جدول `COLLATERAL_TYPE` فقط `SPONSOR_REQUIRED` دارد و تعداد حداقل را نگهداری نمی‌کند. **نباید عدد فرضی** وارد کرد.

## ۲. محدوده قابلیت‌های پیاده‌شده

| مسیر | دسترسی | رفتار |
|---|---|---|
| `GET /api/v1/collaterals/sponsors/readiness` | `clt.collateral.read` | وضعیت فعال‌بودن خواندن، درخواست مرحله‌ای و مدل چرخه عمر |
| `GET /api/v1/collaterals/{collateralId}/sponsors?page=0&size=20` | `clt.collateral.read` | صفحه‌بندی اطلاعات واقعی `CLT.COLLATERAL_SPONSOR` تنها در شعبه JWT |
| `POST /api/v1/collaterals/{collateralId}/sponsors/validate` | `clt.collateral.introduce` | تطبیق با مالک، تکراری‌نبودن، وضعیت وثیقه، `SPONSOR_REQUIRED` و احراز Party از سرویس موجود CIF |
| `POST /api/v1/collaterals/{collateralId}/sponsors/requests` | `clt.collateral.introduce` + Flag | ثبت **درخواست DRAFT** با کلید Idempotency و رویداد Audit در جدول‌های مستقل پیشنهادی؛ بدون DML روی جدول ضامن قطعی |
| `GET /api/v1/collaterals/sponsors/requests/mine` | `clt.collateral.introduce` + Flag | فهرست درخواست‌های مرحله‌ای فقط برای سازنده و شعبه جاری |

**هیچ** endpoint تأیید، لغو یا ثبت قطعی ضامن وجود ندارد. اجرای SQL/DML بر عهده Installer نیست. در صورت فعال‌نبودن سرویس CIF، اعتبارسنجی رد می‌شود؛ مالک/ضامن با شناسه آزاد و ساختگی پذیرفته نمی‌شود. Oracle `NUMBER(19)` در API به رشته برمی‌گردد. رکورد قدیمی ضامن در CLT به‌صورت `PERSISTED_LIFECYCLE_UNVERIFIED` بازگردانده می‌شود.

## ۳. سیاست امنیت و فعال‌سازی

پیش‌فرض‌ها:

```text
CLT_REFERENCE_READS_ENABLED=false
CLT_REGISTRY_READS_ENABLED=false
CLT_SPONSOR_REQUEST_WRITES_ENABLED=false
```

اتصال خواندنی تنها با تنظیمات حساب مختص R1 فعال می‌شود. همه مسیرها در SecurityFilterChain وثایق، علاوه بر JWT معتبر، Scope متناسب و `branch_code` معتبر نیاز دارند. شناسه وثیقه در شعبه دیگر، با یک پاسخ «نیافت شد یا دسترسی ندارید» پنهان می‌شود. فعال‌سازی درخواست‌ها **تنها پس از تصویب DBA/Architecture، نصب دو جدول پیشنهادی و اعطای دسترسی حداقلی** مجاز است؛ حساب اپلیکیشن نباید DML روی `CLT.COLLATERAL_SPONSOR` داشته باشد.

حساب نویسنده درخواست R3A با متغیرهای `CLT_SPONSOR_WRITER_JDBC_URL`, `CLT_SPONSOR_WRITER_JDBC_USERNAME`, `CLT_SPONSOR_WRITER_JDBC_PASSWORD` جداست. گیت پیکربندی از حساب `SYS`, `SYSTEM`, `CLT`, `DBA` جلوگیری می‌کند؛ این کنترل جایگزین اعطای حداقل مجوز توسط DBA نیست.

## ۴. طرح داده تکمیلی (غیرمصوب)

`COLLATERAL_SPONSOR_REQUEST` و `COLLATERAL_SPONSOR_REQUEST_EVENT` صرفاً فضای مرحله‌ای هستند؛ به ۲۶ جدول فعلی مدل EA دست زده نمی‌شود. `01_SPONSOR_REQUEST_MODEL_CANDIDATE_NOT_APPROVED.sql` یک Guard اجراییِ خطادِهنده دارد و DDL پیشنهادی در **کامنت** نگهداری می‌شود. بدون حذف صریح Guard و پس از تصویب رسمی، قابل اجرا نیست. `00_SPONSOR_READONLY_PRECHECK.sql` فقط وضعیت واقعی Oracle را می‌سنجد.

## ۵. معیار خروج R3A و کار باز

۱. Build کامل Spring/Angular و وجود کلاس‌ها داخل JAR؛ ۲. راه‌اندازی Spring؛ ۳. بررسی API در JWT دارای Scope و شعبه؛ ۴. استعلام CIF واقعی؛ ۵. تأیید DBA مدل G-05/G-11 و اثر آن بر محاسبه کفایت ضامنین؛ ۶. آزمون UI توسط تحلیلگر. با Build موفق نمی‌توان صحت اتصال Oracle یا مدل چرخه عمر را اعلام کرد.

گام بعد در R3: **بیمه و ارزیابی** در برش‌های مستقل؛ ثبت قطعی ضامن و گیت تخصیص بعد از مصوبه G-05/G-11. R2 همچنان در انتظار G-03/G-14/G-17 است. شناسه‌ها و نام کلاس‌ها مبتنی بر مسئولیت کسب‌وکارند، نه شماره فاز.
