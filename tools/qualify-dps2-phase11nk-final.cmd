@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
cd /d "%ROOT%" || exit /b 1
if not defined CORE_BANKING_BASE_URL set "CORE_BANKING_BASE_URL=http://127.0.0.1:8091"
if not defined CORE_BANKING_ORACLE_CONNECT (echo ERROR: CORE_BANKING_ORACLE_CONNECT is not set.& exit /b 2)
echo ============================================================
echo DPS2 0.11.0 - Phase 11N-K FINAL Qualification
echo Canonical Steps 00-17 - single heavy qualification pass
echo ============================================================

echo [1/8] Rapid completion static gate...
node tools\verify-dps2-phase11n-steps11-17-rapid-completion.mjs || exit /b 10
node tools\verify-dps2-designer-business-gap-closure-a.mjs || exit /b 11
node tools\verify-dps2-designer-step16-correspondent-account.mjs || exit /b 12

echo [2/8] Canonical Oracle reconciliation Steps 05-10...
call tools\apply-dps2-phase11nd.cmd || exit /b 20

echo [3/8] Wave C/D Oracle regression and Steps 11-17 rapid reconciliation...
call tools\run-oracle-sql.cmd database\oracle\dps2\migrations\0.11.0-phase11l-wave-c-services-access-compliance-pricing-tax.sql || exit /b 21
call tools\run-oracle-sql.cmd database\oracle\dps2\verification\0.11.0-phase11l-wave-c-services-access-compliance-pricing-tax-verifier.sql || exit /b 22
call tools\run-oracle-sql.cmd database\oracle\dps2\migrations\0.11.0-phase11m-wave-d-reconciliation-exceptions-correspondent-rewards.sql || exit /b 23
call tools\run-oracle-sql.cmd database\oracle\dps2\verification\0.11.0-phase11m-wave-d-reconciliation-exceptions-correspondent-rewards-verifier.sql || exit /b 24
call tools\apply-dps2-phase11n-rapid.cmd || exit /b 25

echo [4/8] Stop runtime...
call bin\stop.cmd
if errorlevel 1 exit /b 30

echo [5/8] One production build for final qualification...
call build-production.cmd
if errorlevel 1 exit /b 40

echo [6/8] Start runtime...
start "Core Banking Prototype 0.11.0 Final Qualification" /min /d "%ROOT%" cmd /c "call bin\start.cmd"
set "READY="
for /l %%N in (1,1,120) do (
  powershell -NoProfile -Command "try { $r=Invoke-RestMethod -Uri ($env:CORE_BANKING_BASE_URL.TrimEnd('/') + '/actuator/health') -Method Get -TimeoutSec 2; if($r.status -eq 'UP'){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (set "READY=1"& goto :runtime_ready)
  timeout /t 2 /nobreak >nul
)
:runtime_ready
if not defined READY (echo ERROR: Runtime did not become healthy at %CORE_BANKING_BASE_URL%.& exit /b 50)

echo [7/8] Runtime matrix Steps 00-17...
node tools\runtime-dps2-phase11na-step00-dashboard-e2e.mjs || exit /b 61
node tools\runtime-dps2-phase11nb-steps01-02-e2e.mjs || exit /b 62
node tools\runtime-dps2-phase11nc-steps03-04-e2e.mjs || exit /b 63
node tools\runtime-dps2-phase11i-e2e.mjs || exit /b 64
node tools\runtime-dps2-phase11k-wave-b-e2e.mjs || exit /b 65
node tools\runtime-dps2-phase11nd-steps09-10-e2e.mjs || exit /b 66
node tools\runtime-dps2-phase11e-e2e.mjs || exit /b 67
node tools\runtime-dps2-phase11l-wave-c-e2e.mjs || exit /b 68
node tools\runtime-dps2-phase11m-wave-d-e2e.mjs || exit /b 69
node tools\prepare-dps2-designer-step16-correspondent-products.mjs || exit /b 695
node tools\runtime-dps2-designer-step16-correspondent-account-e2e.mjs || exit /b 70

echo [8/8] Final cross-step qualification complete.
echo ============================================================
echo DPS2_PHASE11NK_STEPS00_17_FINAL_QUALIFICATION_PASS
echo ============================================================
endlocal & exit /b 0
