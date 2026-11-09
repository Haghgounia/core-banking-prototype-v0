# قرارداد تراکنشی بیمه وثیقه — CLT R3 (پیشنهادی)

**مرجع:** BUC-07 / SUC-09 / SVC-07، `Collateral_DB_Specification_FA(2).html`، `Collateral_UI_Final_Offline_V1.42_FA(2).html`، `CLT-R3-POSTING-DECISION-MATRIX-FA.md`.

| محور | قرارداد پیاده‌سازی‌شده | شرط تصویب بانک / محدودیت |
|---|---|---|
| تقاضا | Stage `COLLATERAL_INSURANCE_REQUEST`, Version=3, REVIEWED و Audit/Fingerprint | Schema Stage و Maker/Reviewer واقعی |
| عملیات | `CREATE` یا `RENEW` بر اساس `PREVIOUS_INSURANCE_ID`; درج رکورد جدید، نه overwrite قبلی | عدم فرض Cancel خودکار؛ G-06 باز |
| تعارض | یکتایی `(INSURANCE_COMPANY_ID,INSURANCE_NUMBER)`, نوع/دوره بدون هم‌پوشانی | قید UQ_CI_POLICY در DBA Dictionary معتبر |
| Renewal | قاعده همان وثیقه و نوع؛ آغاز روز بعد؛ عدم وجود جانشین قبلی | قید فیزیکی جدید Unique روی PREVIOUS_INSURANCE_ID؛ D-05 |
| پوشش | Rule نوع وثیقه × نوع بیمه × ارز × Revision و مقایسه حداقل مبلغ | Rule و مبلغ فقط از Approval واقعاً ثبت‌شده Oracle؛ بدون Defaults ساختگی |
| Release | شرط تصویب Business, Data, Integration, Coverage, Renewal, Cancellation Impact | فقط Approved در بازه موثر، Approver مستقل از Poster |
| Atomicity | یک DataSource و TransactionTemplate، Lock Stage/Collateral/Rule/Parent، Insert Ledger/Domain | آزمون E2E همزمان و Deadlock قبل از روشن‌کردن Flag |
| Idempotency | Receipt durable، Unique Request و Branch+Key، Replay مشروط | هیچ استفاده از key مرورگر برای شناسه رکورد بانک |
| Audit | PolicyRevision, Reason, Hash, Actor, CreatedAt/PostedAt, Reference Rule | نام/شماره بیمه و مبلغ در Log/Receipt منتشر نمی‌شود |
| Cancellation | **پیاده‌سازی نشده** | قرارداد ابطال، دلیل، اثر تعهد و تاریخچه Append-only طبق G-06 لازم است |
| Allocation | تأیید نشده | حتی `COMMITTED` و پوشش یک نوع بیمه به معنی کفایت مجموعه پوشش‌های وثیقه نیست |

`01_INSURANCE_POSTING_SCHEMA_CANDIDATE_NOT_APPROVED.sql` فقط برای Review است و عمداً در ابتدا خطا می‌دهد. Installer هیچ‌یک از کدهای SQL Candidate را اجرا نمی‌کند. بانک باید پس از Recon و ثبت اسناد تصمیم، Migration امضاشده جداگانه تولید و در محیط کنترل‌شده تست کند.
