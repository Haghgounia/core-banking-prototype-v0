# Fee Prototype Baseline 1.0 - Oracle Seed Data

این بسته برای مدل 47 جدولی `FEE-Target-DataModel-Baseline-1.0-EA-Oracle` تولید شده است.

## اجرا

در SQLcl / SQL*Plus / SQL Developer Script Runner، از داخل همین پوشه اجرا کنید:

```sql
@fee_seed_all.sql
@99_fee_seed_verify.sql
```

## نکات

- همه شناسه‌های Seed منفی هستند تا با Sequenceهای عملیاتی مثبت Oracle تداخل نداشته باشند.
- `00_fee_seed_cleanup.sql` اختیاری است و فقط رکوردهایی با PK منفی را حذف می‌کند.
- داده‌های BIAN در `FEE_REF_DOMAIN/FEE_REF_VALUE` شامل enumerationهای اصلی Fee Helper هستند.
- منبع مقرراتی CBI 1404 و سقف تخفیف عمومی 30%، دانش‌بنیان 40% و گروه حمایتی 100% Seed شده است.
- تعرفه‌های CBI موجود در این بسته «سناریوهای منتخب برای پوشش موتور محاسبه» هستند و کاتالوگ کامل تمام ردیف‌های بخشنامه محسوب نمی‌شوند. برای Import کامل تعرفه CBI باید ردیف‌های فایل Excel/PDF به `FEE_DEFINITION/FEE_DEFINITION_VERSION` و Rules متناظر تبدیل شوند.
- نرخ‌های `FEE_DEMO_FX_RATE` صرفاً آزمایشی‌اند و نرخ واقعی بازار نیستند.
- تاریخ‌های Oracle به میلادی ذخیره شده‌اند؛ نمایش شمسی وظیفه UI/Calendar layer است.

## سناریوهای Seed شده

1. کارمزد انتقال درصدی با کف/سقف و نرخ توافقی Arrangement
2. تخفیف VIP 20% و کنترل سقف مقرراتی
3. تسهیم 70/20/10 بین بانک، شعبه عامل و سازمان بیرونی
4. کارمزد سالانه ضمانت‌نامه با Actual/Actual و حداقل مبلغ
5. ارزیابی با Tier و Rule Component ترکیبی
6. وصول بروات + هزینه پست عبوری
7. Commitment Fee سالانه پس از 45 روز
8. کارمزد چندمرحله‌ای کارشناسی تسهیلات
9. صدور دسته‌چک + هزینه چاپ + تمبر
10. کارمزد پلکانی نمونه
11. Fee Transaction + Snapshot + Decision Trace + Override + Partial Reversal + Posting + Settlement

## آمار

- جداول مدل: 47
- جداول دارای Seed: 47
- مجموع رکوردهای Seed: 574
