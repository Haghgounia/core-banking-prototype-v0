# FEE3 UI R8-HF1 — Angular TS2322 strict date Input

**Target:** Core Banking Prototype `0.11.0`, with **FEE3 R8** installed.

## Cause

In `fee3-workspace.component.html` lines 841 and 869, the binding
`[minDate]="row['from'] || todayDate()"` may evaluate to `unknown / {}` because
`repeatableFinanceRows()` returns `Record<string, unknown>[]`. Angular 21 AOT
requires a `string` for `PersianDateInputComponent.minDate`.

## Change

- The two problematic bindings now call `effectiveRowEndMinDate(row)`.
- The helper has an explicit `string` return type. It uses the selected
  `from` only when it is a valid ISO date **not before today's local date**;
  otherwise it returns today's local date.
- Both rules for start dates, date formatting, save validation, and
  service-specific conditional logic remain unchanged.
- R8 manifest SHA-256 entries for these two files are refreshed, and a
  read-only regression script is added.

## Install on Windows

1. Back up the project directory and verify **FEE3 R8** is currently installed.
2. Extract this ZIP into `D:\Projects\core-banking-prototype-v0` and overwrite the
   matching files. Do **not** copy the ZIP itself into the project root.
3. Run:

```bat
cd /d D:\Projects\core-banking-prototype-v0
python tools\fee3-ui\verify-reference-r8.py
python tools\fee3-ui\verify-r8-hf1-aot-date-types.py
python tools\fee3-ui\verify-r8-source-compatibility.py
bin\stop.cmd
build-production.cmd
```

## Acceptance

Expected static gates: R8=59, HF1=9, R8 integrity=12. **Build success
is not claimed** until the Angular AOT build succeeds on the target machine.
This hotfix contains no Oracle, Backend, other feature module, or data updates.
