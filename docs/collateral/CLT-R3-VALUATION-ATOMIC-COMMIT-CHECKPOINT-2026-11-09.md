# Checkpoint توسعه R3 — ثبت عملیاتی/تجدید ارزیابی (کاندید)

**2026-11-09**؛ نصب موفق آخرین Patch `CLT-R3-INSURANCE-ATOMIC-COMMIT-2026-11-09` توسط کاربر اعلام شد. خط مبنای این نسخه `core-banking-prototype-v0-2026-11-09-R3-INSURANCE-ATOMIC-COMMIT-SOURCE.zip` است.

- فاز **R3 ضامن، بیمه و ارزیابی** در نقشه راه ثابت `CLT-ROADMAP-R1.0` ادامه دارد؛ عناوین/ترتیب R0..R7 تغییر نکرده‌اند و R4/R5 شروع جدید ندارند.
- کد `CollateralR3ValuationCommitPolicy` و `...Service` مسیر مشروط `REVIEWED -> FINAL History -> SUPERSEDED old -> CURRENT pointer -> COMMITTED Ledger` را در یک تراکنش DataSource مشترک پیاده می‌کند.
- برای ایمنی، قفل Stage/Collateral/Previous، Check دریافت شده از Oracle، سه Audit، Fingerprint، version=3، Ref عدم تکرار، Rule شمول/تناوب/ارزیاب با APPROVED واقعی، CAS اشاره‌گر، Constraint فیزیکی و Idempotency لازم‌اند.
- UI کارتابل مشترک دارای Activation/Receipt/Commit مشروط و تفکیک «رسید جاری» از «رسید تاریخی» است؛ سه فرم سفارشی اصلی ضامن/بیمه/ارزیابی تغییر نمی‌کنند.
- Feature Flag `r3-posting.valuation-enabled=false`؛ هیچ DBA migration یا تغییر داده قطعی در زمان نصب؛ Candidate DDL Fail-Closed.
- G-07/G-18/G-09/G-10 و شکاف فنی D-06 **OPEN**، هیچ تصمیم رسمی یا نرخ Haircut/تاریخ کسب‌وکاری حدسی اعمال نشده؛ اعتبار تخصیص/سررسید رسمی جداگانه است.
- تست‌های کد و نصب صرفاً برای محیط شبیه‌سازی‌شده هستند؛ Oracle Live، Maven/Angular و UAT بانکی تأیید نشده‌اند. گام بعد R3: تکمیل تحلیل قرارداد لغو بیمه و سازگاری R3 با R4، بدون تغییر فاز ثابت.

شرح کامل: [CLT-R3-VALUATION-TRANSACTION-CONTRACT-FA.md](CLT-R3-VALUATION-TRANSACTION-CONTRACT-FA.md).
