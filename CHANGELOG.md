## 0.3.22-prototype-fix12

- Party identifier UI now uses a single issuing-authority selector; the duplicate issuer-code selector was removed.
- `ISSUER_CODE` is derived from the selected issuing authority for compatibility with the current `UQ_IDENTIFIER` key.
- CIF validation responses preserve `HttpErrorResponse`, so field-level `ProblemDetail.fieldErrors` are shown instead of a generic log-only message.
- Duplicate identifier validation wording now refers to the issuing authority selected by the user.

## 0.3.22-prototype-fix11

- `CIF-tables5.xlsx` به‌عنوان آخرین Schema اجرایی Oracle مبنا قرار گرفت: 146 جدول، 1795 ستون، 98 جدول مرجع و 48 جدول عملیاتی.
- SQLهای `PARTY_RISK_ASSESSMENT` با Schema جاری همگام شدند و ارجاع به `CREATED_DATE / LAST_MODIFIED_BY / LAST_MODIFIED_DATE` حذف شد.
- SQL ثبت `SCREENING_RESULT` با Schema جاری همگام شد و ارجاع به `CREATED_DATE` حذف شد.
- DDL snapshot از comment/ALTERهای مربوط به ستون‌های حذف‌شده پاک شد.
- نمایش کد لاتین از Comboها و Searchable Comboهای فرم‌های CIF حذف شد؛ کد همچنان فقط در API/Database نگهداری می‌شود.
- 242 عنوان Material، 126 عنوان فیلد Native، 191 گزینه Material و 43 Searchable Combo از نظر نمایش پیش‌فرض فارسی بازبینی شدند.
- پیام fallback ثبت Party فارسی و تفکیک خطای عدم ارتباط با سرویس اصلاح شد.

## 0.3.22-prototype-fix10

- Fixed backend compilation regression in CIF identifier date validation: `validateIdentifier` now uses the existing `CifModels.PersonProfile` type returned by Party 360/current Party data instead of the nonexistent `PersonRecord` type.
- No database migration is required for this patch.

## 0.3.22-prototype-fix9
- Hardened identity dates end-to-end: a person's birth date is required and must be strictly before the system date; identifier issue date cannot be in the future and cannot precede the person's birth date; identifier expiry date cannot precede issue date. Equal-to-today birth dates receive a dedicated Persian validation message instead of the generic required-fields message.
- Contact validity hardened: `VALID_TO` accepts the system date or later and must not precede `VALID_FROM`.
- Contact verification time is system-controlled in the UI; new/re-verified contact points receive the current system date/time and cannot be back-dated. Historical verification timestamps remain edit-safe when unchanged.
- `CONTACT_POINT.OWNER_TYPE_CODE` is now a Persian ComboBox using the operational-form values `CUSTOMER / REPRESENTATIVE / COMPANY` (خود مشتری / نماینده / شرکت), with server-side validation.
- Contact verification-status and verification-method choices now display Persian labels only; persisted reference codes remain unchanged.
- Employment status is now selectable from the operational-form vocabulary (شاغل، خویش‌فرما، بازنشسته، خانه‌دار، دانشجو، بیکار); `JOB_STATUS` and `EMPLOYMENT_STATUS_CODE` are normalized to the same selected code and validated server-side.
- Added dedicated database-backed `CIF.REF_ADDRESS_SOURCE` because the Party address operational form has its own source vocabulary distinct from generic `REF_DATA_SOURCE`: `CUSTOMER_DECLARATION` (اظهار مشتری), `POSTAL_SYSTEM` (سامانه پست), `RESIDENCE_DOCUMENT` (مدرک سکونت).
- Added idempotent migration `0.3.22-fix9-contact-date-address-source.sql` and synchronized the CIF reference catalog to 98 runtime forms / 168 total reference forms.

## 0.3.22-prototype-fix8
- Party creation: localized the system display values `UNVERIFIED` and `INCOMPLETE` to Persian while keeping the persisted/API codes unchanged.
- `PARTY_IDENTIFIER.ISSUER_CODE` is no longer free text; it is selected from `CIF.REF_ISSUING_AUTHORITY`, matching the current model/index metadata.
- Server-side identifier validation now checks both `ISSUING_AUTHORITY_CODE` and `ISSUER_CODE` against active `REF_ISSUING_AUTHORITY` rows and normalizes both codes to uppercase.
- No database migration is required.

## 0.3.22-prototype-fix7
- Party creation: replaced free/manual birth-place entry with cascading Province -> County -> City selection for Iranian birth locations.
- City choices are reduced to the selected county; the application resolves its districts internally and shows only their cities. The city ComboBox is searchable by city name/code and returns at most the first 100 unfiltered options.
- The selected GEO.CITIES.CITY_ID is persisted in CIF.PERSON.BIRTH_PLACE_ID; the previous onboarding payload bug that always sent birthPlaceId=null is removed.
- Persian is ordered first in language lookups and remains the default for both name language and preferred language.
- PERSON.PHYSICAL_ABILITY is now a ComboBox with the two operational-form values: NORMAL / ACCESS_NEEDED.
- Primary/Active labels in the initial identifier section are localized to Persian; issuer-code wording is clarified.
- No database migration is required.

## 0.3.22-prototype-fix6
- Refreshed the System Specification page to the actual final CIF/Party scope instead of the old GEO/DPS-only snapshot.
- Updated live scope metrics to 167 Reference Data forms (20 general/GEO + 97 CIF Party/Customer + 50 DPS), 12 Party operational screens, and 48 covered CIF operational tables (30 workflow + 18 read-only 360 sources).
- Updated Oracle scope to the three active schemas CIF / GEO / DPS and refreshed architecture, technology and capability descriptions for Party onboarding, Customer role, KYC/Risk, Consent, Lifecycle/Merge, Persian dates, runtime logging and Party/Customer 360.
- Updated the page review date to 2026-08-18. No database migration is required.

