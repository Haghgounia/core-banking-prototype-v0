@echo off
setlocal EnableExtensions
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
echo ============================================================
echo DPS2 0.11.0 - Designer Business Gap Closure B
echo Step 16 - Correspondent Account Master + Extension + Profile
echo ============================================================
node tools\verify-dps2-designer-step16-correspondent-account.mjs || exit /b 10
call build-production.cmd || exit /b 20
call bin\stop.cmd
start "Core Banking Prototype 0.11.0 Step16 Qualification" /min /d "%ROOT%" cmd /c "call bin\start.cmd"
set "READY="
for /l %%N in (1,1,120) do (
  powershell -NoProfile -Command "try { $r=Invoke-RestMethod -Uri ($env:CORE_BANKING_BASE_URL.TrimEnd('/') + '/actuator/health') -Method Get -TimeoutSec 2; if($r.status -eq 'UP'){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (set "READY=1"& goto :runtime_ready)
  timeout /t 2 /nobreak >nul
)
:runtime_ready
if not defined READY (echo ERROR: Runtime did not become healthy.& exit /b 30)
node tools\prepare-dps2-designer-step16-correspondent-products.mjs || exit /b 35
node tools\runtime-dps2-designer-step16-correspondent-account-e2e.mjs || exit /b 40
echo ============================================================
echo DPS2_DESIGNER_STEP16_QUALIFICATION_PASS
echo ============================================================
endlocal & exit /b 0
