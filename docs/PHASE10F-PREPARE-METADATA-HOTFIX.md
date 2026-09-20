# Phase 10F Prepare Tool - Oracle Metadata Required Columns Hotfix

This hotfix updates `tools/prepare-dps2-phase10f-runtime.mjs` so Product Builder create payloads honor required control columns discovered from the live Oracle descriptor.

Handled safely:
- `IS_DELETED` -> 0
- `IS_ACTIVE` -> 1
- `IS_CURRENT` -> 1
- other required `*_FLAG` -> 0
- required columns with explicit metadata options -> first metadata-provided option

If a required column has no database default, no explicit payload value, and no safe metadata-based value, the tool stops with a precise error instead of inventing business data.
