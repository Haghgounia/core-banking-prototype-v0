# R30-HF7-A3/HF1 - Product Version editor safe-save UX correction

Date: 2026-10-14. Baseline: `core-banking-prototype-v0-2026-10-14-R30-HF7A2-HF1.zip` (user has confirmed its full Windows production build).

## Reproduced diagnosis

- User attempted DRAFT -> APPROVED while also changing LOAN origination CLOSED -> OPEN and servicing CLOSED -> ACTIVE.
- The read-only governance preflight reports `canOpen=false` because actual end-to-end loan origination/disbursement/servicing/collection enforcement has NOT been qualified.
- Previously the Angular editor was closed and the wizard jumped to review whenever the preflight denied save. The form selections were lost; no backend write was submitted.
- This refusal to enable loan origination is intentional and must NOT be removed to make the UI pass.

## Changes

1. Use `versionSaveError` displayed with `role=alert` inside the modal, where the user can see it. Keep the modal, selection and current wizard step unchanged when governance/API fails.
2. Show a clear runtime-qualification warning for LOAN while OPEN origination or ACTIVE servicing is selected. Provide an explicit button that changes BOTH fields to CLOSED; the user must still press Save afterwards. There is NO silent downgrade.
3. Distinguish `canApprove` (design) from `canOpen` (operational origination). Filter runtime-only blockers from design-approval errors when applicable. Keep mandatory backend checks unchanged.
4. For newly created versions, the only enabled status is DRAFT, matching the backend lifecycle. Do not accept OPEN origination before version creation.
5. Check the reloaded row's ID and three status codes after a successful API response before reporting save success. Do not misreport success when the refreshed row disagrees.
6. Preserve the existing HF2 `IS_CURRENT` write protection. The current-version action remains separate.
7. Add an executable local regression verifier with static checks and extracted real TypeScript save handler tests; add it to Windows/Linux production build scripts.

## Files changed

- `frontend/src/app/features/product-builder/product-workspace.component.ts`
- `frontend/src/app/features/product-builder/product-workspace.component.html`
- `frontend/src/app/features/product-builder/product-workspace.component.scss`
- `build-production.cmd`
- `build-production.sh`
- `tools/verify-pdl-r30-hf7a3-version-editor.mjs` (new)
- This release note (new).

No backend, Oracle, migration, ledger, fee, loan or deposit transaction code has been changed.

## Verification

- Version-editor verifier: 18/18 PASS using the actual extracted TypeScript save action in a mocked service environment (without Oracle).
- Existing R19, HF2, HF3, HF7-SG, HF7-A1, HF7-A2, R30, R29, R28, R25, HF5 and HF6 regression verifiers: PASS after the change.
- Release layout: PASS. ZIP CRC integrity and overlay byte parity checked during packaging.
- Angular production build and Maven/Surefire must be run again on the user's Windows installation. This environment does not contain the project Maven/Angular dependencies for an end-to-end build.

## Operator workflow

1. For designing a LOAN version, keep origination CLOSED and servicing CLOSED. A new version is first DRAFT.
2. Configure all applicable product/rule data, re-run governance readiness. Only after `canApprove=true` request APPROVED while origination remains CLOSED.
3. OPEN origination must remain unavailable until HF7-C achieves and independently verifies real loan runtime enforcement.
4. When a save is rejected the editor stays open and displays the reason. Use the explicit close-both action only when the operator elects design approval without operations.

## Open limitations / exclusions

- This is a focused UX safety correction, not completion of comprehensive HF7-A3 semantic audit, HF7-B or HF7-C.
- Approval may still be rejected when mandatory product rules are missing or semantically invalid; this patch does not grant approval.
- The pre-existing lifecycle treatment of IN_REVIEW and rejection/rework transitions requires a separately agreed governance workflow review; this patch makes no unsupported lifecycle permissions.
- Do not weaken `canOpen`, enable Oracle writes, run a database migration, or deploy operational LOAN transactions based solely on this patch.

## Install on Windows

Back up the project, extract the patch ZIP into the current root (preserve folder structure and overwrite matching files), then run:

```bat
cd /d D:\Projects\core-banking-prototype-v0
node tools\verify-pdl-r30-hf7a3-version-editor.mjs
build-production.cmd
```

Check that HF7-SG, HF7-A1, HF7-A2 and HF7-A3/HF1 tests pass and the full build completes. No reset of existing PDL data is necessary.
