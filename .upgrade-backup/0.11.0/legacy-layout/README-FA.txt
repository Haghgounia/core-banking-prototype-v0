R10M-HF1 - Phase 11N-B Verifier Compatibility
==============================================

این Hotfix فقط Verifier تاریخی Phase 11N-B را با تصمیم جدید R10M هم‌راستا می‌کند:
- عنوان حساب قابل ویرایش
- واحد نگهدارنده فقط‌خواندنی
- حفظ ORG_UNIT_CODE جاری در Backend
- عدم تغییر OPENING_ORG_UNIT_CODE

پیشنهاد: Patch را خارج از Root پروژه Extract کنید، مثلا:
  D:\Temp\R10M-HF1

سپس:
  D:\Temp\R10M-HF1\patch-r10m-hf1\APPLY-DPS2-ACCOUNT-OPERATIONS-R10M-HF1.cmd D:\Projects\core-banking-prototype-v0

بعد:
  cd /d D:\Projects\core-banking-prototype-v0
  build-production.cmd
