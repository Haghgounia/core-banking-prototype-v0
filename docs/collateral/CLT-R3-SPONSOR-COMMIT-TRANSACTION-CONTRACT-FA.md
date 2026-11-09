# قرارداد فنی پیشنهادی ثبت عملیاتی ضامن — CLT R3

**وضعیت:** CANDIDATE / FAIL-CLOSED — نه حکم بانک، نه Migration مصوب.  **مرجع:** BUC-06, SUC-07, SVC-05، سند فیزیکی EA و `CLT-R3-POSTING-DECISION-MATRIX-FA.md`.

| موضوع | قرارداد موجود در سورس | شرط پیش از استفاده بانکی |
|---|---|---|
| Source | `COLLATERAL_SPONSOR_REQUEST` با `REVIEWED`، Version=3 و Audit=3 | Migration Stage مصوب و Reviewer مستقل واقعی |
| Authority | `SCOPE_clt.collateral.read` و `SCOPE_clt.collateral.review`، Branch از Principal، Maker ≠ Operator ≠ Policy Approver | Security Chain معتبر و تفکیک وظایف/مجوز نهایی بانک |
| Release | `CLT.R3_POSTING_RELEASE` زمان‌دار، دارای ارجاع Business/Data/Integration | Decision Recordهای معتبر، تأیید DBA و مالک کسب‌وکار، کنترل Revocation |
| Idempotency | `R3_POSTING_LEDGER` با دو Unique DB | نصب جداگانه Migration، Backfill و آزمون Replay/Race |
| Domain Write | `INSERT INTO CLT.COLLATERAL_SPONSOR ... RETURNING ID` روی DS/Transaction مشترک | مدل فیزیکی موجود، یکتایی `COLLATERAL_ID,PARTY_ID` و عدم ریسک داده قبلی |
| Audit | Receipt دارای Actor, Reason, Policy Revision, Hash, Domain ID, DB timestamp؛ GET رسید به‌شکل Branch Scoped و بدون افشای Idempotency Key | ممیزی و نگهداری بلندمدت/تغییر وضعیت و ابطال مطابق G-05 |
| Recovery | خطای عملیات پیش از Commit تمام DML را Rollback می‌کند | آزمون واقعی Oracle، Failover، اتصالات موازی و Recovery |
| Safety | `sponsor-enabled=false`؛ release/schema/readiness هم مستقل Fail-Closed | فعال‌سازی فقط پس از تأیید و تست بانک، نه صرف تغییر YAML |

**توجه:** وضعیت ضامن در جدول عملیاتی تأیید/لغو ندارد؛ این عملیات فقط *ایجاد رابطه ضامن* است. حتی بعد از فعال‌سازی تحت مصوبه بانک، حداقل ضامن معتبر G-11 و لغو/ابطال G-05 بسته نمی‌شوند. بیمه و ارزیابی به دلیل G-04/G-06/G-07/G-18 عمداً در این بسته کد ثبت عملیاتی ندارند.

### راهبرد تراکنش

1. Parse ورودی و Scopes؛ Fail-Closed پیش از DML؛ بررسی ستون‌ها، Unique و Release.
2. Lock ردیف Stage و Collateral به همراه Branch از Principal؛ جست‌وجوی Receipt قبلی یا سوءاستفاده از Idempotency Key.
3. Lock ردیف Release معتبر و بازسنجی Preflight + Party/CIF + Payload Hash در همان Transaction.
4. Reserve Ledger با وضعیت `PREPARED`، Insert Domain با `RETURNING`, نهایی‌سازی Ledger با `COMMITTED` و شناسه Domain.
5. در صورت هر خطا، TransactionTemplate کل تغییرات را Rollback می‌کند. Replay فقط با کلید/Hash/دلیل/Version/Actor یکسان مجاز است.

### محدودیت کنترل هم‌زمانی

قفل وثیقه همه Writerهای سازگار با همین قرارداد را سریال می‌کند؛ برای سایر Writerهای بانک قید UNIQUE فیزیکی لازم است. مدل EA فعلی چنین قیدی برای ترکیب `(COLLATERAL_ID,PARTY_ID)` ندارد. ایجاد آن بدون پاک‌سازی رکوردهای تکراری یا بررسی مصرف‌کنندگان فعلی مجاز نیست.
