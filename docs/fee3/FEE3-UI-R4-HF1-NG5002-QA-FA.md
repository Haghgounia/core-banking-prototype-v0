# FEE3 UI R4-HF1 — اصلاح توقف Build در Angular

## علت
Angular 21 الزام می‌کند حلقه‌های کنترلی `@for` دارای عبارت `track` باشند. در `frontend/src/app/features/fee3/fee3-workspace.component.html` خط ۷۳۸، حلقه فهرست کانال آزمون بدون این عبارت نوشته شده بود؛ در نتیجه `NG5002` رخ داد.

## تغییر

```html
@for(opt of allowedReferenceChannels();track opt[0]){
  @if(opt[0]!=='ALL'){<mat-option [value]="opt[0]">{{opt[1]}}</mat-option>}
}
```

- شناسه یکتای `opt[0]` به عنوان کلید ردیابی استفاده می‌شود.
- قواعد نمایش کانال‌ها و UI دست‌نخورده می‌مانند.
- کنترل همه حلقه‌های `@for` به `tools/fee3-ui/verify-parity-r4.py` اضافه شد.
- تغییر در Backend، Oracle، FEE و FEE2: **صفر**.

## نصب
فایل Patch را در ریشه پروژه R4 باز کنید و دو فایل موجود را جایگزین نمایید، سپس:

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-ui\verify-parity-r4.py
bin\stop.cmd
build-production.cmd
```

## کنترل مستقل
- کنترل تغییر دقیق حلقه: موفق.
- همه 83 حلقه `@for`: دارای `track`.
- آزمون منفی: روی HTML قدیمی، اعتبارسنج به‌درستی خطای فاقد `track` را شناسایی کرد.
- کنترل تطبیق و توازن تگ‌ها با اعتبارسنج R4: موفق.
- Angular production build در این محیط **اجرا نشده** (وابستگی‌های کامل Angular قابل دسترس نیستند). نتیجه Runtime فقط پس از اجرای Build در محیط کاربر مشخص می‌شود.

## نسخه‌های مبنا
فقط برای پروژه حاوی `FEE3 UI R4` ارائه شده است. بر مبنای `core-banking-prototype-v0-2026-10-25-1627.zip` با نصب R4.
