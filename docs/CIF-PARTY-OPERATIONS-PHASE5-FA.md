# CIF Party Operations — Phase 5

نسخه: `0.3.16-prototype`

## دامنه
این فاز مدیریت عملیاتی `CIF.PARTY_CLASSIFICATION` را اضافه می‌کند. داده‌های مرجع رسمی این فرم عبارت‌اند از:

- `CIF.REF_CLASSIFICATION_TYPE`
- `CIF.REF_CLASSIFICATION_VALUE`
- `CIF.REF_ASSIGNMENT_REASON`

## قواعد
- مقدار طبقه‌بندی وابسته به نوع طبقه‌بندی است.
- فقط Reference Data فعال پذیرفته می‌شود.
- `VALID_TO` نمی‌تواند قبل از `VALID_FROM` باشد.
- ترکیب Party + نوع + مقدار + تاریخ شروع تکراری مجاز نیست.
- ویرایش با `RECORD_VERSION` و Optimistic Lock انجام می‌شود.

## Searchable Combo
کامپوننت reusable `SearchableComboComponent` اضافه شده است. جست‌وجو با debounce به API Lookup ارسال می‌شود و برای داده‌های مرجع بزرگ لازم نیست همه گزینه‌ها یکجا به Browser منتقل شوند. Endpoint موجود Reference Data از پارامتر `text` استفاده می‌کند. برای `REF_CLASSIFICATION_VALUE` نیز endpoint وابسته به نوع طبقه‌بندی اضافه شده است:

`GET /api/v1/cif/classification-values?typeCode=...&text=...&limit=50`

## جریان
Party → نشانی و تماس → مالی و شغلی → شناسه و مدارک → طبقه‌بندی → روابط و ذی‌نفعان

## Migration
`database/oracle/cif/migrations/0.3.16-party-classification-alignment.sql` ستون `DESCRIPTION_TEXT` را برای محیط‌های موجود به‌صورت idempotent اضافه می‌کند.

## تکمیل Fix17 — Vocabulary و UX طبقه‌بندی

- هر سه ورودی کسب‌وکاری «نوع طبقه‌بندی»، «مقدار طبقه‌بندی» و «علت تخصیص» Searchable Combo هستند و از Reference Data خوانده می‌شوند.
- `REF_CLASSIFICATION_VALUE` وابسته به `REF_CLASSIFICATION_TYPE` است؛ با تغییر نوع، مقدار قبلی پاک و گزینه‌های همان نوع از API بارگذاری می‌شوند.
- Seed عملیاتی به ۴ نوع، ۲۰ مقدار وابسته و ۴ علت تخصیص فارسی گسترش یافته است. نوع `INDUSTRY` فقط گروه صنعت سطح پرتفوی است؛ فعالیت اقتصادی تفصیلی از ISIC نگهداری می‌شود.
- دکمه ثبت دیگر صرفاً به علت ناقص بودن فرم غیرفعال نمی‌شود. کاربر می‌تواند کلیک کند و فهرست فیلدهای ناقص را به‌صورت پیام روشن ببیند.
- کنترل مشترک تاریخ شمسی اصلاح شده و انتخاب «امروز» همان لحظه مقدار را در FormControl ثبت و در UI نمایش می‌دهد.
