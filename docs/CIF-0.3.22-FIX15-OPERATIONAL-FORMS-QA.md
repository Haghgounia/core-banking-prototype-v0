# CIF 0.3.22 Fix15 — Operational Forms Corrections & QA

## Scope

This patch is based on `0.3.22-prototype-fix14` and addresses the reported Phase 3 (financial/employment) and Phase 4 (additional identifiers/documents) usability, lookup, persistence and validation-state defects.

## Corrections

| Area | Defect | Fix15 behavior |
|---|---|---|
| External employer | External employer name could not be entered reliably because internal/external employer inputs shared validation state | Explicit employer mode was added. `EXTERNAL` enables and requires employer name/optional external identifier while disabling `EMPLOYER_PARTY_ID`; `INTERNAL` enables the searchable CIF Party selector and disables external fields. Exactly one employer source is persisted. |
| Economic activity | ISIC code/numeric value could be shown as the activity title | `REF_ISIC_ACTIVITY.NAME_FA` is corrected in seed/migration/metadata and the UI applies a Persian-title fallback. Opaque numeric labels are never rendered; only `ISIC_CODE` is persisted. |
| Income/source type | Source-of-funds selector could be empty/incomplete | `REF_SOURCE_OF_FUNDS` is loaded with five operational values and a controlled runtime fallback: salary, business income, savings, investment and inheritance. Existing databases are aligned by migration. |
| Income/source status | `STATUS_CODE` selector was empty | Uses shared `CIF.REF_WORKFLOW_STATUS`; operational choices are `ACTIVE`, `INACTIVE`, `UNDER_REVIEW`, displayed by Persian title. Default is Active. |
| Income grid | Row presentation made records look like only “salary” was stored | Each row now renders source title + formatted amount/currency + workflow status + documented/not-documented state. All returned records remain independently editable/deletable. |
| Asset/liability item type | Raw `ITEM_TYPE_CODE` was meaningless to business users | Replaced with a controlled Persian ComboBox of 13 asset/liability item types. Title is shown; code is persisted. Backend rejects values outside the same controlled vocabulary. |
| Asset/liability status | `STATUS_CODE` selector was empty | Uses shared workflow status data with the same operational subset and Active default. Backend validates against `REF_WORKFLOW_STATUS`. |
| Additional identifier post-save | Successful save could leave focus/error-submitted state on the identifier-value input | `FormGroupDirective.resetForm(...)` resets the submitted/error state and active-element focus is explicitly blurred after a successful save. |
| Document hash | `CONTENT_HASH` was exposed as a required manual input | Hidden from business UI. Backend computes SHA-256 during file upload; it remains persisted as integrity metadata. |
| Document storage reference | `STORAGE_REF` was exposed as a required manual input | Hidden from business UI. Backend creates an opaque `cif-doc:` reference that points to a private repository path; physical filesystem paths are not exposed to the user. |
| Document load | No practical “Load from file” flow | Added multipart file upload for PDF/JPEG/PNG/TIFF up to 20 MB. The same selector accepts a scanner's output file. Stored documents can be retrieved from their Party document row. |
| Direct scanner | Browser UI cannot safely invoke enterprise TWAIN/WIA/SANE hardware directly | No fake scanner integration was introduced. UI explicitly states that direct hardware scanning requires an approved local scanner Agent/middleware; scanner output files are supported now. |
| Document post-save red fields | Hidden technical Required fields could remain in submitted state | On successful save the form submitted state is reset; hash/storage reference are not rendered as user-editable fields, so they cannot become misleading red inputs. |
| Forward pattern | Similar technical-code fields risk repeating the same UX issue | Shared rule applied to this flow: **Business title in UI / code in persistence**, Reference Data first, controlled fallback only where an authoritative physical REF is missing, and technical storage/integrity fields are system-owned. |

## Reference-data alignment

Migration: `database/oracle/cif/migrations/0.3.22-fix15-operational-lookup-alignment.sql`

The migration idempotently aligns:

- `CIF.REF_SOURCE_OF_FUNDS`: `SALARY`, `BUSINESS_INCOME`, `SAVINGS`, `INVESTMENT`, `INHERITANCE`
- `CIF.REF_ISIC_ACTIVITY`: prototype ISIC values `6419`, `6201`, `4690`, `0111` with Persian business titles
- `CIF.REF_WORKFLOW_STATUS`: operational statuses needed by the corrected forms

Fresh-install seeds and runtime metadata are aligned with the same display titles.

## Document-storage architecture

Prototype flow:

```text
Browser / Scanner output file
       |
       v
POST /api/v1/cif/parties/{partyId}/document-files
       |
       +--> size + MIME allow-list validation
       +--> SHA-256 calculation
       +--> private file repository: data/document-storage/party/{partyId}/YYYY/MM/...
       +--> opaque storageRef: cif-doc:party/...
       |
       v
PARTY_DOCUMENT metadata
(CONTENT_HASH, STORAGE_REF, MIME_TYPE, business metadata)
```

The repository is outside Spring static resources and is excluded from Git/package source-control artifacts. For production banking deployment this adapter should be replaced or backed by the bank-approved DMS/object-storage capability with encryption-at-rest, malware scanning, retention/legal-hold policy, IAM/service identity and audit logging. Direct TWAIN/WIA/SANE scanner invocation should be implemented through an approved workstation Agent, not browser-only JavaScript.

## Backend consistency checks

- Source-of-funds codes are validated against active `REF_SOURCE_OF_FUNDS`.
- Income/asset statuses are validated against active `REF_WORKFLOW_STATUS`.
- Asset/liability item type is validated against the same controlled vocabulary used by the UI.
- Existing employer XOR validation remains enforced server-side.
- Multipart limit is 20 MB at both Spring multipart configuration and storage service level.

## QA performed

- TypeScript parser: no syntax diagnostics in modified CIF TypeScript files.
- Angular HTML structural balance: forms, material fields, sections/articles/divs balanced.
- YAML configuration parse: OK.
- Party reference metadata JSON parse: OK.
- Regression grep: no visible `کد وضعیت`, `کد نوع قلم`, `هش محتوا`, `مرجع ذخیره امن فایل`, or corrupted `CIF.شناسه پارتیENTIFIER` label remains in the corrected forms.
- Java `DocumentStorageService`: compiled successfully with Java 21 against minimal Spring API stubs; this check caught and corrected an incorrect `DigestInputStream` import before packaging.
- `CifService` and `CifController`: javac parse pass showed no Java syntax-pattern errors; full symbol resolution requires Maven dependencies.
- Full Maven build could not download the Maven wrapper distribution because the execution environment has no outbound access to Maven Central.
- Full Angular build could not be run because the supplied ZIP does not contain `node_modules` and the execution environment could not complete npm dependency installation. A partial npm directory created during the attempt is removed before packaging.
- Final ZIP is tested with `unzip -t` after packaging.

## Design rule for subsequent Party forms

1. Never expose technical IDs/codes when a business title exists.
2. Prefer authoritative Reference Data and persist only the code/ID.
3. Do not render backend-owned integrity/storage/audit fields as required user inputs.
4. Reset `FormGroupDirective.submitted` after successful create/update when a form is cleared.
5. Grid rows must expose enough business context to distinguish multiple records of the same type.
6. For fields with no authoritative reference vocabulary, use an explicit controlled vocabulary only when it is already defined by the operational contract; do not invent a new Reference Data table silently.
