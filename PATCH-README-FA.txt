DPS2 0.11.0 - Phase 11N-D R4 Historical 11H Verifier Compatibility Hotfix

Scope:
- Fixes only the historical Phase 11H static verifier after Phase 11N-C formally moved Document Step 04 from PARTIAL to DONE.
- Preserves the historical PARTIAL-until-Step05 rule as evidence and also requires the post-11N-C DONE closure statement.
- No Java business logic change.
- No DDL or DML.
- No runtime behavior change.

Apply over an existing R3 repository, then run:
  set "PATH=E:\oracle;%PATH%"
  node tools\verify-dps2-step04-canonical-profit-11h.mjs
  tools\qualify-dps2-phase11nd.cmd