## 0.3.22-prototype-fix5
- PARTY_ROLE runtime schema correction: removed legacy `CREATED_DATE`, `LAST_MODIFIED_BY`, and `LAST_MODIFIED_DATE` from role INSERT/UPDATE SQL because these columns have been removed from the current Oracle schema.
- Updated the bundled CIF DDL snapshot and Phase 7 documentation so new environments do not recreate the removed PARTY_ROLE audit columns.
- No database migration is required for this fix; existing databases that still contain the legacy columns remain compatible because the application no longer references them.

## 0.3.22-prototype-fix4
- Synchronized `PartyReferenceMetadataRegistryTest` with the new `REF_TENURE_TYPE` reference table introduced by Address Fix 2/3.
- Updated the Party reference catalog runtime count from 96 to 97 while preserving the original 104-source-definition accounting (96 source CIF references + 8 GEO/DPS mappings, plus the local tenure extension).
- Added explicit test coverage for `ref-tenure-type`.

## 0.3.22-prototype-fix3

- Party Address UI realigned with the operational reference form: structured address fields are shown in the same two sections (address details / status and validity).
- Removed user-facing ADDRESS_LINE1/ADDRESS_LINE2 fields; ADDRESS_LINE1 is derived from structured street/plaque/floor/unit fields and ADDRESS_LINE2 remains compatibility-only.
- SOURCE_CODE remains database-backed via CIF.REF_DATA_SOURCE and TENURE_TYPE_CODE via CIF.REF_TENURE_TYPE.
- Postal code, main street and plaque are now required in the UI; Iranian addresses also require province/county/city selections.
- Persian valid-from/valid-to fields are kept and explicitly labeled as address validity, not verification timestamps.

## 0.3.22-prototype-fix2

- Party Address: `SOURCE_CODE` is now selected from `CIF.REF_DATA_SOURCE` instead of free text.
- Party Address: added database-backed `CIF.REF_TENURE_TYPE` for `TENURE_TYPE_CODE` with Owner/Tenant/Organizational/Other seed values per the operational form.
- Party Address: removed the independent `ADDRESS_LINE2` control from the operational UI; the physical optional column remains preserved for compatibility while `ADDRESS_DETAIL` is the single supplementary-address field shown to the user.
- Party Address: clarified `VALID_FROM/VALID_TO` labels as the address-validity interval; no unsupported verification timestamp range was invented.
- Reference lookups now honor `IS_ACTIVE`, `VALID_FROM` and `VALID_TO`; address reference codes are also validated server-side.
- Added idempotent Oracle migration `0.3.22-fix2-address-reference-alignment.sql`.

## 0.3.22-prototype

- Completed Party Operations Phase 11 as the final Party / Customer 360 and end-to-end hardening slice; no new source-domain CRUD was introduced.
- Added a calculated `Party360SummaryRecord` matching the conceptual EA 360 summary without creating a synthetic physical `PARTY_360_SUMMARY` table.
- Added read-only 360 aggregation for the 18 remaining read-only 360 operational tables: products/restrictions/limits, interactions/journey, complaints/alerts, segment/value/metrics/recommendations, organization officers/groups/signatures, registration request and audit metadata.
- Intentionally excludes `SIGNATURE_IMAGE` payload and `AUDIT_EVENT.BEFORE_DATA/AFTER_DATA` payloads from the 360 API; only safe summary/metadata is returned.
- Added `GET /api/v1/cif/parties/{partyId}/readiness` to report workflow completion without changing lifecycle status; customer-specific requirements are conditional on an active Customer Role.
- Hardened customer readiness so the current `PARTY_CUSTOMER` must reference one of the Party's active Customer Role records before its customer number satisfies the workflow.
- Added the final 360 overview UI with summary metrics, readiness blockers and read-only source-system cards, while preserving existing maintenance tabs and routes.
- Closed the remaining backend table-coverage gap: all 48 non-REF operational tables from `CIF-tables4.xlsx` are now referenced by CIF backend code; 18 are explicitly read-only 360 aggregates.
- Added `database/oracle/cif/migrations/0.3.22-registration-request-alignment.sql` because `PARTY_REGISTRATION_REQUEST` exists in the current metadata/EA model but not in the historical bundled CIF DDL snapshot.
- Added `docs/CIF-0.3.22-OPERATIONAL-TABLE-COVERAGE.csv` and Phase 11 documentation for final schema/workflow traceability.

## 0.3.21-prototype

