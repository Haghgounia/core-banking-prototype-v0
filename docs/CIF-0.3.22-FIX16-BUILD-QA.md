# CIF 0.3.22 Fix16 - Build QA

## Scope

Fix16 is a build-only corrective release over Fix15. No business behavior, database schema, REST contract, or reference-data behavior was changed.

## Reported production-build errors

Windows production build reported Angular/TypeScript `TS2345` errors in `party-financial-employment.component.ts` because the following display helpers accept lookup codes that may be `undefined`:

- `workflowStatusLabel(code: string | null | undefined)`
- `assetItemTypeLabel(code: string | null | undefined)`

Both delegate to the shared `label()` helper, whose Fix15 signature accepted only `string | null`.

## Correction

The shared helper signature is now:

```ts
label(items: readonly CifLookupOption[], code: string | null | undefined): string
```

The existing runtime behavior remains unchanged: nullish/empty codes render as an em dash, and known codes render the Persian/business label.

## Verification

- Exact failing call paths were inspected and the shared type contract was corrected at the common helper boundary.
- The source tree contains no bundled `node_modules` or build cache in the deliverable.
- Full Angular build could not be run in this execution environment because npm dependencies are not bundled in the source ZIP and the local npm cache does not contain all required packages. An offline `npm ci` failed on a missing cached package before compilation. This is an environment dependency limitation, not a source compiler result.
- The user environment that originally reproduced the error already has the required npm dependencies; running `build-production.cmd` there is the authoritative final build verification.

## Expected command

```cmd
build-production.cmd
```

The two Fix15 `TS2345` errors at the workflow-status and asset/liability label calls are resolved by this change.
