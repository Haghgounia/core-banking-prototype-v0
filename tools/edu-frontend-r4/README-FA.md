# EDU Frontend R4

Baseline: core-banking-prototype-v0-2026-11-01-1800.zip + EDU Lookup R3 already installed.

Install: extract ZIP into project root, then run `tools\edu-frontend-r4\INSTALL.cmd`.

The patch adds a **read-only** UI lookup panel to six EDU reference-data pages. It does NOT replace the save/edit controls, modify Backend, CIF, GEO or Oracle, or migrate data.

After install: stop, build-production.cmd, start. Open EDU reference pages and test all four lookup types, search, select, and API error/empty states.

Installer protects the two existing files with exact baseline SHA256 checks and backs them up. Build and browser UAT are NOT verified here.
