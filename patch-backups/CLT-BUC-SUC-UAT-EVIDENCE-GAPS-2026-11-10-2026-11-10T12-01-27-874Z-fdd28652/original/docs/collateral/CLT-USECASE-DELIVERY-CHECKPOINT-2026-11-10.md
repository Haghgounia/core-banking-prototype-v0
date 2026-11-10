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
| 11 | آزادسازی نهایی وثیقه | **BUC11 new**: bank-permit-gated Oracle atomic final status/history/receipt source implementation; UI conditional commit + replay. Existing preview retained | DBA-approved permit schema and issuer, approved status mapping, lock protocol, live Oracle tests/UAT | IMPLEMENTED IN SOURCE; OPERATIONAL GATE OPEN |
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

## BUC11 continuation (2026-11-10)

The new BUC11 bank-permit-gated *final posting code* is in `collateral/release/CollateralReleasePostingService.java`. Unlike R5C read-only preflight, it contains atomic DML for `CLT.COLLATERAL` and `CLT.COLLATERAL_STATUS_HISTORY`, durable permit consumption and replay. The operation is default-disabled and requires a bank-approved auxiliary permit table; the default local prototype authentication cannot post. See `CLT-BUC11-FINAL-RELEASE-POSTING-2026-11-10.md`. This is **not** operational acceptance. Continue implementing remaining BUCs while banking gates are resolved.


## Checkpoint 2026-11-10 — BUC-15 Cross-unit authorization/source

- BUC-15/BR-10/SUC-19: owner-branch grant request, independent bank decision and revocation, exact cross-branch lookup with double grant, and durable nonfinancial consumer/analyst intent implemented in code and UI.
- G-13 is **NOT CLOSED**: 4 CLT tables/sequences/indexes require bank/DBA approval and actual Oracle execution; organizational branch authority and reviewer scope require bank contract.
- Cross-branch allocation posting and capacity usage (BUC-09) remain **NOT IMPLEMENTED/NOT ENABLED**; no false BUC-15 operational acceptance.
- Details in `CLT-BUC15-CROSS-UNIT-USECASE-2026-11-10.md`. R0–R7 roadmap phase definitions unchanged.

### 2026-11-10 — BUC09/10/11/12/15 lifecycle interlock (after posting audit installation)

**Baseline confirmed installed by user:** `CLT-BUC09-10-16-POSTING-AUDIT-2026-11-10`.
Latest proposed source adds single-use Stage CAS on BUC09/10 financial posting; independent bank cross-unit permission revalidation for BUC15 allocation, unresolved/null financial transaction and open cross-unit grant checks for BUC11, and BUC12 reintroduction cross-unit clearance proof. Analyst UI displays read-only lifecycle interlocks. All bank/DBA migrations remain **CANDIDATE NOT APPROVED**. No live end-to-end Oracle acceptance is claimed. R0..R7 labels and criteria are unchanged; reference `CLT-BUC09-10-11-12-15-LIFECYCLE-INTERLOCK-2026-11-10.md`.


### Checkpoint 2026-11-10 - R6 16-BUC / 20-SUC analyst cockpit (source only)

After user-confirmed installation of CLT-BUC09-10-11-12-15-LIFECYCLE-2026-11-10, this slice adds 32 manually scored scenarios across all 16 BUC and mapped 20 SUC, strict evidence export/import, a non-writing GET Oracle dictionary readiness probe with UNKNOWN for unavailability, live BUC-16 history within profile, and amendment navigation. Earlier 10 official UI cases and 17 reference tests remain separate. R6 is **NOT accepted**: live Oracle and full business UAT have not been executed; R0..R7 unchanged. See CLT-R6-BUC-SUC-INTEGRATED-UAT-2026-11-10.md.
