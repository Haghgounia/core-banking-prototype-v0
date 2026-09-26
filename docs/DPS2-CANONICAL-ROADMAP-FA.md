# نقشه راه Canonical عملیات حساب سپرده (DPS2)

**وضعیت سند:** Canonical / Single Source of Truth  
**نسخه پروژه:** `0.11.0`  
**تاریخ تثبیت:** 2026-09-23  
**آخرین به‌روزرسانی اجرایی:** 2026-09-24  
**دامنه:** Deposit Account Operations / «چهار سپرده»  

> این فایل مرجع رسمی ادامه توسعه DPS2 است. گفتگوهای ChatGPT، نام‌گذاری موقت فازها، یا پیشنهادهای معماری به‌تنهایی مجاز به تغییر مسیر پروژه نیستند. هر تغییر Roadmap باید ابتدا در این فایل ثبت شود.

---

## 1. هدف

این Roadmap برای جلوگیری از تغییر مسیر بین Chatها و Releaseها ایجاد شده است. مبنای برنامه‌ریزی، پیاده‌سازی و اعلام `CLOSED` فقط Trace بین سند مرجع، Package، Action، Table، API، Implementation و Verification است.

قاعده اصلی:

```text
Reference Document
  -> Operational Step
  -> Owning Package(s)
  -> Required Action(s)
  -> Required Table/Field(s)
  -> API / Service
  -> Implementation Phase
  -> Static Gate
  -> DB Gate
  -> Runtime E2E Gate
```

هیچ Capability یا Operational Step فقط با PASS شدن یک فاز فنی `DONE/CLOSED` محسوب نمی‌شود.

---

## 2. اسناد مرجع الزام‌آور

نسخه‌های Canonical مرجع همراه Repository نگهداری می‌شوند:

1. `docs/dps2/reference/Deposit_Account_Operations_6_RC_2026-09-22.xml`
   - مدل Enterprise Architect / XMI
   - مرجع Packageها، Entity/Tableها، Relationshipها و Scope دامنه

2. `docs/dps2/reference/Deposit_Account_Operations_Operational_v11_RC_2026-09-22.html`
   - Operational UI / Prototype
   - مرجع گام‌های 00 تا 17 و Actionهای کاربر

3. `docs/dps2/reference/Deposit_Account_Operations_Traceability_Guide_FA_v2_2026-09-22.html`
   - Trace Contract رسمی بین UI -> XML -> Database -> Verification
   - مرجع اصلی Coverage Matrix

### اولویت مرجع در تعارض

```text
1) XML Domain Model
2) Traceability Guide
3) Operational HTML
4) این Canonical Roadmap
5) Phase QA / Runbook
6) Chat / پیشنهاد موقت
```

اگر بین اسناد 1 تا 3 تعارض واقعی مشاهده شد، موضوع باید به‌عنوان `DOCUMENT_CONFLICT` در همین Roadmap ثبت شود و قبل از تغییر مدل Target تعیین تکلیف گردد.

---

## 3. قواعد وضعیت

فقط سه وضعیت برای Coverage سند استفاده می‌شود:

- `DONE`: تمام Actionهای مستند، Persistence/Trace، API و Runtime Gate پوشش داده شده‌اند.
- `PARTIAL`: بخشی از Requirement مستند پیاده شده است.
- `NOT_STARTED`: پیاده‌سازی عملیاتی Requirement شروع نشده یا صرفاً DDL/Prototype موجود است.

### تفاوت مهم Technical Phase و Document Step

ممکن است یک Technical Phase مانند `11G` از نظر Static/DB/Runtime **CLOSED** باشد، اما Operational Step متناظر در سند هنوز `PARTIAL` باشد.

مثال فعلی:

```text
Historical example before 11N-C:
Technical Phase 11G Profit / Interest Processing = CLOSED
Document Step 04 Deposit Profit Operations       = PARTIAL

Current state after 11N-C final qualification:
Technical Phase 11N-C Steps 03–04 Canonical Closure = CLOSED
Document Step 03 Term Deposit Operations             = DONE
Document Step 04 Deposit Profit Operations            = DONE
```

این دو وضعیت از این پس هرگز معادل فرض نمی‌شوند.

---

## 4. فازهای فنی اجراشده تا امروز

| فاز فنی | Scope | وضعیت فنی | توضیح Coverage سند |
|---|---|---:|---|
| 11A | Account Operations Schema | CLOSED | Foundation؛ معادل بسته‌شدن هیچ Step کامل نیست |
| 11B | Account Servicing Core | CLOSED | بخشی از Step 01 |
| 11C | Lifecycle / Hold | CLOSED | بخش مهمی از Step 02 |
| 11D | Balance / Subledger | CLOSED | Package 17 foundation مشترک |
| 11E | Closure / Reopening | CLOSED | بخشی از Step 08 |
| 11F | Term Deposit Operations | CLOSED | بخش مهمی از Step 03 |
| 11G | Profit / Interest Processing | CLOSED | بخشی از Step 04؛ Accrual + Same-deposit posting |
| 11H | Step 04 Canonical Profit Completion | CLOSED | Profile / Period / Accrual Detail / Adjustment / Payment؛ مقصدهای خارجی وابسته به Step 05 |
| 11I | Step 05 Deposit Transaction Processing | CLOSED | Static 64/64 PASS؛ Oracle DB 29/29 PASS؛ Runtime E2E در 2026-09-24 با cash deposit/withdrawal/reversal/internal transfer/rejection PASS شد |
| 11J | Wave A Servicing & Lifecycle Completion (Steps 01–02) | CLOSED | Static 78/78 PASS؛ Oracle DB 29/29 PASS؛ Production Build + 51 Maven tests PASS؛ Runtime Attribute/Condition/Signatory/Bulk PASS و Activation-only qualifier با BLOCKED_BY_SOURCE_READINESS به‌صورت کنترل‌شده PASS شد |
| 11K | Wave B Statements, Limits & Maturity Batch (Steps 06–08) | CLOSED | Static 92/92 + Oracle DB 34/34 + Production Build + Runtime E2E PASS در 2026-09-24 |
| 11L | Wave C Account Services, Party Access, Compliance, Pricing & Tax (Steps 09–13) | CLOSED | Static 89/89 + Oracle DB 40/40 + Production Build + 51 Maven tests + Runtime E2E + `DPS2_WAVE_C_11L_QUALIFICATION_PASS` در 2026-09-24 |
| 11M | Wave D Reconciliation, Exceptions, Correspondent & Rewards (Steps 14–17) | CLOSED | Final hotfix qualification در 2026-09-24: Static 92/92 PASS؛ Oracle DB 40/40 retained PASS؛ Production Build + 51 Maven tests PASS؛ Runtime E2E PASS با Reconciliation/Discrepancy/Exception/Correction واقعی و defer صریح Correspondent/Reward بدون config |
| 11N-A | Step 00 Read-only Dashboard | CLOSED | Static 36/36 + Oracle DB 9/9 + Production Build + 51 Maven tests + Runtime E2E + `DPS2_PHASE11NA_STEP00_DASHBOARD_QUALIFICATION_PASS` در 2026-09-24 |
| 11N-B | Steps 01–02 Canonical Closure | CLOSED | Qualification نهایی 2026-09-24: Static 77/77، Oracle reconciliation PASS، DB 20/20، Production Build + 51 Maven tests PASS؛ controlled Product Version `1->5` ساخته شد؛ Runtime واقعی READY→ACTIVE، ORG transfer، Condition/Signatory، Product Change، Lifecycle/Bulk و Collateral origin-idempotency همگی PASS؛ `DPS2_PHASE11NB_STEPS01_02_QUALIFICATION_PASS` |
| 11N-C | Steps 03–04 Canonical Closure | CLOSED | Qualification نهایی 2026-09-25: Static 72/72، canonical CHECK reconciliation PASS، DB 18/18، `PHASE11NC_IMPLEMENTATION_PASS`، Production Build + Angular Build + Maven 51/51 PASS؛ Runtime واقعی External Profit `TX-39`، Partial Withdrawal `TX-40`، Maturity `TX-41` و Early Termination `TX-42/CLOSURE-9` همگی PASS؛ `PHASE11NC_RUNTIME_E2E_PASS` و `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`. |

