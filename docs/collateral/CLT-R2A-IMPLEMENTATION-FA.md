# CLT-R2A - Collateral Registry and Search (read only)

Roadmap: CLT-ROADMAP-R1.0; phase R2. Baseline: Core Banking Prototype 0.11.0 with R1A + R1B installed.

## Delivered

- Java package `com.behsazan.corebanking.collateral.registry`: registry/read APIs, strict decimal-string IDs, scoped-by-branch SQL and independent JWT security chain.
- Four GET workflows for registry: list, exact registration number, record by ID, active type list and configured type attributes. A readiness endpoint is also present.
- Angular standalone registry workspace: list, direct lookup, read-only profile, browse type attributes for introduction design.
- 2 limited Frontend integrations (routes and sidebar); no changes to IAM, CIF, DPS2, existing Collateral reference classes, pom.xml, VERSION or global config.
- Independent registry flag `CLT_REGISTRY_READS_ENABLED=false` default; requires `CLT_REFERENCE_READS_ENABLED=true` + valid JWT (RS256 issuer, audience, expiration) + `SCOPE_clt.collateral.read` + JWT `branch_code` + least privilege read-only CLT account.

## Not delivered / deliberately disabled

- No `POST`, `PUT`, `PATCH` or `DELETE` on `/api/v1/collaterals`.
- No DDL, DML, migration, seed import, live Oracle verification or change of bank policies.
- No generation of registration numbers until G-17 resolved against NUMBER(19) and actual CIF identifier lengths.
- No mutable introduction/amendment, counted items, Party verification or cross-branch visibility until corresponding gaps and contracts are resolved.
- UI V1.42 is a reference, not embedded as JavaScript; the initial R2 screens implement the visible search/list/profile and show the introduction type/attribute prerequisites.

## API

- `GET /api/v1/collaterals?page=0&size=20`
- `GET /api/v1/collaterals?registrationNumber=12345`
- `GET /api/v1/collaterals/by-registration/12345`
- `GET /api/v1/collaterals/123` (internal ID)
- `GET /api/v1/collaterals/types`
- `GET /api/v1/collaterals/types/12/attributes`
- `GET /api/v1/collaterals/readiness`

Note: Oracle `NUMBER(19)` values are normalized to strings in JSON; frontend never uses JS number for registration IDs. Missing and unauthorized collateral records both return 404 with no row data. Branch scope comes exclusively from the verified JWT claim `branch_code`.

## Integration

All R1 endpoints remain under `/api/v1/collateral/reference/**`. New R2 endpoints are plural `/api/v1/collaterals/**` and have their own security chain at order 3, behind IAM order 1 / reference order 2. No global SecurityFilterChain changes are introduced. All SQL names are hardcoded; parameters are bound.

## R1 closure gate

R1B is installed but not proven closed: real Maven/Angular builds, Oracle dictionary/privilege check, IdP JWT and maker/checker transactional tests, G-10 and G-15 decisions remain open. R2 read-only development is allowed to proceed in parallel; R1 cannot be marked CLOSED without evidence.

## Next R2 slice

The next implementation must finalize G-17 numbering, G-03/G-12 counted-item model, G-01 amendments and verified CIF Party lookup contract. Only after those decisions, with audit, branch policy and idempotency tests, can a gated `POST /api/v1/collaterals` be implemented and allowed in a controlled environment.
