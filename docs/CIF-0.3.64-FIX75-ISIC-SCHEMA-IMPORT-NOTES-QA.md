# QA — CIF 0.3.64 / FIX75 — ISIC Schema / Import / Normalized Notes

## مسئله گزارش‌شده

DDL استخراج‌شده از Oracle هنوز مدل قدیمی `CIF.REF_ISIC_ACTIVITY2` را نشان می‌داد؛ از جمله:

- `BASE_ISIC_CODE`
- `PARENT_ISIC_CODE`
- `SECTION_CODE`
- نبود `LEVEL_NO`
- `NAME_FA` قابل NULL
- شش ستون CLOB مستقیم روی Activity

این وضعیت نشان می‌دهد Build برنامه انجام شده ولی DDL/Seed جدید روی Oracle اجرا نشده است. `build-production.cmd` عمداً Migration دیتابیس را اجرا نمی‌کند.

## قرارداد دیتامدل در 0.3.64

مدل جدید از `CIF.REF_ISIC_ACTIVITY` قدیمی مستقل است و فقط آبجکت‌های زیر را ایجاد/بازسازی می‌کند:

- `CIF.REF_ISIC_RELEASE`
- `CIF.REF_ISIC_ACTIVITY2`
- `CIF.REF_ISIC_ACTIVITY_NOTE`
- `CIF.V_REF_ISIC_ACTIVITY_LOOKUP2`

### REF_ISIC_ACTIVITY2

جدول Activity فقط ساختار طبقه‌بندی و مشخصات عنوان را نگه می‌دارد:

- `ISIC_ACTIVITY_ID`
- `ISIC_RELEASE_ID`
- `PARENT_ACTIVITY_ID`
- `ISIC_CODE`
- `LEVEL_CODE`
- `LEVEL_NO`
- `NAME_FA NOT NULL`
- `NAME_EN NOT NULL`
- `TRANSLATION_STATUS_CODE`
- `IS_SELECTABLE`
- `IS_ACTIVE`
- `SORT_ORDER`
- Validity/Audit

ستون‌های `BASE_ISIC_CODE`، `PARENT_ISIC_CODE`، `SECTION_CODE` و CLOBهای توضیحی دیگر در این جدول وجود ندارند.

### REF_ISIC_ACTIVITY_NOTE

متن‌های طولانی در جدول فرزند و بر اساس نوع/زبان ذخیره می‌شوند:

- `NOTE_TYPE_CODE`: `EXPLANATORY`, `INCLUDES`, `ALSO_INCLUDES`, `EXCLUDES`
- `LANGUAGE_CODE`: فعلاً `fa` / `en`
- `NOTE_TEXT CLOB`

فرم Activity همچنان این محتوا را در همان Editor نمایش می‌دهد، ولی Persistence آن در جدول Note انجام می‌شود. فیلد «همچنین شامل» فارسی و انگلیسی نیز اضافه شده است.

## Import Data

`02-import-isic-rev4-unsd.sql` شامل 766 Merge برای UNSD ISIC Rev.4 است:

| Level | Count |
|---|---:|
| SECTION | 21 |
| DIVISION | 88 |
| GROUP | 238 |
| CLASS | 419 |
| Total | 766 |

برای تمام 766 رکورد `NAME_FA` و `NAME_EN` مقدار دارد و 419 Class قابل انتخاب هستند. عنوان فارسی با `BANK_TRANSLATED` علامت‌گذاری می‌شود.

منبع Structure-only مورد استفاده شامل متن تفصیلی Explanatory Notes نیست؛ بنابراین برای جدول Note داده ساختگی Seed نشده است. Noteها فقط از منبع معتبر بعدی یا از طریق فرم مدیریتی ثبت می‌شوند.

## نحوه اعمال روی Oracle

فایل Standalone زیر را جدا از Build برنامه اجرا کنید:

`database/oracle/cif/isic2/CIF_ISIC2_FULL_INSTALL.sql`

این فایل:

1. View و سه آبجکت جدید ISIC را Drop می‌کند.
2. Schema جدید را ایجاد می‌کند.
3. 766 رکورد Rev.4 را Import می‌کند.
4. Registry غیرفعال IR-SCI را ثبت می‌کند.
5. Verification را اجرا می‌کند.

`CIF.REF_ISIC_ACTIVITY` قدیمی در Reset/Install هیچ‌گاه Drop یا Update نمی‌شود.

## کنترل مورد انتظار پس از نصب

خروجی `04-verify-isic2.sql` باید نشان دهد:

```text
UNSD Rev.4 total=766, NAME_FA=766, NAME_EN=766, BANK_TRANSLATED=766, selectable=419
bad_parent=0, bad_level=0, bad_selectable=0, legacy_activity2_columns=0, note_table=1
```

## QA اجراشده روی Source

- تمام `tools/verify-*.mjs`: PASS
- `verify-cif-isic2.mjs`: 766 row / hierarchy / normalized notes / forms: PASS
- `verify-release-layout.mjs`: نسخه `0.3.64` بدون suffix و INSTALL زیر `docs/install`: PASS
- `IsicModels.java`: `javac` مستقیم: PASS
- Repository/Service: Java syntax parse بدون خطای syntax؛ Compile کامل به علت نبود Maven/dependencies در محیط انجام نشد.
- TypeScript فایل‌های ISIC: syntax parse بدون خطای syntax؛ Angular dependencies در محیط موجود نبودند.

## نکته استقرار

اجرای `build-production.cmd` به‌تنهایی Schema Oracle را تغییر نمی‌دهد. برای این FIX، اجرای Full Install دیتابیس بخش الزامی است.