### Qualification نهایی 11G

آخرین Runtime qualification ثبت‌شده:

```text
PHASE11G_RUNTIME_E2E_ACCOUNT=9
PHASE11G_RUNTIME_E2E_ACCRUAL=3
PHASE11G_RUNTIME_E2E_POSTING=1
PHASE11G_RUNTIME_E2E_PASS
```

آخرین DB qualification:

```text
PHASE11G_DB_VERIFIER_PASS=30
PHASE11G_DB_VERIFIER_FAIL=0
PHASE11G_DB_BASELINE_PASS
PHASE11G_IMPLEMENTATION_PASS
```

این PASS فقط Scope فنی 11G را می‌بندد، نه تمام Step 04 سند را.

---

## 5. Coverage Matrix رسمی سند 00 تا 17

> این جدول مبنای انتخاب کار بعدی است. تا زمانی که Action-by-Action audit کامل نشده، وضعیت محافظه‌کارانه `PARTIAL` یا `NOT_STARTED` حفظ می‌شود.

| Step | Operational Area | Packageهای اصلی سند | وضعیت Canonical | آنچه فعلاً داریم | Gap اصلی |
|---:|---|---|---|---|---|
| 00 | نمای عملیاتی سپرده | 01A, 01B, 01C, 03, 13, 17 | DONE | Phase 11N-A: Dashboard read-only روی Account/Hold/Transaction/Balance با aggregation ارزی؛ Static 36/36، DB 9/9، Build، 51 tests و Runtime PASS | — |
| 01 | نگهداری حساب سپرده | 01A, 05, 06, 00 | DONE | 11N-B final qualification: current `ORG_UNIT_CODE` از immutable Opening snapshot جدا شد؛ Condition Override و Signatory Authority audited؛ Positive same-Product Product Version Change با Maker/Checker و history واقعی PASS | — |
| 02 | چرخه عمر، راکدی و انسداد | 01B, 01C, 00, 17 | DONE | 11N-B final qualification: governed Opening -> READY -> atomic READY→ACTIVE، activation-run/status-history trace، Lifecycle/Bulk و Collateral Hold/Release با business-origin idempotency همگی Runtime PASS | — |
| 03 | عملیات سپرده مدت‌دار | 13, 06, 03, 17, 00 | DONE | 11N-C final qualification: maturity instruction execution، renewal/settlement، conversion audit، Partial Withdrawal و Early Termination با Step 05 transaction/subledger trace و controlled Package 16 closure؛ Runtime PASS | — |
| 04 | مدیریت سود سپرده | 02, 17 | DONE | Canonical Profile/Period/Accrual/Detail/Adjustment/Payment؛ Same-deposit posting و `LINKED_ACCOUNT`/`CUSTOMER_SELECTED_ACCOUNT` internal destinations از Step 05 با transaction/subledger trace؛ 11G contract/posting فقط compatibility projection | — |
| 05 | تراکنش‌های سپرده | 03, 17, 01C, 15, 07, 00 | DONE | 11I Source + Oracle + Runtime: Transaction lifecycle، Validation، Maker/Checker Authorization، Legs، Cash/Transfer Detail، transaction-aware 11D posting trace و non-destructive Reversal؛ Static 64/64، DB 29/29، Runtime PASS | — |
| 06 | صورتحساب سپرده | 14, 03, 17 | DONE | Phase 11K: Template/version, Subledger-grounded Statement header/items, governed delivery + Static 92/92 + Oracle 34/34 + Runtime PASS | — |
| 07 | سقف و محدودیت حساب | 15, 00 | DONE | Phase 11K: four prototype limit types, Restriction lifecycle + materialized Usage from actual POSTED status history + Runtime POST/Reversal PASS | — |
| 08 | بستن حساب و پردازش سررسید | 16, 01A, 01B, 01C, 13, 03, 17, 00 | PARTIAL | 11E Closure/Reopening core + 11K Maturity Batch + 11N-C اجرای مالی واقعی maturity از Step 03/05/Package16 با Runtime PASS | 11N-D audit/regression نهایی Action-by-Action پیش از `DONE` |
| 09 | خدمات حساب | 05 | DONE | Phase 11L: Inquiry, Confirmation, Notification Preference/Event و API Access با Oracle/Build/Runtime PASS | — |
| 10 | طرف، دسترسی و ابزار | 06 | DONE | Phase 11L: Signature Rule, Delegation, Authorized User, Beneficiary و Payment Instrument روی Party فعال حساب؛ Oracle/Build/Runtime PASS | — |
| 11 | مقررات و انطباق | 07, 00 | PARTIAL | Phase 11L: Regulatory Restriction + Release و Compliance Evaluation از Rule فعال governed | Reserve Requirement/Position و Regulatory Report/Item هنوز خارج از 11L |
| 12 | قیمت‌گذاری و کارمزد | 08, 00 | PARTIAL | Phase 11L: Account Pricing Override با Maker/Checker + Trace موجود Fee Assessment/Profitability | Fee Rule/Tier و Package Enrollment workflow کامل هنوز باز است |
| 13 | مالیات و کسورات | 09, 02, 03, 00 | PARTIAL | Phase 11L: Exemption + governed Tax Calculation + Certificate با Transaction/Profit trace | Tax Adjustment, Liability/Payment, Reconciliation و Rule authoring هنوز باز است |
| 14 | تطبیق و مغایرت | 10, 11 | PARTIAL | Phase 11M qualified: Run/Item، Match/Mismatch، Discrepancy، Suspense و Exception handoff | 11N-H audit/regression نهایی Action-by-Action پیش از `DONE` |
| 15 | استثنا و اصلاح | 11, 03, 17 | PARTIAL | Phase 11M qualified: Exception/Assignment/RCA + Maker/Checker Correction؛ REVERSAL مالی از Step 05 | 11N-H audit نهایی؛ CORRECTION/BACKDATED_CORRECTION نیازمند instruction مالی مستقل در صورت الزام سند |
| 16 | نوسترو / وسترو | 04, 01A, 10 | PARTIAL | Phase 11M source: Account Extension + Reconciliation Profile؛ تطبیق از Step14 reuse می‌شود | External settlement network همچنان boundary خارجی است؛ Runtime config فقط با داده معتبر |
| 17 | جوایز و قرعه‌کشی | 12, 03 | PARTIAL | Phase 11M source: مصرف Program/Eligibility governed، Enrollment و مشاهده Entry/Winner با payment transaction trace | Program/Draw/Winner fabrication خارج از runtime؛ qualification با config واقعی یا defer صریح |

---

## 6. Gap دقیق Step 04 که بعد از 11G باقی مانده است

مدل مستند Step 04 این اجزا را دارد:

```text
DEPOSIT_ACCOUNT_PROFIT_PROFILE
  -> DEPOSIT_PROFIT_PERIOD
     -> DEPOSIT_PROFIT_ACCRUAL
        -> DEPOSIT_PROFIT_ACCRUAL_DETAIL
     -> DEPOSIT_PROFIT_ADJUSTMENT
     -> DEPOSIT_PROFIT_PAYMENT
```

11G فعلی این مسیر عملیاتی را نیز اضافه کرده است:

```text
DEPOSIT_PROFIT_CONTRACT
  -> DEPOSIT_PROFIT_ACCRUAL
  -> DEPOSIT_PROFIT_POSTING
```

و برای Coexistence در `DEPOSIT_PROFIT_ACCRUAL` dual-write انجام می‌شود.

### وضعیت Step 04 پس از 11N-C

