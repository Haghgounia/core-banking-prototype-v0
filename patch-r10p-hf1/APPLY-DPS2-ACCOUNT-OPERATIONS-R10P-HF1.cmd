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
echo Applying cumulative DPS2 Account Operations R10N+R10O+R10P-HF1 to:
echo   %TARGET%
xcopy /E /I /Y "%PATCH_DIR%files\*" "%TARGET%\" >nul
if errorlevel 1 (
  echo ERROR: patch file copy failed.
  exit /b 1
)
if not exist "%TARGET%\docs\patches" mkdir "%TARGET%\docs\patches"
for %%F in (
  APPLY-DPS2-ACCOUNT-OPERATIONS-R10M.cmd
  APPLY-DPS2-ACCOUNT-OPERATIONS-R10M-HF1.cmd
  APPLY-DPS2-ACCOUNT-OPERATIONS-R10N.cmd
  APPLY-DPS2-ACCOUNT-OPERATIONS-R10O.cmd
  APPLY-DPS2-ACCOUNT-OPERATIONS-R10P.cmd
) do (
  if exist "%TARGET%\%%F" move /Y "%TARGET%\%%F" "%TARGET%\docs\patches\" >nul
)
pushd "%TARGET%"
node tools\verify-dps2-account-operations-r10p-workspace-navigation.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-lifecycle-r8.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10o-owner-product-flow.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10n-uat-fixes.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10m-maintenance-delta.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10l-fk-reference-selectors.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10i-business-input-ux.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-maintenance-r7.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10g-regression-compat.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10h-designer-compat.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-main-parity-r9.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-ui-alignment.mjs || (popd & exit /b 1)
node tools\verify-dps2-wave-a-servicing-lifecycle-11j.mjs || (popd & exit /b 1)
node tools\verify-dps2-closure-reopening-11e.mjs || (popd & exit /b 1)
node tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs || (popd & exit /b 1)
node tools\verify-release-layout.mjs || (popd & exit /b 1)
popd
echo.
echo R10P-HF1 cumulative patch applied and static/regression verification passed.
echo Next: run build-production.cmd
exit /b 0
