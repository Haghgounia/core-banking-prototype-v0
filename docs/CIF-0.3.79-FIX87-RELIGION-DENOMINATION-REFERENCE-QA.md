# CIF 0.3.79 FIX87 — Religion / Denomination Reference QA

## Scope

Two governed reference tables are added to the CIF `Identity and Party` package:

- `CIF.REF_RELIGION` — دین
- `CIF.REF_RELIGIOUS_DENOMINATION` — مذهب / شاخه دینی

The second table has a mandatory FK to religion and optional `PARENT_CODE` self hierarchy.

## UI

Both tables are exposed through the existing metadata-driven Party Reference CRUD engine under:

`اطلاعات پایه → اطلاعات پایه مشتری / Party → هویت و Party`

No special Angular page is required.

## Database

Run for an existing database:

`database/oracle/cif/migrations/0.3.79-fix87-religion-denomination-reference.sql`

Expected initial row counts:

- REF_RELIGION: 7
- REF_RELIGIOUS_DENOMINATION: 16

## Privacy

Religion is sensitive personal information. These reference tables only define allowed codes. Storing a Party's religion/denomination should be introduced separately with explicit authorization, audit and data-minimization controls.
