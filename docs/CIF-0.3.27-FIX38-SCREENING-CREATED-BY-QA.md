# CIF 0.3.27 FIX38 - Screening CREATED_BY alignment

## Runtime evidence

Runtime 0.3.26 successfully completed legal-entity onboarding (`partyId=28`). Subsequent attempts to create a screening result failed with:

`ORA-01400: cannot insert NULL into ("CIF"."SCREENING_RESULT"."CREATED_BY")`

## Root cause

Oracle metadata defines `SCREENING_RESULT.CREATED_BY` as NOT NULL. The create-screening controller already resolves an actor through `X-User-Id` (default `1`), and `CifService.createScreening` receives that actor. However, the service discarded it when calling the repository, and `CifRepository.insertScreening` did not include `CREATED_BY` in the INSERT statement.

The update path was already correct because `updateScreening(..., actor)` writes `UPDATED_BY`.

## Fix

- Changed repository signature to `insertScreening(long partyId, ScreeningResultRequest request, String actor)`.
- Added `CREATED_BY` and `:actor` to the Oracle INSERT.
- Changed service call to `repository.insertScreening(partyId, request, actor)`.
- Bumped backend/runtime to `0.3.27-SNAPSHOT` and UI release to `0.3.27-prototype-fee-p1`.

## Audit

A static scan of 27 explicit CIF INSERT statements against the 2026-08-23 16:24 Oracle metadata found `SCREENING_RESULT.CREATED_BY` as the only missing mandatory `CREATED_BY` column. `PARTY` and `PARTY_NAME` omit some other NOT NULL fields that are supplied by their existing database defaults/generation path and are proven by successful onboarding.

## Expected verification

Startup should show:

`Starting CoreBankingApplication v0.3.27-SNAPSHOT`

UI badge should show:

`0.3.27-prototype-fee-p1`

Creating a screening result should no longer raise ORA-01400 for `SCREENING_RESULT.CREATED_BY`.
