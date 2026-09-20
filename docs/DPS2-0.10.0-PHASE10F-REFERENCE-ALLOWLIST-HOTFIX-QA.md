# DPS2 0.10.0 Phase 10F Reference Allow-list Hotfix

## Root cause
`DepositOpeningRuntimeValidator` validates Phase 10 reference values through `DepositOpeningRuntimeRepository.referenceCodeExists(...)`.
The repository protects dynamic reference-table lookup with the static `REFERENCE_TABLES` allow-list. Seven tables introduced/used by Phase 10 were missing from this allow-list, so valid Oracle rows were rejected before SQL lookup.

Missing tables:
- REF_DEP_OPEN_JOINT_BASIS
- REF_DEP_OPEN_ACTIVATION_STATUS
- REF_DEP_OPEN_FUND_PURPOSE
- REF_DEP_OPEN_OBLIGATION_TYPE
- REF_DEP_OPEN_SETTLEMENT_STATUS
- REF_DEP_OPEN_CHECK_PHASE
- REF_DEP_OPEN_BLOCKING_SCOPE

## Fix
The seven Phase 10 reference tables were added to the repository allow-list.
The Phase 10E/10F static verifier now compares every `checkRef(...)` reference table used by `DepositOpeningRuntimeValidator` with the repository allow-list and fails if any table is missing.

## Static verification
Expected result:

`DPS2 Operational Opening v5 Phase 10E/10F verification: 32/32 passed.`

## Runtime qualification sequence
After applying this source patch:
1. Rebuild backend JAR.
2. Replace `app/core-banking-prototype.jar`.
3. Restart the application.
4. Run `tools/prepare-dps2-phase10f-runtime.mjs`.
5. Run `tools/runtime-dps2-phase10f-e2e.mjs docs/examples/phase10f-e2e-fixture.runtime.json`.

No database migration or reference seed is required for this hotfix if Phase 10 foundation migration already completed successfully.
