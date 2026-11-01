@echo off
setlocal
cd /d "%~dp0..\.."
if not exist "frontend\src\app\features\fee3\fee3-workspace.component.html" (
  echo FEE3_R5_INVALID_PROJECT_ROOT
  exit /b 2
)
python "tools\fee3-stage6-r5\apply.py"
if errorlevel 1 exit /b 1
echo FEE3_R5_APPLY_PASS
exit /b 0