- Profit snapshot / applied rate trace: IMPLEMENTED
- Daily accrual + ACT/365 و ACT/360: IMPLEMENTED
- Canonical `DEPOSIT_ACCOUNT_PROFIT_PROFILE`: IMPLEMENTED
- Canonical `DEPOSIT_PROFIT_PERIOD`: IMPLEMENTED
- `DEPOSIT_PROFIT_ACCRUAL_DETAIL`: IMPLEMENTED
- `DEPOSIT_PROFIT_ADJUSTMENT` maker-checker workflow: IMPLEMENTED
- `DEPOSIT_PROFIT_PAYMENT`: IMPLEMENTED
- Same-deposit payment: Package 17 trace retained
- `LINKED_ACCOUNT` / `CUSTOMER_SELECTED_ACCOUNT` internal destination payment: Step 05 transaction + leg + subledger trace IMPLEMENTED
- 11G `DEPOSIT_PROFIT_CONTRACT` / `DEPOSIT_PROFIT_POSTING`: compatibility projection only; canonical business truth remains Profile -> Period -> Accrual/Adjustment/Payment

Final Windows qualification on 2026-09-25 emitted `PHASE11NC_RUNTIME_E2E_PASS` and `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`; therefore Step 04 is `DONE`.

---

## 7. Gap اصلی Step 05 — Transaction Processing

Step 05 باید یک Transaction Engine مستقل بسازد. وجود `DepositBalanceService.post()` در 11D به‌تنهایی این Step را پوشش نمی‌دهد.

حداقل اجزای مستند:

```text
DEPOSIT_TRANSACTION
DEPOSIT_TRANSACTION_VALIDATION
DEPOSIT_TRANSACTION_AUTHORIZATION
DEPOSIT_TRANSACTION_STATUS_HISTORY
DEPOSIT_TRANSACTION_LEG
DEPOSIT_TRANSFER_DETAIL
DEPOSIT_CASH_TRANSACTION_DETAIL
DEPOSIT_TRANSACTION_REVERSAL
DEPOSIT_SUBLEDGER_ENTRY
DEPOSIT_ACCOUNT_BALANCE
```

Validation باید با Hold، Account Limit و Regulatory Restriction هم ترکیب شود.

---

## 8. ترتیب ادامه کار

از این تاریخ، ادامه توسعه به روش زیر انجام می‌شود:

### Gate A — Coverage Audit

قبل از نام‌گذاری هر فاز جدید، برای Step موردنظر باید این Matrix تکمیل شود:

```text
Action
Table/Field
Existing API
Existing Service
Static Test
DB Verification
Runtime E2E
Status
```

### Gate B — انتخاب Scope بعدی

اولویت پیش‌فرض:

1. تکمیل `PARTIAL`های زودتر در زنجیره وابستگی؛
2. سپس `NOT_STARTED`ها به ترتیب Dependency، نه صرفاً شماره UI؛
3. هر انحراف از این ترتیب باید در همین Roadmap با دلیل ثبت شود.

### Coverage Audit ثبت‌شده برای Step 04

Audit کد و Schema در 2026-09-24 نشان داد:

| Action مستند Step 04 | وضعیت قبل از 11H | Gap |
|---|---|---|
| بارگذاری Profit Profile | PARTIAL | runtime از `DEPOSIT_PROFIT_CONTRACT` می‌خواند و `DEPOSIT_ACCOUNT_PROFIT_PROFILE` canonical نبود |
| شناسایی سود | PARTIAL | Accrual وجود داشت، اما `PROFIT_PERIOD_ID` و `DEPOSIT_PROFIT_ACCRUAL_DETAIL` canonical تولید نمی‌شد |
| تعدیل سود | NOT_STARTED | workflow روی `DEPOSIT_PROFIT_ADJUSTMENT` وجود نداشت |
| پرداخت سود | PARTIAL | Same-deposit posting وجود داشت، اما `DEPOSIT_PROFIT_PAYMENT` canonical ثبت نمی‌شد |

وابستگی مستند: پرداخت به مقصدهای `LINKED_ACCOUNT` و `CUSTOMER_SELECTED_ACCOUNT` برای اجرای مالی کامل به Step 05 / Package 03 Transaction Processing وابسته است.

### Phase 11H — Step 04 Canonical Profit Completion

**وضعیت:** CLOSED

**Trace:** Operational Step 04 -> Package 02 + Package 17 -> Profile / Period / Accrual Detail / Adjustment / Payment.

Scope مصوب 11H:

1. Provision/Backfill عملیاتی `DEPOSIT_ACCOUNT_PROFIT_PROFILE` از Opening Profit Snapshot؛
2. ایجاد و نگهداری `DEPOSIT_PROFIT_PERIOD` برای قراردادهای MATURITY موجود؛
3. اتصال Accrual جدید به Period و ایجاد `DEPOSIT_PROFIT_ACCRUAL_DETAIL`؛
4. workflow غیرمخرب Adjustment با Maker/Checker روی `DEPOSIT_PROFIT_ADJUSTMENT`؛
5. ثبت canonical `DEPOSIT_PROFIT_PAYMENT` برای Same-deposit payment و Trace آن به 11D Subledger؛
6. حفظ `DEPOSIT_PROFIT_CONTRACT` / `DEPOSIT_PROFIT_POSTING` به‌عنوان `MODEL_EXTENSION` سازگار، نه جایگزین مدل مستند؛
7. Static + DB + Runtime E2E Gate مستقل 11H.

خارج از Scope 11H و وابسته به Step 05:

- اجرای مالی `LINKED_ACCOUNT`؛
- اجرای مالی `CUSTOMER_SELECTED_ACCOUNT`؛
- Transaction authorization/legs/reversal کامل.

**Qualification نهایی 11H:**

```text
PHASE11H_DB_VERIFIER_PASS=18
PHASE11H_DB_VERIFIER_FAIL=0
PHASE11H_DB_BASELINE_PASS
PHASE11H_IMPLEMENTATION_PASS
PHASE11H_RUNTIME_REOPENED_PERIOD_ACCOUNT=9
PHASE11H_RUNTIME_REOPENED_PERIOD=2
PHASE11H_RUNTIME_REOPENED_PERIOD_POSTING=3
PHASE11H_RUNTIME_REOPENED_PERIOD_PAYMENT=3
PHASE11H_RUNTIME_REOPENED_PERIOD_PASS
PHASE11H_RUNTIME_E2E_ACCOUNT=11
PHASE11H_RUNTIME_E2E_ACCRUAL=6
PHASE11H_RUNTIME_E2E_PERIOD=3
PHASE11H_RUNTIME_E2E_DETAIL=4
PHASE11H_RUNTIME_E2E_ADJUSTMENT=3
PHASE11H_RUNTIME_E2E_POSTING=4
PHASE11H_RUNTIME_E2E_PAYMENT=4
PHASE11H_RUNTIME_E2E_PASS
```

**قانون Closure سند (historical):** در پایان 11H، Step 04 تا زمان اجرای destinationهای خارجی/داخلی انتخاب‌شده از مسیر Step 05 `PARTIAL` بود. این شرط در 11N-C برآورده شد و Step 04 پس از qualification نهایی 2026-09-25 به `DONE` رسید.


### Phase 11I — Step 05 Deposit Transaction Processing

**Historical state at that point:** Source implementation complete; Static Gate PASS; Oracle DB requalification 29/29 PASS; Runtime qualification was pending. Final state: `CLOSED`.

**Trace:** Operational Step 05 -> Package 03 + Package 17 + Package 01C + Package 15 + Package 07 + Package 00.

Scope مصوب 11I مستقیماً از Action Trace سند:

