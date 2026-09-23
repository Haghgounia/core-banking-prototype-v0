# QA Report — 0.11.0 Phase 11B

Status: SOURCE QUALIFIED / TARGET DB VERIFICATION REQUIRED

Static contract: 22/22 PASS.
Regression: Phase 8, Phase 9, 0.9.2, 0.9.3, 10, 10B, 10D and 10E/10F PASS.

Database migration `0.11.0-phase11b-account-servicing-core.sql` performs an idempotent account-party materialization from opening data. It does not delete or rewrite opening records.

Runtime target must execute `tools\\apply-dps2-phase11b.cmd` before Phase 11B is declared CLOSED.