- Added Party Operations Phase 10 at `/cif/parties/{partyId}/operations/lifecycle-merge`, following the supplied operational forms for time-bound lifecycle status changes and Party merge.
- Added append-only `PARTY_STATUS_HISTORY` to Party 360 and a dedicated transactional status-change API that closes the current open period, creates the next period and updates `PARTY` with optimistic locking.
- Reserved lifecycle status `MERGED` for the dedicated merge operation; ordinary status changes cannot move a Party into or out of MERGED.
- Added `PARTY_MERGE_HISTORY` to Party 360 and a transactional merge API that records source/target/reason/conflict handling, marks the source Party as `MERGED`, sets `MERGED_INTO_PARTY_ID` and writes the corresponding status history.
- Merge requires source and target to be distinct, non-merged and of the same Party type; the real `DUPLICATE_MERGED` status reason from `REF_PARTY_STATUS_REASON` is used for the lifecycle transition.
- Kept `MERGE_REASON_CODE` and `CONFLICT_RESOLUTION_CODE` free of synthetic Reference Data because `CIF-tables4.xlsx` defines no physical REF tables for those columns; UI choices are the exact operational options from the supplied HTML.
- Added idempotent migration `database/oracle/cif/migrations/0.3.21-party-lifecycle-merge.sql`, including safe sequence creation from `MAX(ID)+1`, table creation when missing, and physical `PARTY_MERGE_HISTORY.CREATED_DATE` alignment.
- New Party creation now records its initial lifecycle row using the real `NEW_REGISTRATION` status reason, making lifecycle history complete for newly created Parties.
- Added direct navigation from Phase 9 and Party 360 to lifecycle/merge operations.
- Aligned Merge with the supplied operational form: currently-valid `PARTY_NAME`, active/current `PARTY_IDENTIFIER`, and currently-valid `PARTY_CLASSIFICATION` rows follow the canonical target; expired/historical rows remain on the merged source for audit, target primary name/identifier wins on primary conflicts, and duplicate classification periods are not re-created.

## 0.3.20-prototype

- Phase 9 build hotfix: corrected `DatabaseTablesComponent` import path in `party-consents-preferences.component.ts` so Angular can resolve the shared standalone component.

- Added Party Operations Phase 9 at `/cif/parties/{partyId}/onboarding/consents-preferences`, following the supplied HTML workflow after KYC/Risk.
- Added end-to-end `PARTY_CONSENT` CRUD semantics: create/update and lifecycle-preserving revoke, customer decision, capture channel, declaration time, validity, consent-text version, multi-scope text, limitations and evidence.
- Aligned `PARTY_CONSENT` to all 21 columns in `CIF-tables4.xlsx` and added idempotent migration `database/oracle/cif/migrations/0.3.20-consent-preference-alignment.sql`.
- Added `COMMUNICATION_PREFERENCE` CRUD with server-searchable channel/purpose, allowed flag, preferred time window, language, allowed days, time zone and marketing opt-out; aligned all 16 physical columns.
- Added `PARTY_GENERAL_PREFERENCE` CRUD for service/general preferences using actual `REF_PREFERENCE_TYPE` and `REF_SOURCE_SYSTEM`; overlapping validity periods for the same type are blocked.
- Reused the existing Phase 4 `PARTY_DOCUMENT` workflow instead of creating duplicate document CRUD inside Phase 9.
- Added Consent/Preference data to Party 360 and linked Phase 8 directly to the new operational form.
- Preserved physical-model gaps explicitly: no synthetic `REF_LANGUAGE`, consent-text-version, time-zone or allowed-days tables were introduced, and no unsupported preference types were seeded.
- Kept customer rejection distinct from consent lifecycle: `CUSTOMER_DECISION_CODE=REJECT` is authoritative; because the supplied status catalog has no `REJECTED`, the lifecycle remains non-granted without inventing a new reference code.

## 0.3.19-prototype

- Added Party Operations Phase 8 at `/cif/parties/{partyId}/onboarding/kyc-risk` for the supplied operational KYC/Risk/Screening workflow.
- Aligned `KYC_CASE` with the current 27-column `CIF-tables4.xlsx` model, adding the nine operational customer-understanding/PEP/EDD fields end-to-end.
- Added idempotent migration `database/oracle/cif/migrations/0.3.19-kyc-case-alignment.sql` for existing databases; no synthetic FK/REF or new Y/N database checks were introduced where the supplied physical metadata has none.
- Upgraded existing `PARTY_RISK_ASSESSMENT` and `SCREENING_RESULT` CRUD into the dedicated onboarding workflow with optimistic locking and searchable Reference Data.
- Added `EXTERNAL_INQUIRY_RESULT` CRUD, validation and Party 360 visibility; payload reference/hash pairing and request/response/expiry rules mirror the supplied database constraints.
- Added an application guard that prevents physical KYC-case deletion while risk, screening or document records still depend on it.
- Fixed server-searchable Party Reference combos by normalizing `REF_*` UI resource names to the API's kebab-case resource contract centrally in `CifService`.
- Corrected the bundled base DDL so the nine Phase 8 columns belong to `KYC_CASE` only and `ADDRESS` remains unchanged.
- Corrected the root release marker from the inherited 0.3.17 value to `0.3.19-prototype`; backend and frontend versions are synchronized.

## 0.3.18-prototype

- Added Party Operations Phase 7 at `/cif/parties/{partyId}/onboarding/roles` for Party Role and banking-customer relationship management.
- Implemented `PARTY_ROLE` CRUD with optimistic locking, server-searchable `REF_ROLE_TYPE`, `REF_CONTEXT_TYPE` and `REF_WORKFLOW_STATUS`, optional context pairing and related/principal Party selection.
- Implemented the source-model boundary `Party -> Role -> Customer`: only role type `CUSTOMER` creates `PARTY_CUSTOMER` and a customer number; all other roles remain Party roles without customer numbers.
- Added Party 360 role/customer data and a dedicated «نقش‌ها و رابطه بانکی» tab.
- Added idempotent migration `database/oracle/cif/migrations/0.3.18-party-role-customer.sql`: aligns the historical 16-column `PARTY_ROLE` snapshot to the current 24-column model and creates the 13-column `PARTY_CUSTOMER` model when absent.
- Added operational role values from the supplied Party form to `REF_ROLE_TYPE` without changing the generic Role model.
- Added isolated prototype `SEQ_CUSTOMER_NO`; the supplied EA/XMI defines `CUSTOMER_NO` but does not define a bank numbering algorithm, so this sequence is explicitly replaceable by the production customer-number policy.
- Customer roles are not physically deleted; they are closed by status/end-date so `CUSTOMER_NO` history remains stable.
- Fixed a duplicated `SELECT` token in the existing `PARTY_DOCUMENT` read query discovered during Phase 7 QA.

