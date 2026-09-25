@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%"
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"

echo ============================================================
echo DPS2 0.11.0 - Phase 11J Activation Hotfix Qualification
echo Existing evidence retained: 11J DB 29/29 + prior runtime controls PASS.
echo ============================================================

echo [1/5] Static verification...
node tools\verify-dps2-wave-a-servicing-lifecycle-11j.mjs
if errorlevel 1 exit /b 1

echo [2/5] Stop runtime...
call bin\stop.cmd
if errorlevel 1 exit /b 1

echo [3/5] Production build...
call build-production.cmd
if errorlevel 1 exit /b 1

echo [4/5] Start runtime...
start "Core Banking Prototype 0.11.0" /min /d "%ROOT%" cmd /c "call bin\start.cmd"
set "READY="
for /l %%N in (1,1,90) do (
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
  exit /b 1
)

echo [5/5] Activation-only Runtime E2E...
node tools\runtime-dps2-phase11j-activation-only.mjs
if errorlevel 1 exit /b 1

echo ============================================================
echo DPS2_PHASE11J_ACTIVATION_HOTFIX_QUALIFICATION_PASS
echo ============================================================
endlocal & exit /b 0
