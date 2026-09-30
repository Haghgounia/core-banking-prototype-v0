# DPS2 0.11.0 - Final Wizard Verifier Hotfix

مشکل: verify-dps2-four-deposits.mjs هنوز عنوان مرحله هفتم قرارداد قدیمی Opening را بررسی می‌کرد و Build را با پیام seven-step opening wizard contract is incomplete متوقف می‌کرد.

اصلاح: Verifier اکنون دقیقاً هفت عنوان نهایی Reviewed را به ترتیب بررسی می‌کند و علاوه بر آن وجود step()===7 و عنوان «۷. ایجاد حساب، تسویه و فعال‌سازی کنترل‌شده» را الزام می‌کند.

این Hotfix هیچ Business Logic، DDL، API یا UI را تغییر نمی‌دهد و فقط Regression Guard را با UI نهایی هم‌راستا می‌کند.

Verification:
DPS2 Four Deposits verification OK: 59 reference forms, 25 operational forms, 4 families, 7-step wizard.
