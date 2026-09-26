@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
echo ============================================================
echo DPS2 0.11.0 - Phase 11N-D Qualification R2
echo Steps 05-10 ONLY - Canonical Regression / Audit
echo ============================================================

echo [1/9] Step 05 static + Oracle regression...
node tools\verify-dps2-step05-transaction-processing-11i.mjs || exit /b 11
call tools\run-oracle-sql.cmd database\oracle\dps2\migrations\0.11.0-phase11i-step05-transaction-processing.sql || exit /b 12
call tools\run-oracle-sql.cmd database\oracle\dps2\verification\0.11.0-phase11i-step05-transaction-processing-verifier.sql || exit /b 13
echo PHASE11I_IMPLEMENTATION_PASS

echo [2/9] Steps 06-08 static + Oracle regression...
node tools\verify-dps2-wave-b-statements-limits-maturity-11k.mjs || exit /b 14
call tools\run-oracle-sql.cmd database\oracle\dps2\migrations\0.11.0-phase11k-wave-b-statements-limits-maturity.sql || exit /b 15
call tools\run-oracle-sql.cmd database\oracle\dps2\verification\0.11.0-phase11k-wave-b-statements-limits-maturity-verifier.sql || exit /b 16
echo PHASE11K_IMPLEMENTATION_PASS

echo [3/9] Scoped 11N-D Oracle audit for Steps 05-10 only...
call tools\apply-dps2-phase11nd.cmd || exit /b 17

echo [4/9] Stop runtime...
call bin\stop.cmd
if errorlevel 1 exit /b 20

echo [5/9] Production build...
call build-production.cmd
if errorlevel 1 exit /b 30

echo [6/9] Start runtime...
start "Core Banking Prototype 0.11.0" /min /d "%ROOT%" cmd /c "call bin\start.cmd"
set "READY="
for /l %%N in (1,1,120) do (
  powershell -NoProfile -Command "try { $r=Invoke-RestMethod -Uri ($env:CORE_BANKING_BASE_URL.TrimEnd('/') + '/actuator/health') -Method Get -TimeoutSec 2; if($r.status -eq 'UP'){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (set "READY=1"& goto :runtime_ready)
  timeout /t 2 /nobreak >nul
)
:runtime_ready
if not defined READY (echo ERROR: Runtime did not become healthy at %CORE_BANKING_BASE_URL%.& exit /b 40)

echo [7/9] Runtime regression Steps 05-08...
node tools\runtime-dps2-phase11i-e2e.mjs || exit /b 51
node tools\runtime-dps2-phase11k-wave-b-e2e.mjs || exit /b 52

echo [8/9] Runtime audit Steps 09-10 and controlled Step08 closure...
node tools\runtime-dps2-phase11nd-steps09-10-e2e.mjs || exit /b 53
node tools\runtime-dps2-phase11e-e2e.mjs || exit /b 54

echo [9/9] Qualification complete.
echo ============================================================
echo DPS2_PHASE11ND_STEPS05_10_QUALIFICATION_PASS
echo ============================================================
endlocal & exit /b 0
