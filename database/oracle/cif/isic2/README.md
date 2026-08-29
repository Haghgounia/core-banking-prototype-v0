# CIF ISIC2

این بسته مدل نسخه‌محور ISIC را بدون تغییر جدول قدیمی `CIF.REF_ISIC_ACTIVITY` نصب می‌کند.

## آبجکت‌های جدید

- `CIF.REF_ISIC_RELEASE`
- `CIF.REF_ISIC_ACTIVITY2`
- `CIF.V_REF_ISIC_ACTIVITY_LOOKUP2`

## اجرا

با SQLcl/SQL*Plus از همین پوشه:

```sql
@00-install-isic2.sql
```

یا به‌ترتیب `01` تا `04` را اجرا کنید.

Seed شماره 02 ساختار رسمی UNSD ISIC Rev.4 را با 766 ردیف (21 Section, 88 Division, 238 Group, 419 Class) بارگذاری می‌کند. اسکریپت 03 فقط Release مربوط به IR-SCI را به صورت `DRAFT/Inactive` ثبت می‌کند و داده ساختگی برای نسخه ملی ایران ایجاد نمی‌کند.

**مهم:** هیچ‌یک از این اسکریپت‌ها `CIF.REF_ISIC_ACTIVITY` قدیمی را Drop/Truncate/Alter نمی‌کنند.
