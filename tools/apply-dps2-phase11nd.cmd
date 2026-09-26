@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
set "MIG=%ROOT%\database\oracle\dps2\migrations\0.11.0-phase11nd-steps05-10-canonical-audit.sql"
set "VER=%ROOT%\database\oracle\dps2\verification\0.11.0-phase11nd-steps05-10-canonical-audit-verifier.sql"
echo ============================================================
echo Core Banking Prototype 0.11.0 - DPS2 Phase 11N-D
echo Steps 05-10 - Canonical Regression / Audit
echo ============================================================
echo [1/3] Static verification...
node "%ROOT%\tools\verify-dps2-phase11nd-steps05-10-canonical-audit.mjs" || exit /b 10
echo [2/3] Applying scoped Oracle reconciliation...
call "%ROOT%\tools\run-oracle-sql.cmd" "%MIG%" || exit /b 21
call "%ROOT%\tools\run-oracle-sql.cmd" "%VER%" || exit /b 22
echo [3/3] Oracle gate complete.
echo ============================================================
echo PHASE11ND_IMPLEMENTATION_PASS
echo ============================================================
endlocal & exit /b 0
