# CLT-R2C | Serial Item Draft, Asset Candidate and Final Registration Preflight

**Canonical roadmap:** CLT-ROADMAP-R1.0 / R2 (Registry, Introduction and Search)  
**Product baseline:** Core Banking Prototype 0.11.0 with R1A, R1B, R2A, R2B installed.  
**Scope and authority:** implementation slice; not an approval of Oracle DDL or bank policy.

## What R2C delivers

1. Uses the existing `CLT.COLLATERAL_TYPE.HAS_MULTIPLICITY='Y'` toggle to show a serial-item grid. The UI reference V1.42 (post-V1.36) treats this as the canonical physical countability flag; the old UI-only `IS_COUNTABLE_FLAG` is not introduced into Oracle.
2. Normalizes Persian/Arabic decimal digits to ASCII while preserving exact string representations and leading zeroes. Uses `BigInteger` on the server and `BigInt` in Angular; does not coerce up-to-100-digit serial numbers into floating point.
3. Validates each inclusive serial range (1..20 items), detects overlap even across zero-padded equivalents, enforces positive integer row values (IRR) and equal row-total versus the declared collateral estimate. At most 80 draft rows.
4. Allows **draft-only** submission for countable types after server-side eligibility checks. R2C fields `assetReference` and `itemRanges` are part of the replay fingerprint and can be stored in the separate R2B stage table after approved opt-in R2C migration.
5. Adds `POST /api/v1/collaterals/introduction-drafts/registration-preflight` under the existing JWT `SCOPE_clt.collateral.introduce` contract. It returns a draft eligibility result, item totals and immutable finalization blockers. It performs no DML and issues no registration number.
6. Keeps older R2B non-countable draft writes functioning even without the R2C stage extension, via a fixed allowlisted legacy SQL path.
7. Reuses the existing Angular introduction route and components; no shared App Route, Sidebar, pom.xml, Spring Boot global config or IAM changes.

## Hard safety constraints

- `eligibleForFinalRegistration=false` always: no final registration endpoint and no `CLT.COLLATERAL` DML.
- `CLT_INTRODUCTION_DRAFT_WRITES_ENABLED=false` remains the default.
- No implementation chooses a bank-issued number or asserts asset identifier uniqueness.
- A user-entered `assetReference` is **only a candidate** recorded in a draft, never a trusted authoritative identifier.
- Existing JWT branch and subject are authoritative; no user-controlled branch or maker field is accepted.
- No DDL/DML is executed by the installer, verifier or build command.
- The existing R2B stage has its own DBA-approval requirement; R2C's extension adds two nullable fields to that stage, not to the 26-table base XMI.

## Optional DBA review

- `database/oracle/clt/r2c/00_DRAFT_EXTENSION_READONLY_PRECHECK.sql`: dictionary-only, no change.
- `database/oracle/clt/r2c/01_DRAFT_STAGE_EXTENSION_DBA_OPT_IN.sql`: optional reviewed migration, **never automatically run**. Requires independently approved R2B stage.
- `database/oracle/clt/r2c/02_FINAL_REGISTRATION_DATA_MODEL_DECISION_ONLY.sql`: design reminders, no registration DDL.

Drafts with no new R2C fields continue using the original R2B SQL when no extension is present. Drafts with asset candidate/ranges require the extension. The stage writer remains isolated; do not grant it access to permanent collateral tables.

## R2C API example (proposal input, not bank policy)

`POST /api/v1/collaterals/introduction-drafts/registration-preflight` with a valid JWT bearing scope `clt.collateral.introduce`:

```json
{
  "partyId":"123", "collateralTypeId":"456", "estimateAmount":"300",
  "currencyCode":"IRR", "validFrom":"2026-11-03", "validTo":null,
  "depositNumber":null, "remarks":null, "attributes":[],
  "assetReference":"DRAFT-IDENTIFIER-ONLY",
  "itemRanges":[
    {"serialFrom":"001", "serialTo":"003", "totalAmount":"100"},
    {"serialFrom":"005", "serialTo":"005", "totalAmount":"200"}
  ]
}
```

The real type ID and attribute configuration must exist in an authorized Oracle reader and be countable. Without an enabled JWT/Oracle reader this endpoint fails closed; the example is **not** an executable UAT fixture.

## Unresolved decisions

| Gap | Owner decision before enabling final registration |
|---|---|
| G-01 | Introductions amendment storage, maker/checker and lifecycle |
| G-03 | Approved permanent item-range data model and cross-collateral overlap contract |
| G-12 | Formal sign-off of canonical countable-type policy and migration of any legacy flags |
| G-14 | Stable authoritative asset identity, owner scope and re-introduction lineage |
| G-17 | Registration-number grammar, year, CIF identifier length, checksum and sequence concurrency |
| G-16 | Transaction status `PENDING` vs `PENDING_APPROVAL` before recording the final event |

## QA and exit criterion

The source release can pass local syntax tests and staged installer tests. **R2 remains OPEN** until Oracle/JWT Runtime, DB migration approval, operational registration transaction tests and business UAT pass, and all applicable R2 gaps are closed. Follow the canonical R0-R7 phase names; do not rename R2 to a new phase.