1. `DEPOSIT_TRANSACTION` lifecycle: `INITIATED -> VALIDATED -> PENDING_AUTH/AUTHORIZED -> POSTED` و مسیرهای `REJECTED/FAILED/REVERSED`;
2. ثبت `DEPOSIT_TRANSACTION_VALIDATION` برای Account Status، Hold، Limit و Regulatory Restriction؛
3. ثبت و تصمیم `DEPOSIT_TRANSACTION_AUTHORIZATION` با Maker/Checker؛
4. تولید `DEPOSIT_TRANSACTION_STATUS_HISTORY` برای تمام transitionهای عملیاتی؛
5. تولید `DEPOSIT_TRANSACTION_LEG` و اتصال posting reference به Package 17؛
6. `DEPOSIT_TRANSFER_DETAIL` برای Transfer و `DEPOSIT_CASH_TRANSACTION_DETAIL` برای Cash؛
7. posting اتمیک/Idempotent از طریق 11D `DepositBalanceService`;
8. `DEPOSIT_TRANSACTION_REVERSAL` و posting معکوس non-destructive؛
9. Runtime E2E شامل حداقل Credit، Debit، Transfer، validation failure، authorization و reversal؛
10. اتصال Step 04 external profit destinations به Transaction Engine فقط بعد از PASS خود 11I.

**خارج از Scope 11I:** پیاده‌سازی کامل UI/CRUD مستقل Step 07 Limits و Step 11 Regulatory/Compliance؛ در 11I فقط enforcement روی داده‌های موجود/فعال آن Packageها انجام می‌شود. Gapهای خود Step 07 و 11 همچنان جداگانه `NOT_STARTED`/`PARTIAL` می‌مانند.

**Source Gate 11I (2026-09-24):**

```text
PHASE11I_STATIC_VERIFIER_PASS=64
PHASE11I_STATIC_VERIFIER_FAIL=0
PHASE11I_STATIC_BASELINE_PASS
```

Regression baseline پس از Source implementation:

```text
11H = 64/64 PASS
11G = 92/92 PASS
11F = 64/64 PASS
11E = 49/49 PASS
11D = 56/56 PASS
```

Oracle DB Gate 11I در 2026-09-24 ابتدا 19/19 و پس از Sequence High-Water hardening با `PHASE11I_DB_VERIFIER_PASS=27` و `FAIL=0` عبور کرد. Production Build سپس یک compile regression در `DepositTransactionService.requireAccount` آشکار کرد؛ hotfix constructor-type ثبت و Static Gate به 57/57 ارتقا یافت.

Oracle requalification نهایی 11I در 2026-09-24 با `PHASE11I_DB_VERIFIER_PASS=29` و `FAIL=0` عبور کرد. Runtime نهایی نیز با `PHASE11I_RUNTIME_E2E_PASS` عبور کرد؛ بنابراین Phase 11I `CLOSED` است.

### Phase 11J — Wave A Servicing & Lifecycle Completion (Steps 01–02)

**وضعیت:** CLOSED — Static Gate `78/78 PASS`؛ Oracle DB Gate `29/29 PASS`؛ Windows Production Build و 51 تست Maven PASS؛ Runtime Attribute/Condition/Signatory/Bulk PASS و Activation-only qualifier نیز در 2026-09-24 با `PHASE11J_RUNTIME_ACTIVATION_PASS` و `DPS2_PHASE11J_ACTIVATION_HOTFIX_QUALIFICATION_PASS` عبور کرد. نتیجه `BLOCKED_BY_SOURCE_READINESS` یک outcome معتبر و مستند بود و readiness را جعل نکرد.

**Trace:** Operational Step 01 + Step 02 -> Packages 01A + 01B + 01C + 05 + 06 + 00 + 17.

Scope مصوب 11J از Gap Audit و Action Trace سند:

1. تکمیل `DEPOSIT_ACCOUNT_ATTRIBUTE` در نگهداری حساب؛
2. تکمیل `DEPOSIT_ACCOUNT_CONDITION_OVERRIDE` با `DEPOSIT_OPERATION_APPROVAL_REQUEST`؛
3. تغییر نسخه محصول همان Product/Family با `DEPOSIT_ACCOUNT_PRODUCT_HISTORY` و Approval trace؛
4. Resolve کردن `PARTY_ID -> SIGNATORY_ID` و ثبت `DEPOSIT_ACCOUNT_SIGNATORY_AUTHORITY`؛
5. اجرای کنترل‌شده Activation روی `DEPOSIT_ACTIVATION_RUN` + `DEPOSIT_ACTIVATION_CHECK` برای حساب `PENDING_ACTIVATION`؛
6. اجرای Bulk lifecycle/hold روی `DEPOSIT_ACCOUNT_BULK_ACTION` + `DEPOSIT_ACCOUNT_BULK_ACTION_ITEM` بدون دورزدن ownership/release policy؛
7. Angular UI/API برای تمام Actionهای بالا؛
8. Static + DB + Runtime gates مستقل؛ هیچ جدول جدیدی ساخته نمی‌شود چون Oracle baseline 2026-09-24 همه جدول‌های Trace را دارد.

**قاعده اجرا:** 11J یک Wave تجمیعی بود تا توسعه به micro-hotfix خرد نشود. Phase 11I و 11J اکنون CLOSED هستند؛ Coverage سند Steps 01–02 همچنان مستقل و محافظه‌کارانه audit می‌شود.

**Source Gate 11J (2026-09-24):**

```text
PHASE11J_STATIC_VERIFIER_PASS=78
PHASE11J_STATIC_VERIFIER_FAIL=0
PHASE11J_STATIC_BASELINE_PASS
PHASE11J_REPOSITORY_JAVAC_PASS
PHASE11J_SERVICE_JAVAC_PASS
PHASE11J_TS_PARSE_PASS_MISSING_DEPS_ONLY
```

Windows production build در 2026-09-24 PASS شد: Backend compile/package PASS، Angular production build PASS و Maven tests برابر 51/51 PASS. Oracle DB verifier نیز 29/29 PASS شد. Activation-only Runtime qualifier نیز PASS شد؛ بنابراین Technical Phase 11J `CLOSED` است.

### Phase 11K — Wave B Statements, Limits & Maturity Batch (Steps 06–08)

**Historical state at phase start:** implementation source در 2026-09-24 آغاز شده بود و تا Static + Oracle + Production Build + Runtime E2E باز می‌ماند. Final state: `CLOSED`.

**Trace:** Operational Step 06 -> Packages 14 + 03 + 17؛ Step 07 -> Packages 15 + 00؛ Step 08 -> Packages 16 + 01A + 01B + 01C + 13 + 03 + 17 + 00.

Scope مصوب 11K مستقیماً از Action Trace سند:

1. `DEPOSIT_STATEMENT_TEMPLATE` با version/status و retire غیرمخرب نسخه‌های قبلی؛
2. `DEPOSIT_STATEMENT_REQUEST` + `DEPOSIT_STATEMENT` + `DEPOSIT_STATEMENT_ITEM` با Opening/Closing/Running Balance بازتولیدپذیر از `DEPOSIT_SUBLEDGER_ENTRY` و Transaction trace هرجا موجود باشد؛
3. `DEPOSIT_STATEMENT_DELIVERY` با attempt history و channel/destination validation؛ `EMAIL/SMS` فقط از Contact فعال حساب و `PORTAL/API` فقط از مقصد سیستمی Prototype؛
4. `DEPOSIT_ACCOUNT_LIMIT` + `DEPOSIT_ACCOUNT_LIMIT_USAGE` با مصرف periodized که از تراکنش‌های واقعاً `POSTED` بازسازی می‌شود و بعد از Reversal کاهش پیدا می‌کند؛
5. `DEPOSIT_ACCOUNT_TRANSACTION_RESTRICTION` با Release غیرمخرب و حفظ origin/release policy؛
6. `DEPOSIT_MATURITY_PROCESS_RUN` + `DEPOSIT_MATURITY_PROCESS_ITEM` + `DEPOSIT_MATURITY_EVENT` برای کشف قراردادهای term سررسیدشده؛
7. Maturity Batch فقط event را در `PENDING` materialize می‌کند و نتیجه مالی را جعل نمی‌کند؛ Renewal/Settlement/Closure نهایی از workflowهای Step 03/05/11E عبور می‌کند؛
8. Term Renewal موجود در صورت وجود event همان cycle، همان event را به نتیجه نهایی progress می‌دهد و duplicate ایجاد نمی‌کند؛
9. Angular UI/API برای Step 06/07 و Maturity Batch Step 08؛
10. Migration صرفاً fail-fast + sequence high-water reconciliation است، چون Oracle baseline 2026-09-24 تمام جداول canonical را از قبل دارد.
11. Source Gate 11K پس از تطبیق مجدد با Operational prototype برابر `88/88 PASS` است؛ Oracle target برابر `34/34` است و هنوز باید روی Windows baseline اجرا شود.
12. Qualification تجمیعی با `tools\qualify-dps2-wave-b-11k.cmd` انجام می‌شود: Oracle -> یک Production Build -> health wait -> Runtime E2E.

