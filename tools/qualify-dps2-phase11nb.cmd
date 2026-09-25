@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)

echo ============================================================
echo DPS2 0.11.0 - Phase 11N-B Qualification
echo Steps 01-02 - Canonical Coverage Closure
echo ============================================================
echo [1/7] Phase 11N-B Oracle qualification...
call tools\apply-dps2-phase11nb.cmd
if errorlevel 1 exit /b 10

echo [2/7] Stop runtime...
call bin\stop.cmd
if errorlevel 1 exit /b 20

echo [3/7] Production build...
call build-production.cmd
if errorlevel 1 exit /b 30

echo [4/7] Start runtime...
start "Core Banking Prototype 0.11.0" /min /d "%ROOT%" cmd /c "call bin\start.cmd"
set "READY="
for /l %%N in (1,1,120) do (
  powershell -NoProfile -Command "try { $r=Invoke-RestMethod -Uri ($env:CORE_BANKING_BASE_URL.TrimEnd('/') + '/actuator/health') -Method Get -TimeoutSec 2; if($r.status -eq 'UP'){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (set "READY=1"& goto :runtime_ready)
  timeout /t 2 /nobreak >nul
)
:runtime_ready
if not defined READY (echo ERROR: Runtime did not become healthy at %CORE_BANKING_BASE_URL%.& exit /b 40)

echo [5/7] Prepare controlled alternate Product Version for qualification...
node tools\prepare-dps2-phase11nb-product-version.mjs
if errorlevel 1 exit /b 45

echo [6/7] Phase 11N-B Runtime E2E...
node tools\runtime-dps2-phase11nb-steps01-02-e2e.mjs
if errorlevel 1 exit /b 50

echo [7/7] Qualification complete.
echo ============================================================
echo DPS2_PHASE11NB_STEPS01_02_QUALIFICATION_PASS
echo ============================================================
endlocal & exit /b 0
