Core Banking Prototype 0.3.71 - build guard fix

Issue:
  tools/verify-calendar2-reference.mjs expected referenceForms: 203
  while system-specification.data.ts correctly contains referenceForms: 205
  after adding two GEO name romanization forms.

Fix:
  Replace tools/verify-calendar2-reference.mjs with the corrected file.

Then run from project root:
  bin\stop.cmd
  build-production.cmd
  bin\start.cmd

Expected build guard output:
  CAL2 verification OK: ...
  GEO name romanization CRUD static contract: OK

After start, Spring Boot log must show:
  Starting CoreBankingApplication v0.3.71-SNAPSHOT

Validate endpoints:
  curl -i http://localhost:8091/api/v1/catalog/name-romanization-dictionary
  curl -i http://localhost:8091/api/v1/catalog/name-affix-dictionary
