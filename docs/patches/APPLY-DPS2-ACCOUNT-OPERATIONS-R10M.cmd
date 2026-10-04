@echo off
setlocal EnableExtensions
set "PATCH_DIR=%~dp0files"
if "%~1"=="" (
  set "ROOT=%CD%"
) else (
  set "ROOT=%~1"
)
if not exist "%ROOT%\backend\pom.xml" (
  echo ERROR: Project root not found: %ROOT%
  echo Usage: APPLY-DPS2-ACCOUNT-OPERATIONS-R10M.cmd [project-root]
  exit /b 2
)
echo Applying cumulative DPS2 Account Operations R10M to:
echo   %ROOT%
xcopy /e /i /y "%PATCH_DIR%\*" "%ROOT%\" >nul
if errorlevel 1 (
  echo ERROR: Patch copy failed.
  exit /b 3
)
cd /d "%ROOT%"
node tools\verify-dps2-account-operations-r10l-fk-reference-selectors.mjs
if errorlevel 1 exit /b 4
node tools\verify-dps2-account-operations-r10m-maintenance-delta.mjs
if errorlevel 1 exit /b 5
node tools\verify-dps2-account-operations-r10i-business-input-ux.mjs
if errorlevel 1 exit /b 6
node tools\verify-dps2-account-operations-r10k-angular-duplicate-method-hotfix.mjs
if errorlevel 1 exit /b 7
echo.
echo R10M cumulative patch applied and static verification passed.
echo Next: run build-production.cmd
endlocal
