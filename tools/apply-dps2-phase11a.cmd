@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0.."
cd /d "%ROOT%" || exit /b 1

if "%CORE_BANKING_ORACLE_CONNECT%"=="" (
  echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.
  echo Example:
  echo   set CORE_BANKING_ORACLE_CONNECT=SYSTEM/YourPassword@//localhost:1521/FREEPDB1
  exit /b 2
)

set "MIGRATION_SQL=%ROOT%\database\oracle\dps2\migrations\0.11.0-phase11a-account-operations-schema-reconciliation.sql"
set "VERIFIER_SQL=%ROOT%\database\oracle\dps2\verification\0.11.0-phase11a-account-operations-schema-verifier.sql"
set "MIGRATION_CONTAINER=/tmp/core-banking-0.11.0-phase11a-migration.sql"
set "VERIFIER_CONTAINER=/tmp/core-banking-0.11.0-phase11a-verifier.sql"

if not exist "%MIGRATION_SQL%" (
  echo ERROR: Phase 11A migration SQL was not found.
  exit /b 3
)
if not exist "%VERIFIER_SQL%" (
  echo ERROR: Phase 11A DB verifier SQL was not found.
  exit /b 3
)

echo ============================================================
echo Core Banking Prototype 0.11.0 - DPS2 Phase 11A
echo Account Operations Schema Reconciliation
echo ============================================================

echo.
echo [1/3] Static contract verification...
node "%ROOT%\tools\verify-dps2-account-operations-schema-reconciliation-11a.mjs" || exit /b 10

echo.
echo [2/3] Applying Oracle migration...
where sqlplus >nul 2>nul
if not errorlevel 1 (
  echo INFO: Using local sqlplus from PATH.
  sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%MIGRATION_SQL%"
  if errorlevel 1 exit /b 21
  echo.
  echo [3/3] Verifying Oracle baseline...
  sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%VERIFIER_SQL%"
  if errorlevel 1 exit /b 22
  goto PHASE11A_DONE
)

echo INFO: Local sqlplus was not found in PATH. Trying Oracle Docker container fallback...
where docker >nul 2>nul
if errorlevel 1 (
  echo ERROR: Neither local sqlplus nor docker was found in PATH.
  exit /b 20
)

set "ORACLE_CONTAINER=%CORE_BANKING_ORACLE_CONTAINER%"
if not defined ORACLE_CONTAINER (
  for /f "tokens=1 delims=|" %%C in ('docker ps --format "{{.Names}}|{{.Image}}" ^| findstr /I "oracle"') do (
    if not defined ORACLE_CONTAINER set "ORACLE_CONTAINER=%%C"
  )
)

if not defined ORACLE_CONTAINER (
  echo ERROR: No running Oracle Docker container could be auto-detected.
  echo Set it explicitly, for example:
  echo   set CORE_BANKING_ORACLE_CONTAINER=oracle-free
  docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}"
  exit /b 20
)

echo INFO: Using Oracle Docker container: %ORACLE_CONTAINER%
docker cp "%MIGRATION_SQL%" "%ORACLE_CONTAINER%:%MIGRATION_CONTAINER%" >nul || exit /b 20
docker cp "%VERIFIER_SQL%" "%ORACLE_CONTAINER%:%VERIFIER_CONTAINER%" >nul || exit /b 20

docker exec "%ORACLE_CONTAINER%" sh -lc "command -v sqlplus >/dev/null 2>&1"
if errorlevel 1 (
  echo ERROR: sqlplus was not found inside Oracle container %ORACLE_CONTAINER%.
  goto DOCKER_CLEANUP_ERROR
)

docker exec "%ORACLE_CONTAINER%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" "@%MIGRATION_CONTAINER%"
set "MIGRATION_RC=!ERRORLEVEL!"
if not "!MIGRATION_RC!"=="0" goto DOCKER_CLEANUP_MIGRATION_ERROR

echo.
echo [3/3] Verifying Oracle baseline...
docker exec "%ORACLE_CONTAINER%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" "@%VERIFIER_CONTAINER%"
set "VERIFY_RC=!ERRORLEVEL!"
docker exec "%ORACLE_CONTAINER%" rm -f "%MIGRATION_CONTAINER%" "%VERIFIER_CONTAINER%" >nul 2>nul
if not "!VERIFY_RC!"=="0" exit /b 22
goto PHASE11A_DONE

:DOCKER_CLEANUP_MIGRATION_ERROR
docker exec "%ORACLE_CONTAINER%" rm -f "%MIGRATION_CONTAINER%" "%VERIFIER_CONTAINER%" >nul 2>nul
exit /b 21

:DOCKER_CLEANUP_ERROR
docker exec "%ORACLE_CONTAINER%" rm -f "%MIGRATION_CONTAINER%" "%VERIFIER_CONTAINER%" >nul 2>nul
exit /b 20

:PHASE11A_DONE
echo.
echo ============================================================
echo PHASE11A_IMPLEMENTATION_PASS
echo ============================================================
endlocal
