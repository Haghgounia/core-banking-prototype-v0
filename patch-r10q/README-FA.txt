R10Q — پالایش حرفه‌ای رابط عملیات حساب

این Patch تجمعی است و تغییرات R10N + R10O + R10P-HF1 + R10Q را حمل می‌کند.
Baseline هدف: core-banking-prototype-v0-2026-10-04-1409

اصلاحات R10Q:
1) حذف نمایش تکراری فرآیند بستن/بازگشایی و قرار دادن آن داخل Workspace مرحله 08.
2) تفکیک بصری روشن «انواع سقف حساب» و «محدودیت بر اساس نوع تراکنش» در مرحله 07.
3) تفکیک فرم ثبت درخواست بستن/بازگشایی از وضعیت درخواست و Actionهای تأیید/اجرا.
4) Responsive و Theme-token based؛ بدون DDL یا تغییر Business Backend.

اجرا از Root پروژه (در صورت Extract شدن ZIP در همان Root):
  patch-r10q\APPLY-DPS2-ACCOUNT-OPERATIONS-R10Q.cmd D:\Projects\core-banking-prototype-v0

سپس:
  build-production.cmd