**خارج از Scope 11K:** بازنویسی 11E Closure/Reopening، جعل Settlement مالی maturity، و بسته‌شدن خودکار Document Step 08 صرفاً به دلیل PASS فنی Wave B.

---

## 9. Change Control برای جلوگیری از تغییر مسیر بین Chatها

از این پس این قوانین اجباری‌اند:

1. هر Chat جدید برای DPS2 باید ابتدا این فایل را بخواند.
2. هیچ Roadmap جدیدی صرفاً در Chat ساخته نمی‌شود.
3. هر فاز جدید باید در همین فایل ثبت شود **قبل از شروع کدنویسی**.
4. هر Phase QA باید به Step/Package/Action/Table سند مرجع اشاره کند.
5. Technical Phase `CLOSED` فقط بعد از Static + DB + Runtime Gate مجاز است.
6. Document Step `DONE` فقط وقتی مجاز است که تمام Actionهای Traceability Guide پوشش داده شده باشند.
7. اگر حین پیاده‌سازی مدل جدیدی خارج از XML اضافه شد:
   - باید به‌عنوان `MODEL_EXTENSION` ثبت شود؛
   - دلیل آن نوشته شود؛
   - Coexistence/Migration strategy مشخص شود؛
   - تا زمان اصلاح سند EA، نباید آن را بخشی از Requirement اصلی معرفی کرد.
8. تغییر نام یا حذف Requirement سند بدون ثبت `DOCUMENT_CHANGE` ممنوع است.
9. هر Release ZIP باید این Roadmap و سه سند مرجع را همراه داشته باشد.
10. در پایان هر فاز، همین Roadmap باید هم‌زمان با کد Update شود.

---

## 10. Architectural Debt ثبت‌شده

### AD-01 — Profit Model Coexistence

**وضعیت:** OPEN  
**کشف‌شده در:** Phase 11G

دو نسل مدل Profit هم‌زمان وجود دارند:

```text
Document model:
ACCOUNT_PROFIT_PROFILE -> PROFIT_PERIOD -> ACCRUAL / ADJUSTMENT / PAYMENT

11G extension:
PROFIT_CONTRACT -> ACCRUAL -> PROFIT_POSTING
```

اقدام موقت: Dual-write روی `DEPOSIT_PROFIT_ACCRUAL`.

اقدام نهایی: در زمان تکمیل Step 04 باید Target Model رسمی تعیین و سپس EA/XML یا implementation مطابق تصمیم نهایی هم‌راستا شود.

### AD-02 — Technical Phase Closure vs Document Coverage

فازهای 11A-G قبلاً به‌عنوان Technical delivery بسته شده‌اند، اما این نباید به معنی DONE بودن Stepهای 00-17 تفسیر شود.

---

## 11. Definition of Done برای کل Deposit Account Operations

کل پروژه DPS2 فقط زمانی از منظر سند مرجع `COMPLETE` است که:

- تمام Stepهای 00 تا 17 `DONE` باشند؛
- برای هر Action مستند API/Service واقعی وجود داشته باشد؛
- Persistence و Trace keys با XML/Traceability Guide هم‌راستا باشند؛
- تمام mutationها Idempotency/Approval/History لازم را داشته باشند؛
- اثر مالی از Transaction/Subledger/Balance قابل Trace باشد؛
- DB verifier و Runtime E2E برای هر Step PASS باشند؛
- هیچ `DOCUMENT_CONFLICT` یا `MODEL_EXTENSION` باز و تعیین‌تکلیف‌نشده‌ای باقی نماند.

---

## 12. Current Canonical State

```text
Project version: 0.11.0
Technical delivery: 11A .. 11N-C CLOSED
Document coverage: NOT COMPLETE
Highest verified runtime milestone: Phase 11N-C PASS — `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`
Current implementation scope: Phase 11N-D — Steps 05–10 Canonical Regression / Audit
Current source baseline: Steps 00–07 and 09–10 are DONE; Step 08 remains PARTIAL pending 11N-D final action-by-action audit. 11N-C final Windows qualification on 2026-09-25 passed Static 72/72، schema reconciliation، DB 18/18، Production/Angular Build، Maven 51/51 and all Runtime branches: External Profit `TX-39`، Partial Withdrawal `TX-40`، Maturity `TX-41`، Early Termination `TX-42/CLOSURE-9`.
Next action: Phase 11N-D — audit/regression of Steps 05–10 only; no new feature unless the canonical source audit proves a real document gap.
Roadmap authority: docs/DPS2-CANONICAL-ROADMAP-FA.md
```

این بخش باید در پایان هر فاز به‌روزرسانی شود.

### Phase 11L — Wave C Account Services, Party Access, Compliance, Pricing & Tax (Steps 09–13)

Scope 11L بر اساس Action Trace سند: 
1. Step 09: Inquiry، Confirmation، Notification Preference/Event و API Access.
2. Step 10: Signature Rule، Delegation، Authorized User، Beneficiary و Payment Instrument.
3. Step 11: Account Regulatory Restriction و Compliance Evaluation؛ ساخت Regulatory Rule و Reserve/Report engine خارج از Scope این فاز است.
4. Step 12: Account Pricing Override با Maker/Checker و نمایش Trace Fee Assessment/Profitability؛ Rule/Tier/Package authoring در Product/Pricing bounded context باقی می‌ماند.
5. Step 13: Tax Exemption، Tax Calculation فقط با Tax Rule فعال و taxable-event trace، و Tax Certificate؛ Adjustment/Liability/Payment/Reconciliation در Wave بعدی باقی می‌ماند.
6. هیچ Regulatory/Fee/Tax Rule مصنوعی توسط Runtime یا Migration ساخته نمی‌شود.
7. Migration فقط existence/sequence reconciliation است و business DML ندارد.
8. Runtime 11L جریان‌های مستقل از Rule authoring را کامل تست می‌کند و subflowهای وابسته به Rule governed را فقط در صورت وجود Rule واقعی مجاز می‌داند.


### Phase 11M — Wave D Reconciliation, Exceptions, Correspondent & Rewards (Steps 14–17)

Scope 11M بر اساس Action Trace سند:
1. Step 14: `DEPOSIT_RECONCILIATION_RUN/ITEM`، `DEPOSIT_DISCREPANCY` و `DEPOSIT_SUSPENSE_OPEN_ITEM` با handoff به Exception.
2. Step 15: Exception Case/Assignment، Root Cause Analysis و Correction Request/Authorization/Entry. اجرای مالی `REVERSAL` فقط از Step 05 Transaction engine انجام می‌شود و اصل تراکنش حذف/بازنویسی نمی‌شود.
3. Step 16: `CORRESPONDENT_ACCOUNT_EXTENSION` و `CORRESPONDENT_RECONCILIATION_PROFILE` برای NOSTRO/VOSTRO؛ شبکه settlement خارجی خارج از bounded context است.
4. Step 17: Reward Program/Eligibility موجود به‌عنوان governed configuration مصرف می‌شود؛ Enrollment و Entry/Winner trace نمایش داده می‌شود و Prize payment باید به `DEPOSIT_TRANSACTION` متصل باشد.
5. Migration فقط schema/sequence reconciliation است و business seed/DML ندارد.
6. Runtime 11M یک Cash Deposit واقعی Step05 ایجاد می‌کند، Match/Mismatch و Suspense/Exception را تست می‌کند و Correction REVERSAL را Maker/Checker تا corrective transaction اجرا می‌کند.
7. Correspondent/Reward در Runtime فقط در صورت وجود config معتبر اجرا می‌شوند؛ در غیر این صورت defer صریح ثبت می‌شود و داده ساختگی ایجاد نمی‌شود.

