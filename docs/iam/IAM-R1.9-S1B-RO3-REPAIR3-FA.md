# IAM-R1.9 / S1B-RO3 / REPAIR3 - Test-context registration fix

Status: source-only repair of the signed HTTP integration test. No change to IAM runtime, UI, Oracle schema, or enabled features.

## Confirmed failure on Windows (2026-11-03 08:23 +03:30)

REPAIR2 installed successfully (`5/5`) and Maven test classes compiled, but Spring failed before the HTTP matrix because a field-level named `@MockitoBean` could not locate `iamS1bJwtDecoder` in its test ApplicationContext.

## Change

The isolated `LabApp` now registers `@Bean("iamS1bJwtDecoder")` returning a test-only, *real Nimbus* RS256 decoder bound to a fresh RSA public key. It constructs the HTTP security filter by **calling the actual production** `IamS1bSecurityConfiguration.iamApiSecurity(...)` method. The lab repository is a mock and `DataSourceAutoConfiguration` remains excluded. Every token submitted to the HTTP test is checked directly by that real Nimbus decoder in the production Spring Security filter chain; no JWT bypass has been added.

The `@MockitoBean` mechanism is not used at all, avoiding both `REPLACE` and `REPLACE_OR_CREATE` bean-override ambiguity. **Only the Oracle read repository is mocked**; JWT signature and claims are validated using the real Nimbus implementation.

## Acceptance gates

1. Pre-stop Maven `-Dtest=IamS1bSignedHttpIntegrationTest test` must pass and emit `IAM_S1B_RO3_SIGNED_HTTP_LAB_PASS=12/12`.
2. Full Maven/Angular production build, current JAR artifact proofs, and live disabled-API HTTP checks must pass before accepting deployment.
3. Real bank IdP/JWKS and Oracle DEV read-only access are **still pending external acceptance**.

No IAM POST/PUT/PATCH/DELETE, Oracle DDL/DML/COMMIT, token fixture endpoint, or production security change is part of this repair. The canonical IAM-00..IAM-08 roadmap remains unchanged.
