# CLT-R2A QA evidence and release gate - 2026-11-03

**Source baseline**: Core Banking Prototype 0.11.0 + CLT-R1A + CLT-R1B.
**Canonical roadmap**: CLT-ROADMAP-R1.0. This release starts R2 read-only; R1 is not CLOSED.

## Tests performed in this environment

| Check | Result | Boundary |
|---|---|---|
| Fresh ZIP 0.11.0 -> R1A -> R1B -> R2A installation | PASS | R1A changed 25, R1B 37, R2A 20 files in isolated fixture |
| R2A idempotent second installation | PASS | PLANNED=0, no overwrite |
| Expected conflict on edited CollateralRegistryPolicy.java | PASS | Precheck stops with EXISTING_MODULE_FILE_DIFFERS |
| All R2A payload SHA-256 after install | PASS | 18 files |
| Shared frontend integration | PASS | Exactly 2 shared files, anchored merge |
| IAM/CIF/DPS2 backend and frontend feature roots | PASS | Byte-for-byte identical to source baseline |
| Java AST syntax | PASS | 9 Java files, not semantic compilation |
| TypeScript syntax/transpile | PASS | 3 TypeScript files, not Angular template compilation |
| Standalone pure Java validation smoke test | PASS | 19 validations for IDs, claim branch and page bounds |
| Security source checks | PASS | GET only; reads default false, dual flag and scope, branch filtered in every record query |
| Oracle execution | NONE | No DB connection, DDL, DML or seed import |

## Not performed / not passed off as validated

- Full Maven compile, Spring Boot context/JUnit, complete Angular AOT build, template compile, end-to-end browser and production JAR integration were **NOT RUN**. Maven/Angular dependency caches are absent in this sandbox.
- Live Oracle CLT privilege and SQL query checks were **NOT RUN** (no bank database connection).
- Token issuer/JWKS, JWT branch_code claim and actual user privileges were **NOT TESTED** against IAM/IdP. Dev claims must not be substituted.
- Functional UAT for introduction, amendment, countable assets and assignment are not implemented in R2A.
- R1 remains OPEN because G-10, G-15 signoff, approved source-of-truth schema, real maker/checker transaction, IAM integration and HQ UAT have no verified evidence.

## Security defaults / design decisions

- R2 registry no POST/PUT/PATCH/DELETE operations.
- `CLT_REGISTRY_READS_ENABLED=false` defaults to deny; even when true, it requires `CLT_REFERENCE_READS_ENABLED`, named JWT decoder, `SCOPE_clt.collateral.read` and `branch_code` trusted claim.
- Direct registration and ID lookup use server-side `BRANCH_CODE = ?` and return 404 for records not found **or** outside authorized branch; G-13 cross-branch remains out of scope.
- Oracle IDs are serialized as decimal strings to avoid JavaScript Number precision loss.
- R1B source code, IAM S1B, CIF and DPS2 are unchanged. Core Banking master version remains 0.11.0.

## Next gates

1. Execute the single `INSTALL-AND-BUILD.cmd` in the intended Windows build environment, capture the Maven/Angular build report and the integrated JAR class presence.
2. Verify SELECT-only user, actual `CLT` Data Dictionary, JWT issuer/audience/branch_code and browser token contract.
3. Obtain approved Party/CIF lookup contract and G-17 numbering decision before enabling registration write path.
4. Resolve G-01 and G-03/G-12 at data-model level before finishing R2 introduction/change flows.
