# CLT – Use Case Driven Delivery Checkpoint (2026-11-10)

**Baseline:** `core-banking-prototype-v0-2026-11-10-0815.zip` supplied by the user; `VERSION=0.11.0` remains unchanged. The prior insurance cancellation patch was confirmed installed. **Roadmap R0..R7 phase names, sequence and gates are NOT modified.** In response to the explicit implementation priority, engineering proceeds with BUCs R4/R5 while BANK-GATED R3 items stay OPEN. This is *not* approval or operational closure of R3.

## Business capability status — official `Collateral_Business_Use_Cases_FA(2).html`

| BUC | Official business operation | Evidence in supplied source / this patch | Remaining for actual BUC completion | Status |
|---|---|---|---|---|
| 01 | تعریف داده‌های مرجع وثیقه | CLT reference CRUD/UI and governance foundations | 17/17 catalog UAT and bank-policy decisions | PARTIAL |
| 02 | استعلام و انتخاب شخص مالک | CIF Party lookup/introduction UI | verified integration and end-to-end role/ownership acceptance | PARTIAL |
| 03 | معرفی وثیقه جدید | introduction Stage/review and registration preflight | bank-approved final domain posting and numbering, Oracle UAT | PARTIAL |
| 04 | ثبت اقلام شمارشی | candidate item range/specificity and draft checks | atomic physical item registration and series controls | PARTIAL |
| 05 | اصلاح پیش از وابستگی عملیاتی | amendment Stage and independent decision | operational amendment DML and G-01/G-03/G-14 evidence | PARTIAL |
| 06 | ثبت ضامن مستقل | Stage and conditional atomic Sponsor posting candidate | approval/invalidity policy, DBA uniqueness, Oracle tests | PARTIAL |
| 07 | ثبت یا تمدید بیمه‌نامه | Stage and conditional CREATE/RENEW posting candidate | coverage bank rules/DBA, actual Oracle and external-insurer acceptance | PARTIAL |
| 08 | ارزیابی و تجدید ارزیابی | Stage and conditional valuation FINAL/renewal candidate | valuer policy, cadence, migration and Oracle UAT | PARTIAL |
| 09 | درخواست تخصیص وثیقه | **NEW** analyst-staged request POST/receipt/journal through shared DataSource in local prototype, with idempotency, debtors and branch checks | Consumer attestation, approved CLT MODULE_ID, policy, serializable capacity lock and **final allocation to CLT.COLLATERAL_ALLOCATION** | PARTIAL – Stage-only |
| 10 | رفع تخصیص برای یک تعهد | **NEW** branch-limited analyst Stage submit/cancel with optimistic version and audit; Stage only | Consumer owner instruction/attestation, approved mapping, real `USED`/capacity finalization and Oracle UAT | PARTIAL – Stage-only |
| 11 | آزادسازی نهایی وثیقه | R5 read-only evidence and preview exists; unchanged by this patch | bank legal-hold and ownership policy, all obligations removed, status history + final state mutation | NOT IMPLEMENTED for final effect |
| 12 | معرفی مجدد دارایی آزادشده | history and uniqueness preflight foundations | approved reintroduction identity/registration, BUC11 final state, G-14 | NOT IMPLEMENTED end-to-end |
| 13 | استعلام مستقیم با شماره انتظامی | direct lookup UI/API | identity uniqueness and G-17 authoritative numbering/UAT | PARTIAL |
| 14 | پایش کسری پوشش | capacity/shortfall snapshots and report previews | bank minimum policy, insured/valued/current/allocated facts, operational scheduler | PARTIAL |
| 15 | مدیریت بین‌واحدی وثیقه | branch filtering and cross-branch guards | introduction-level grants and authorized cross-branch transaction processing | PARTIAL |
| 16 | مشاهده تاریخچه و ردگیری | read-only history/ledger/reporting and Stage receipts | full final operational audit and trace-to-BUC UAT | PARTIAL |

**None of the 16 BUCs is being declared END-TO-END ACCEPTED on the basis of this patch.** Functional completion requires working UI → API → Oracle mutation (where applicable) → observable response plus valid bank approvals. Static Java smoke tests alone do NOT prove business acceptance.

## Delivery changes in this iteration

1. All active Collateral Java classes and JUnit classes using `CollateralR3*`/`CltR3*` are renamed to **business-domain class names**. Angular workflow/worklist/cancellation/preview source files and symbols are similarly domain-named. Historical numbered SQL/docs and past smoke-test identifiers are retained for traceability.
2. Canonical API `/api/v1/collaterals/workflow` replaces the frontend call to `/api/v1/collaterals/r3-workflow`. **Both old and new routes** are exposed by the same controller, not duplicate Spring beans. `/.../{id}/lifecycle-evidence` now has a domain alias preserving `/.../{id}/r3/lifecycle-evidence`.
3. R4 BUC09 local analyst Stage request: `POST /api/v1/collaterals/{collateralId}/allocation-requests/intake/analyst` with Idempotency-Key. It can store **request data only**, never approve/allocate capacity. The existing consumer-authorized service route is not altered.
4. R5 BUC10 local analyst Stage: `POST /api/v1/collaterals/{collateralId}/allocations/{allocationId}/unallocation/analyst-requests` with Idempotency-Key / If-Match, and `POST .../{requestId}/cancel`, branch ownership and snapshot consistency. It does **not** touch `CLT.COLLATERAL_ALLOCATION.USED` or actual capacity.
5. For `core-banking.collateral.prototype.enabled=true` **and only when the relevant Stage flag is enabled**, R4/R5 request writers reuse the application `DataSource`, not another Hikari credential/pool. Existing alternative writer remains for non-prototype deployments, guarded by its original configuration. No database credentials are embedded in the patch.
6. Original sponsor, insurance (including insurer logo/site changes) and valuation HTML forms remain unchanged. Installer removes old class/source paths *with SHA/backup safeguards* to prevent duplicate beans.

## Gated behaviors / risk register

- **Default disabled:** `core-banking.collateral.allocation.intake.request-writes-enabled=false`, `core-banking.collateral.unallocation.intake.request-writes-enabled=false`. Do not enable until the CLT-owned Stage tables and sequences (only SQL Candidates in `database/oracle/clt/r4c` and `r5b`) are DBA-approved and actually deployed.
- Local Stage endpoints require the existing **internally minted, loopback-only** `CLT_UNIFIED_LOCAL_DEV` principal and branch resolver; no browser JWT or new user management. `CLT_LOCAL_ANALYST` is *not* an authenticated external loan/guarantee/LC consumer. Do not confuse local Stage with a bank-authorized financial instruction.
- G-02 (durable allocation idempotency/finalization), G-04/G-05/G-07/G-09/G-10/G-13/G-16/G-18, and R5 G-19..G-22 contract topics remain OPEN; no artificial gap closure. Schema reconciliation and authorizations are prerequisites, not reasons to stop R4/R5 source development.
- R5 final release remains read-only/preview. No status mutation, Oracle migration, policy or financial effect is performed by installer.
- QA must separate policy smoke, Java syntax/typecheck with mocked dependencies, Angular source parser and guarded installer tests from a real Maven/Angular build, Oracle/live concurrency and analyst UAT.

## Next engineering slice / exit criterion

Implement the **approved consumer obligation adapter and true atomic allocation with capacity lock** (R4 / BUC09) after bank-domain ownership and Oracle schema facts are confirmed; then **real unallocation** (R5 / BUC10), **final collateral release** (R5 / BUC11), **reintroduction** (BUC12), and remaining BUC01..16 to complete verified UAT. Track per-BUC independently; do not redefine or rename the canonical R0..R7 phases.
