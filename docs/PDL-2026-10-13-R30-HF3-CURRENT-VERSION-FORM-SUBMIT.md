# R30-HF3 - Governed current-version form submission fix

Date: 2026-10-13. Base: the user's 2026-10-13 08:07 project ZIP with R30-HF2 applied.

## Observed runtime problem

Product 31, approved ACTIVE version 35, stayed IS_CURRENT=0 after the
user entered a reason and clicked the governed make-current confirmation.
Instead, the page navigated/reloaded to step 1 (product identity).

## Root cause in source

The step-2 confirmation was a native `<form>` with `(ngSubmit)` but **no
Angular form directive** (`[formGroup]`/`ngForm`). Angular could not own
and cancel native browser submission. Consequently, the click could reload
the browser to the default route instead of calling the dedicated endpoint.
This was not covered by the old HF2 static verifier.

## Fix

- Attach `[formGroup]="currentVersionForm"` to the current-version confirmation.
- Register the reason with `formControlName="reason"` and preserve its required,
  5-to-500-character validators.
- Retain `(ngSubmit)="makeApprovedVersionCurrent()"`, which Angular now handles.
- On a backend error, stay on step 2 and show the error next to the form.
- On success, refresh the product workspace and require that the target version
  is actually returned with IS_CURRENT=1 before reporting success.
- Add regression checks to `build-production.cmd` and `build-production.sh`.
- Strengthen the existing HF2 verifier so it rejects an unbound confirmation form.

No Oracle DDL/DML migration or backend business-logic change is introduced.
R30-HF2 database migration remains a prerequisite, already run in the user's environment.

## Install (Windows CMD)

Back up the project first. Stop the running application. Extract **only the
HF3 patch ZIP** into the project root, preserving directory names. Do not
apply the earlier full HF2 bundle over newer FEE2 changes. Alternatively,
extract the provided cumulative full HF3 ZIP built from the user's own last ZIP.

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf3-submit.mjs
node tools\verify-pdl-r30-hf2-current-version.mjs
build-production.cmd > "%TEMP%\build-r30-hf3.log" 2>&1
echo BUILD_EXIT_CODE=%ERRORLEVEL%
if exist app\BUILD-DIRTY (echo BUILD_NOT_COMPLETE) else (echo BUILD_CLEAN)
```

Restart the runtime using the newly produced JAR and force-refresh the browser.

## UAT: product 31 / version 35

1. Select version 35 on the step-2 page.
2. Click the independent 'make current' governance button.
3. Enter a reason of at least 5 characters; confirm.
4. There must be no browser navigation to step 1. On rejection, a readable
   error must remain beside the confirmation controls. Fix only the reported
   real governance blocker; never set IS_CURRENT in direct SQL.
5. On success, step 2 remains selected, a success notice is shown, and version
   35 has `IS_CURRENT=1` in Oracle; an audit row exists for this transition.
6. Only then proceed to step 6 to finalize and issue the system product code.

```sql
SELECT PRODUCT_VERSION_ID, PRODUCT_ID, VERSION_STATUS_CODE,
       RECORD_STATUS_CODE, IS_CURRENT, APPROVED_AT, APPROVED_BY,
       VALID_FROM, VALID_TO
  FROM PDL.PRODUCT_VERSION
 WHERE PRODUCT_VERSION_ID = 35 AND PRODUCT_ID = 31;

SELECT AUDIT_ID, PRODUCT_ID, TARGET_VERSION_ID, ACTION_CODE, CHANGED_AT
  FROM PDL.PRODUCT_VERSION_CURRENT_AUDIT
 WHERE PRODUCT_ID = 31 AND TARGET_VERSION_ID = 35
 ORDER BY AUDIT_ID DESC;
```

Tests are non-writing/static and are NOT evidence of a real Oracle UAT.
Do not use the make-current endpoint to bypass approval/readiness controls.
