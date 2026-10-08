# گزارش تغییرات و وضعیت آزمون محصول ساز - Baseline 2026-10-08 11:33

مرجع قراردادی: Unified_Product_Builder_Interactive_Forms_FA(2).html

## تغییرات این بسته
1. بازگشت به محصول جاری و گام 4 یا 5 به جای اتکای صرف به history مرورگر.
2. انتقال Context بازگشت هنگام ورود به جداول فرزند از ردیف‌های مادر.
3. بازگرداندن گام در Workspace از پارامتر `step`.
4. بالابردن stacking context تقویم شمسی هنگام بازبودن در جدول.
5. شفاف سازی عنوان، کنترل و راهنمای پیکربندی قابلیت‌ها در مرحله ماژول.

## وضعیت کنترل کیفیت
- بررسی وجود مسیرهای ناوبری و Context: PASS (Static).
- بررسی وجود CSS لایه بندی تقویم: PASS (Static).
- عدم تغییر API و قرارداد داده PDL: PASS (diff source-level).
- npm ci offline: FAIL (dependency `zod-to-json-schema` not cached).
- Angular production build: NOT RUN (dependencies unavailable).
- Java backend build / Oracle CRUD / Browser E2E: NOT RUN.
- تطبیق جامع 47 فرم با HTML مرجع: NOT COMPLETED.

این نسخه برای آزمایش اولیه است و پذیرش نهایی QA/Regression محسوب نمی شود.
