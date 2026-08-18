# فرم‌های عملیاتی Party — فاز ۶: روابط و ذی‌نفعان

نسخه: `0.3.17-prototype`

این فاز سه بخش مدل Party را عملیاتی می‌کند:

1. `CIF.PARTY_RELATIONSHIP` برای رابطه Party-to-Party؛ Party مرتبط فقط از Partyهای موجود CIF قابل انتخاب است.
2. `CIF.BENEFICIAL_OWNERSHIP` برای مالک واقعی/کنترل‌کننده شخص حقوقی و UBO.
3. `CIF.PARTY_AUTHORITY` برای وکالت، نمایندگی، حق امضا و حدود اختیار.

## قواعد اصلی

- رابطه یک Party با خودش مجاز نیست.
- روابط `SPOUSE/PARENT/CHILD` فقط بین دو Party از نوع `PERSON` و روابط `PARENT_COMPANY/AFFILIATE` فقط بین دو Party از نوع `ORGANIZATION` پذیرفته می‌شوند.
- برای رابطه `BENEFICIAL_OWNER` درصد مالکیت/کنترل باید بیشتر از صفر باشد.
- `RELATED_PARTY_ID` و Partyهای مالک/دارنده اختیار باید در `CIF.PARTY` وجود داشته باشند.
- برای UBO حداقل یکی از درصد مالکیت مستقیم، غیرمستقیم یا کنترل باید مشخص باشد.
- UBO فقط برای Party نوع `ORGANIZATION` ثبت می‌شود.
- در `PARTY_AUTHORITY`، `PRINCIPAL_PARTY_ID` Party جاری است؛ `AUTHORIZED_PARTY_ID` دارنده اختیار و `PARTY_ID` نیز مطابق تعریف مدل برابر دارنده اختیار نگهداری می‌شود.
- سقف مبلغ اختیار فقط همراه با کد ارز ثبت می‌شود.
- همه ویرایش‌ها با `RECORD_VERSION` و Optimistic Lock انجام می‌شوند.
- `RELATIONSHIP_TYPE_CODE` در مدل supplied به REF مستقل متصل نشده است؛ بنابراین این نسخه REF ساختگی ایجاد نمی‌کند و مجموعه کدهای عملیاتی فرم را در Application نگه می‌دارد.

## Combo جست‌وجویی

انتخاب Party مرتبط/مالک/دارنده اختیار از `SearchableComboComponent` استفاده می‌کند. جست‌وجو با debounce به API جست‌وجوی Party ارسال می‌شود و امکان جست‌وجو با `PARTY_ID`، نام و شناسه اصلی وجود دارد.

## Migration

Snapshot تاریخی `database/oracle/cif/ddl/CIF-050517.sql` جدول `PARTY_RELATIONSHIP` را ندارد، در حالی که این جدول در `CIF-tables3.xlsx` وجود دارد. برای محیط‌های ارتقایافته فایل زیر اجرا شود:

`database/oracle/cif/migrations/0.3.17-party-relationship.sql`

Migration idempotent است و اگر جدول از قبل داده داشته باشد ولی Sequence موجود نباشد، Sequence از `MAX(PARTY_RELATIONSHIP_ID)+1` شروع می‌شود.
