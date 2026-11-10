# CLT BUC-03 / BUC-13 registration readback checkpoint (2026-11-10)

- Canonical roadmap: `CLT-ROADMAP-R1.0` (R0-R7 unchanged).
- Version: `0.11.0`; feature: `CLT-BUC03-BUC13-REGISTRATION-READBACK`.
- Source basis: verified HF1 P1 baseline, not the unpatched 17:22 archive.
- Evidence: `CLT-ORACLE-REFERENCE-RECONCILIATION-2026-11-10.json`, checked `2026-11-10T14:56:38Z`, database FREEPDB1.
- Installation baseline: `CLT_P1_APPLIED=16/16`, `CLT_HF1_RUNTIME_VERIFIED_PASS=YES`, `CLT_HF1_VERIFICATION_PASS=YES` supplied by analyst.

## Actual Oracle status at P1 checkpoint

17 references visible, 13 with rows, 4 observed empty: `COLLATERAL_TYPE`, `COLLATERAL_ATTRIBUTE_DEFINITION`, `COLLATERAL_TYPE_ATTRIBUTE`, `COLLATERAL_INSURANCE_TYPE`. `CLT_NO_DATA` branch and 0 own-branch rows do not constitute positive BUC-03 UAT. G-03/G-12 and G-14 remain NOT_VERIFIED, G-16 is BLOCKED (`DEFAULT_REFERENCE_MISMATCH`), G-17 numbering format remains unapproved, BUC-03 write/readback remains BLOCKED. `bankAccepted=false`.

## This code delivery (non-financial readback, no bank approval)

1. BUC-13: `GET /api/v1/collaterals/by-registration/{number}/verified-readback`. First fetch the canonical branch-scoped collateral. If it is absent, return the existing 404 without querying related entities. If present, read registration ledger, number authority, independent review, source draft, verified asset link, persisted attribute values and registered serial ranges when the corresponding objects exist. Numeric registration identifiers remain decimal strings.
2. BUC-03: `GET /api/v1/collaterals/introduction-reviews/{reviewId}/registration-readback` is scoped to the maker and its server-derived branch; no other maker's reviewed draft is disclosed.
3. The existing introduction workflow and direct-number UI expose optional on-demand evidence panels; they do not create a registration, issue a number, grant bank approvals or fabricate Oracle rows.
4. The local read-only UAT chain only admits the BUC-13 GET endpoint. The maker-only BUC-03 GET retains its separate authorization requirement, which the generic local read-only principal does not impersonate.
5. New Java pure-policy JUnit regression and static read-only contract verification are part of the delivered source.

## Explicit acceptance separation

| Dimension | Delivery evidence | Acceptance claim |
|---|---|---|
| SOURCE_IMPLEMENTED | New readback API, integrity comparisons, UI and test sources | YES (read-only features) |
| SOURCE_POLICY_TESTED | Local 18-assertion pure-Java smoke and 24 static contract checks | YES (restricted scope) |
| BUILD_VERIFIED | Production Windows Maven + Angular + JAR | NOT_VERIFIED at packaging |
| ORACLE_VERIFIED | New BUC-03 and BUC-13 positive E2E readback | NOT_VERIFIED |
| ANALYST_UAT_PASSED | Positive/negative evidence from target UI | NOT_VERIFIED |
| BANK_ACCEPTED | Independent banking approval | NO |
| FINAL_REGISTRATION_WRITER | Approved safe posting with identity/numbering gate | NOT IMPLEMENTED / DISABLED |

## Business blockers intentionally not bypassed

- No approved `COLLATERAL_TYPE` master rows in the last observation; this is an observed empty table, not a technical outage.
- G-03/G-12: approved countability model and durable registered serial-range table; there are competing proposal shapes; do not execute either candidate as a migration.
- G-14: authoritative verified asset identity and one-current collateral constraint.
- G-16: Oracle transaction status default `PENDING` versus active status reference; do not infer an approved replacement.
- G-17: approved decimal-string numbering/issuance scheme within Oracle NUMBER(19,0), concurrency and uniqueness.
- Independent bank/DBA approval for any schema change or DML; any release must fail closed until it exists.

## Recommended UAT once real approved test data exists

1. Open the existing collateral direct lookup with a known 1-19-digit registration number in the authorized branch; verify the actual core profile loads, then select Oracle Readback.
2. Observe state `HISTORICAL_CORE_WITHOUT_LEDGER` for pre-existing core records without the proposed ledger; never mark them NOT_REGISTERED.
3. Use a reviewed, maker-owned introduction review and execute its registration readback. `NOT_REGISTERED` only applies after the ledger schema is observable and has no corresponding registration row. `LEDGER_SCHEMA_NOT_READY` means missing schema/evidence, not zero records.
4. Validate that conflicting numbers, record ownership, approval row, draft hash, status, asset link or missing child data never result in `BANK_ACCEPTED=true`.
5. Validate that an unauthorized branch or maker receives no registration information. No write endpoint was added.

## Next engineering work

After bank approval and the explicit Oracle migration/data decision, implement and test the atomic first-registration writer, number-issuance authority, asset identity linkage, immutable serial ranges and status history, followed by actual BUC-03/13 write/readback UAT. R0-R7 remain fixed; no new phase or R3/R6 class naming.
