# Oracle database scripts

ساختار این شاخه براساس مالک فیزیکی اشیای Oracle تنظیم شده است.

```text
database/oracle/
├── geo/
│   ├── ddl/
│   └── data/
└── dps/
    ├── ddl/
    ├── data/
    └── pending/
```

- `GEO`: جداول فعال اطلاعات پایه عمومی.
- `DPS/ddl`: DDL فعال اطلاعات پایه محصول سپرده (`REF_*`).
- `DPS/data`: داده‌های پایه تأییدشده برای نگهداری در Git.
- `DPS/pending`: اشیای استخراج‌شده مربوط به فازهای آینده، مانند Indexهای `DEPOSIT_PRODUCT*`.
- `database/oracle/exports`: خروجی زمان‌دار ابزار Export؛ Generated است و در Git ثبت نمی‌شود.

اسکریپت‌های DDL و Data باید تا حد ممکن همان خروجی تولیدشده از Oracle باشند. تغییر دستی فقط برای موارد مستند و ضروری انجام شود.
