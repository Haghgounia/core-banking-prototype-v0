@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "TARGET=%~1"
if not defined TARGET for %%I in ("%~dp0..\..\..") do set "TARGET=%%~fI"
echo CLT_R6K_CMD_ENTRY; TARGET=%TARGET%
if not exist "%~dp0..\r6k\run-local-uat.cjs" (
  echo CLT_R6K_LAUNCHER_MISSING
  endlocal & exit /b 31
)
node "%~dp0..\r6k\run-local-uat.cjs" "%TARGET%"
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" echo CLT_R6K_START_INCOMPLETE; EXIT=%RC%
endlocal & exit /b %RC%