## 0.3.17-prototype

- Added Party Operations Phase 6 at `/cif/parties/{partyId}/onboarding/relationships` for Party-to-Party relationships, beneficial ownership/UBO and authority/representation.
- Added end-to-end CRUD and optimistic locking for `PARTY_RELATIONSHIP`, `BENEFICIAL_OWNERSHIP` and `PARTY_AUTHORITY`.
- Added server-searchable Party selection to the reusable ComboBox flow; related parties are searched by Party ID, name or primary identifier and self-relationship is rejected.
- Added relationship semantic validation: family relations are PERSON-to-PERSON, parent-company/affiliate are ORGANIZATION-to-ORGANIZATION, and BENEFICIAL_OWNER requires a positive ownership/control percentage.
- Added UBO rules: ORGANIZATION-only ownership records, at least one direct/indirect/control percentage, 0..100 validation and `REF_CONTROL_BASIS` lookup.
- Added authority rules using `REF_AUTHORITY_TYPE` and `REF_AUTHORITY_SCOPE`, amount/currency pairing and source-model semantics where `PRINCIPAL_PARTY_ID` is the grantor while `PARTY_ID` mirrors the authorized holder.
- Added Phase 6 data to Party 360 and linked Phase 5 directly to the new operational step.
- Added idempotent migration `database/oracle/cif/migrations/0.3.17-party-relationship.sql` because the supplied current `CIF-tables3.xlsx` contains `PARTY_RELATIONSHIP` while the historical repository DDL snapshot does not; missing sequences continue from `MAX(ID)+1` when data already exists.
- Kept `RELATIONSHIP_TYPE_CODE` as an application-controlled code list because the supplied model does not define an explicit relationship-type REF table; no synthetic REF catalog was introduced.

## 0.3.16-prototype

- Added Party Operations Phase 5 for `CIF.PARTY_CLASSIFICATION`.
- Added create/update/delete APIs with optimistic locking and reference validation.
- Added dependent lookup for `REF_CLASSIFICATION_VALUE` filtered by classification type.
- Added reusable server-searchable ComboBox UI component with debounce.
- Added classifications to Party 360 response and UI.
- Aligned `PARTY_CLASSIFICATION.DESCRIPTION_TEXT` with `CIF-tables3.xlsx` and added an idempotent Oracle migration.
- Added onboarding route `/cif/parties/:partyId/onboarding/classifications`.

## 0.3.15-prototype

- Added Party operational-form Phase 4 at `/cif/parties/{partyId}/onboarding/identifiers-documents`, linked from Phase 3 and Customer 360.
- Added a protected read-only Primary Identifier section and operational CRUD for secondary `PARTY_IDENTIFIER` records; Phase 4 never promotes, demotes or deletes the primary identity created during Phase 1.
- Added reference-backed identifier fields for identifier type, country, issuing authority, verification status, data source and verification method, with validity/issue/expiry controls and optimistic `RECORD_VERSION` editing.
- Added application validation for the Oracle `UQ_IDENTIFIER` business key so duplicate type/value/issuer/valid-from combinations are rejected before a raw Oracle constraint error reaches the UI.
- Added a dedicated `PARTY_DOCUMENT` operational editor including optional KYC association, document type/number, issuer, dates, verification metadata, content hash, secure storage reference and MIME type.
- Synchronized `PARTY_DOCUMENT` with the latest supplied `CIF-tables3.xlsx` by adding `ISSUING_AUTHORITY_TEXT`, `CONTROL_STATUS_CODE` and `DESCRIPTION_TEXT` end-to-end in domain/API, Oracle repository and UI.
- Added an idempotent Oracle migration `database/oracle/cif/migrations/0.3.15-party-document-alignment.sql` for existing environments and aligned the bundled CIF DDL snapshot.
- When a document type matches the Party primary identifier type, the operational form reads the document number from that primary identifier instead of requesting a second manually-entered identity value.
- No synthetic reference catalog was created for `PARTY_DOCUMENT.CONTROL_STATUS_CODE` because the supplied model does not define an explicit REF source for that column.

## 0.3.14-prototype

- Added Party operational-form Phase 3 at `/cif/parties/{partyId}/onboarding/financial-employment` and linked Phase 2 plus Customer 360 directly to this step.
- Added operational CRUD and Party 360 coverage for `FINANCIAL_PROFILE`, `PARTY_EMPLOYMENT`, `PARTY_INCOME_SOURCE`, `PARTY_ASSET_LIABILITY` and `PARTY_LICENSE`, aligned to the supplied `CIF-tables3.xlsx` column set.
- Preserved the model boundary that `PARTY_EMPLOYMENT` is PERSON-only; ORGANIZATION economic activity is maintained on `ORGANIZATION`, with activity licenses in `PARTY_LICENSE`.
- Added financial snapshot fields from the latest schema including net/other monthly income, expected transaction count, funds countries, relationship-purpose code, real-estate/investment values, monthly installments, estimated net worth and financial-capacity code.
- Added employment fields from the latest schema including employment status, occupation group, employer identifier, contract type, insurance number and tax code.
- Added source-of-funds/wealth, tax, occupation, economic-sector, ISIC, verification and license-type lookups only where explicit reference sources exist; no synthetic REF mapping was introduced for unmapped code columns.
- Added database-constraint-aware validation: exactly one employer source for employment, unique financial snapshot per Party/as-of date, license type/number uniqueness, date ordering, non-negative financial amounts and optimistic `RECORD_VERSION` updates.
- Extended Customer 360 with a dedicated «مالی و شغلی» tab for financial profiles, income sources, employment/licenses and asset/liability records.
- No database migration is included; this release targets the supplied current CIF schema definition in `CIF-tables3.xlsx`.

