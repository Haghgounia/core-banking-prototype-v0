# SYS 0.3.76 — FIX84 Global Breadcrumb QA

## هدف
قرار دادن مسیر بازگشت استاندارد در بالای تمام صفحات سامانه بدون Hard-code کردن Breadcrumb داخل هر Feature.

## پیاده‌سازی
- Component مشترک: `frontend/src/app/shared/ui/app-breadcrumb.component.ts`
- محل Render: `AppShell` قبل از `router-outlet`
- عنوان صفحه جاری: H1 واقعی صفحه
- مسیر والد: براساس Route hierarchy سامانه
- Breadcrumbهای قدیمی صفحه‌ای با CSS عمومی مخفی می‌شوند.

## نمونه‌ها
- اطلاعات پایه ← تقویم دو ← روزهای تقویم کاری
- اطلاعات پایه ← اطلاعات پایه عمومی ← تبدیل نام فارسی به انگلیسی
- مدیریت کارمزد ← جدول جاری
- محصول‌ساز یکپارچه ← محصولات ← محصول جاری
- جست‌وجو و پرونده Party ← پرونده Party ← مرحله جاری

## Regression Guard
`node tools/verify-global-breadcrumb.mjs`

انتظار: `Global breadcrumb verification OK`
