# ORG R2-A menu cleanup and branch contact baseline (24 Oct 2026)

## Data and business baseline

R1 transfer has been committed and read again from a fresh Oracle connection:
- 1,340 canonical organization-unit-location rows; 1,340 exact matches; zero mismatches.
- 1,337 original location rows and 1,337 original address rows remain intact.
- The four valid assignments sharing `LOCATION_ID=1327` remain separate. Their shared building address does not prove a physical presence for the three virtual units.

These are user-supplied Oracle verification results, not a new live Oracle query from this package.

## Menu change (UI only)

Three legacy generic **entry points** are no longer shown in the ORG menu or quick links:
1. `locations` (`ORG.LOCATIONS`)
2. `postal-addresses` (`ORG.POSTAL_ADDRESSES`)
3. `organization-unit-locations` (`ORG.ORGANIZATION_UNIT_LOCATIONS` generic editor)

The supported primary link is the read-only canonical view at:
`http://localhost:8091/#/organization/unit-locations-canonical`.

The old 3-table diagnostic page remains accessible via the canonical page for reconciliation, but no longer advertises legacy editor links. The three database tables, all IDs, historical source records, descriptors, and direct legacy routes are preserved: **this is menu cleanup, not a database cutover or an authorization rule**. The direct legacy editor routes remain technically reachable. Do not continue normal address editing through those routes without a synchronization plan, or canonical/source drift may result.

A separate ORG menu group now exposes contact roles and organization-unit contact assignments. The service-point contact entry stays under service points.

## Contact-point model review: unresolved ownership

Actual ORG DDL:
- `ORG.CONTACT_ROLES` defines the *business use/role* of a contact, not the communication medium.
- `ORG.ORGANIZATION_UNIT_CONTACT_POINTS` holds `ORGANIZATION_UNIT_ID`, `CONTACT_POINT_ID`, `CONTACT_ROLE_ID`, `PRIMARY_FLAG`, `EFFECTIVE_FROM`, `EFFECTIVE_TO`.
- `ORG.SERVICE_POINT_CONTACT_POINTS` holds the equivalent service point association but currently has no effective-date columns.
- `CONTACT_POINT_ID` in both ORG association tables is `NUMBER(8)` without a foreign key to a contact master in the supplied ORG schema; the UI currently exposes a raw numeric field.
- A candidate `CIF.CONTACT_POINT` exists with `NUMBER(19)` and `PARTY_ID`. It is modeled as a **party-owned** contact point and cannot safely be declared the ORG master based only on the identical ID name or overlapping numeric values.

**Do not auto-create customer Party records, auto-link branch contacts to CIF, assign contact values to raw numeric IDs, or rename/drop tables in this stage.** The correct owner (ORG-owned contact master, verified enterprise master, or a properly scoped CIF relationship) must first be established.

## Read-only Oracle preflight

Run `database/oracle/org/contact-points/01-ORG-BRANCH-CONTACTS-PREVIEW-READONLY.sql` with SQL Developer F5. It gives:
- contact-role counts and usage;
- organization-unit contact assignment counts and coverage by unit type;
- primary contact overlaps by unit and role (screening, not an automatic error classification);
- repeated/shared contact IDs;
- referenced column metadata and actual foreign keys;
- optional **aggregate-only** contact ID matches with CIF, if access is available.

The script does not change schema, data, or transaction state and does not print customer telephone/email values.

## Acceptance / outstanding work

- Static ORG menu and R2-A regressions must pass; live front-end browser validation still required.
- The final contacts UX must provide a valid contact selector and display type, actual contact value, role, effective period, primary status, and provenance, not an unexplained numeric ID.
- Define primary and overlapping-period rules per medium and business role, with an explicit distinction between branch public contact and personal/employee contacts.
- Check verification, masking, branch virtual addresses, shared call-center numbers and accessibility of official Bank Mellat branch directory information before any import.
- No Oracle DDL/DML included in this patch. Contact master ownership, operational CRUD, and the final physical removal of redundant location tables are **not completed** here.
