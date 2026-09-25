@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"
if not defined CORE_BANKING_ORACLE_CONNECT (
  echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.
  exit /b 2
)

echo ============================================================
echo DPS2 0.11.0 - Wave B Phase 11K Qualification
echo Steps 06-08: Statements / Limits / Maturity Batch
echo ============================================================

echo [1/6] Phase 11K Oracle qualification...
call tools\apply-dps2-phase11k.cmd
if errorlevel 1 exit /b 10

echo [2/6] Stop runtime...
call bin\stop.cmd
if errorlevel 1 exit /b 20

echo [3/6] Production build...
call build-production.cmd
if errorlevel 1 exit /b 30

echo [4/6] Start runtime...
start "Core Banking Prototype 0.11.0" /min /d "%ROOT%" cmd /c "call bin\start.cmd"
set "READY="
for /l %%N in (1,1,120) do (
  powershell -NoProfile -Command "try { $r=Invoke-RestMethod -Uri ($env:CORE_BANKING_BASE_URL.TrimEnd('/') + '/actuator/health') -Method Get -TimeoutSec 2; if($r.status -eq 'UP'){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (
    set "READY=1"
    goto :runtime_ready
  )
  timeout /t 2 /nobreak >nul
)
:runtime_ready
if not defined READY (
  echo ERROR: Runtime did not become healthy at %CORE_BANKING_BASE_URL%.
  echo Review logs\core-banking-prototype.log and the runtime window.
  exit /b 40
)

echo [5/6] Phase 11K Runtime E2E...
node tools\runtime-dps2-phase11k-wave-b-e2e.mjs
if errorlevel 1 exit /b 50

echo [6/6] Qualification complete.
echo ============================================================
echo DPS2_WAVE_B_11K_QUALIFICATION_PASS
echo ============================================================
endlocal & exit /b 0
