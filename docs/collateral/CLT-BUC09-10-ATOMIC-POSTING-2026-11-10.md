# Collateral BUC-09 / BUC-10 — تراکنش نهایی تخصیص و رفع تخصیص (Candidate)

**Baseline:** `CLT-ROADMAP-R1.0-FA.md` (R4/R5 ثابت).  **Source:** installed BUC-15 cumulative source, 2026-11-10.

## Execution contracts

- `GET /api/v1/collaterals/{collateralId}/posting/readiness` — readable by branch analyst; reports enabled/config/schema evidence, not an approval.
- `POST /api/v1/collaterals/{collateralId}/posting/allocate` — signed service JWT with scope `clt.allocation.post`, `client_credentials`, verified `client_id` and bank-owned LOAN/etc mapping. Body **only** `{ "permitId":"..." }` plus `Idempotency-Key: UUID`.
- `POST /api/v1/collaterals/{collateralId}/posting/unallocate` — signed service JWT `clt.unallocation.post`, same request format. NO analyst/browser POST is exposed.
- Payload does not accept amount, debtor ID, policy factor or permission: these live in independently **approved, signed-off, persisted bank permit**. No issuing/approving endpoint is present.

### BUC-09 atomic unit of work

Authenticated service → parent `CLT.COLLATERAL FOR UPDATE WAIT 2` → approved permit row lock → compare approved R4C Stage digest and amount/file/debtors → re-validate current parent version, active type/status, effective dates, current FINAL valuation, bank-attested coverage and consumer obligation → read actual current exposures → verify one currency and certified capacity limit (bounded by recorded eligible value) → insert `CLT.COLLATERAL_ALLOCATION` → append `CLT.COLLATERAL_TRANSACTION` with bank-issued transaction number → increment parent version → mark permit consumed and store idempotent receipt → COMMIT. On any failure: single transaction ROLLBACK.

### BUC-10 atomic unit of work

Authenticated service → same parent lock order → approved permit and R5B Stage digest → validate original posted allocation transaction and target allocation/version under lock → calculate `FULL`/`PARTIAL` against exact outstanding amount → set `USED='NO'` on full or decrease active `ALLOCATE_AMOUNT` on partial → append transaction referencing `ORIGINAL_TRANSACTION_ID` → bump parent version → consume permit and durable idempotency key → COMMIT. Full unallocation preserves original amount in the historical inactive row, while partial updates current outstanding amount.

## Non-negotiable acceptance and open gates

- Feature flags **default OFF** (`CLT_ALLOCATION_POSTING_ENABLED`, `CLT_UNALLOCATION_POSTING_ENABLED`). All Oracle mapping values default blank and are checked against catalog metadata. Local prototype and synthetic JWT cannot issue financial posting.
- DBA/middleware must approve `CLT_COVERAGE_POST_PERMIT` migration, indexes, ref codes and existing core insert semantics/Sequence high-water; candidate script starts with `RAISE_APPLICATION_ERROR` and contains no executable DDL. Installer never runs SQL. Need actual Oracle E2E and concurrent transactions.
- Domain owner must issue the obligation/debtor attestation and bank must issue an independent, expiring approval with a certified capacity limit/coverage policy. No approval is inferred from CLT Type percentages, raw capacity, analyst input or a successful preflight.
- All external financial writers must use the same parent-row lock before any allocation mutation, otherwise global serializability **cannot** be claimed. BUC-15 cross-unit requests are distinct: final cross-branch posting remains denied here until a branch-transfer/permission posting contract is approved.
- `G-02 / G-09 / G-10 / G-13 / G-16 / G-19 / G-20` remain OPEN for bank-approved policy/migrations and live validation. BUC-09/10 **source candidate implemented**, not UAT/Oracle/business acceptance.
- Existing analyst Stage UI is preserved; new readiness labels show the separate operational posting gate. Customized insurance/sponsor/valuation HTML untouched. No IAM changes.

## UAT target scenarios (not run)

1. BUC-09 positive: bank permit+service instruction → one active allocation and one posted transaction, exactly one permit consumed.
2. BUC-09 concurrency: two different permits for the same collateral → no over-allocation under 2s parent lock.
3. Replay same key → same receipt and no new write; different key after commit → 409; duplicate key on another permit → atomic rollback.
4. Stale valuation, changed collateral version, mixed-currency use or missing Stage → denied, no write.
5. BUC-10 partial → active row amount reduced, ledger event appended; FULL → USED=NO and capacity returned; original transaction linked.
6. Wrong consumer domain, client, scope, branch, expired permit, non-independent maker/checker/poster → denied.
7. Restart/retry/concurrent Oracle exception → no half-committed state; DB data and journal reconcile.

Detailed constraints and discovery: `database/oracle/clt/posting/00_BUC09_10_READONLY_DISCOVERY.sql`, `01_BUC09_10_BANK_PERMIT_CANDIDATE_NOT_APPROVED.sql`.
