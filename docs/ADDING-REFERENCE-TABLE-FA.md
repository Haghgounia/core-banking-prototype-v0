# افزودن جدول اطلاعات پایه جدید

برای فعال‌سازی یک جدول جدید:

1. DDL، Sequence و داده جدول در Oracle و Schema مالک آن، مانند `GEO` یا `DPS`، آماده شود.
2. یک کلاس `ReferenceDescriptorProvider` یا Descriptor جدید به Provider دامنه اضافه شود.
3. نام جدول، Sequence، کلید اصلی، فیلد کد و فیلد نام تعیین شود.
4. نوع کنترل، Required، Default، Searchable و Grid visibility هر فیلد مشخص شود.
5. برای رابطه اصلی والد–فرزند، `ParentDescriptor` تعریف شود؛ این رابطه Combo فیلتر Grid و زنجیره سلسله‌مراتب فرم را ایجاد می‌کند.
6. برای روابط مستقل دیگر، فیلد `LOOKUP` همراه `lookupResource` تعریف شود.
7. تست Registry، Parent chain و CRUD اجرا شود.
8. تعداد فرم‌ها و فهرست قابلیت‌ها در صفحه «مشخصات فنی سیستم» به‌روزرسانی شود.

با ثبت Descriptor، منبع به‌صورت خودکار در Catalog با وضعیت `ACTIVE` ظاهر می‌شود و نیازی به Controller، Service، Repository یا Angular Component اختصاصی نیست.

Schemaها از تنظیمات متمرکز خوانده می‌شوند:

```yaml
core-banking:
  schemas:
    reference-data: GEO
    deposit-product-factory: DPS
```