## 0.3.13-prototype

- Added Party operational-form Phase 2 at `/cif/parties/{partyId}/onboarding/contact-address` and redirected successful Phase 1 onboarding directly into this step.
- Synchronized `ADDRESS`, `PARTY_ADDRESS` and `CONTACT_POINT` application models with the supplied `CIF-tables3.xlsx`, including county/structured address details, address verification/source fields, telephone dialing fields, contact owner and verification metadata.
- Added operational support for `CONTACT_POINT_ADDRESS` so a contact point can be explicitly associated with a Party address.
- Added create/edit/delete operations in the Phase 2 UI for addresses, contact points and contact-address associations with optimistic `RECORD_VERSION` updates.
- Added cascading GEO lookups for province -> county -> district -> city using the existing reference-data API; no duplicate geography reference catalog was introduced.
- Added reference lookups for address/contact type, contact purpose, contact-address association type and verification status/method.
- Preserved free optional code entry for `PARTY_ADDRESS.TENURE_TYPE_CODE`, `PARTY_ADDRESS.SOURCE_CODE` and `CONTACT_POINT.OWNER_TYPE_CODE` because the supplied model does not define explicit independent REF/FK mappings for those columns.
- Made address/contact deletion association-safe by removing dependent `CONTACT_POINT_ADDRESS` records inside the same transaction.
- Corrected primary-record semantics so `IS_PRIMARY` is exclusive within each address/contact type, not globally across the whole Party.
- Added a direct «ادامه فرم عملیاتی» action from Customer 360 back to Phase 2.
- No database migration is included; this release targets the supplied current CIF schema definition in `CIF-tables3.xlsx`.

## 0.3.12-prototype

- Synchronized the operational CIF base models with `CIF-tables3.xlsx` for `PARTY`, `PERSON`, `ORGANIZATION`, `PARTY_NAME` and `PARTY_IDENTIFIER`.
- Added `PERSON.NATIONALITY_COUNTRY_CODE` end-to-end in Oracle repository, domain/API models and Customer 360 UI.
- Added the latest `ORGANIZATION` fields end-to-end: `REGISTRATION_COUNTRY_CODE`, `ACTIVITY_STATUS_CODE`, `MAIN_ACTIVITY_DESCRIPTION`, `EMPLOYEE_COUNT`, `ENTERPRISE_SIZE_CODE` and `OWNERSHIP_TYPE_CODE`.
- Extended Party creation with `STATUS_REASON_CODE`, `VALID_FROM` and `VALID_TO` and replaced free-text creation source with `CIF.REF_SOURCE_SYSTEM` where applicable.
- Added the first dedicated operational Party onboarding form at `/cif/parties/new`, separated from the legacy list modal and aligned with the supplied Party operational-form flow.
- Added atomic onboarding API `POST /api/v1/cif/parties/onboarding` covering `PARTY + PARTY_NAME + PERSON/ORGANIZATION + PARTY_IDENTIFIER` in one transaction.
- Added a dedicated «ایجاد Party جدید» navigation item and clarified Party search semantics independently from the banking-customer role.
- Preserved database-supported lifecycle reference codes; no invented `DRAFT` code is persisted because the current `CIF.REF_PARTY_LIFECYCLE_STATUS` catalog does not define it.
- No database/DDL changes; application code is aligned to the supplied current schema definition.

## 0.3.11-prototype

- Reworked the main navigation around business domains instead of listing reference-data domains directly in the sidebar.
- Replaced the three Public / CIF Party / Deposit reference-data sidebar entries with a single «اطلاعات پایه» entry and a dedicated domain-selection hub.
- Moved «درخت جغرافیایی» under «اطلاعات پایه عمومی → اطلاعات جغرافیایی» while preserving its existing route and geography-level management links.
- Removed the «دامنه‌های برنامه‌ریزی‌شده / ادیان» area from the main dashboard.
- Replaced the three reference-domain cards on the dashboard with one compact «اطلاعات پایه» quick-access card.
- Kept Public, CIF/Party and Deposit reference forms physically and functionally separated behind the new reference-data hub.
- No database/DDL changes.

## 0.3.10-prototype

- Removed the detailed public/general reference-data section from the main dashboard.
- Removed geography and other public reference statistic cards from the dashboard; these forms remain available from the dedicated «اطلاعات پایه عمومی» menu.
- Removed the no-longer-needed `/api/v1/dashboard/counts` request from the dashboard component.
- Kept the three independent reference-domain entry cards for Public, CIF/Party and Deposit reference data.

## 0.3.9-prototype

- Separated public reference data from CIF/Party reference data in the main navigation.
- Moved all DPS deposit-product reference forms out of the global sidebar/dashboard list into a dedicated Deposit Reference Data menu.
- Moved all CIF/Party reference forms out of the global sidebar into a dedicated Party Reference Data menu grouped by package.
- Added dedicated reference menu pages with search, form counts and domain-specific routing for Public, Party and Deposit reference data.
- Dashboard now lists only public reference forms in detail; Party and Deposit reference forms are represented by separate domain entry cards.
- Preserved the 0.3.8 database-table labels on all forms.

## 0.3.8-prototype

- Added a visible database-table context to every active data-entry/search form.
- Generic GEO/DPS reference forms now show the exact descriptor-backed `SCHEMA.TABLE` name.
- CIF Party reference forms now show their exact `CIF.REF_*` table name.
- CIF Party list/create and Customer 360 forms now show the operational table or tables actually read/written by each form, including the two-table address form (`CIF.ADDRESS` + `CIF.PARTY_ADDRESS`).
- Added a reusable `DatabaseTablesComponent` so future forms can expose their physical table mapping consistently.

