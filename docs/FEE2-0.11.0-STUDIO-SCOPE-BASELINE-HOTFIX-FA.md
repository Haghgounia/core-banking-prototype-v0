# FEE2 Studio Scope Baseline Hotfix

علت خالی بودن «محدوده سازمانی / اجرایی» در Studio این است که UI از `FEE2.FEE_SCOPE` می‌خواند و DDL تحویلی FEE2 برای این جدول داده پایه ایجاد نمی‌کند. بدون Scope، Dashboard و Catalog عمداً Queryهای وابسته به `SCOPE_ID` را اجرا نمی‌کنند.

این Hotfix:

- Scopeهای غیرفعال را در انتخابگر Studio نمایش نمی‌دهد.
- عنوان Scope را به صورت `CODE · NAME_FA` نمایش می‌دهد.
- وقتی هیچ Scope فعالی وجود ندارد، به جای فرم ظاهراً خراب، پیام دقیق، لینک مدیریت `FEE_SCOPE` و «بارخوانی مجدد» نشان می‌دهد.
- یک SQL idempotent برای ایجاد Scope پایه `BANK · بانک` ارائه می‌کند.
- هیچ کدی از Simple Fee Engine وارد Core Banking Prototype نمی‌کند؛ فقط رفتار مورد انتظار Layout/UX را با پیاده‌سازی فعلی Angular/Spring و Schema موجود FEE2 تطبیق می‌دهد.

برای DB فعلی ابتدا اجرا شود:

```sql
@database/oracle/fee2/FEE2_Studio_Bank_Scope_Baseline_2026-10-10.sql
```

سپس برنامه/صفحه Studio Refresh شود.
