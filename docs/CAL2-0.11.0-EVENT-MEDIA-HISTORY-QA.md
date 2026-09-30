# CAL2 0.11.0 — Event / Occasion Media History

## هدف

افزودن تصویر نسخه‌دار به فرم «رویدادها و مناسبت‌ها» بدون تغییر ماهیت `CAL2.EVENT` و `CAL2.EVENT_OCCURRENCE`.

نیاز کسب‌وکاری:

- برای هر مناسبت امکان ثبت تصویر وجود داشته باشد.
- تصویر فعلاً در Oracle به‌صورت `BLOB` نگهداری شود.
- تصویر می‌تواند هر سال تغییر کند؛ سال و نوع تقویم به‌عنوان Metadata نسخه تصویر ثبت می‌شود.
- در هر لحظه فقط یک تصویر برای هر `EVENT` فعال باشد.
- ثبت تصویر جدید، تمام تصاویر Active قبلی همان `EVENT` را غیرفعال کند؛ هیچ رکورد تاریخی حذف نشود.
- Admin در فرم رویدادها بتواند نسخه فعال و همه نسخه‌های قبلی را با Preview مشاهده کند.
- در روز وقوع مناسبت، تصویر Active آن مناسبت در صفحه اصلی سامانه نمایش داده شود.

## مدل داده

جدول جدید:

`CAL2.EVENT_MEDIA`

فیلدهای اصلی:

- `EVENT_MEDIA_ID`
- `EVENT_ID` → `CAL2.EVENT`
- `DISPLAY_YEAR_NO`
- `DISPLAY_CALENDAR_CODE` = `PERSIAN | GREGORIAN | ISLAMIC`
- `FILE_NAME`
- `MIME_TYPE`
- `FILE_SIZE_BYTES`
- `IMAGE_CONTENT BLOB`
- `CAPTION_FA`
- `ALT_TEXT_FA`
- `ACTIVE_FLAG`
- `CREATED_AT / CREATED_BY`
- `DEACTIVATED_AT / DEACTIVATED_BY`

یک Function-based Unique Index تضمین می‌کند که برای هر `EVENT_ID` حداکثر یک رکورد `ACTIVE_FLAG='Y'` وجود داشته باشد.

## رفتار Versioning

Upload تصویر جدید در یک Transaction انجام می‌شود:

1. رویداد Active با `FOR UPDATE` قفل می‌شود.
2. تمام تصاویر Active قبلی همان `EVENT_ID` به `ACTIVE_FLAG='N'` تغییر می‌کنند.
3. `DEACTIVATED_AT` ثبت می‌شود.
4. نسخه جدید به‌صورت Active درج می‌شود.
5. BLOB نسخه قبلی حذف یا overwrite نمی‌شود.

بنابراین مدل تاریخچه Non-destructive است.

## Runtime نمایش مناسبت امروز

Endpoint:

`GET /api/v1/calendar2/event-media/today`

انتخاب بر اساس:

`EVENT_MEDIA(active) -> EVENT(active) -> EVENT_OCCURRENCE -> CANONICAL_DAY.CANONICAL_DATE = TRUNC(SYSDATE)`

رخدادهای `CANCELLED` در نمایش امروز وارد نمی‌شوند.

Content تصویر از Endpoint مستقل و cacheable ارائه می‌شود:

`GET /api/v1/calendar2/event-media/{mediaId}/content`

## Admin UI

در حالت ویرایش فرم «رویدادها و مناسبت‌ها» بخش «تصویر مناسبت» اضافه شده است:

- انتخاب فایل JPEG / PNG / WebP
- حداکثر حجم 5 MB
- سال تصویر
- نوع تقویم سال
- عنوان/Caption
- Alt Text
- ثبت نسخه جدید
- نمایش Preview نسخه فعال و تمام نسخه‌های قبلی
- نمایش وضعیت فعال/غیرفعال و زمان ثبت/غیرفعال‌شدن

برای Event جدید ابتدا خود Event ذخیره می‌شود و سپس در حالت Edit تصویر به آن متصل می‌شود؛ در نتیجه هیچ Media بدون `EVENT_ID` معتبر ساخته نمی‌شود.

## صفحه اصلی

Dashboard در زمان ورود، `todayEventMedia()` را فراخوانی می‌کند و اگر برای مناسبت‌های امروز تصویر Active وجود داشته باشد، تصویر/تصاویر در ابتدای داشبورد نمایش داده می‌شوند.

Prototype فعلی Route مستقل Login ندارد. API `today` و Content مستقل از Dashboard طراحی شده‌اند تا همان Component/Endpoint در Login سامانه اصلی نیز قابل استفاده باشد.

## Migration

فایل:

`database/oracle/cal2/migrations/0.11.0-event-media-history.sql`

اجرا در Windows:

```bat
cd /d D:\Projects\core-banking-prototype-v0
sqlplus "%CORE_BANKING_ORACLE_CONNECT%" @database\oracle\cal2\migrations\0.11.0-event-media-history.sql
```

اگر Application با User جدا از `CAL2` متصل است، Grant مربوط به `SEQ_CAL2_EVENT_MEDIA` نیز در `02-grant-cal2-to-application-user.sql` اضافه شده است.

## Static QA

```bat
node tools\verify-calendar2-event-media.mjs
node tools\verify-calendar2-reference.mjs
node tools\verify-calendar2-month-view.mjs
node tools\verify-calendar2-business-calendar-lookups.mjs
node tools\verify-calendar2-business-calendar-day-grid.mjs
node tools\verify-calendar2-business-calendar-schedule.mjs
node tools\verify-calendar2-day-resolution-policy.mjs
node tools\verify-node-tool-path-portability.mjs
```

نتیجه در بسته تحویلی:

- `CAL2 event-media`: 11/11 PASS
- CAL2 reference regression: PASS
- CAL2 month view: PASS
- CAL2 business-calendar lookups: PASS
- CAL2 business-calendar Persian grid: PASS
- CAL2 schedule/exception: 15/15 PASS
- CAL2 invalid-day policy: PASS
- Node verifier portability: 73 verifier scripts PASS

## Runtime QA پیشنهادی بعد از Migration/Build

1. یک Event مانند «روز سعدی» را Edit کنید.
2. تصویر سال 1405 را Upload کنید.
3. History باید یک رکورد Active نشان دهد.
4. تصویر دوم برای همان Event ثبت کنید.
5. رکورد اول باید Inactive و رکورد دوم Active باشد.
6. هیچ BLOB قبلی نباید حذف شده باشد.
7. برای روزی که `EVENT_OCCURRENCE` همان Event روی `SYSDATE` قرار دارد، Dashboard باید نسخه Active را نشان دهد.
8. رخداد `CANCELLED` نباید Banner تولید کند.
9. اگر در یک روز چند مناسبت دارای تصویر وجود دارد، همه تصاویر قابل نمایش باشند.
