@echo off
setlocal EnableExtensions
set "PATCH_DIR=%~dp0"
set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=%CD%"

if not exist "%TARGET%\build-production.cmd" (
  echo ERROR: target does not look like the Core Banking Prototype root:
  echo   %TARGET%
  echo Usage:
  echo   APPLY-DPS2-ACCOUNT-OPERATIONS-R10M-HF1.cmd D:\Projects\core-banking-prototype-v0
  exit /b 1
)

set "VERIFIER=%TARGET%\tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs"
if not exist "%VERIFIER%" (
  echo ERROR: Phase 11N-B verifier was not found:
  echo   %VERIFIER%
  exit /b 1
)

findstr /C:"UI editor changes current org rather than opening snapshot" "%VERIFIER%" >nul 2>&1
if errorlevel 1 (
  findstr /C:"UI keeps current owning unit read-only in maintenance and preserves canonical current org" "%VERIFIER%" >nul 2>&1
  if errorlevel 1 (
    echo ERROR: verifier content is not the expected R10M/R10M-HF1 baseline.
    exit /b 1
  )
)

copy /Y "%PATCH_DIR%files\tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs" "%VERIFIER%" >nul || exit /b 1
if not exist "%TARGET%\docs\patches" mkdir "%TARGET%\docs\patches"
copy /Y "%PATCH_DIR%files\docs\patches\DPS2-0.11.0-ACCOUNT-OPERATIONS-R10M-HF1-PHASE11NB-VERIFIER-COMPAT-FA.md" "%TARGET%\docs\patches\" >nul || exit /b 1

echo Applying DPS2 Account Operations R10M-HF1 verifier compatibility to:
echo   %TARGET%
pushd "%TARGET%"
node tools\verify-dps2-account-operations-r10m-maintenance-delta.mjs || (popd & exit /b 1)
node tools\verify-dps2-wave-a-servicing-lifecycle-11j.mjs || (popd & exit /b 1)
node tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs || (popd & exit /b 1)
node tools\verify-dps2-phase11nc-steps03-04-canonical-closure.mjs || (popd & exit /b 1)
node tools\verify-dps2-phase11nd-steps05-10-canonical-audit.mjs || (popd & exit /b 1)
node tools\verify-dps2-phase11n-steps11-17-rapid-completion.mjs || (popd & exit /b 1)
popd

echo.
echo R10M-HF1 verifier compatibility patch applied successfully.
echo No UI, Backend or DDL files were changed.
echo Next: run build-production.cmd
exit /b 0
