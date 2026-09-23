@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0.."
cd /d "%ROOT%" || exit /b 1
if "%CORE_BANKING_ORACLE_CONNECT%"=="" (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
set "MIG=%ROOT%\database\oracle\dps2\migrations\0.11.0-phase11c-lifecycle-hold-runtime-readiness.sql"
set "VER=%ROOT%\database\oracle\dps2\verification\0.11.0-phase11c-lifecycle-hold-verifier.sql"
echo ============================================================
echo Core Banking Prototype 0.11.0 - DPS2 Phase 11C
echo Lifecycle + Hold/Block
echo ============================================================
echo [1/3] Static verification...
node "%ROOT%\tools\verify-dps2-lifecycle-hold-11c.mjs" || exit /b 10
echo [2/3] Applying Oracle runtime-readiness DDL...
where sqlplus >nul 2>nul
if not errorlevel 1 (sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%MIG%" || exit /b 21 & goto VERIFY)
where docker >nul 2>nul || (echo ERROR: sqlplus/docker not found.& exit /b 20)
set "C=%CORE_BANKING_ORACLE_CONTAINER%"
if not defined C for /f "tokens=1 delims=|" %%C in ('docker ps --format "{{.Names}}|{{.Image}}" ^| findstr /I "oracle"') do if not defined C set "C=%%C"
if not defined C (echo ERROR: Oracle container not found.& exit /b 20)
echo INFO: Using Oracle Docker container: %C%
docker cp "%MIG%" "%C%:/tmp/phase11c-mig.sql" >nul || exit /b 20
docker cp "%VER%" "%C%:/tmp/phase11c-ver.sql" >nul || exit /b 20
docker exec "%C%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @/tmp/phase11c-mig.sql || exit /b 21
echo [3/3] Verifying Oracle baseline...
docker exec "%C%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @/tmp/phase11c-ver.sql || exit /b 22
docker exec "%C%" rm -f /tmp/phase11c-mig.sql /tmp/phase11c-ver.sql >nul 2>nul
goto DONE
:VERIFY
echo [3/3] Verifying Oracle baseline...
sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%VER%" || exit /b 22
:DONE
echo ============================================================
echo PHASE11C_IMPLEMENTATION_PASS
echo ============================================================
endlocal
