# CIF ISIC — مدل مستقل نسخه‌محور و سلسله‌مراتبی

این بسته مدل ISIC را مستقل از `CIF.REF_ISIC_ACTIVITY` قدیمی نصب می‌کند. جدول قدیمی نه Drop می‌شود، نه خوانده می‌شود و نه در Seed جدید استفاده می‌شود.

## آبجکت‌ها

- `CIF.REF_ISIC_RELEASE` — Registry نسخه‌ها، Revisionها و Variantهای ISIC
- `CIF.REF_ISIC_ACTIVITY2` — درخت فعالیت‌های اقتصادی با Parent ID، سطح و عنوان دو‌زبانه اجباری
- `CIF.REF_ISIC_ACTIVITY_NOTE` — یادداشت‌های توضیحی زبان‌محور برای `EXPLANATORY / INCLUDES / ALSO_INCLUDES / EXCLUDES`
- `CIF.V_REF_ISIC_ACTIVITY_LOOKUP2` — View سبک برای Lookup و نمایش در UI

## طراحی

هر Activity به یک Release تعلق دارد. رابطه سلسله‌مراتبی با `PARENT_ACTIVITY_ID` برقرار می‌شود و FK مرکب تضمین می‌کند والد و فرزند در یک Release باشند. سطح‌ها `SECTION=1`، `DIVISION=2`، `GROUP=3`، `CLASS=4` و توسعه اختیاری `NATIONAL_SUBCLASS=5` هستند.

`NAME_FA` و `NAME_EN` اجباری‌اند. Seed اولیه UNSD Rev.4 شامل 766 رکورد کامل ساختاری است: 21 Section، 88 Division، 238 Group و 419 Class. عنوان انگلیسی بر پایه ساختار رسمی UNSD و عنوان فارسی ترجمه پروژه است؛ بنابراین رکوردهای فارسی با `TRANSLATION_STATUS_CODE='BANK_TRANSLATED'` علامت‌گذاری شده‌اند.

متن‌های بلند توضیحی دیگر ستون‌های CLOB در `REF_ISIC_ACTIVITY2` نیستند. آن‌ها در `REF_ISIC_ACTIVITY_NOTE` بر اساس نوع یادداشت و زبان نگهداری می‌شوند. این تفکیک از ساختار طبقه‌بندی اصلی جلوگیری از تکرار ستون‌های زبان/نوع می‌کند و امکان افزودن زبان‌های بعدی را می‌دهد.

فایل ساختار رسمی Rev.4 که مبنای Seed است فقط Code/Description ساختار را دارد؛ بنابراین برای Explanatory Notes متن ساختگی وارد نمی‌شود. جدول Note در Seed ساختاری می‌تواند خالی باشد و فقط زمانی پر می‌شود که منبع معتبر توضیحات بارگذاری شود یا کاربر از فرم مدیریتی ثبت کند.

## نصب/بازطراحی Prototype

Build برنامه **هیچ DDLای را خودکار روی Oracle اجرا نمی‌کند**. پس از جایگزینی Source باید این اسکریپت را جداگانه در SQLcl/SQL*Plus اجرا کنید:

```sql
@CIF_ISIC2_FULL_INSTALL.sql
```

یا از همین پوشه:

```sql
@00-install-isic2.sql
```

این نصب فقط آبجکت‌های جدید ISIC را Drop/Recreate می‌کند و `CIF.REF_ISIC_ACTIVITY` قدیمی دست‌نخورده می‌ماند.

اجرای مرحله‌ای:

```sql
@00-reset-isic2-redesign.sql
@01-create-isic2-tables.sql
@02-import-isic-rev4-unsd.sql
@03-register-ir-sci-release.sql
@04-verify-isic2.sql
```

اسکریپت Verify علاوه بر تعداد 766/419 و کامل بودن `NAME_FA/NAME_EN`، نبود ستون‌های Legacy در `REF_ISIC_ACTIVITY2` و وجود `REF_ISIC_ACTIVITY_NOTE` را نیز کنترل می‌کند.
