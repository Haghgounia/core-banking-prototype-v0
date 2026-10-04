DPS2 Account Operations R10P - Professional Workspace Navigation
===============================================================
Baseline: core-banking-prototype-v0-2026-10-04-1409
Cumulative: R10N + R10O + R10P
DDL/Migration: none

هدف R10P:
- حذف تجربه «همه 17 عملیات پشت سر هم» و تبدیل Dockable Menu به Navigator واقعی.
- در هر لحظه فقط Workspace انتخاب‌شده نمایش داده می‌شود.
- Context حساب انتخاب‌شده ثابت می‌ماند و عملیات جاری را نشان می‌دهد.
- Steps 01-15 و 17 فقط برای حساب انتخاب‌شده فعال‌اند.
- عملیات گروهی از Step 02 جدا و فقط روی حساب‌های همان مشتری اجرا می‌شود؛ شناسه خام حساب قابل ورود نیست و Backend نیز scope مشتری را کنترل می‌کند.
- اگر حساب مشترک از جستجوی یک شریک انتخاب شود، همان مشتری مبنای Workspace چندحسابی باقی می‌ماند.
- در عملیات موردی چرخه عمر، فقط حساب‌های همان مشتری قابل انتخاب‌اند.
- Step 16 نوسترو/وسترو از جریان حساب شخصی جدا و به «عملیات ویژه بانکی» منتقل شده است.
- اصلاحات R10N و R10O نیز داخل Patch وجود دارند.

Qualification استاتیک/Regression روی Tree نهایی:
R10P 41/41 PASS
R10O 20/20 PASS
R10N 15/15 PASS
R10M 13/13 PASS
R10L 10/10 PASS
R10I 24/24 PASS
Maintenance R7 27/27 PASS
R10G 9/9 PASS
R10H 10/10 PASS
MAIN Parity R9 44/44 PASS
UI Alignment 103/103 PASS
Phase 11J 78/78 PASS
Phase 11E 50/50 PASS
Phase 11N-B 78/78 PASS
Release Layout PASS

نکته Build:
در محیط تولید Patch، Maven Wrapper به دلیل عدم دسترسی شبکه نتوانست Maven را دانلود کند و npm ci نیز به دلیل نبود Dependencyهای محلی کامل نشد. بنابراین Build کامل Java/Angular باید روی Windows پروژه با build-production.cmd اجرا شود. TypeScript syntax check: TS_PARSE_ERRORS=0.

اجرا از Root پروژه، اگر ZIP را همان‌جا Extract کرده‌اید:
  patch-r10p\APPLY-DPS2-ACCOUNT-OPERATIONS-R10P.cmd D:\Projects\core-banking-prototype-v0

بعد از PASS:
  cd /d D:\Projects\core-banking-prototype-v0
  build-production.cmd

گزارش کامل:
  docs\DPS2-0.11.0-ACCOUNT-OPERATIONS-R10P-WORKSPACE-NAVIGATION-FA.md
