@echo off
setlocal EnableExtensions
set "PATCH_DIR=%~dp0"
if "%~1"=="" (
  set "TARGET=%CD%"
) else (
  set "TARGET=%~1"
)
if not exist "%TARGET%\build-production.cmd" (
  echo ERROR: target project root not found: %TARGET%
  exit /b 1
)
echo Applying cumulative DPS2 Account Operations R10N+R10O to:
echo   %TARGET%
xcopy /E /I /Y "%PATCH_DIR%files\*" "%TARGET%\" >nul
if errorlevel 1 (
  echo ERROR: patch file copy failed.
  exit /b 1
)
if not exist "%TARGET%\docs\patches" mkdir "%TARGET%\docs\patches"
if exist "%TARGET%\APPLY-DPS2-ACCOUNT-OPERATIONS-R10M-HF1.cmd" move /Y "%TARGET%\APPLY-DPS2-ACCOUNT-OPERATIONS-R10M-HF1.cmd" "%TARGET%\docs\patches\" >nul
if exist "%TARGET%\APPLY-DPS2-ACCOUNT-OPERATIONS-R10N.cmd" move /Y "%TARGET%\APPLY-DPS2-ACCOUNT-OPERATIONS-R10N.cmd" "%TARGET%\docs\patches\" >nul
pushd "%TARGET%"
node tools\verify-dps2-account-operations-r10o-owner-product-flow.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10n-uat-fixes.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-maintenance-r7.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10m-maintenance-delta.mjs || (popd & exit /b 1)
node tools\verify-dps2-wave-a-servicing-lifecycle-11j.mjs || (popd & exit /b 1)
node tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs || (popd & exit /b 1)
popd
echo.
echo R10O cumulative patch applied and static verification passed.
echo Next: run build-production.cmd
exit /b 0
