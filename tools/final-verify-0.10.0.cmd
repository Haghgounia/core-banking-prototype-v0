@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0.."
cd /d "%ROOT%" || exit /b 1

if "%CORE_BANKING_BASE_URL%"=="" set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"

if "%CORE_BANKING_ORACLE_CONNECT%"=="" (
  echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.
  echo Example:
  echo   set CORE_BANKING_ORACLE_CONNECT=DPS2_APP/YourPassword@//localhost:1521/FREEPDB1
  echo Use an Oracle account that can read DPS2, PDL and CIF metadata/data.
  exit /b 2
)

set "DB_VERIFIER_SQL=%ROOT%\database\oracle\dps2\verification\0.10.0-final-verifier.sql"
set "DB_VERIFIER_CONTAINER_PATH=/tmp/core-banking-0.10.0-final-verifier.sql"

echo ============================================================
echo Core Banking Prototype 0.10.0 - FINAL RELEASE VERIFIER
echo ============================================================

echo.
echo [1/3] Source / static / runtime-artifact baseline...
node "%ROOT%\tools\verify-final-closure-0.10.0.mjs" --require-runtime || exit /b 10

echo.
echo [2/3] Oracle DB / reference / constraint baseline...
where sqlplus >nul 2>nul
if not errorlevel 1 (
  echo INFO: Using local sqlplus from PATH.
  sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" @"%DB_VERIFIER_SQL%"
  if errorlevel 1 exit /b 21
  goto DB_VERIFIED
)

echo INFO: Local sqlplus was not found in PATH. Trying Oracle Docker container fallback...
where docker >nul 2>nul
if errorlevel 1 (
  echo ERROR: Neither local sqlplus nor docker was found in PATH.
  echo Install/add Oracle SQL*Plus to PATH or make Docker Desktop available.
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
  echo Running containers:
  docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}"
  exit /b 20
)

echo INFO: Using Oracle Docker container: %ORACLE_CONTAINER%
docker cp "%DB_VERIFIER_SQL%" "%ORACLE_CONTAINER%:%DB_VERIFIER_CONTAINER_PATH%" >nul
if errorlevel 1 (
  echo ERROR: Could not copy the DB verifier SQL into container %ORACLE_CONTAINER%.
  exit /b 20
)

docker exec "%ORACLE_CONTAINER%" sh -lc "command -v sqlplus >/dev/null 2>&1"
if errorlevel 1 (
  echo ERROR: sqlplus was not found inside Oracle container %ORACLE_CONTAINER%.
  docker exec "%ORACLE_CONTAINER%" rm -f "%DB_VERIFIER_CONTAINER_PATH%" >nul 2>nul
  exit /b 20
)

docker exec "%ORACLE_CONTAINER%" sqlplus -L -S "%CORE_BANKING_ORACLE_CONNECT%" "@%DB_VERIFIER_CONTAINER_PATH%"
set "DB_VERIFY_RC=!ERRORLEVEL!"
docker exec "%ORACLE_CONTAINER%" rm -f "%DB_VERIFIER_CONTAINER_PATH%" >nul 2>nul
if not "!DB_VERIFY_RC!"=="0" exit /b 21

:DB_VERIFIED
echo.
echo [3/3] Runtime health...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $r=Invoke-RestMethod -Uri ($env:CORE_BANKING_BASE_URL.TrimEnd('/') + '/actuator/health') -Method Get; if($r.status -ne 'UP'){throw ('Runtime health is ' + $r.status)}; Write-Host 'FINAL_RUNTIME_HEALTH_PASS'"
if errorlevel 1 exit /b 30

echo.
echo ============================================================
echo FINAL_RELEASE_CLOSURE_PASS
echo ============================================================
endlocal
