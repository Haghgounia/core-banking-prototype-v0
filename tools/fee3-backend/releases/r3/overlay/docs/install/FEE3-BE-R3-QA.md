# FEE3-BE-R3 Quality and Safety Report

- Baseline: FEE3-BE-R2, Oracle physical DDL with 11 existing FEE3 tables.
- New API layer: dedicated R3 controller, policy, preview, service, repository,
  controller-scoped error handler (no modification to existing R1/R2 controllers).
- Arithmetic: 33 independent runtime checks, including three-collateral
  portfolio cases (1,475,000 IRR allocated; 1,700,000 IRR covered).
- Policy: Maker/Checker separation and fail-closed transition guards tested.
- Static source verification: 31 checks.
- Java compile against synthetic Spring/Jackson/JDBC/Servlet signatures: PASS.
  This cannot substitute for Spring Boot 4.1.0 Maven compilation.
- New JUnit test source: prepared; execution with Maven pending.
- Angular templates and JavaScript: not modified by R3.
- Oracle integration, real bank session, real RBAC, Maven, Angular AOT, browser:
  NOT EXECUTED in this runtime. No claim of production readiness.
- Feature flags default to false; Maker/Checker requires actual servlet identity.
- Approval is not publication, activation, accrual, fee collection or posting.
- Known intentional gaps: complete business validation matrix for all FCE
  methods, arbitrary formula sandbox, ACT/ACT Jalali, PMT/IPMT, certified
  financial test evidence, real authorization and entitlement infrastructure,
  overlapping scope checking across live versions, revision 2+, PDL reference
  lookup and banking source-of-truth verification.
