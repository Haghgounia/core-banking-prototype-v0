# CIF 0.3.22 Fix23 - Build QA

## Scope

Fix23 is a compile-correction release on top of Fix22. No database schema or business behavior changes are introduced.

## Defect

Angular compiler error in `party-360.component.html`:

`TS2339: Property 'workflowStatuses' does not exist on type 'Party360Component'.`

The KYC status selector in Party 360 referenced `workflowStatuses()` while `Party360Component` did not declare or load that lookup.

## Correction

- Added `workflowStatuses` as `signal<readonly CifLookupOption[]>([])`.
- Added lookup initialization from `REF_WORKFLOW_STATUS` through the existing Party Reference lookup service.
- Kept the KYC status selector unchanged so its options are now supplied from CIF reference data.
- Bumped release to `0.3.22-prototype-fix23`.

## Regression controls

- Diff against Fix22 confirms the functional code change is limited to two lines in `party-360.component.ts`: declaration and lookup loading.
- Static template/member scan found no other unresolved callable members in `party-360.component.html`.
- `node tools/sync-system-specification.mjs` completed successfully for `0.3.22-prototype-fix23`.
- No database migration is required.
- Fix22 legal-entity changes are unchanged.

## Environment limitation

A full Angular build could not be completed in the execution environment because `npm ci` timed out before dependencies were installed. The reported TS2339 contract mismatch itself is directly corrected and statically verified.
