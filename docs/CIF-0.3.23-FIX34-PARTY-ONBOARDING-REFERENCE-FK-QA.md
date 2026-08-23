# CIF 0.3.23 FIX34 - Party onboarding vs reference FKs

## Symptom
Party onboarding returns the generic database-integrity message after reference foreign keys were enabled.

## Findings
- The 2026-08-23 11:55 and 16:24 Oracle metadata exports contain the same 154 FK rows; no FK was added or removed between those two snapshots.
- `CIF.PARTY_NAME.SCRIPT_CODE` has enabled FK `FK_PARTY_NAME_REF_SCRIPT` to `CIF.REF_SCRIPT.SCRIPT_CODE`.
- CIF reference seed/model uses `ARAB` and `LATN`, while Party creation sent `Arab`/`Latn` and backend `createParty()` hard-coded `Arab`.
- `CIF.PARTY.CREATION_SOURCE_CODE` and `CIF.PARTY_NAME.SOURCE_CODE` both reference `CIF.REF_DATA_SOURCE`, while the Party creation UI loaded `REF_SOURCE_SYSTEM`. `CORE` happens to overlap, but other source-system codes are not guaranteed to exist in `REF_DATA_SOURCE`.
- Existing error handling mapped `ORA-02291` to the generic data-conflict message, hiding the missing-parent nature of the error.

## Fix
- Use `ARAB`/`LATN` in UI defaults/options and backend-generated primary names.
- Normalize reference-backed Party Name script/source codes before persistence.
- Validate Party Name reference codes against active reference rows before insert/update.
- Load Party creation source from `REF_DATA_SOURCE` rather than `REF_SOURCE_SYSTEM`.
- Map `ORA-02291` to `REFERENCE_VALUE_NOT_FOUND` with a clearer Persian message.

## Verification
Run `database/oracle/cif/verify-party-onboarding-reference-fks.sql` against the target Oracle database before retesting onboarding.

## Build note
Backend Maven and frontend Angular builds were not executable in the isolated review environment because dependency distributions were not locally cached and outbound package download was unavailable. Source-level changes were checked for consistency and the database metadata exports were compared directly.
