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
echo Applying DPS2 Account Operations R10N to:
echo   %TARGET%
xcopy /E /I /Y "%PATCH_DIR%files\*" "%TARGET%\" >nul
if errorlevel 1 (
  echo ERROR: patch file copy failed.
  exit /b 1
)
if exist "%TARGET%\APPLY-DPS2-ACCOUNT-OPERATIONS-R10M-HF1.cmd" (
  if not exist "%TARGET%\docs\patches" mkdir "%TARGET%\docs\patches"
  move /Y "%TARGET%\APPLY-DPS2-ACCOUNT-OPERATIONS-R10M-HF1.cmd" "%TARGET%\docs\patches\" >nul
)
pushd "%TARGET%"
node tools\verify-dps2-account-operations-r10n-uat-fixes.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-operations-r10m-maintenance-delta.mjs || (popd & exit /b 1)
node tools\verify-dps2-account-maintenance-r7.mjs || (popd & exit /b 1)
node tools\verify-dps2-closure-reopening-11e.mjs || (popd & exit /b 1)
node tools\verify-dps2-phase11nb-steps01-02-canonical-closure.mjs || (popd & exit /b 1)
popd
echo.
echo R10N patch applied and static verification passed.
echo Next: run build-production.cmd
exit /b 0
