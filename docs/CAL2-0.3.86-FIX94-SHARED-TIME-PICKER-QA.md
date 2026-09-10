# CAL2 0.3.86 / FIX94 — Shared Material Time Picker QA

## هدف

جایگزینی کنترل native مرورگر (`input type=time`) با یک Time Picker مشترک، قابل استفاده مجدد و سازگار با Theme سامانه برای تمام فیلدهای خالص ساعت/زمان.

## طراحی

Component مشترک:

`frontend/src/app/shared/ui/time-input.component.ts`

ویژگی ها:

- مبتنی بر Angular Material `MatTimepickerModule`.
- Reactive Forms / `ControlValueAccessor`.
- Validator مشترک برای خطای Parse و Min/Max.
- نمایش 24 ساعته با Locale محلی `en-GB` درون Component.
- فهرست انتخاب با Interval پیش فرض 5 دقیقه.
- امکان تایپ مستقیم `HH:mm`.
- Min/Max پیش فرض `00:00` تا `23:59`.
- دکمه پاک کردن مقدار.
- سازگار با Light/Dark Theme موجود Angular Material.
- مقدار بیرونی Component همیشه String استاندارد `HH:mm` باقی می ماند؛ Date داخلی Timepicker به API نشت نمی کند.

## قرارداد Metadata

در CAL2 یک Field Type مستقل اضافه شد:

`TIME`

چهار فیلد زیر در هر دو فرم Weekly Schedule و Single-day Exception دیگر TEXT عمومی نیستند و به صورت TIME معرفی می شوند:

- `staffStartTime`
- `staffEndTime`
- `customerOpenTime`
- `customerCloseTime`

Backend نیز TIME را به صورت `VARCHAR` می خواند/می نویسد و قالب `^([01][0-9]|2[0-3]):[0-5][0-9]$` را کنترل می کند.

## دامنه استفاده

1. CAL2 — الگوی هفتگی ساعات کاری.
2. CAL2 — استثناهای تک روز.
3. CAL — Fieldهای Metadata از نوع TIME.
4. CIF — `preferredTimeFrom` / `preferredTimeTo`.
5. CIF — ترجیح عمومی `CONTACT_TIME`.

هیچ `input type="time"` در Source فعال Frontend باقی نمانده است.

## Database

FIX94 تغییری در Schema ندارد و Migration جدید لازم نیست. ستون های زمان Policy در FIX93 همچنان `VARCHAR2(5 CHAR)` با قرارداد `HH24:MI` هستند.

## Regression Guard

Verifier جدید:

`tools/verify-time-picker.mjs`

کنترل می کند:

- Component مشترک Material Timepicker وجود دارد.
- CVA/Validator فعال است.
- قرارداد 24 ساعته و Interval 5 دقیقه حفظ شده است.
- native `type=time` در Frontend وجود ندارد.
- CAL2 دارای FieldType.TIME در Frontend و Backend است.
- Repository و Service قرارداد HH:mm را رعایت می کنند.
- CAL2 / CAL / CIF همگی Component مشترک را استفاده می کنند.

نتیجه Static QA در زمان بسته بندی: `14/14 PASS`.

همچنین کل مجموعه verifierهای پروژه پس از تغییر اجرا شده و `28/28` اسکریپت verifier با موفقیت عبور کردند.

## محدودیت Build در محیط بسته بندی

Full Angular/Maven compile در محیط بسته بندی به دلیل عدم دسترسی به Repositoryهای npm/Maven قابل تکمیل نبود. `build-production.cmd` در محیط توسعه/نصب بانک Compile واقعی Java و Angular را قبل از تولید JAR انجام می دهد.