## 0.3.7-prototype

- Completed the Party/Customer reference catalog with Phase 6 Analytics and Recommendation.
- Added 7 CIF reference tables and 24 reviewed seed rows for metrics, metric units, analytics models, recommendations and score metadata.
- Reused `DPS.REF_CUSTOMER_SEGMENT_CODE` instead of creating duplicate `CIF.REF_CUSTOMER_SEGMENT`.
- Original Party Reference source catalog exposes 96 CIF-owned forms; all 104 source definitions are resolved with 8 GEO/DPS mappings and no deferred items. Runtime catalog is 97 after the local `REF_TENURE_TYPE` extension.
- Added completion mapping/documentation for the entire Party/Customer reference catalog.

## 0.3.6-prototype

- Activated all 17 `Workflow and Interaction` CIF reference tables (71 seed rows).
- Added reviewed Persian form titles, primary-key labels and normalized Persian seed captions for the new forms.
- Preserved the explicit `Journey -> Stage -> Event Type` reference hierarchy with Oracle foreign keys and UI lookups.
- Customer 360 KYC status now uses `CIF.REF_WORKFLOW_STATUS` instead of free text.
- Party Reference catalog now exposes 89 active CIF forms; 7 source references remain mapped to GEO/DPS, leaving 8 Analytics/Recommendation source tables for the next phase.

## 0.3.5-prototype

- Activated 11 new `Organization and Product` CIF reference tables (44 seed rows).
- Reused `GEO.CURRENCIES` and `DPS.REF_ORG_UNIT_CODE` instead of creating duplicate CIF sources.
- Added reviewed Persian titles/labels and normalized broken Persian seed captions in the new forms.
- Customer 360 ORGANIZATION now uses lookups for legal form, economic sector and ISIC activity.
- Organization creation now selects legal form from `CIF.REF_LEGAL_FORM` and no longer falls back to the invented code `OTHER`.
- Party Reference catalog now exposes 72 active CIF forms; 7 source references are mapped to existing GEO/DPS data, leaving 25 deferred source tables.

## 0.3.4-prototype

- Activated 8 Contact reference tables (33 seed rows).
- Kept existing GEO country/province/city/district/language as the single geography source of truth.
- Customer 360 now uses lookups for address type, contact type, contact purpose and country.
- Party Reference catalog now exposes 61 active forms.

# Changelog


## 0.3.3-prototype

- Activated all 21 `Compliance and Risk` Party/Customer reference tables (87 seed rows).
- Added reviewed Persian form titles and primary-key labels for the new reference forms.
- Normalized verification status `NOT_VERIFIED` to operational CIF code `UNVERIFIED`.
- Replaced free-text KYC/risk/screening/verification fields in Customer 360 with reference-data lookups where a source table exists.
- Party Reference catalog now exposes 53 active forms.

## Unreleased
- Build fix for Spring Boot 4.1 / Jackson 3: migrated Party reference metadata loading from `com.fasterxml.jackson.databind.ObjectMapper` to `tools.jackson.databind.json.JsonMapper`; retained Jackson annotations and corrected Angular lookup typing to remove NG8102.

## 0.3.2-prototype
- Added CIF Party/Customer Reference Data Phase 1 generated from the supplied interactive reference model.
- Added 32 code-keyed reference forms: all 31 `Identity and Party` tables plus `REF_LEGAL_CAPACITY`.
- Added a new generic CIF reference engine supporting textual primary keys and the composite key of `REF_CLASSIFICATION_VALUE` without introducing surrogate IDs.
- Added Oracle DDL and 123 seed rows for the enabled phase.
- Connected Customer 360 PERSON and selected PARTY/name/identifier/document fields to the new CIF reference lookups while keeping country/language on existing GEO sources.
- Deferred geography/currency/language duplicates and verification-status normalization pending explicit mapping.


## 0.3.1-prototype

- تکمیل فرم PERSON در Customer 360 با فیلدهای تاریخ فوت و توانایی جسمانی.
- تبدیل کشور محل تولد و زبان اصلی به Lookup واقعی از `GEO.COUNTRIES` و `GEO.LANGUAGES`.
- تبدیل جنسیت و وضعیت اقامت به Lookup از جداول مرجع موجود `DPS.REF_GENDER_CODE` و `DPS.REF_RESIDENCY_STATUS_CODE`.
- عدم اختراع کدهای مرجع برای وضعیت تأهل، اهلیت قانونی و وضعیت حیات؛ این فیلدها تا دریافت DDL/Data مرجع مستقل، کد فعلی را حفظ می‌کنند.
- نمایش پیام واقعی ProblemDetail سمت Backend در عملیات CIF به‌جای پیام عمومی ثابت.
- تشخیص اختصاصی `ORA-01950` و بازگرداندن خطای قابل فهم برای کمبود Quota در Oracle.
- بدون تغییر در DDL جداول CIF و بدون نیاز به Migration پایگاه داده.

## 0.3.0-prototype