**Final qualification evidence — 2026-09-24:**

```text
PHASE11M_STATIC_VERIFIER_PASS=92
PHASE11M_STATIC_VERIFIER_FAIL=0
PHASE11M_RUNTIME_ACCOUNT=11
PHASE11M_RUNTIME_RECON_RUN=5
PHASE11M_RUNTIME_DISCREPANCY=4
PHASE11M_RUNTIME_EXCEPTION=4
PHASE11M_RUNTIME_CORRECTION=4
PHASE11M_RUNTIME_CORRECTIVE_TX=28
PHASE11M_RUNTIME_CORRESPONDENT=DEFERRED_NO_EXISTING_CONFIG
PHASE11M_RUNTIME_REWARD=DEFERRED_NO_ACTIVE_PROGRAM
PHASE11M_RUNTIME_E2E_PASS
DPS2_PHASE11M_RECON_RUN_HOTFIX_QUALIFICATION_PASS
```

Technical Phase 11M is therefore `CLOSED`. Document Steps 14–17 remain subject to Phase 11N canonical action-by-action closure.

### Phase 11N — Canonical Coverage Closure

Phase 11N is a closure/audit program, not a general feature wave. A change is permitted only when it closes a documented XML/Traceability/Operational gap or resolves a registered `MODEL_EXTENSION` / `DOCUMENT_CONFLICT`.

| Slice | Canonical scope | Closure intent |
|---|---|---|
| 11N-A | Step 00 | Read-only Dashboard: Accounts, Holds, Transactions, Balance/Availability؛ بدون Business Write |
| 11N-B | Steps 01–02 | Product Version positive branch، Condition/Signatory audit، READY→ACTIVE واقعی، Lifecycle، Hold/Release، Bulk، Collateral idempotency |
| 11N-C | Steps 03–04 | Maturity execution، Renewal/Settlement، Conversion، Partial withdrawal، Early termination، Profit profile/accrual/adjustment/payment |
| 11N-D | Steps 05–10 | Canonical regression/audit؛ بدون Feature جدید مگر Gap واقعی سند |
| 11N-E | Step 11 | Regulatory Restriction، Compliance Evaluation، Reserve Requirement/Position، Regulatory Report/Items |
| 11N-F | Step 12 | Fee Rule/Tier، Pricing Override، Fee Assessment، Package/Benefit/Enrollment، Profitability، Collection posting trace |
| 11N-G | Step 13 | Exemption، Calculation، Adjustment، Liability، Payment، Certificate، Tax Reconciliation |
| 11N-H | Steps 14–15 | Reconciliation، Suspense، Discrepancy، Exception، Assignment، Correction، RCA، regression نهایی |
| 11N-I | Step 16 | Correspondent Account master، NOSTRO/VOSTRO Extension، Reconciliation Profile، Correspondent Reconciliation |
| 11N-J | Step 17 | Reward Program، Eligibility، Enrollment، Draw/Entry/Winner، Prize Payment through Step 05 |
| 11N-K | Cross-Step | UI↔API↔Service↔Oracle، Approval، Idempotency، Transaction، Subledger، Balance، Trace Keys، full 00–17 runtime matrix |

#### Phase 11N-A — Step 00 Dashboard

**Status:** `CLOSED` — full qualification PASS on 2026-09-24.

1. Read source is limited to `DEPOSIT_ACCOUNT`, `DEPOSIT_ACCOUNT_HOLD`, `DEPOSIT_TRANSACTION`, and `DEPOSIT_ACCOUNT_BALANCE`.
2. No new business table or migration is introduced.
3. Dashboard repository is SELECT-only and service transaction is `readOnly=true`.
4. Balance aggregation is by `CURRENCY_CODE`; heterogeneous currencies are never summed into one amount.
5. Qualification evidence:

```text
PHASE11NA_STATIC_VERIFIER_PASS=36
PHASE11NA_STATIC_VERIFIER_FAIL=0
PHASE11NA_DB_VERIFIER_PASS=9
PHASE11NA_DB_VERIFIER_FAIL=0
PHASE11NA_RUNTIME_ACCOUNTS=9
PHASE11NA_RUNTIME_ACTIVE_HOLDS=0
PHASE11NA_RUNTIME_TRANSACTIONS=17
PHASE11NA_RUNTIME_BALANCE_CURRENCIES=1
PHASE11NA_RUNTIME_E2E_PASS
DPS2_PHASE11NA_STEP00_DASHBOARD_QUALIFICATION_PASS
```

Step 00 is therefore `DONE`.

#### Phase 11N-B — Steps 01–02 Canonical Closure

**Status:** `CLOSED` — final qualification PASS on 2026-09-24.

1. Canonical model distinguishes immutable `OPENING_ORG_UNIT_CODE` from required current servicing `ORG_UNIT_CODE`. The migration first preserves the pre-11N mutable value as current ownership, then restores Opening snapshot from `DEPOSIT_OPENING_REQUEST.ORG_UNIT_CODE`.
2. Future account creation initializes both fields from Opening; servicing updates only `ORG_UNIT_CODE` and writes Servicing History.
3. Product Version closure requires a real same-Product alternate version and a Maker/Checker request through executed `DEPOSIT_ACCOUNT_PRODUCT_HISTORY`; no `SKIP_NO_SAME_PRODUCT_CANDIDATE` is accepted as closure evidence.
4. Qualification-only Product Version preparation may create an alternate version only under dedicated `P10F_*` test products via Product Builder API; it must refuse real products, keep the new version non-current, close Origination, keep Servicing active, and never direct-DML PDL tables.
4. Condition override and Signatory Authority remain approval/Party-trace audited.
5. Activation Runtime creates a governed Opening, reaches source `READY`, snapshots an Activation Run, then executes real `READY -> ACTIVE`; direct activation is not accepted.
6. Lifecycle/Bulk remain per-item controlled; generic CLOSE stays outside Step 02 and belongs to Step 08.
7. Collateral Hold requires `COLLATERAL_PLEDGE + SERVICE + ORIGIN_ONLY`; create/release idempotency is additionally keyed by the business origin tuple so a changed transport idempotency key cannot duplicate the same collateral request.
8. Closure gates completed in order: Static 77/77 -> Oracle migration/reconciliation PASS -> DB verifier 20/20 -> Production Build + 51 Maven tests PASS -> Runtime E2E PASS. Final marker: `DPS2_PHASE11NB_STEPS01_02_QUALIFICATION_PASS`. Therefore Steps 01–02 are `DONE` and 11N-B is `CLOSED`.

Final Runtime evidence:

```text
PHASE11NB_QUALIFICATION_PRODUCT_VERSION_CREATED=1:1->5
PHASE11NB_RUNTIME_READY_ACTIVE=7:14
PHASE11NB_RUNTIME_ORG_UNIT=001->11NB-QA
PHASE11NB_RUNTIME_CONDITION=4
PHASE11NB_RUNTIME_SIGNATORY=4
PHASE11NB_RUNTIME_PRODUCT_CHANGE=3:1->5
PHASE11NB_RUNTIME_LIFECYCLE_BULK=5,6
PHASE11NB_RUNTIME_COLLATERAL=4
PHASE11NB_RUNTIME_ACCOUNT=4
PHASE11NB_RUNTIME_ACTIVATED_ACCOUNT=14
PHASE11NB_RUNTIME_E2E_PASS
DPS2_PHASE11NB_STEPS01_02_QUALIFICATION_PASS
```

#### Phase 11N-C — Steps 03–04 Canonical Closure

**Status:** `CLOSED` — Final Windows qualification passed on 2026-09-25; Steps 03 and 04 are `DONE`.

Initial audit scope:

