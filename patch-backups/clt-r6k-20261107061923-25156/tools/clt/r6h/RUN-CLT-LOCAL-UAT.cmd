@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "TARGET=%~1"
if not defined TARGET set "TARGET=D:\Projects\core-banking-prototype-v0"
if not exist "%~dp0RUN-CLT-LOCAL-UAT.ps1" (
  echo CLT_R6H_R2_RUNTIME_PS1_MISSING
  exit /b 31
)
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0RUN-CLT-LOCAL-UAT.ps1" -TargetRoot "%TARGET%"
exit /b %ERRORLEVEL%