- تغییر عنوان رابط کاربری به «سامانه دموی بانکداری متمرکز».
- افزودن Schema `CIF` به تنظیمات Runtime با همان مشخصات Oracle محیط تست.
- افزودن ماژول «مدیریت مشتری / CIF» با فهرست Party و صفحه Customer 360.
- فعال‌سازی فاز اول CIF برای ۱۲ جدول: PARTY، PERSON، ORGANIZATION، PARTY_NAME، PARTY_IDENTIFIER، ADDRESS، PARTY_ADDRESS، CONTACT_POINT، KYC_CASE، PARTY_DOCUMENT، PARTY_RISK_ASSESSMENT و SCREENING_RESULT.
- افزودن CRUD تایپ‌شده Spring/JdbcClient برای مشخصات پایه، نام و شناسه، نشانی و تماس، KYC و مدرک، ریسک و غربالگری.
- افزودن Optimistic Lock بر پایه `RECORD_VERSION` در عملیات ویرایش CIF.
- افزودن آمار CIF به Dashboard و لینک مستقیم به فهرست مشتریان.
- جلوگیری از شکست کامل آمار اطلاعات پایه در صورت خطای یک جدول؛ شمارش جداول سالم ادامه پیدا می‌کند و خطای جدول ناموفق در Log ثبت می‌شود.
- نگهداری Snapshot واقعی `CIF-050517.sql` در `database/oracle/cif/ddl`.
- اصلاح بسته سورس 0.3.0 و بازگرداندن کامل `frontend/src/app` شامل Route، منوی CIF و صفحات Customer 360.

- انتقال قاره‌ها، کشورها و شهرهای خارجی به گروه «اطلاعات جغرافیایی» در منو و Dashboard.
- نمایش ستون «نام انگلیسی» به‌جای «نسخه جاری» در Gridهای اطلاعات پایه محصول سپرده.
- حذف خروجی‌های زمان‌دار `database/oracle/exports` از Source و افزودن آن به `.gitignore`.
- ثبت مستقیم مشخصات اتصال Oracle محیط تست در هر دو فایل `application.yml` برای اجرای بدون تنظیم CMD.
- جداسازی Indexهای آینده `DEPOSIT_PRODUCT*` از DDL فعال جدول‌های `REF_*` و انتقال آن‌ها به `database/oracle/dps/pending`.
- افزودن Comment ستون `CREATED_BY` برای همه ۵۰ جدول مرجع DPS.
- افزودن `frontend/public/.gitkeep` برای تطابق ساختار Repository با تنظیمات Angular.

- افزودن ابزار استخراج DDL و داده Oracle از طریق `bin\export-database.cmd`.
- دریافت تأیید کاربر پیش از اتصال و شروع عملیات.
- استخراج Sequence، Table، Index، PK/UK/CHECK، FK، Comment، Trigger و Object Grant برای هر جدول.
- تولید داده هر جدول در قالب `INSERT INTO ... VALUES ...` با خروجی UTF-8 و Manifest اجرا.

## در حال توسعه

- فعال‌سازی ۵۰ فرم `DPS.REF_*` زیر منوی «اطلاعات پایه محصول سپرده».
- افزودن Descriptor مشترک برای ساختار کد، عنوان فارسی و انگلیسی، وضعیت، بازه اعتبار و نسخه‌بندی جداول مرجع DPS.
- پشتیبانی فرم عمومی از فیلدهای `DATE` و توضیحات چندخطی.
- ثبت `CREATED_BY` متناسب با نوع `VARCHAR2(100)` و جلوگیری از تغییر آن در Update.
- افزودن کنترل هم‌زمانی Optimistic بر اساس `RECORD_VERSION` برای جداول دارای این ستون.
- نگهداری اسکریپت‌های Oracle دریافت‌شده در `database/oracle/dps/ddl`.
- اصلاح `bin/start.cmd` برای بازماندن پنجره پس از خاتمه یا خطای Java.

## 0.2.0-prototype

- تغییر هویت پروژه از Reference Data Prototype به `core-banking-prototype`.
- تغییر Maven Artifact، نام JAR، نام پروژه Angular و عنوان رابط کاربری.
- تغییر Root Package به `com.behsazan.corebanking`.
- انتقال کد موجود اطلاعات پایه به ماژول منطقی `referencedata` بدون تغییر رفتار اجرایی.
- انتقال اجزای مشترک فعلی به `com.behsazan.corebanking.shared`.
- یکپارچه‌سازی Property مالک جداول اطلاعات پایه در `core-banking.schemas.reference-data`.
- تعریف Schema محصول‌ساز سپرده با نام `DPS` در تنظیمات، بدون ایجاد کلاس یا قابلیت فرضی.
- بازآرایی اسکریپت‌های Oracle در `database/oracle/geo/{ddl,data}`.
- افزودن ساختار `database/oracle/dps/{ddl,data}` برای دریافت اسکریپت‌های واقعی پایگاه داده.
- افزودن Data Scriptهای موجود GEO و اصلاح مستند Owner دو Export مشاغل از CIF به GEO.
- تغییر مسیر UI فرم‌های اطلاعات پایه به `/#/reference-data/{resource}` با حفظ Contract فعلی REST.
- به‌روزرسانی صفحه مشخصات فنی، مستند معماری و راهنمای فاز Deposit Product Factory.

## 0.1.6.2-prototype

- یکپارچه‌سازی فونت تمام صفحات با پشته محلی و قابل اتکای `Tahoma`, `Segoe UI`, `Arial` و حذف وابستگی متن به Vazirmatn آنلاین.
- حفظ فونت اختصاصی Material Symbols برای آیکون‌ها و فونت Monospace برای کدها و شماره نسخه‌ها.
- تغییر عنوان «کاربر نمونه» به «کاربر مدیر».

## 0.1.6-prototype