1. Step 03: Maturity instruction execution, Renewal/Settlement, Conversion, Partial Withdrawal, Early Termination, financial/transaction trace.
2. Step 04: Profit Profile, Period, Accrual/Detail, Adjustment, Payment and external-destination execution through Step 05.
3. Resolve AD-01: coexistence/ownership between canonical `ACCOUNT_PROFIT_PROFILE -> PROFIT_PERIOD -> ACCRUAL/ADJUSTMENT/PAYMENT` and 11G `PROFIT_CONTRACT -> ACCRUAL -> PROFIT_POSTING`.
4. Source precedence remains XML -> Traceability Guide -> Operational HTML -> Canonical Roadmap -> QA/Runbook -> chat.
5. No new DDL/API is added unless the action-by-action audit proves a real documented gap.


Coverage Audit result (2026-09-24):

1. **Step 03 real gap confirmed (pre-11N-C):** `DEPOSIT_TERM_PARTIAL_WITHDRAWAL` execution posted directly to Package 17 and left `TRANSACTION_ID` empty; the Traceability Guide requires `DEPOSIT_TRANSACTION -> DEPOSIT_SUBLEDGER_ENTRY` trace and a `DEPOSIT_TERM_SETTLEMENT` summary.
2. **Step 03 real gap confirmed (pre-11N-C):** there was no backend action that executed the stored `MATURITY_INSTRUCTION_CODE`; 11K only materializes a pending maturity event. The documented branches `RENEW_PRINCIPAL`, `RENEW_PRINCIPAL_PAY_PROFIT`, `PAY_TO_ACCOUNT`, `CLOSE_AND_SETTLE`, and `WAIT_INSTRUCTION` therefore require canonical execution orchestration.
3. **Step 03 real gap confirmed (pre-11N-C):** Early Termination created Closure handoff/settlement calculation but did not populate `SETTLEMENT_TRANSACTION_ID`; successful financial settlement must be traceable through Step 05 while final account `CLOSED` remains exclusively Package 16 / Step 08 responsibility.
4. **Step 03 audited/no new feature:** Conversion already preserves `OPENED_PRODUCT_VERSION_ID`, updates current product history under approval, and XML makes `TRANSACTION_ID` optional; no forced financial transaction is added unless a conversion actually has a monetary adjustment.
5. **Step 04 real gap confirmed (pre-11N-C):** `LINKED_ACCOUNT` and `CUSTOMER_SELECTED_ACCOUNT` profit destinations were explicitly blocked. These destinations must execute through Step 05 and expose transaction/subledger trace; unresolved external references remain an external-settlement boundary and must not be fabricated.
6. **AD-01 resolution for 11N-C:** canonical ownership is `DEPOSIT_ACCOUNT_PROFIT_PROFILE -> DEPOSIT_PROFIT_PERIOD -> DEPOSIT_PROFIT_ACCRUAL/DETAIL -> DEPOSIT_PROFIT_ADJUSTMENT -> DEPOSIT_PROFIT_PAYMENT`. `DEPOSIT_PROFIT_CONTRACT` and `DEPOSIT_PROFIT_POSTING` remain operational/compatibility projections (`MODEL_EXTENSION`) for 11G backward compatibility. Dual-write may remain, but closure evidence and future canonical behavior are governed by the canonical profile/period/payment chain.
7. **Implementation boundary:** derived financial actions reuse the existing Step 05 Transaction Engine internally; the public customer transaction API remains limited to its existing user-facing types. No parallel posting engine will be introduced.

#### Phase 11N-D — Steps 05–10 Canonical Regression / Audit

**Status:** `IN_PROGRESS` — Coverage Audit started on 2026-09-25 from the post-11N-C qualified baseline.

Audit result before implementation:

1. **Steps 05, 06, 07, 09 and 10:** current 11I/11K/11L implementations cover every Action listed by the Traceability Guide. 11N-D treats them as regression targets; no new feature is introduced unless Runtime/DB evidence disproves this audit.
2. **Step 08 real gap confirmed:** generic positive-balance Closure in 11E posts directly to Package 17 and does not create a `DEPOSIT_TRANSACTION`; however Step 08 Action Trace explicitly requires `DEPOSIT_TRANSACTION -> DEPOSIT_SUBLEDGER_ENTRY -> DEPOSIT_ACCOUNT_BALANCE` for financial closure execution.
3. **Step 08 real gap confirmed:** `DEPOSIT_ACCOUNT_CLOSURE_SETTLEMENT_ITEM.TRANSACTION_ID` is a canonical FK to `DEPOSIT_TRANSACTION`, but historical generic Closure leaves it null. New executions must populate this trace; verified historical direct-11D rows may remain as legacy evidence and are not rewritten.
4. **Step 08 real gap confirmed:** `DEPOSIT_ACCOUNT_STATUS_HISTORY.APPROVAL_REQUEST_ID` is the canonical maker/checker trace for approval-governed status changes. Closure/Reopening currently omit it even though their lifecycle events retain the approval. New status-history rows must persist the owning Approval ID.
5. **Implementation boundary:** generic financial Closure reuses the existing internal Step 05 derived-transaction primitive; no parallel posting engine is introduced. Package 16 remains the sole owner of the final `ACTIVE -> CLOSED` transition.
6. **Legacy compatibility:** no fabricated transaction/backfill is created for historical closures. DB audit accepts exact historical `ACCOUNT_CLOSURE` Package-17 posting trace, while all new qualified positive-balance closures must expose Step 05 transaction, leg, subledger, settlement-item and closure-reference coherence.
7. Step 08 remains `PARTIAL` until 11N-D Static + Oracle + Production Build + Runtime qualification passes.
8. **11N-D R1 scope correction:** the first qualifier incorrectly invoked full 11L apply/runtime (Steps09–13). The user's Windows run stopped before Oracle execution, so no out-of-scope Oracle migration was applied. R1 is superseded for qualification purposes.
9. **11N-D R2 scope contract:** Oracle/runtime qualification is strictly Step05 via 11I, Steps06–08 via 11K, dedicated Steps09–10 audit/runtime, and evolved 11E controlled closure for Step08. Full 11L apply/runtime is forbidden by the 11N-D static guard; Steps11–13 remain owned by 11N-E/F/G.
10. **Remote Oracle contract:** 11N-D uses `CORE_BANKING_ORACLE_CONNECT` against the configured Oracle host. Local SQL*Plus is preferred when present; otherwise the project OJDBC driver executes the SQL directly. Docker is not part of the remote-Oracle path.
11. **11N-D R2 local static evidence:** `PHASE11ND_STATIC_VERIFIER_PASS=63`, `FAIL=0`; historical regression gates remain 11E=50/50, 11I=64/64, 11K=92/92. Java runner compiles locally. Oracle/Production Build/Runtime remain pending Windows qualification.

### Phase 11H runtime hotfix — reopened paid period
- Runtime qualification on legacy/backfilled account 9 exposed a Step 04 state-transition defect: a previously `PAID` period could receive new accrual/approved adjustment but remain `PAID`, making its outstanding payable balance invisible to posting.
- Hotfix: non-CLOSED periods reopen to `CALCULATED`/`APPROVED` when `PAYABLE_AMOUNT > PAID_AMOUNT`; payment selection excludes only `CLOSED`.
- Post-hotfix qualification **PASSED** on 2026-09-24: legacy account 9 reopened-period settlement PASS and full E2E on account 11 PASS. Phase 11H is `CLOSED`.


### Phase 11I Runtime blocker — Sequence High-Water Hotfix

- Runtime initiate returned HTTP 500 before Step 05 transaction creation.
- Root-cause candidate confirmed by schema/code audit: existing Step 05 sequences were only checked for existence and were not reconciled above `MAX(PK)` on evolved tables.
- Hotfix: reconcile all eight Step 05 sequence high-water marks and verify them in Oracle.
- Static evidence after hotfix: `59/59 PASS`.
- Historical blocker status: at this point 11I remained open pending Oracle sequence gate + production build + runtime E2E; those gates later passed and 11I is `CLOSED`.


### Phase 11I Runtime blocker — Oracle NUMBER RowMapper Hotfix

