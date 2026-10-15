# R30-HF4 - Deposit Dormancy and Closure Form Repair

Date: 2026-10-13
Baseline: R30-HF3 cumulative source delivered for the 2026-10-13 project.
Scope: PDL Product Builder only. Oracle data are not modified by this patch.

## Root causes and changes

1. **Dormancy / automatic reactivation.** The toggle looked OFF while the Reactive Form could retain NULL, preventing a NOT NULL field from passing validation until the user toggled it. New forms now explicitly use `AUTO_REACTIVATION_ALLOWED=0`; the Backend uses the same default on CREATE only, without overwriting explicit 0 or changing stored rows.
2. **Main closure rule.** Its business code fields are now read from the reviewed PDL form contract, with actual Oracle CHECK-allowed values constraining the choices where present. `IS_CLOSURE_ALLOWED` and `IS_PARTIAL_CLOSURE_ALLOWED` are visible boolean switches with correct defaults.
3. **Pre-closure checks.** `CHECK_TYPE_CODE` and `FAILURE_ACTION_CODE` were not properly resolved to controlled choices. They now expose reviewed Persian-captioned business codes (4 check types and 3 failure actions); `IS_MANDATORY` is a switch, and `SEQUENCE_NO` defaults to 1.
4. **Closure settlement details.** The components, settlement methods, and destinations use reviewed PDL business codes rather than potentially incompatible DPS surrogate codes. A transfer method requires a destination. The details remain linked by `CLOSURE_RULE_ID` to their saved parent.
5. **Closure approval details.** Reviewed approval-level options are provided; `IS_MANDATORY` and approval sequence have explicit defaults. Bounds and ordering receive server validation.
6. **Previously-issued code message.** The review page now neutrally displays the registered immutable code; it no longer incorrectly implies a second issuance was attempted immediately after successful creation. A genuine first issuance already triggers a distinct successful issuance message.

The option set is derived from `docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html`, not from guessed codes. An Oracle CHECK takes priority. If an Oracle CHECK exposes unrecognized numeric codes, the selector remains unavailable instead of pretending that arbitrary DPS numeric identifiers have equivalent meanings; collect the actual CHECK expression and reference rows for mapping review.

## Business contract

| Table | Field | Recognized form codes |
|---|---|---|
| DEPOSIT_PRODUCT_CLOSURE_RULE | CLOSURE_TYPE_CODE | NORMAL, EARLY, FORCED, PARTIAL |
| DEPOSIT_PRODUCT_CLOSURE_RULE | BALANCE_DESTINATION_CODE | CUSTOMER_ACCOUNT, CASH, SUSPENSE, CUSTOMER_SELECTED |
| DEPOSIT_PRODUCT_CLOSURE_RULE | STATUS_CODE | DRAFT, ACTIVE, INACTIVE |
| DEPOSIT_PRODUCT_CLOSURE_PRECHECK | CHECK_TYPE_CODE | ACTIVE_HOLD, PENDING_CHEQUE, OUTSTANDING_DEBT, PENDING_TRANSACTION |
| DEPOSIT_PRODUCT_CLOSURE_PRECHECK | FAILURE_ACTION_CODE | BLOCK, REQUIRE_APPROVAL, WARN |
| DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE | SETTLEMENT_COMPONENT_CODE | PRINCIPAL, PROFIT, COMMISSION, TAX, PENALTY |
| DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE | SETTLEMENT_METHOD_CODE | PAY, DEDUCT, TRANSFER, WAIVE |
| DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE | DESTINATION_CODE | CUSTOMER_ACCOUNT, CASH, BANK_ACCOUNT, CUSTOMER_SELECTED |
| DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE | APPROVAL_LEVEL_CODE | TELLER, BRANCH_MANAGER, REGION_MANAGER, HEAD_OFFICE |

These are *configuration* semantics; execution-time checks and actual money movement remain responsibilities of the operational Deposit domain, not a Product Builder write.

## Verification

On the installed R30-HF3 source, extract the HF4 patch preserving folder structure, then run:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf4-deposit-closure.mjs
node tools\verify-pdl-r30-hf3-submit.mjs
node tools\verify-pdl-r30-hf2-current-version.mjs
build-production.cmd > "%TEMP%\build-r30-hf4.log" 2>&1
echo BUILD_EXIT_CODE=%ERRORLEVEL%
if exist app\BUILD-DIRTY (echo BUILD_NOT_COMPLETE) else (echo BUILD_CLEAN)
```

Local independent verification on the prepared source: HF4 static 42 pass, canonical contract 9 pass, Java scenarios 41 pass, zero failures. HF3/HF2/HF1/R30 verifiers and FEE2 Studio verifier pass. A complete Windows Maven/Angular build and live Oracle UAT are **not** verified in this environment and must be run locally.

## UAT scenarios

- **Dormancy:** Create a new rule and leave automatic reactivation OFF without touching the toggle. Supply the required inactivity period, unit, reactivation method and validity date. Save once; verify the saved flag is 0. Edit and confirm OFF remains selected. Repeat with ON and verify value 1.
- **Main closure:** Create a closure rule and choose a closure type, permitted destination, validity dates, and switches. Check that partial closure ON with closure OFF is rejected. Save and obtain its parent `CLOSURE_RULE_ID`.
- **Precheck:** From the saved parent, add a pre-closure check. Confirm all 4 check types and 3 failure actions show Persian labels and that a saved `CLOSURE_RULE_ID` is linked to the parent.
- **Settlement:** From the same parent, configure a component and method. If method is TRANSFER, omit destination to verify rejection, then choose a destination and save.
- **Approval:** Verify approval level choices, explicit mandatory toggle, positive ordering and noninverted min/max amounts.
- **Review:** After one-time finalization, the system should display the registered immutable code without implying that the initial issuance was a repeat.

## Read-only Oracle diagnostics for empty choices

```sql
SELECT table_name, constraint_name, constraint_type,
       status, validated, search_condition_vc
FROM all_constraints
WHERE owner = 'PDL'
  AND table_name IN (
    'DEPOSIT_PRODUCT_CLOSURE_RULE',
    'DEPOSIT_PRODUCT_CLOSURE_PRECHECK',
    'DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE',
    'DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE'
  )
  AND constraint_type = 'C'
ORDER BY table_name, constraint_name;
```

`SEARCH_CONDITION_VC` may truncate long conditions; request the full constraint DDL if necessary. An empty choice list caused by incompatible CHECK codes is intentionally blocking until the real PDL code contract is established. **Do not update database reference codes by hand.** No HF4 Oracle migration is required.

The `DEPOSIT_PRODUCT_PROFILE` model itself is unchanged: its two business fields are `DEPOSIT_GROUP_CODE` and `DEPOSIT_TYPE_CODE`, normally fixed from the selected product family. Other business policies belong to separate deposit-rule tables, not the deposit-profile row.
