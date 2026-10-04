DPS2 Account Operations R10P-HF1 - Professional Workspace + Lifecycle R8 Compatibility
=====================================================================================
Baseline: core-banking-prototype-v0-2026-10-04-1409
Cumulative: R10N + R10O + R10P + HF1
DDL/Migration: none

این بسته همان R10P حرفه‌ای است به‌علاوه Hotfix سازگاری Verifier چرخه عمر R8.

علت HF1:
- R10P برای کاندیدهای فعال‌سازی، status=PENDING_ACTIVATION را حفظ کرده و partyId مشتری را نیز برای Scope صحیح اضافه کرده بود.
- Verifier قدیمی R8 فقط Exact String بدون partyId را قبول می‌کرد و در build-production.cmd یک False Negative ایجاد می‌کرد.
- HF1 Verifier را Semantic می‌کند و خود Installer نیز R8 را قبل از Build اجرا می‌کند.

Qualification روی Baseline تمیز 14:09:
- Lifecycle R8: 34/34 PASS
- R10P: 41/41 PASS
- R10O: 20/20 PASS
- R10N: 15/15 PASS
- R10M: 13/13 PASS
- R10L: 10/10 PASS
- R10I: 24/24 PASS
- Release Layout: PASS
- تمام Static Gateهای build-production.sh تا مرحله Maven: PASS
- توقف Build Linux فقط به دلیل عدم امکان دانلود Maven 3.9.16 از repo.maven.apache.org بود.

اجرا از Root پروژه اگر ZIP همان‌جا Extract شده:
  patch-r10p-hf1\APPLY-DPS2-ACCOUNT-OPERATIONS-R10P-HF1.cmd D:\Projects\core-banking-prototype-v0

سپس:
  cd /d D:\Projects\core-banking-prototype-v0
  build-production.cmd