- Sequence High-Water hypothesis was ruled out by Oracle evidence: all eight Step 05 sequences were ahead of their table maxima and DB Gate passed 27/27.
- Runtime still failed during `POST /transactions` because Initiate builds its response through `view()`.
- `DepositTransactionRepository` used direct casts such as `(Long) ResultSet.getObject(...)` for nullable Oracle `NUMBER(19)` columns. Oracle JDBC may return `BigDecimal` for `NUMBER`, causing `ClassCastException` while mapping Transaction Legs/Idempotency/Reversal state.
- Because `initiate()` is transactional, the response-mapping failure rolled back the freshly inserted transaction, explaining why Oracle still showed `MAX(TRANSACTION_ID)=0`.
- Hotfix uses JDBC typed extraction `ResultSet.getObject(index, Long.class)` consistently for nullable numeric identifiers/counts in the Step 05 repository.
- Static gate after this hotfix: `60/60 PASS`.
- At this historical blocker point, 11I remained `IN_PROGRESS` pending Windows production build/restart and `PHASE11I_RUNTIME_E2E_PASS`; that Runtime gate later passed and 11I is now `CLOSED`.


### Phase 11I Runtime blocker — Transaction-aware Subledger Posting Hotfix

- Oracle NUMBER RowMapper hotfix allowed Runtime to pass Initiate/Validation/Authorization and reach financial Posting.
- Runtime then failed at `POST /transactions/{id}/post` with HTTP 409 `DUPLICATE_VALUE`.
- Step 05 previously created each 11D Subledger row with `TRANSACTION_ID=NULL`, `TRANSACTION_LEG_ID=NULL`, `ENTRY_SEQUENCE_NO=1` and attached transaction trace afterward. This is unsafe for multi-leg transactions because the transaction-scoped unique index requires a distinct `ENTRY_SEQUENCE_NO` per transaction.
- Step 05 now passes `TRANSACTION_ID`, `TRANSACTION_LEG_ID` and `LEG_NO` into the 11D posting primitive so trace is stored atomically in the Subledger INSERT.
- Step 05 posting references now use the dedicated namespaces `DPS2-TX-<tx>-L<leg>` and `DPS2-REV-<tx>-L<leg>` to avoid collision with historical/global posting-reference values.
- Phase 11I migration now reconciles `SEQ_DEPOSIT_SUBLEDGER_ENTRY` above `MAX(SUBLEDGER_ENTRY_ID)`.
- DB verifier additionally proves Subledger sequence high-water and `ENTRY_SEQUENCE_NO = DEPOSIT_TRANSACTION_LEG.LEG_NO` for Step 05 transaction-linked entries.
- Backward compatibility is preserved by retaining the original 10-argument `PostEntryRequest` constructor for non-Step-05 callers. A local `javac 21` constructor compatibility check passed.
- 11I Runtime E2E subsequently passed; Phase 11I is CLOSED.

---

## 13. Oracle / Document Delta Audit — 2026-09-24

مرجع Evidence جدید برای ادامه سریع توسعه:

`docs/DPS2-DELTA-AUDIT-2026-09-24.md`

سه snapshot واقعی Oracle نیز در `docs/dps2/reference/` ثبت شدند:

- `DPS-2026-09-24.txt`
- `DPS-2026-09-24-2.txt`
- `DPS-2026-09-24-3.txt`

نتیجه Audit: سه سند XML/Trace/Operational ارسالی با baseline Repository یکسان‌اند؛ تمام 107 جدول یکتای مورد استفاده در Action Trace گام‌های 00..17 در Full Oracle DDL موجودند. بنابراین ادامه کار باید عمدتاً روی Service/API/UI/Runtime coverage و orchestration تمرکز کند، نه ایجاد مجدد Schema.


### Phase 11J Runtime blocker — Activation Readiness Snapshot Mapping Hotfix

- 11I Runtime E2E passed on 2026-09-24 and Phase 11I is CLOSED.
- 11J Runtime passed Attribute, Condition Override, Signatory Authority and Bulk lifecycle operations; failure is isolated to activation-run creation for a PENDING_ACTIVATION account.
- Source/DDL audit found a contract mismatch: Opening `WAIVER_REASON` is free text (up to 500) but target `DEPOSIT_ACTIVATION_CHECK.RESULT_REASON_CODE` is an optional machine-readable code (60).
- Hotfix stops writing free text to `RESULT_REASON_CODE`, stores bounded readable evidence in `DETAILS`, and prefers `SOURCE_EVALUATION_REFERENCE` for traceability.
- No DB migration is required; rebuild/restart and rerun only the 11J Runtime E2E.

- Targeted qualification helper: `tools\qualify-dps2-phase11j-activation-hotfix.cmd`; it reuses the already-passed 11J DB gate and prior runtime evidence, rebuilds/restarts once, and runs only the pending Activation Runtime gate.

### Phase 11K runtime requalification evidence - 2026-09-24

- Static Gate قبل از Runtime: `88/88 PASS`؛ پس از Limit Usage hotfix: `92/92 PASS`.
- Oracle reconciliation: `PHASE11K_WAVE_B_RECONCILIATION_PASS`.
- Oracle DB verifier: `34/34 PASS`.
- Production build: PASS؛ Maven tests: `51/51 PASS`; Angular build: PASS.
- Runtime اولیه تا Step 07 رسید و با `LIMIT_USAGE_POST_MISMATCH` متوقف شد.
- اصلاح جاری: زمان پیش‌فرض Limit/Restriction از Oracle `SYSTIMESTAMP` گرفته می‌شود و Usage بر اساس `DEPOSIT_TRANSACTION_STATUS_HISTORY.EFFECTIVE_AT` برای وضعیت `POSTED` بازسازی می‌شود؛ requalification runtime هنوز لازم است.

### 11N-C R8 qualification note

- R7 removed the non-canonical `HANDED_OFF_TO_CLOSURE` state and passed Static/Oracle/Build.
- Runtime then exposed a read-projection correlation gap only: the Controlled Closure was created before the Early Termination row, so a `REQUESTED_AT >= CREATED_AT` heuristic could not resolve it.
- R8 uses the shared `APPROVAL_REQUEST_ID` evidence as the exact Early Termination ↔ Controlled Closure trace and adds a DB invariant for that relation.
- No DDL / no business backfill. Phase remains `IN_PROGRESS` until the Windows qualifier emits `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`.


### 11N-C R9 status-history reason reconciliation

- Runtime R8 exposed a genuine canonical cross-package gap: `DEPOSIT_ACCOUNT_CLOSURE` requires `TERM_EARLY_TERMINATION`, but `DEPOSIT_ACCOUNT_STATUS_HISTORY.REASON_CODE` CHECK omitted it.
- Resolution: preserve the exact documented reason and reconcile only the CHECK constraint; no business-data backfill.
- Final Windows qualification on 2026-09-25 emitted `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`; 11N-C is therefore `CLOSED`.

### 11N-C Final Closure Evidence — 2026-09-25

- Static: `PHASE11NC_STATIC_VERIFIER_PASS=72`, fail `0`.
- Schema reconciliation: `PHASE11NC_SCHEMA_RECONCILIATION_PASS`.
- DB: `PHASE11NC_DB_VERIFIER_PASS=18`, fail `0`, `PHASE11NC_DB_BASELINE_PASS`.
- Implementation gate: `PHASE11NC_IMPLEMENTATION_PASS`.
- Production backend/frontend builds: PASS.
- Maven tests: `51` run, `0` failures, `0` errors, `0` skipped.
- Runtime: External Profit `TX-39`; Partial Withdrawal `TX-40`; Maturity `TX-41`; Early Termination `TX-42` with `CLOSURE-9`.
- Runtime final: `PHASE11NC_RUNTIME_E2E_PASS`.
- Qualification final: `DPS2_PHASE11NC_STEPS03_04_QUALIFICATION_PASS`.
- Closure consequence: Step 03=`DONE`, Step 04=`DONE`, 11N-C=`CLOSED`; next scope is 11N-D Steps 05–10 audit/regression.
