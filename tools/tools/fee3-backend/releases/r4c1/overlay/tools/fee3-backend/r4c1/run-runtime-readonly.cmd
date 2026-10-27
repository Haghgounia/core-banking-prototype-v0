@echo off
setlocal
cd /d "%~dp0\..\..\.."
python tools\fee3-backend\r4c1\runtime-readonly.py
exit /b %errorlevel%
