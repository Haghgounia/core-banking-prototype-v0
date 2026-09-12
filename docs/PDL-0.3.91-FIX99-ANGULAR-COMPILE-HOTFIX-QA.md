# PDL 0.3.91 / FIX99 — Angular Compile Hotfix

## مسئله
Build نسخه 0.3.90 در مرحله `ng build` با خطای `TS4111` متوقف می‌شد. متغیر `payload` در `product-workspace.component.ts` از نوع `Record<string, any>` است و با فعال بودن TypeScript/Angular strict option مربوط به index signatures، دسترسی dot به کلیدهای پویا مجاز نیست.

خط مشکل‌دار:

```ts
delete payload.APPROVED_AT;
delete payload.APPROVED_BY;
```

## اصلاح
دسترسی به کلیدهای index-signature به bracket notation تبدیل شد:

```ts
delete payload['APPROVED_AT'];
delete payload['APPROVED_BY'];
```

این تغییر هیچ اثر Business/Data Contract ندارد و صرفاً سازگاری Compile با Angular/TypeScript فعلی را اصلاح می‌کند.

## Regression Guard
Verifier `tools/verify-pdl-product-builder.mjs` توسعه یافت تا:

- وجود bracket access برای `APPROVED_AT` و `APPROVED_BY` را الزام کند.
- بازگشت dot access مشکل‌دار را Fail کند.

## اثر دیتابیس
هیچ DDL، Migration یا Seed جدیدی ندارد. بسته تعرفه FEE مربوط به FIX98 بدون تغییر باقی می‌ماند.

## علت عدم وجود JAR در گزارش کاربر
`build-production.cmd` قبل از Packaging نهایی، Frontend را Build می‌کند. با Failure در `ng build`، مرحله Copy Static + Maven Package + Copy canonical JAR اجرا نشده است. بنابراین `bin\start.cmd` به درستی اعلام می‌کند `app\core-banking-prototype.jar` وجود ندارد.

## انتظار پس از Fix
- Verifierهای Static باید Pass شوند.
- `ng build` دیگر نباید TS4111 مذکور را تولید کند.
- پس از تکمیل Build باید `app\core-banking-prototype.jar` و `app\BUILD-VERSION=0.3.91` ساخته شوند.

## Compile-focused regression test
یک تست مستقل TypeScript با `--strict --noPropertyAccessFromIndexSignature` اجرا شد:

- syntax قدیمی `delete payload.APPROVED_AT` همان `TS4111` را بازتولید کرد.
- syntax اصلاح‌شده `delete payload['APPROVED_AT']` و `delete payload['APPROVED_BY']` با exit code صفر Compile شد.

در محیط بسته‌بندی، `node_modules` پروژه موجود نیست؛ بنابراین Full `ng build` باید در محیط توسعه کاربر اجرا شود. Static verifierهای پروژه همگی پس از Hotfix Pass شده‌اند.
