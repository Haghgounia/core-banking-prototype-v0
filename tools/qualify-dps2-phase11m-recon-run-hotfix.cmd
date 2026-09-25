@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"

echo ============================================================
echo DPS2 0.11.0 - Phase 11M Reconciliation Run Hotfix
echo Existing evidence retained: 11M Oracle DB 40/40 PASS.
echo ============================================================

echo [1/5] Phase 11M static verification...
node tools\verify-dps2-wave-d-reconciliation-exceptions-correspondent-rewards-11m.mjs
if errorlevel 1 exit /b 10

echo [2/5] Stop runtime...
call bin\stop.cmd
if errorlevel 1 exit /b 20

echo [3/5] Production build...
call build-production.cmd
if errorlevel 1 exit /b 30

echo [4/5] Start runtime...
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

echo [5/5] Phase 11M Runtime E2E...
node tools\runtime-dps2-phase11m-wave-d-e2e.mjs
if errorlevel 1 exit /b 50

echo ============================================================
echo DPS2_PHASE11M_RECON_RUN_HOTFIX_QUALIFICATION_PASS
echo ============================================================
endlocal & exit /b 0
