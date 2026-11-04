# R6A | مانع ثبت Route در Angular (نیازمند تصمیم یکپارچه‌سازی)

**شاهد سورس در ۲۰۲۶-۱۱-۰۴:** `frontend/src/app/app.routes.ts` شامل تعریف کامپوننت‌های `CollateralRegistryComponent` و `CltReferenceWorkspaceComponent` نیست. از این رو Build قبلی حتی با `PASS` الزاما کامپوننت‌های وثایق را در Browser قابل دسترس نمی‌کند. این یک مشکل **Integration Entry Point** است، نه ایراد محاسبات مالی.

## پیشنهاد تغییر کنترل‌شده — در این بسته اجرا نمی‌شود

پس از موافقت مالک فایل مشترک و تثبیت UX Shell، زیر `children` در `app.routes.ts` و پیش از `reference/:resource` درج شود:

```ts
{path: 'collateral', loadComponent: () => import('./features/collateral/collateral-registry.component').then(m => m.CollateralRegistryComponent)},
{path: 'collateral/reference-data', loadComponent: () => import('./features/collateral/clt-reference-workspace.component').then(m => m.CltReferenceWorkspaceComponent)},
{path: 'collateral/reference-data/:table', loadComponent: () => import('./features/collateral/clt-reference-workspace.component').then(m => m.CltReferenceWorkspaceComponent)},
```

**توجه:** باید درباره افزودن میان‌بر منو در App Shell نیز تصمیم گرفته شود؛ بدون آن امکان ورود با URL مستقیم پس از ثبت Route وجود دارد ولی منوی کامل مستقل مطابق ۱۵ نمای مرجع نیست. کنترل دسترسی **Backend JWT و Branch** مستقل از Route است. در `app.routes` فرایند IAM/JWT را دور نزنید و Auth Guard موردنیاز Shell را حفظ کنید.

## معیار پذیرش این تغییر بعد از تأیید

- کنترل Git Diff فقط تغییر مسیرهای CLT، بدون لمس مسیرهای دیگر.
- `ng build` واقعی در حضور Routeهای CLT؛ رفع Warningهای `NG8102`.
- UAT-01 و UAT-02 با JWT معتبر/نامعتبر، شعبه مجاز/غیرمجاز و خطاهای 401/403/404/503.
- عدم افشای اطلاعات بین شعبه‌ها و نبود هیچ endpoint نوشتنی مالی در UI.

**وضعیت:** `BLOCKED_NEEDS_SHARED_ROUTE_APPROVAL`؛ R6A عمداً هیچ اسکریپتی برای ویرایش فایل مشترک اجرا نمی‌کند.
