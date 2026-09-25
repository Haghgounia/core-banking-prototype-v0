@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0.."
cd /d "%ROOT%" || exit /b 1
if "%CORE_BANKING_ORACLE_CONNECT%"=="" (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
set "MIG=%ROOT%\database\oracle\dps2\migrations\0.11.0-phase11l-wave-c-services-access-compliance-pricing-tax.sql"
set "VER=%ROOT%\database\oracle\dps2\verification\0.11.0-phase11l-wave-c-services-access-compliance-pricing-tax-verifier.sql"
echo ============================================================
echo Core Banking Prototype 0.11.0 - DPS2 Phase 11L
echo Wave C - Operational Steps 09-13
echo ============================================================
echo [1/3] Static verification...
node "%ROOT%\tools\verify-dps2-wave-c-services-access-compliance-pricing-tax-11l.mjs" || exit /b 10
echo [2/3] Applying Oracle sequence reconciliation...
where sqlplus >nul 2>nul
if not errorlevel 1 (sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%MIG%" || exit /b 21 & goto VERIFY)
where docker >nul 2>nul || (echo ERROR: sqlplus/docker not found.& exit /b 20)
set "C=%CORE_BANKING_ORACLE_CONTAINER%"
if not defined C for /f "tokens=1 delims=|" %%C in ('docker ps --format "{{.Names}}|{{.Image}}" ^| findstr /I "oracle"') do if not defined C set "C=%%C"
if not defined C (echo ERROR: Oracle container not found.& exit /b 20)
echo INFO: Using Oracle Docker container: %C%
docker cp "%MIG%" "%C%:/tmp/phase11l-mig.sql" >nul || exit /b 20
docker cp "%VER%" "%C%:/tmp/phase11l-ver.sql" >nul || exit /b 20
docker exec "%C%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @/tmp/phase11l-mig.sql || exit /b 21
echo [3/3] Verifying Oracle baseline...
docker exec "%C%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @/tmp/phase11l-ver.sql || exit /b 22
docker exec "%C%" rm -f /tmp/phase11l-mig.sql /tmp/phase11l-ver.sql >nul 2>nul
goto DONE
:VERIFY
echo [3/3] Verifying Oracle baseline...
sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%VER%" || exit /b 22
:DONE
echo ============================================================
echo PHASE11L_IMPLEMENTATION_PASS
echo ============================================================
endlocal & exit /b 0
