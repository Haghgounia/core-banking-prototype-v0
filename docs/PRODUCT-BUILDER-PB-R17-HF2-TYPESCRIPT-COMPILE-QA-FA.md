# PB-R17-HF2 — رفع خطای کامپایل TypeScript TS1117

## علت
در `frontend/src/app/features/product-builder/pdl-table.component.ts`، داخل شیء `labels` در متد `fieldLabel`، کلید `DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.SETTLEMENT_METHOD_CODE` دو بار تعریف شده بود. این مسئله خطای TS1117 در `ng build` ایجاد می‌کرد.

## اصلاح
- حذف تعریف تکراری با برچسب کوتاه «روش تسویه» و حفظ تعریف دقیق‌تر «روش تسویه هنگام خاتمه».
- افزودن کنترل عدم تکرار کلیدهای همان نقشه به آزمون `tools/verify-pb-r17.mjs` (اکنون ۴۱ کنترل).
- هیچ تغییری در منطق کسب‌وکار، Backend، جداول Oracle، داده‌های بانکی، یا قرارداد API انجام نشده است.

## آزمون‌ها
- PB-R17: 41 PASS، 0 FAIL
- R10S: 54 PASS، 0 FAIL
- Verifierهای R11 تا R16 و ساختار انتشار: PASS
- تحلیل مستقیم کامپایلر TypeScript برای خطاهای نحوی و TS1117 در فایل: 0 مورد
- **Build کامل Angular/Maven در این محیط انجام نشده است**؛ نتیجه نهایی باید از `build-production.cmd` در ویندوز بررسی شود.

## دستور نصب و آزمون
Patch را روی PB-R17-HF1 در ریشه پروژه استخراج کنید، سپس:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pb-r17.mjs
build-production.cmd
```

در صورت خطای جدید، تمام سطرهای شروع‌شده با `[ERROR]` و خروجی `build-production.cmd` ارسال شوند.
