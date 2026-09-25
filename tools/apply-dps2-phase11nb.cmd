@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
set "MIG=%ROOT%\database\oracle\dps2\migrations\0.11.0-phase11nb-steps01-02-canonical-closure.sql"
set "VER=%ROOT%\database\oracle\dps2\verification\0.11.0-phase11nb-steps01-02-canonical-closure-verifier.sql"
echo ============================================================
echo Core Banking Prototype 0.11.0 - DPS2 Phase 11N-B
echo Steps 01-02 - Canonical Coverage Closure
echo ============================================================
echo [1/3] Static verification...
node "%ROOT%\tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs" || exit /b 10

echo [2/3] Applying Oracle canonical reconciliation...
where sqlplus >nul 2>nul
if not errorlevel 1 (
  sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%MIG%" || exit /b 21
  sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%VER%" || exit /b 22
  goto DONE
)
where docker >nul 2>nul || (echo ERROR: sqlplus/docker not found.& exit /b 20)
set "C=%CORE_BANKING_ORACLE_CONTAINER%"
if not defined C for /f "tokens=1 delims=|" %%C in ('docker ps --format "{{.Names}}|{{.Image}}" ^| findstr /I "oracle"') do if not defined C set "C=%%C"
if not defined C (echo ERROR: Oracle container not found.& exit /b 20)
echo INFO: Using Oracle Docker container: %C%
docker cp "%MIG%" "%C%:/tmp/phase11nb-mig.sql" >nul || exit /b 20
docker cp "%VER%" "%C%:/tmp/phase11nb-ver.sql" >nul || exit /b 20
docker exec "%C%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @/tmp/phase11nb-mig.sql || exit /b 21
docker exec "%C%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @/tmp/phase11nb-ver.sql || exit /b 22
docker exec "%C%" rm -f /tmp/phase11nb-mig.sql /tmp/phase11nb-ver.sql >nul 2>nul
:DONE
echo [3/3] Oracle gate complete.
echo ============================================================
echo PHASE11NB_IMPLEMENTATION_PASS
echo ============================================================
endlocal & exit /b 0
