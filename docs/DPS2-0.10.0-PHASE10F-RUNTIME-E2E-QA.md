# DPS2 0.10.0 — Phase 10F Runtime / E2E Qualification

## Qualification matrix
The runtime harness covers all four supported deposit families:
- `QARD_SAVINGS`
- `CURRENT_ACCOUNT`
- `SHORT_TERM_DEPOSIT`
- `LONG_TERM_DEPOSIT`

Each case performs: Runtime validation -> Aggregate persistence -> Account creation -> Settlement -> Activation Readiness -> Activation. Optional closure is supported when the fixture supplies the real expected record version.

## Evidence policy
The harness intentionally has no built-in fake KYC/CDD/SIAH/tax/compliance evidence. A local fixture must contain references that are valid in the target environment. Placeholder markers are rejected before any mutation call.

## Command
`node tools/runtime-dps2-phase10f-e2e.mjs <fixture.json>`

Default base URL is `http://localhost:8091`; override with `CORE_BANKING_BASE_URL`.

A Phase 10F runtime PASS is only claimed when the script ends with `PHASE10F_RUNTIME_E2E_PASS`.

## Windows qualification sequence
From the project root:

1. `build-production.cmd`
2. `bin\stop.cmd` then `bin\start.cmd`
3. Prepare a local fixture from `docs\examples\phase10f-e2e-fixture.template.json` using only real Oracle/CIF/PDL references and real Compliance/Inquiry evidence.
4. `node tools\runtime-dps2-phase10f-e2e.mjs <your-fixture.json>`

The release remains a candidate if any family fails or if the final marker is absent. Do not replace missing KYC/CDD/SIAH/tax/compliance evidence with dummy values; the harness deliberately rejects placeholder markers.

