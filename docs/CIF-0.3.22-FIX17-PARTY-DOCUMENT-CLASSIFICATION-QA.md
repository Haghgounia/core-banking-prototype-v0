# CIF 0.3.22 Fix17 — Party Document / Classification / Persian Date QA

## دامنه اصلاح

این Fix روی سه مسئله گزارش‌شده در Phase 4 و Phase 5 تمرکز دارد:

1. ثبت و بازخورد عملیاتی `PARTY_DOCUMENT`؛
2. تکمیل Reference Data و UX `PARTY_CLASSIFICATION`؛
3. رفتار کنترل مشترک تاریخ شمسی هنگام انتخاب «امروز».

## یافته‌های ریشه‌ای

### PARTY_DOCUMENT

- پیام خطای صفحه هنوز ساختار قدیمی `fields/message` را می‌خواند، در حالی که Backend فعلی `ProblemDetail.fieldErrors/detail` برمی‌گرداند؛ بنابراین برخی خطاها برای کاربر واضح نبودند.
- فایل مدرک از نظر مدل فعلی اجباری است (`CONTENT_HASH`, `STORAGE_REF`, `MIME_TYPE` در DDL NOT NULL)، اما UX این الزام را به‌اندازه کافی صریح نشان نمی‌داد.
- Constraint `CK_DOC_VERIFY_DATE` شرط `VERIFIED_AT >= CREATED_AT` داشت. این شرط از نظر معنای کسب‌وکاری صحیح نیست و می‌تواند ثبت یک مدرک معتبر را Fail کند؛ چون زمان اعتبارسنجی می‌تواند قبل از زمان درج رکورد در دیتابیس باشد.

### PARTY_CLASSIFICATION

- UI از ابتدا از Reference Data استفاده می‌کرد، اما Seed فقط ۴ مقدار داشت و برای بعضی Typeها هیچ Value فعالی موجود نبود.
- Button به `form.invalid` قفل شده بود؛ بنابراین کاربر بدون دریافت توضیح کافی نمی‌توانست Save را امتحان کند.
- عناوین فارسی تعدادی از Seedها ترجمه نشده/ترکیبی بودند.

### Persian Date

- دکمه «امروز» فقط Selection داخلی Picker را تغییر می‌داد و مقدار را به `ControlValueAccessor.onChange` Commit نمی‌کرد؛ کاربر مجبور بود بعد از «امروز» یک بار دیگر «انتخاب» را بزند.

## اصلاحات

- پیام موفق/ناموفق Upload و Save مدارک داخل همان Section نمایش داده می‌شود.
- Save ناقص دقیقاً فیلدهای مفقود را اعلام می‌کند.
- فایل مدرک به‌صورت واضح با `*` الزامی نمایش داده می‌شود.
- Parser خطا با `ProblemDetail.fieldErrors`, `detail`, `message`, `title` سازگار شد.
- `CK_DOC_VERIFY_DATE` از Snapshot و Migration موجود حذف می‌شود.
- طبقه‌بندی‌ها: ۴ Type، ۲۰ Value وابسته و ۴ Assignment Reason با عنوان فارسی Seed شدند.
- Submit طبقه‌بندی فقط هنگام Busy/Loading غیرفعال است؛ فرم ناقص پیام مشخص تولید می‌کند.
- پس از Save موفق طبقه‌بندی، `FormGroupDirective` reset می‌شود.
- «امروز» در Persian Date همان لحظه Commit و نمایش داده می‌شود؛ باز کردن Picker بدون انتخاب، مقدار فرم را تغییر نمی‌دهد.

## QA انجام‌شده

- TypeScript syntax برای سه فایل تغییرکرده با TypeScript transpiler: PASS.
- JSON parse برای `party-reference-model.json`: PASS.
- شمارش Metadata: 4 Classification Type / 20 Classification Value / 4 Assignment Reason: PASS.
- Migration invariant: 20 Value یکتا در چهار Type + حذف `CK_DOC_VERIFY_DATE`: PASS.
- توازن پایه Form tagهای دو Angular Template: PASS.
- `tools/sync-system-specification.mjs`: PASS و Version روی `0.3.22-prototype-fix17` همگام شد.

## محدودیت Build در محیط تولید Artifact

`npm ci` در محیط اجرایی به علت Timeout دسترسی Dependency تکمیل نشد؛ بنابراین Angular full build در این محیط قابل اتکا نبود. اجرای TypeScript compiler عمومی نیز به‌طور طبیعی به علت نبود Angular packages خطاهای Module Resolution می‌دهد. Syntax فایل‌های تغییرکرده مستقل از Dependencyها بررسی شده است. Build نهایی باید در محیط Windows پروژه با Dependencyهای موجود اجرا شود.

## Migration اجباری برای دیتابیس موجود

```text
database/oracle/cif/migrations/0.3.22-fix17-party-document-classification-date.sql
```

این Migration قبل از تست Save مدارک و Comboهای طبقه‌بندی روی دیتابیس موجود اجرا شود.