- غیرفعال‌سازی Font Inlining در Build تولید Angular برای جلوگیری از خطای Build در محیط‌های بدون دسترسی به `fonts.googleapis.com`
- یکسان‌سازی Schema تمام جداول فعال روی `GEO`
- اصلاح Schema پیش‌فرض دامنه اشتغال از مقدار قبلی به `GEO`
- حذف راهنمای تنظیم Schemaهای جایگزین از مستندات Runtime
- اصلاح و تجمیع اسکریپت‌های DDL عمومی، آموزشی، مشاغل و شهرهای خارجی با مالکیت `GEO`
- افزودن صفحه «مشخصات فنی سیستم» شامل معماری، فناوری‌ها، ابزارهای Build و قابلیت‌های نسخه جاری
- افزودن لینک صفحه مشخصات فنی در داشبورد و منوی اصلی
- افزودن Theme روشن، تیره و هماهنگ با سیستم
- نگهداری انتخاب Theme کاربر در Local Storage مرورگر
- بازطراحی رنگ‌های عمومی صفحات و فرم‌ها بر پایه CSS Variable برای پشتیبانی کامل از Theme
- افزودن راهنمای نگهداری و به‌روزرسانی صفحه مشخصات فنی در هر Release
- افزودن همگام‌سازی خودکار شماره Release و نسخه فناوری‌ها از `VERSION`، `pom.xml` و `package.json`

## 0.1.5-prototype

- فعال‌سازی ۱۴ فرم جدید و افزایش فرم‌های فعال از ۶ به ۲۰ مورد
- افزودن فرم‌های قاره، زبان، ارز، کشور، گروه خونی، بانک و شهر خارجی
- افزودن فرم‌های گروه شغلی و شغل
- افزودن فرم‌های گروه، زیرگروه، مقطع، رشته و دانشگاه
- افزودن Combo والد و فیلتر والد برای همه روابط سلسله‌مراتبی جدید
- افزودن Lookupهای مستقل برای روابط چندگانه کشور و دانشگاه
- پشتیبانی Repository از نام متفاوت ستون‌های Audit در جداول مشاغل
- قابل تنظیم شدن Schemaهای عمومی، آموزشی و اشتغال از طریق `application.yml`
- حذف وضعیت Planned از جداولی که اکنون فعال شده‌اند

## 0.1.4-prototype

- جایگزینی عنوان عمومی «والد» در Grid با عنوان فارسی والد تعریف‌شده در Descriptor؛ مانند «استان» در Grid شهرستان‌ها
- افزودن Combo فیلتر والد به همه فرم‌های سلسله‌مراتبی؛ مانند فیلتر استان در فهرست شهرستان‌ها
- اعمال فیلتر والد به Query سمت سرور و بازگشت خودکار به صفحه اول
- افزایش سقف Lookup از ۵۰۰ به ۵۰۰۰ گزینه برای پوشش کامل سطوح جغرافیایی فعلی

## 0.1.3-prototype

- تثبیت مستقیم تنظیمات Oracle در هر دو فایل `application.yml`
- استفاده از Service Name برابر `FREEPDB1`
- تنظیم کاربر Oracle روی `SYSTEM` و رمز محلی تعیین‌شده برای نمونه
- حذف وابستگی اتصال Oracle به متغیرهای محیطی `ORACLE_URL`، `ORACLE_USERNAME` و `ORACLE_PASSWORD`

## 0.1.2-prototype

- اصلاح نمایش آیکون‌های Angular Material با Material Symbols Rounded در محیط RTL
- جلوگیری از نمایش متن شکسته آیکون‌ها مانند `age`، `city` و `ree`
- نمایش پیام روشن در داشبورد هنگام خطای Backend یا اتصال Oracle
- اصلاح آدرس پیش‌فرض Oracle به قالب Service Name: `@//localhost:1521/FREEPDB1`
- حذف رمز آزمایشی `change-me` و استفاده از مقدار خالی/متغیر محیطی `ORACLE_PASSWORD`
- اصلاح `start.cmd` و `start.sh` برای یافتن JAR در پوشه `app` یا `backend/target`
- هم‌راستاسازی Build با Startup از طریق کپی JAR نهایی در پوشه `app`

## 0.1.1-prototype

- اصلاح فرم پویا در Angular با جایگزینی `FormGroup<Record<...>>` با `FormRecord<FormControl<unknown>>`
- رفع خطای TypeScript `TS2769` در `removeControl` برای کلیدهای Runtime

## 0.1.0-prototype

- پروژه مستقل Java/Angular بدون وابستگی به SchemaForge
- Runtime عمومی Descriptor-driven برای اطلاعات پایه
- CRUD کامل شش سطح جغرافیایی Oracle
- Grid با جست‌وجو، مرتب‌سازی و صفحه‌بندی سمت سرور
- فرم سلسله‌مراتبی با Lookupهای وابسته
- درخت جغرافیایی Lazy-load
- Audit صحیح: عدم مقداردهی `LAST_MODIFIED_*` در Insert
- Catalog توسعه آینده برای کشور، ارز، زبان، مشاغل و اطلاعات تحصیلی
- پاسخ خطای استاندارد ProblemDetail
- Build نهایی Angular داخل Executable JAR

## 0.1.6.1-prototype

- نمایش صریح دکمه «تم» در نوار بالا، به‌جای اتکا به آیکون تنها.
- اضافه‌شدن لینک دوم «مشخصات فنی» در نوار بالا، علاوه بر منوی اصلی و داشبورد.
- نمایش شماره نسخه در نوار بالا برای تشخیص سریع Build در حال اجرا.
- حذف JAR قدیمی در ابتدای Build تا Build ناموفق با نسخه قبلی اشتباه نشود.
- کنترل اشغال‌بودن پورت 8091 پیش از اجرا و نمایش PID نسخه قبلی.
- اضافه‌شدن `bin/stop.cmd` برای توقف کنترل‌شده سرویس روی پورت 8091.
