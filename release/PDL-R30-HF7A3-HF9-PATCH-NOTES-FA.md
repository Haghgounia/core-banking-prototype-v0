# HF9 / Final-C2 — Patch Notes

Baseline: `R30-HF7A3-HF8` (not HF7/HF6).
Scope: PDL tiered pricing semantics for design approval only; no database migration.

To install: back up the HF8 project, extract the root-relative overlay ZIP into the project root, then run:

```bat
node tools\verify-pdl-r30-hf7a3-hf9-single-rate.mjs
build-production.cmd > "%TEMP%\HF9-build.log" 2>&1
```

Do not claim HF9 accepted until the full Windows build is successful. Oracle UAT is separate.
More details: `docs/PDL-2026-10-16-R30-HF7A3-HF9-SINGLE-RATE-QA-FA.md`.
