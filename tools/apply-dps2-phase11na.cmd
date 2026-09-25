@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
set "VER=%ROOT%\database\oracle\dps2\verification\0.11.0-phase11na-step00-dashboard-verifier.sql"
echo ============================================================
echo Core Banking Prototype 0.11.0 - DPS2 Phase 11N-A
echo Step 00 - Read-only Operational Dashboard
echo ============================================================
echo [1/2] Static verification...
node "%ROOT%\tools\verify-dps2-phase11na-step00-dashboard.mjs" || exit /b 10
echo [2/2] Verifying Oracle read-model sources...
where sqlplus >nul 2>nul
if not errorlevel 1 (sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%VER%" || exit /b 22 & goto DONE)
where docker >nul 2>nul || (echo ERROR: sqlplus/docker not found.& exit /b 20)
set "C=%CORE_BANKING_ORACLE_CONTAINER%"
if not defined C for /f "tokens=1 delims=|" %%C in ('docker ps --format "{{.Names}}|{{.Image}}" ^| findstr /I "oracle"') do if not defined C set "C=%%C"
if not defined C (echo ERROR: Oracle container not found.& exit /b 20)
echo INFO: Using Oracle Docker container: %C%
docker cp "%VER%" "%C%:/tmp/phase11na-ver.sql" >nul || exit /b 20
docker exec "%C%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @/tmp/phase11na-ver.sql || exit /b 22
docker exec "%C%" rm -f /tmp/phase11na-ver.sql >nul 2>nul
:DONE
echo ============================================================
echo PHASE11NA_IMPLEMENTATION_PASS
echo ============================================================
endlocal & exit /b 0
