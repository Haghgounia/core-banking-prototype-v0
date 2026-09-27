# UAT Preflight Hotfix — 2026-09-27

مشکل: در Windows CMD متغیرهای `%APP_VERSION%` و `%BUILT_VERSION%` داخل بلوک‌های پرانتزی پیش از اجرای `set /p` expand می‌شدند و خروجی نسخه خالی دیده می‌شد.

اصلاح:
- فعال‌سازی `EnableDelayedExpansion`
- استفاده از `!APP_VERSION!` و `!BUILT_VERSION!` در بلوک‌ها
- استفاده از `!RUNNING_PID!` برای گزارش PID داخل بلوک

Scope: فقط ابزار UAT Preflight؛ هیچ منطق کسب‌وکاری، DDL، API یا Runtime تغییر نکرده است.
