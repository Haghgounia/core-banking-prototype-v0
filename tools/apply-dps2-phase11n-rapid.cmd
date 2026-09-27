@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
echo ============================================================
echo DPS2 0.11.0 - Phase 11N Rapid Completion Steps 11-17
echo ============================================================
node "%ROOT%\tools\verify-dps2-phase11n-steps11-17-rapid-completion.mjs" || exit /b 10
call "%ROOT%\tools\run-oracle-sql.cmd" "%ROOT%\database\oracle\dps2\migrations\0.11.0-phase11n-steps11-17-rapid-completion.sql" || exit /b 20
call "%ROOT%\tools\run-oracle-sql.cmd" "%ROOT%\database\oracle\dps2\verification\0.11.0-phase11n-steps11-17-rapid-completion-verifier.sql" || exit /b 21
echo PHASE11N_RAPID_IMPLEMENTATION_PASS
endlocal & exit /b 0
