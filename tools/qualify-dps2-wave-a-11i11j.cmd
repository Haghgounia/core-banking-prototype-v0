@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%"

if not defined CORE_BANKING_ORACLE_CONNECT (
  echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.
  exit /b 1
)
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"

echo ============================================================
echo DPS2 0.11.0 - Consolidated Qualification - Phase 11I + 11J
echo ============================================================

echo [1/7] Phase 11I Oracle qualification...
call tools\apply-dps2-phase11i.cmd
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo ERROR: Phase 11I qualification failed with RC=%RC%.
  exit /b %RC%
)

echo [2/7] Phase 11J Oracle qualification...
call tools\apply-dps2-phase11j.cmd
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo ERROR: Phase 11J qualification failed with RC=%RC%.
  exit /b %RC%
)

echo [3/7] Stopping existing runtime...
call bin\stop.cmd
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo ERROR: Runtime stop failed with RC=%RC%.
  exit /b %RC%
)

echo [4/7] Production build...
call build-production.cmd
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo ERROR: Production build failed with RC=%RC%.
  exit /b %RC%
)

echo [5/7] Starting runtime in a separate window...
start "Core Banking Prototype 0.11.0" /min /d "%ROOT%" cmd /c "call bin\start.cmd"

set "READY="
for /l %%N in (1,1,60) do (
  powershell -NoProfile -Command "try { $r=Invoke-WebRequest -UseBasicParsing -Uri '%CORE_BANKING_BASE_URL%/api/v1/deposit-accounts?limit=1' -TimeoutSec 2; if($r.StatusCode -ge 200 -and $r.StatusCode -lt 500){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (
    set "READY=1"
    goto :runtime_ready
  )
  timeout /t 2 /nobreak >nul
)

:runtime_ready
if not defined READY (
  echo ERROR: Runtime did not become reachable at %CORE_BANKING_BASE_URL%.
  echo Review logs\core-banking-prototype.log and the runtime window.
  exit /b 1
)

echo [6/7] Phase 11I Runtime E2E...
node tools\runtime-dps2-phase11i-e2e.mjs
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo ERROR: Phase 11I runtime E2E failed with RC=%RC%.
  exit /b %RC%
)

echo [7/7] Phase 11J Runtime E2E...
node tools\runtime-dps2-phase11j-wave-a-e2e.mjs
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo ERROR: Phase 11J runtime E2E failed with RC=%RC%.
  exit /b %RC%
)

echo ============================================================
echo DPS2_WAVE_A_11I11J_QUALIFICATION_PASS
echo ============================================================
exit /b 0
