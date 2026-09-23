Core Banking Prototype 0.10.0 - Final Verifier Docker Fallback Hotfix

Purpose:
- Keeps local SQL*Plus support.
- If sqlplus.exe is not on Windows PATH, automatically falls back to a running Oracle Docker container.
- CORE_BANKING_ORACLE_CONTAINER can be set explicitly if auto-detection is ambiguous.

Apply by extracting this ZIP over the project root, then run:
  tools\final-verify-0.10.0.cmd

No database migration, rebuild, or application restart is required for this verifier-only hotfix.
