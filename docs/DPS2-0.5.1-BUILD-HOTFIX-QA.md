# DPS2 0.5.1 — Phase 5 Build Hotfix QA

## Scope

این نسخه توسعه Functional جدیدی نسبت به Phase 5 اضافه نمی‌کند. هدف آن اصلاح Release/Build contract پس از مشاهده Build واقعی Windows است.

## Root causes

1. **Patch baseline mismatch**: Patch نسخه 0.5.0 برای Overlay روی 0.4.0 تولید شده بود. اعمال آن روی 0.3.99 باعث می‌شد بعضی فایل‌های جدید Phase 4 در Source مقصد وجود نداشته باشند، درحالی‌که فایل‌های تغییرکرده Phase 5 به آن‌ها reference می‌دادند.
2. **Jackson major-version mismatch**: `DepositOpeningAuditService` از `com.fasterxml.jackson.core.JsonProcessingException` و `com.fasterxml.jackson.databind.ObjectMapper` استفاده می‌کرد، درحالی‌که Spring Boot 4.1.0 در این پروژه از Jackson 3 و namespace `tools.jackson` استفاده می‌کند.
3. **Build guard gap**: Build script قدیمی در سناریوی Overlay اشتباه، پیش از Maven compile کامل‌بودن package فاز 4 و اجرای verifier فاز 5 را تضمین نمی‌کرد.

## Fix

- استفاده از `tools.jackson.core.JacksonException`.
- تزریق `tools.jackson.databind.json.JsonMapper` مطابق الگوی موجود پروژه.
- اضافه‌شدن Source completeness guard برای Account Lifecycle.
- اجرای `verify-dps2-deposit-opening-phase5.mjs` و `verify-dps2-build-hotfix-051.mjs` در هر دو build script ویندوز و Unix.
- Patch 0.5.1 به‌صورت cumulative نسبت به 0.3.99 بسته‌بندی می‌شود تا Phase 4 و Phase 5 به‌طور کامل Overlay شوند.

## Acceptance

- تمام verifierهای static/regression باید PASS شوند.
- Overlay patch روی 0.3.99 باید byte-identical با target 0.5.1 شود.
- Full ZIP و cumulative Patch ZIP باید CRC سالم داشته باشند.
- Production Maven/Angular Build نهایی باید روی محیط کاربر اجرا شود؛ لاگ موفق آن Release Gate باقی‌مانده را می‌بندد.
