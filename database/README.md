# Oracle database scripts

ساختار این شاخه براساس مالک فیزیکی اشیای Oracle تنظیم شده است.

```text
database/oracle/
├── geo/
│   ├── ddl/
│   └── data/
├── dps/
│   ├── ddl/
│   ├── data/
│   └── pending/
├── cif/
├── cal/
└── cal2/
    ├── 00-create-cal2-schema.sql
    ├── 01-create-cal2-tables.sql
    ├── 02-grant-cal2-to-application-user.sql
    ├── migrations/
    │   └── 0.3.45-fix56-event-recurrence-rule.sql
    ├── 99-verify-cal2-schema.sql
    └── README.md
```

- `GEO`: جداول فعال اطلاعات پایه عمومی.
- `DPS/ddl`: DDL فعال اطلاعات پایه محصول سپرده (`REF_*`).
- `DPS/data`: داده‌های پایه تأییدشده برای نگهداری در Git.
- `DPS/pending`: اشیای استخراج‌شده مربوط به فازهای آینده، مانند Indexهای `DEPOSIT_PRODUCT*`.
- `CIF`: DDL/Migrationهای Party/Customer و Reference Data مشتری.
- `CAL`: مدل تقویم یک؛ مستقل از CAL2.
- `CAL2`: مدل تقویم دو با ۱۶ جدول مستقل، قواعد تکرار مناسبت و Import مستقیم بسته ZIP از طریق Backend/JDBC.
- `database/oracle/exports`: خروجی زمان‌دار ابزار Export؛ Generated است و در Git ثبت نمی‌شود.

برای نصب `CAL2` ابتدا `cal2/00-create-cal2-schema.sql` را با حساب دارای `CREATE USER` اجرا کنید، سپس `cal2/01-create-cal2-tables.sql` را اجرا کنید. اگر DataSource برنامه با User دیگری متصل است، `cal2/02-grant-cal2-to-application-user.sql` نیز اجرا شود. اسکریپت `99-verify-cal2-schema.sql` فقط‌خواندنی است.

اسکریپت‌های DDL و Data باید تا حد ممکن همان Contract فیزیکی/CSV تحویلی را حفظ کنند. تغییر دستی فقط برای موارد مستند و ضروری انجام شود.

برای CAL2های ایجادشده با FIX54/FIX55، Migration `cal2/migrations/0.3.45-fix56-event-recurrence-rule.sql` جدول قواعد مناسبت و provenance رخدادها را اضافه می‌کند.

### ISIC2 نسخه‌محور

بسته `database/oracle/cif/isic2` مدل مستقل ISIC را با `REF_ISIC_RELEASE` و `REF_ISIC_ACTIVITY2` نگهداری می‌کند. در FIX73 عنوان فارسی/انگلیسی فعالیت اجباری و Seed UNSD Rev.4 شامل 766 عنوان دو‌زبانه است. برای نصب Prototype فایل Standalone `CIF_ISIC2_FULL_INSTALL.sql` قابل اجرا است.
