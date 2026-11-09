# R3 HF2 - All Three Panel HTML Safe Merge / 2026-11-09

**Product version:** `0.11.0`  
**Phase:** R3 - Sponsor, Insurance and Valuation  
**Last successful installation confirmed by user:** `CLT-R2-REVIEW-DECISION-INTEGRITY-2026-11-09`  
**Failed attempts:** R3 Shared Intake (insurer HTML hash mismatch), then R3 Shared Intake HF1 (sponsor and valuation HTML hash mismatches). Both stopped at precheck, so neither is a confirmed successful installation.

## Root cause and resolution

The HF1 installer protected the insurer HTML with a surgical append-only merge but retained whole-file SHA-256 replacement for the sponsor and valuation HTML. Those two files have local changes not represented in the known hash allowlist; safely refusing to overwrite them was correct. The exact changes in the user's local files are not visible in this authoring environment.

HF2 treats all three Angular HTML panels as managed append-only merge targets:

- Preserve original existing HTML bytes including site/logo or other unrelated changes, UTF-8 BOM, and LF/CRLF line endings.
- Validate required panel-specific anchors and the final section boundary; add only one R3 Stage worklist component before the root closing section.
- Reject malformed existing worklist snippets or missing structural anchors before writing any file.
- Check the SHA-256 fingerprints of every remaining file, backing up all changed files, and abort atomically on conflicts.
- Keep the original shared app DataSource, original starter, Stage write preconditions and all existing reference-data behavior.

## Delivery limits

R3 Stage tables are still candidates for controlled DBA migration, so the code does not claim live Oracle write readiness. No DDL/DML is run by the installer. No IAM development, final collateral registration or final sponsor/insurer/valuation approval is included. Business gaps and phase R0-R7 names/order remain unchanged.

## Verification status

Package validation and simulated installs on the reconstructed R2 baseline, customized HTML variants, idempotent re-installs, original-state rollbacks and fail-closed scenarios are documented in `QA-REPORT-FA.md`. Actual Windows build, user's exact HTML files, live Oracle and analyst UAT are **NOT VERIFIED** by this package author.
