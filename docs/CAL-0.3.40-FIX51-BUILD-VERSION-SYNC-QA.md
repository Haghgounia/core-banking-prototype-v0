# CAL 0.3.40 / FIX51 - Build Version Sync QA

## Problem
`build-production.cmd` executed `verify-cif-persisted-grids.mjs` before the Angular `prebuild` hook. The verifier checks `frontend/src/app/features/system-specification/system-version.generated.ts` against the root `VERSION`, so a clean package carrying a stale generated file could fail before `npm run build` had a chance to synchronize it.

Observed failure:

`Generated system version does not match VERSION (0.3.39-prototype-fee-p1)`

## Fix
`build-production.cmd` now executes:

`node "%ROOT%tools\\sync-system-specification.mjs" || exit /b 1`

immediately after reading and printing the root `VERSION`, before all static verifiers.

The generated source file is also synchronized in the release package itself.

## Expected build order
1. Read root `VERSION`.
2. Synchronize system specification generated metadata.
3. Execute static verifiers.
4. Build Angular frontend.
5. Package Spring Boot backend.
6. Copy the newly built JAR into `app/` only after Maven success.

## Version
- Root release: `0.3.40-prototype-fee-p1`
- Backend: `0.3.40-SNAPSHOT`
- Frontend: `0.3.40`

No database migration is required.
