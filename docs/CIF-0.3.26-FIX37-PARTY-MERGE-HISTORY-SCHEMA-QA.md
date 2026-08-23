# CIF 0.3.26 FIX37 - Party Merge History schema alignment

## Root cause
Runtime 0.3.25 successfully passed Party/Party Name/Organization inserts, including `REGISTRATION_COUNTRY_CODE=IRN`. The transaction then failed while `createParty()` called `loadParty360()`. The final query, `findMergeHistory()`, selected `PARTY_MERGE_HISTORY.CREATED_DATE`, but the Oracle 2026-08-23 16:24 schema contains only `CREATED_AT` and does not contain `CREATED_DATE`. Oracle therefore raised `ORA-00904`. Because onboarding is transactional, this read error rolled back the preceding writes.

## Fix
- Removed `CREATED_DATE` from `findMergeHistory()` SELECT and JDBC mapper.
- Removed `CREATED_DATE` from `insertMergeHistory()` INSERT.
- Removed redundant `createdDate` from backend and frontend `PartyMergeHistoryRecord`.
- Canonical creation timestamp remains `CREATED_AT`.
- Bumped runtime/package version to 0.3.26.

## Expected runtime evidence
Startup: `Starting CoreBankingApplication v0.3.26-SNAPSHOT`
On successful onboarding: `Party onboarding completed: partyId=...`
No `ORA-00904: CREATED_DATE` should appear.
