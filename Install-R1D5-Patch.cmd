@echo off
setlocal
if "%~1"=="" (
  echo USAGE: Install-R1D5-Patch.cmd "D:\Projects\core-banking-prototype-v0" -Approve
  echo No files changed.
  exit /b 2
)
if /I not "%~2"=="-Approve" (
  echo EXPLICIT_APPROVAL_REQUIRED: second argument must be -Approve
  exit /b 2
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install-R1D5-Patch.ps1" -ProjectRoot "%~1" -Approve
set "exitCode=%ERRORLEVEL%"
echo R1D5_INSTALL_EXIT_CODE=%exitCode%
exit /b %exitCode%
